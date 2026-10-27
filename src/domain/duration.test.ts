import { durationDigits, hhmm, typedDuration } from './duration';

describe('duration', () => {
  it('AC-004-27: a duration is typed as hh:mm, filling from the right', () => {
    let state = { digits: '', minutes: 0 as number | null };
    for (const d of '130') state = typedDuration(`${hhmm(state.digits)}${d}`, state.digits);
    expect(hhmm(state.digits)).toBe('01:30');
    expect(state.minutes).toBe(90);
    expect(typedDuration('01:3', '130')).toEqual({ digits: '13', minutes: 13 });
    expect(hhmm(durationDigits(50))).toBe('00:50');
  });

  it('AC-004-27: minutes over 59 or a total over 5 hours are not a duration', () => {
    expect(typedDuration('00:90', '9').minutes).toBeNull();
    expect(typedDuration('05:01', '50').minutes).toBeNull();
    expect(typedDuration('05:00', '50').minutes).toBe(300);
    expect(typedDuration('12:345', '1234').digits).toBe('1234');
  });
});
