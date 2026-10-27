// Scheduling classes (spec 005). Every write runs in one transaction.
import { getGroup } from '@/data/repos/groupRepo';
import * as repo from '@/data/repos/scheduleRepo';
import { getStudent } from '@/data/repos/studentRepo';
import type { Cents } from '@/domain/money';
import {
  addDays,
  HORIZON_DAYS,
  localToUtc,
  occurrences,
  utcToLocal,
  weekdayOf,
  type LocalDate,
  type LocalTime,
  type Repeat,
} from '@/domain/recurrence';

import type { Ctx as CatalogCtx, Result } from './catalog';

/** The device time zone comes in with the clock, so tests can freeze both. */
export type Ctx = CatalogCtx & { zone: string };

export type LessonDraft = Readonly<{
  who: { studentId: string } | { groupId: string };
  placeId: string;
  date: LocalDate;
  time: LocalTime;
  plannedMin: number;
  valueCents: Cents | null;
  travelFeeCents: Cents | null;
  repeat: Repeat | null;
  notes?: string | null;
}>;

export type ScheduleError = 'notFound' | 'placeNotLinked' | 'started';

const fail = <E extends string>(error: E) => ({ ok: false, error }) as const;
const today = (ctx: Ctx) => utcToLocal(ctx.now, ctx.zone).date;
const horizon = (ctx: Ctx) => addDays(today(ctx), HORIZON_DAYS);

/** Fills one series from where it stopped up to `until`; existing dates are left alone. */
function fillRule(db: Ctx['db'], ctx: Ctx, rule: repo.RuleRow, until: LocalDate) {
  if (rule.generatedUntil >= until) return;
  const from = addDays(rule.generatedUntil, 1);
  const dates = occurrences(
    {
      startsOn: rule.startsOn,
      endsOn: rule.endsOn,
      repeat: rule.repeat,
      skips: repo.skipsOf(db, rule.id),
    },
    from > rule.startsOn ? from : rule.startsOn,
    until,
  );
  repo.insertLessons(
    db,
    dates.map((date) => ({
      id: ctx.newId(),
      ruleId: rule.id,
      occurrenceDate: date,
      studentId: rule.studentId,
      groupId: rule.groupId,
      placeId: rule.placeId,
      startAt: localToUtc(date, rule.startTime, rule.timeZone),
      plannedMin: rule.durationMin,
      valueCents: rule.valueCents,
      travelFeeCents: rule.travelFeeCents,
    })),
    ctx.now,
  );
  repo.updateRule(db, rule.id, { generatedUntil: until }, ctx.now);
}

function placeAllowed(db: Ctx['db'], who: LessonDraft['who'], placeId: string): boolean {
  if ('groupId' in who) return getGroup(db, who.groupId)?.placeId === placeId;
  return !!getStudent(db, who.studentId)?.links.some((l) => l.placeId === placeId);
}

function newRule(ctx: Ctx, d: LessonDraft & { repeat: Repeat }, id: string): repo.RuleRow {
  return {
    id,
    studentId: 'studentId' in d.who ? d.who.studentId : null,
    groupId: 'groupId' in d.who ? d.who.groupId : null,
    placeId: d.placeId,
    weekday: weekdayOf(d.date),
    startTime: d.time,
    durationMin: d.plannedMin,
    repeat: d.repeat,
    startsOn: d.date,
    endsOn: null,
    timeZone: ctx.zone,
    valueCents: d.valueCents,
    travelFeeCents: d.travelFeeCents,
    generatedUntil: addDays(d.date, -1),
    createdAt: ctx.now,
    updatedAt: ctx.now,
  };
}

/**
 * Agendar: a one-off class, or a series filled 8 weeks ahead. Only the student's linked
 * places, or the group's own place, are allowed (AC-005-2).
 */
export function scheduleLesson(
  ctx: Ctx,
  draft: LessonDraft,
): Result<{ lessonId: string }, ScheduleError> {
  return ctx.db.transaction((tx) => {
    if (!placeAllowed(tx, draft.who, draft.placeId)) return fail('placeNotLinked' as const);
    if (draft.repeat) {
      const rule = newRule(ctx, { ...draft, repeat: draft.repeat }, ctx.newId());
      repo.insertRule(tx, rule, ctx.now);
      fillRule(tx, ctx, rule, horizon(ctx) > draft.date ? horizon(ctx) : draft.date);
      const first = repo.lessonAt(tx, rule.id, draft.date)!;
      if (draft.notes) repo.updateLessonRow(tx, first.id, { notes: draft.notes }, ctx.now);
      return { ok: true, value: { lessonId: first.id } };
    }
    const id = ctx.newId();
    repo.insertLessons(
      tx,
      [
        {
          id,
          studentId: 'studentId' in draft.who ? draft.who.studentId : null,
          groupId: 'groupId' in draft.who ? draft.who.groupId : null,
          placeId: draft.placeId,
          startAt: localToUtc(draft.date, draft.time, ctx.zone),
          plannedMin: draft.plannedMin,
          valueCents: draft.valueCents,
          travelFeeCents: draft.travelFeeCents,
          notes: draft.notes ?? null,
        },
      ],
      ctx.now,
    );
    return { ok: true, value: { lessonId: id } };
  });
}

