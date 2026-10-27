import { prepareDatabase } from '@/data/db';
import { migrations } from '@/data/migrations';
import { ensureBuiltinPlaces } from '@/data/repos/placeRepo';
import { getLesson, listLessons } from '@/data/repos/scheduleRepo';
import { BUILTIN_IDS, type Student } from '@/domain/places';
import { utcToLocal } from '@/domain/recurrence';
import { nodeFiles, tempDatabase } from '@tests/sqliteNode';

import { savePlace, saveStudent } from './catalog';
import {
  editLesson,
  fillSchedule,
  removeLesson,
  scheduleLesson,
  suggestedTravel,
  type Ctx,
  type LessonDraft,
} from './schedule';

// Monday 26 Oct 2026, 09:00 in São Paulo.
const NOW = '2026-10-26T12:00:00.000Z';
const ZONE = 'America/Sao_Paulo';
let n = 0;

function setup(now = NOW) {
  const t = tempDatabase();
  prepareDatabase(t.driver, nodeFiles, migrations);
  ensureBuiltinPlaces(t.db, now);
  const ctx: Ctx = { db: t.db, now, zone: ZONE, newId: () => `id-${String(++n).padStart(5, '0')}` };
  const ana: Omit<Student, 'id' | 'archivedAt'> = {
    name: 'Ana Souza',
    subject: 'Matemática',
    phone: null,
    notes: null,
    color: 'amber',
    rateType: 'hour',
  };
  const s = saveStudent(ctx, ana, [
    { placeId: BUILTIN_IDS.online, isDefault: true, rateCents: 12000 },
    { placeId: BUILTIN_IDS.student_home, isDefault: false, rateCents: null },
  ]);
  if (!s.ok) throw new Error(s.error);
  return { ctx, studentId: s.value.id };
}

const draft = (studentId: string, over: Partial<LessonDraft> = {}): LessonDraft => ({
  who: { studentId },
  placeId: BUILTIN_IDS.online,
  date: '2026-10-26',
  time: '14:00',
  plannedMin: 60,
  valueCents: null,
  travelFeeCents: null,
  repeat: 'weekly',
  ...over,
});

const all = (ctx: Ctx) =>
  listLessons(ctx.db, '2026-01-01T00:00:00.000Z', '2027-12-31T00:00:00.000Z');
const dates = (ctx: Ctx) => all(ctx).map((l) => utcToLocal(l.startAt, ZONE).date);
const later = (ctx: Ctx, days: number) => ({
  ...ctx,
  now: new Date(Date.parse(NOW) + days * 86_400_000).toISOString(),
});

describe('scheduling', () => {
  it("AC-005-2: only the student's linked places, or the group's place, are allowed", () => {
    const { ctx, studentId } = setup();
    const p = savePlace(ctx, {
      builtin: null,
      name: 'Salinha',
      address: null,
      kind: 'salinha',
      payer: 'place',
      rateType: 'hour',
      rateCents: 6000,
      commissionBp: 0,
      payCycle: 'monthly',
      monthlySalaryCents: null,
      defaultDurationMin: 60,
      travelMode: 'none',
      travelFeeCents: 0,
    });
    if (!p.ok) throw new Error();
    expect(scheduleLesson(ctx, draft(studentId, { placeId: p.value.id }))).toEqual({
      ok: false,
      error: 'placeNotLinked',
    });
  });

  it('AC-005-3: a class keeps its date, start time and duration', () => {
    const { ctx, studentId } = setup();
    const r = scheduleLesson(
      ctx,
      draft(studentId, { repeat: null, time: '16:30', plannedMin: 90 }),
    );
    const lesson = r.ok ? getLesson(ctx.db, r.value.lessonId) : undefined;
    expect(utcToLocal(lesson!.startAt, ZONE)).toMatchObject({ date: '2026-10-26', time: '16:30' });
    expect(lesson?.plannedMin).toBe(90);
  });

  it('AC-005-5: Casa do Aluno with travel "varies" suggests the last fee for that student', () => {
    const { ctx, studentId } = setup();
    scheduleLesson(
      ctx,
      draft(studentId, { placeId: BUILTIN_IDS.student_home, travelFeeCents: 2500, repeat: null }),
    );
    expect(suggestedTravel(ctx, studentId, BUILTIN_IDS.student_home)).toBe(2500);
  });

  it('AC-005-8: filling keeps 8 weeks ahead and never duplicates', () => {
    const { ctx, studentId } = setup();
    scheduleLesson(ctx, draft(studentId));
    expect(all(ctx)).toHaveLength(9); // 26/10 to 21/12, 8 weeks ahead inclusive
    fillSchedule(ctx);
    fillSchedule(ctx);
    expect(all(ctx)).toHaveLength(9);
    fillSchedule(later(ctx, 7));
    expect(dates(ctx).at(-1)).toBe('2026-12-28');
    expect(all(ctx)).toHaveLength(10);
  });
});

