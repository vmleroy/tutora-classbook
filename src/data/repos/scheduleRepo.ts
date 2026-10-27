import { and, asc, desc, eq, gt, gte, isNotNull, isNull, lt, or } from 'drizzle-orm';

import type { Place } from '@/domain/places';
import type { LocalDate } from '@/domain/recurrence';
import type { Lesson } from '@/domain/schedule';

import { lesson, ruleSkip, scheduleRule } from '../schema';
import { getGroup } from './groupRepo';
import { getPlace } from './placeRepo';
import type { Db } from './settingsRepo';
import { getStudent } from './studentRepo';

export type RuleRow = typeof scheduleRule.$inferSelect;
export type LessonRow = typeof lesson.$inferSelect;
type NewLesson = Omit<typeof lesson.$inferInsert, 'createdAt' | 'updatedAt'>;

export function insertRule(db: Db, rule: Omit<RuleRow, 'createdAt' | 'updatedAt'>, now: string) {
  db.insert(scheduleRule)
    .values({ ...rule, createdAt: now, updatedAt: now })
    .run();
}

export function getRule(db: Db, id: string): RuleRow | undefined {
  return db.select().from(scheduleRule).where(eq(scheduleRule.id, id)).get();
}

export function updateRule(db: Db, id: string, patch: Partial<RuleRow>, now: string) {
  db.update(scheduleRule)
    .set({ ...patch, updatedAt: now })
    .where(eq(scheduleRule.id, id))
    .run();
}

/** Rules still running on `today`. */
export function activeRules(db: Db, today: LocalDate): RuleRow[] {
  return db
    .select()
    .from(scheduleRule)
    .where(or(isNull(scheduleRule.endsOn), gte(scheduleRule.endsOn, today)))
    .all();
}

export function skipsOf(db: Db, ruleId: string): LocalDate[] {
  return db
    .select({ d: ruleSkip.occurrenceDate })
    .from(ruleSkip)
    .where(eq(ruleSkip.ruleId, ruleId))
    .all()
    .map((r) => r.d);
}

export function addSkip(db: Db, ruleId: string, occurrenceDate: LocalDate) {
  db.insert(ruleSkip).values({ ruleId, occurrenceDate }).onConflictDoNothing().run();
}

/** Inserts classes; one already there for the same series date is left alone. */
export function insertLessons(db: Db, rows: NewLesson[], now: string) {
  if (rows.length === 0) return;
  db.insert(lesson)
    .values(rows.map((r) => ({ ...r, createdAt: now, updatedAt: now })))
    .onConflictDoNothing()
    .run();
}

export function getLessonRow(db: Db, id: string): LessonRow | undefined {
  return db.select().from(lesson).where(eq(lesson.id, id)).get();
}

export function lessonAt(db: Db, ruleId: string, occurrenceDate: LocalDate) {
  return db
    .select()
    .from(lesson)
    .where(and(eq(lesson.ruleId, ruleId), eq(lesson.occurrenceDate, occurrenceDate)))
    .get();
}

export function updateLessonRow(db: Db, id: string, patch: Partial<LessonRow>, now: string) {
  db.update(lesson)
    .set({ ...patch, updatedAt: now })
    .where(eq(lesson.id, id))
    .run();
}

export function deleteLesson(db: Db, id: string) {
  db.delete(lesson).where(eq(lesson.id, id)).run();
}

/** Deletes a series' classes from a date on that have not started; detached ones only if asked. */
export function deleteSeriesFrom(
  db: Db,
  ruleId: string,
  from: LocalDate,
  now: string,
  { detachedToo }: { detachedToo: boolean },
) {
  db.delete(lesson)
    .where(
      and(
        eq(lesson.ruleId, ruleId),
        gte(lesson.occurrenceDate, from),
        gt(lesson.startAt, now),
        detachedToo ? undefined : eq(lesson.isDetached, false),
      ),
    )
    .run();
}

/** The travel fee of this student's latest class at this place, to suggest for the next. */
export function lastTravelFee(db: Db, studentId: string, placeId: string): number | null {
  return (
    db
      .select({ fee: lesson.travelFeeCents })
      .from(lesson)
      .where(
        and(
          eq(lesson.studentId, studentId),
          eq(lesson.placeId, placeId),
          isNotNull(lesson.travelFeeCents),
        ),
      )
      .orderBy(desc(lesson.startAt))
      .limit(1)
      .get()?.fee ?? null
  );
}

/** Classes starting in `[from, to)`, with who and where, ready to price. */
export function listLessons(db: Db, from: string, to: string): Lesson[] {
  const rows = db
    .select({ l: lesson, repeat: scheduleRule.repeat })
    .from(lesson)
    .leftJoin(scheduleRule, eq(scheduleRule.id, lesson.ruleId))
    .where(and(gte(lesson.startAt, from), lt(lesson.startAt, to)))
    .orderBy(asc(lesson.startAt))
    .all();
  return toLessons(db, rows);
}

export function getLesson(db: Db, id: string): Lesson | undefined {
  const row = db
    .select({ l: lesson, repeat: scheduleRule.repeat })
    .from(lesson)
    .leftJoin(scheduleRule, eq(scheduleRule.id, lesson.ruleId))
    .where(eq(lesson.id, id))
    .get();
  return row ? toLessons(db, [row])[0] : undefined;
}

/** The first class after `now`, looking up to 8 weeks ahead. */
export function firstLessonAfter(db: Db, now: string): Lesson | undefined {
  const row = db
    .select({ l: lesson, repeat: scheduleRule.repeat })
    .from(lesson)
    .leftJoin(scheduleRule, eq(scheduleRule.id, lesson.ruleId))
    .where(gt(lesson.startAt, now))
    .orderBy(asc(lesson.startAt))
    .limit(1)
    .get();
  return row ? toLessons(db, [row])[0] : undefined;
}

function toLessons(db: Db, rows: { l: LessonRow; repeat: RuleRow['repeat'] | null }[]): Lesson[] {
  const places = new Map<string, Place | undefined>();
  const placeOf = (id: string) => {
    if (!places.has(id)) places.set(id, getPlace(db, id));
    return places.get(id);
  };
  return rows.flatMap(({ l, repeat }) => {
    const place = placeOf(l.placeId);
    if (!place) return [];
    let who: Lesson['who'] | undefined;
    if (l.studentId) {
      const s = getStudent(db, l.studentId);
      if (s) {
        const { links, ...student } = s;
        who = { kind: 'student', student, link: links.find((k) => k.placeId === l.placeId) };
      }
    } else if (l.groupId) {
      const group = getGroup(db, l.groupId);
      if (group) who = { kind: 'group', group };
    }
    if (!who) return [];
    return [
      {
        id: l.id,
        ruleId: l.ruleId,
        occurrenceDate: l.occurrenceDate,
        repeat,
        startAt: l.startAt,
        plannedMin: l.plannedMin,
        valueCents: l.valueCents,
        travelFeeCents: l.travelFeeCents,
        isDetached: l.isDetached,
        notes: l.notes,
        who,
        place,
      },
    ];
  });
}
