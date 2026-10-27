import {
  builtinPlace,
  earningsType,
  payMode,
  randomTag,
  tagColors,
  unlink,
  withDefault,
} from './places';

describe('places', () => {
  it('AC-004-4: Escola and Cursinho count as job, the rest as private lessons', () => {
    expect(earningsType('school')).toBe('job');
    expect(earningsType('course')).toBe('job');
    for (const kind of ['salinha', 'private', 'student_home', 'online'] as const) {
      expect(earningsType(kind)).toBe('private');
    }
  });

  it('AC-004-5: Salinha, Cursinho and Particular ask who pays', () => {
    for (const kind of ['salinha', 'course', 'private'] as const)
      expect(payMode(kind)).toBe('choose');
    expect(payMode('online')).toBe('student');
  });

  it('AC-004-6: an Escola is paid by a monthly salary', () => {
    expect(payMode('school')).toBe('salary');
  });

  it('AC-004-28: a new student gets any of the color tags at random', () => {
    const picked = new Set([0, 0.15, 0.3, 0.45, 0.6, 0.75, 0.99].map(randomTag));
    expect([...picked].sort()).toEqual([...tagColors].sort());
  });

  it('AC-004-8: places default to 60 minutes', () => {
    expect(builtinPlace('online').defaultDurationMin).toBe(60);
  });

  it('AC-004-11: the first place is the default and the star moves when it is removed', () => {
    const links = withDefault([
      { placeId: 'a', isDefault: false, rateCents: null },
      { placeId: 'b', isDefault: false, rateCents: null },
    ]);
    expect(links.map((l) => l.isDefault)).toEqual([true, false]);
    expect(unlink(links, 'a')).toEqual([{ placeId: 'b', isDefault: true, rateCents: null }]);
  });
});