/** Keeps every running series filled 8 weeks from today. Safe to run any number of times. */
export function fillSchedule(ctx: Ctx): void {
  ctx.db.transaction((tx) => {
    for (const rule of repo.activeRules(tx, today(ctx))) fillRule(tx, ctx, rule, horizon(ctx));
  });
}

export type LessonChange = Partial<
  Pick<LessonDraft, 'date' | 'time' | 'plannedMin' | 'valueCents' | 'travelFeeCents' | 'notes'>
>;
export type Scope = 'one' | 'following';

/**
 * Editar aula. "Só esta aula" changes this class and detaches it from its series.
 * "Esta e as próximas" ends the series the day before and starts a new one from this class
 * with the change; later classes changed on their own keep their changes. A class that
 * already started never changes (AC-005-15).
 */
export function editLesson(
  ctx: Ctx,
  id: string,
  change: LessonChange,
  scope: Scope,
): Result<{ lessonId: string }, ScheduleError> {
  return ctx.db.transaction((tx) => {
    const row = repo.getLessonRow(tx, id);
    if (!row) return fail('notFound' as const);
    if (row.startAt <= ctx.now) return fail('started' as const);
    const rule = row.ruleId ? repo.getRule(tx, row.ruleId) : undefined;
    const zone = rule?.timeZone ?? ctx.zone;
    const local = utcToLocal(row.startAt, zone);
    const next = {
      date: change.date ?? local.date,
      time: change.time ?? local.time,
      plannedMin: change.plannedMin ?? row.plannedMin,
      valueCents: change.valueCents !== undefined ? change.valueCents : row.valueCents,
      travelFeeCents:
        change.travelFeeCents !== undefined ? change.travelFeeCents : row.travelFeeCents,
      notes: change.notes !== undefined ? change.notes : row.notes,
    };

    if (scope === 'one' || !rule || !row.occurrenceDate) {
      repo.updateLessonRow(
        tx,
        id,
        {
          startAt: localToUtc(next.date, next.time, zone),
          plannedMin: next.plannedMin,
          valueCents: next.valueCents,
          travelFeeCents: next.travelFeeCents,
          notes: next.notes,
          isDetached: !!rule,
        },
        ctx.now,
      );
      return { ok: true, value: { lessonId: id } };
    }

    repo.updateRule(tx, rule.id, { endsOn: addDays(row.occurrenceDate, -1) }, ctx.now);
    repo.deleteSeriesFrom(tx, rule.id, row.occurrenceDate, ctx.now, { detachedToo: false });
    repo.deleteLesson(tx, id);
    const who = rule.studentId ? { studentId: rule.studentId } : { groupId: rule.groupId! };
    const fresh = newRule(
      { ...ctx, zone },
      { who, placeId: rule.placeId, repeat: rule.repeat, ...next },
      ctx.newId(),
    );
    repo.insertRule(tx, fresh, ctx.now);
    fillRule(tx, ctx, fresh, horizon(ctx) > next.date ? horizon(ctx) : next.date);
    const first = repo.lessonAt(tx, fresh.id, next.date)!;
    if (next.notes) repo.updateLessonRow(tx, first.id, { notes: next.notes }, ctx.now);
    return { ok: true, value: { lessonId: first.id } };
  });
}

/**
 * Remover. "Só esta aula" removes it and remembers the date, so filling never brings it
 * back. "Esta e as próximas" ends the series and removes it from this class on.
 */
export function removeLesson(ctx: Ctx, id: string, scope: Scope): Result<null, ScheduleError> {
  return ctx.db.transaction((tx) => {
    const row = repo.getLessonRow(tx, id);
    if (!row) return fail('notFound' as const);
    if (row.startAt <= ctx.now) return fail('started' as const);
    if (scope === 'following' && row.ruleId && row.occurrenceDate) {
      repo.updateRule(tx, row.ruleId, { endsOn: addDays(row.occurrenceDate, -1) }, ctx.now);
      repo.deleteSeriesFrom(tx, row.ruleId, row.occurrenceDate, ctx.now, { detachedToo: true });
      return { ok: true, value: null };
    }
    if (row.ruleId && row.occurrenceDate) repo.addSkip(tx, row.ruleId, row.occurrenceDate);
    repo.deleteLesson(tx, id);
    return { ok: true, value: null };
  });
}

/** The travel fee to suggest at Casa do Aluno: the last one this student paid there. */
export function suggestedTravel(ctx: Pick<Ctx, 'db'>, studentId: string, placeId: string) {
  return repo.lastTravelFee(ctx.db, studentId, placeId);
}
