import { builtinPlace, type Place, type Student } from './places';
import { expected, greeting, layoutDay, lessonWorth, nextLesson } from './schedule';

const ana: Student = {
  id: 's',
  name: 'Ana Souza',
  subject: null,
  phone: null,
  notes: null,
  color: 'amber',
  rateType: 'hour',
  archivedAt: null,
};
const online: Place = { ...builtinPlace('online'), rateCents: 10000 };
const home: Place = { ...builtinPlace('student_home'), rateCents: 12000 };
const at = (place: Place, over = {}) => ({
  plannedMin: 90,
  valueCents: null,
  travelFeeCents: null,
  place,
  who: { kind: 'student' as const, student: ana },
  ...over,
});

describe('schedule', () => {
  it('AC-005-4: a class is worth its price for the duration plus travel, unset without a price, or a typed value', () => {
    expect(lessonWorth(at(online))).toEqual({ kind: 'set', cents: 15000, source: 'place' });
    expect(lessonWorth(at(home, { travelFeeCents: 2000 }))).toMatchObject({ cents: 20000 });
    expect(lessonWorth(at({ ...online, rateCents: null }))).toEqual({ kind: 'unset' });
    expect(lessonWorth(at(online, { valueCents: 9000 }))).toEqual({
      kind: 'set',
      cents: 9000,
      source: 'hand',
    });
  });

  it('AC-005-16: a scheduled class follows the current price; a typed value stays', () => {
    const raised = { ...online, rateCents: 12000 };
    expect(lessonWorth(at(raised))).toMatchObject({ cents: 18000 });
    expect(lessonWorth(at(raised, { valueCents: 9000 }))).toMatchObject({ cents: 9000 });
    const custom = {
      kind: 'student' as const,
      student: ana,
      link: { placeId: online.id, isDefault: true, rateCents: 13000 },
    };
    expect(lessonWorth(at(raised, { who: custom }))).toMatchObject({ cents: 19500 });
  });

  it('AC-005-19: Previsto sums priced classes and flags the ones a salary covers', () => {
    expect(
      expected([
        { kind: 'set', cents: 12000, source: 'place' },
        { kind: 'unset' },
        { kind: 'salary' },
      ]),
    ).toEqual({ cents: 12000, salary: true });
  });

  it('AC-005-21: overlapping classes sit side by side', () => {
    const layout = layoutDay([
      { id: 'a', start: 600, end: 660 },
      { id: 'b', start: 630, end: 720 },
      { id: 'c', start: 800, end: 860 },
    ]);
    expect(layout.get('a')).toEqual({ column: 0, columns: 2 });
    expect(layout.get('b')).toEqual({ column: 1, columns: 2 });
    expect(layout.get('c')).toEqual({ column: 0, columns: 1 });
  });

  it('AC-005-22: the greeting follows the time of day', () => {
    expect([greeting(8), greeting(14), greeting(20), greeting(2)]).toEqual([
      'morning',
      'afternoon',
      'evening',
      'evening',
    ]);
  });

  it('AC-005-23: the next class is the first one that has not started', () => {
    const lessons = [
      { id: 'later', startAt: '2026-10-27T17:00:00.000Z' },
      { id: 'past', startAt: '2026-10-26T11:00:00.000Z' },
      { id: 'soon', startAt: '2026-10-26T17:00:00.000Z' },
    ];
    expect(nextLesson(lessons, '2026-10-26T16:35:00.000Z')?.id).toBe('soon');
  });
});
