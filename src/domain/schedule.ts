// What scheduled classes are worth and how a day lays out (spec 005).
import { lessonValue, type Cents } from './money';
import type { Group, Place, PlaceLink, Student } from './places';
import { resolvePrice, type PriceSource } from './pricing';
import type { LocalDate, Repeat } from './recurrence';

export type Who =
  { kind: 'student'; student: Student; link?: PlaceLink } | { kind: 'group'; group: Group };

export type Lesson = Readonly<{
  id: string;
  ruleId: string | null;
  occurrenceDate: LocalDate | null;
  repeat: Repeat | null;
  startAt: string;
  plannedMin: number;
  /** A value the teacher typed for this class; null = follow the price. */
  valueCents: Cents | null;
  /** Travel for this class at Casa do Aluno when travel "varies". */
  travelFeeCents: Cents | null;
  isDetached: boolean;
  notes: string | null;
  who: Who;
  place: Place;
}>;

export type LessonWorth =
  | { kind: 'set'; cents: Cents; source: PriceSource | 'hand' }
  | { kind: 'unset' }
  | { kind: 'salary' };

/** The travel fee this class adds: only at Casa do Aluno. */
export function travelFor(place: Place, lessonTravel: Cents | null): Cents {
  if (place.kind !== 'student_home') return 0;
  if (place.travelMode === 'fixed') return place.travelFeeCents;
  return place.travelMode === 'varies' ? (lessonTravel ?? 0) : 0;
}

/**
 * What a scheduled class is expected to earn. A hand-typed value wins; otherwise the
 * current price (spec 004) for its duration, so price changes reach every scheduled class.
 * Travel is added on top.
 */
export function lessonWorth(
  lesson: Omit<
    Lesson,
    'id' | 'ruleId' | 'occurrenceDate' | 'repeat' | 'startAt' | 'isDetached' | 'notes'
  >,
): LessonWorth {
  const travel = travelFor(lesson.place, lesson.travelFeeCents);
  if (lesson.valueCents !== null) {
    return { kind: 'set', cents: lesson.valueCents + travel, source: 'hand' };
  }
  const price =
    lesson.who.kind === 'student'
      ? resolvePrice(lesson.place, { student: lesson.who.student, link: lesson.who.link })
      : resolvePrice(lesson.place, { group: lesson.who.group });
  if (price.kind !== 'set') return price;
  return {
    kind: 'set',
    cents: lessonValue(price.cents, price.rateType, lesson.plannedMin) + travel,
    source: price.source,
  };
}

/** Previsto for a set of classes: those with a price, and whether a salary covers others. */
export function expected(worths: readonly LessonWorth[]): { cents: Cents; salary: boolean } {
  return worths.reduce<{ cents: Cents; salary: boolean }>(
    (acc, w) => ({
      cents: acc.cents + (w.kind === 'set' ? w.cents : 0),
      salary: acc.salary || w.kind === 'salary',
    }),
    { cents: 0, salary: false },
  );
}

export type Span = { id: string; start: number; end: number };

/**
 * Side-by-side columns for overlapping classes: each class gets a column, and every class
 * in a cluster of overlaps shares the cluster's column count.
 */
export function layoutDay(
  spans: readonly Span[],
): Map<string, { column: number; columns: number }> {
  const sorted = [...spans].sort((a, b) => a.start - b.start || a.end - b.end);
  const result = new Map<string, { column: number; columns: number }>();
  let cluster: { id: string; column: number }[] = [];
  let ends: number[] = [];
  let clusterEnd = -Infinity;
  const close = () => {
    for (const c of cluster) result.set(c.id, { column: c.column, columns: ends.length });
    cluster = [];
    ends = [];
  };
  for (const s of sorted) {
    if (s.start >= clusterEnd) close();
    let column = ends.findIndex((end) => end <= s.start);
    if (column === -1) column = ends.push(s.end) - 1;
    else ends[column] = s.end;
    cluster.push({ id: s.id, column });
    clusterEnd = Math.max(clusterEnd, s.end);
  }
  close();
  return result;
}

export type Greeting = 'morning' | 'afternoon' | 'evening';

export function greeting(hour: number): Greeting {
  if (hour >= 5 && hour < 12) return 'morning';
  return hour >= 12 && hour < 18 ? 'afternoon' : 'evening';
}

/** The first class that has not started yet. */
export function nextLesson<T extends { startAt: string }>(
  lessons: readonly T[],
  now: string,
): T | undefined {
  return [...lessons]
    .sort((a, b) => a.startAt.localeCompare(b.startAt))
    .find((l) => l.startAt > now);
}