describe('editing and removing', () => {
  function series() {
    const { ctx, studentId } = setup();
    scheduleLesson(ctx, draft(studentId));
    const lessons = all(ctx);
    return { ctx, studentId, lessons };
  }

  it('AC-005-10: a class opens with its date, time, duration, value, notes and series', () => {
    const { ctx, lessons } = series();
    expect(getLesson(ctx.db, lessons[0]!.id)).toMatchObject({
      repeat: 'weekly',
      plannedMin: 60,
      valueCents: null,
      notes: null,
    });
  });

  it('AC-005-11: a recurring class can change only itself or itself and the following', () => {
    const { ctx, lessons } = series();
    expect(editLesson(ctx, lessons[1]!.id, { plannedMin: 90 }, 'one').ok).toBe(true);
    expect(editLesson(ctx, lessons[2]!.id, { plannedMin: 45 }, 'following').ok).toBe(true);
  });

  it('AC-005-12: "Só esta aula" changes one class, and filling never overwrites it', () => {
    const { ctx, lessons } = series();
    editLesson(ctx, lessons[1]!.id, { date: '2026-11-03', time: '10:00' }, 'one');
    fillSchedule(later(ctx, 14));
    const moved = getLesson(ctx.db, lessons[1]!.id)!;
    expect(utcToLocal(moved.startAt, ZONE)).toMatchObject({ date: '2026-11-03', time: '10:00' });
    expect(moved.isDetached).toBe(true);
    expect(dates(ctx)).not.toContain('2026-11-02');
  });

  it('AC-005-13: "Esta e as próximas" moves this and later classes, keeping hand-changed ones', () => {
    const { ctx, lessons } = series();
    editLesson(ctx, lessons[4]!.id, { valueCents: 9000 }, 'one'); // 23/11 changed on its own
    editLesson(ctx, lessons[2]!.id, { date: '2026-11-10', time: '15:00' }, 'following');
    const after = all(ctx).map((l) => ({
      ...utcToLocal(l.startAt, ZONE),
      value: l.valueCents,
    }));
    expect(after.slice(0, 2).map((l) => l.date)).toEqual(['2026-10-26', '2026-11-02']);
    expect(
      after.filter((l) => l.date >= '2026-11-09' && l.time === '15:00').map((l) => l.date),
    ).toContain('2026-11-10');
    expect(after.some((l) => l.date === '2026-11-09' && l.time === '14:00')).toBe(false);
    expect(after.find((l) => l.date === '2026-11-23')).toMatchObject({
      time: '14:00',
      value: 9000,
    });
  });

  it('AC-005-14: removing asks the scope; "Esta e as próximas" ends the series', () => {
    const { ctx, lessons } = series();
    removeLesson(ctx, lessons[1]!.id, 'one');
    fillSchedule(later(ctx, 1));
    expect(dates(ctx)).not.toContain('2026-11-02');
    removeLesson(ctx, lessons[3]!.id, 'following');
    fillSchedule(later(ctx, 30));
    expect(dates(ctx)).toEqual(['2026-10-26', '2026-11-09']);
  });

  it('AC-005-15: past classes can change on their own; series changes never reach them', () => {
    const { ctx, lessons } = series();
    const after = later(ctx, 8); // past the 26/10 and 02/11 classes
    expect(editLesson(after, lessons[0]!.id, { plannedMin: 30 }, 'one').ok).toBe(true);
    expect(getLesson(ctx.db, lessons[0]!.id)?.plannedMin).toBe(30);
    expect(editLesson(after, lessons[0]!.id, { plannedMin: 30 }, 'following')).toEqual({
      ok: false,
      error: 'started',
    });
    editLesson(after, lessons[2]!.id, { time: '18:00' }, 'following');
    expect(getLesson(ctx.db, lessons[1]!.id)?.plannedMin).toBe(60);
    expect(utcToLocal(getLesson(ctx.db, lessons[1]!.id)!.startAt, ZONE).time).toBe('14:00');
    expect(removeLesson(after, lessons[1]!.id, 'one').ok).toBe(true);
  });
});
