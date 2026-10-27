import { localToUtc, occurrences, utcToLocal, weekdayOf, weekStart } from './recurrence';

describe('recurrence', () => {
  it('AC-005-6: a class repeats weekly or every two weeks, or not at all', () => {
    const weekly = { startsOn: '2026-10-26', endsOn: null, repeat: 'weekly' as const };
    expect(occurrences(weekly, '2026-10-26', '2026-11-16')).toEqual([
      '2026-10-26',
      '2026-11-02',
      '2026-11-09',
      '2026-11-16',
    ]);
    expect(occurrences({ ...weekly, repeat: 'biweekly' }, '2026-10-26', '2026-11-16')).toEqual([
      '2026-10-26',
      '2026-11-09',
    ]);
  });

  it('AC-005-7: a series fills 8 weeks on its weekday, from any window, up to its end, minus skips', () => {
    const rule = { startsOn: '2026-10-26', endsOn: null, repeat: 'weekly' as const };
    const eight = occurrences(rule, '2026-10-26', '2026-12-20');
    expect(eight).toHaveLength(8);
    expect(eight.every((d) => weekdayOf(d) === 1)).toBe(true);
    expect(occurrences({ ...rule, repeat: 'biweekly' }, '2026-11-03', '2026-11-30')).toEqual([
      '2026-11-09',
      '2026-11-23',
    ]);
    expect(
      occurrences(
        { ...rule, endsOn: '2026-11-08', skips: ['2026-11-02'] },
        '2026-10-26',
        '2026-12-20',
      ),
    ).toEqual(['2026-10-26']);
    expect(weekStart('2026-11-01')).toBe('2026-10-26');
  });

  it('AC-005-9: a class keeps its local clock time across a daylight-saving change', () => {
    const zone = 'America/New_York';
    const before = localToUtc('2026-10-26', '18:00', zone);
    const after = localToUtc('2026-11-02', '18:00', zone);
    expect(before).toBe('2026-10-26T22:00:00.000Z');
    expect(after).toBe('2026-11-02T23:00:00.000Z');
    expect(utcToLocal(after, zone).time).toBe('18:00');
    expect(localToUtc('2026-10-26', '14:00', 'America/Sao_Paulo')).toBe('2026-10-26T17:00:00.000Z');
  });
});
