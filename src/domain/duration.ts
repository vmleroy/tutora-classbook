// Durations are whole minutes. Typed as hh:mm, filling from the right like money.

/** Longest duration a field accepts: 5 hours, the stepper's own limit. */
export const MAX_DURATION_MIN = 300;

export type TypedDuration = { digits: string; minutes: number | null };

/**
 * The digits of an hh:mm field after the teacher edits its text: typing appends on the
 * right, deleting drops the last one, at most four digits. `minutes` is null while the
 * minutes part is over 59 or the total is over `max`.
 */
export function typedDuration(
  text: string,
  previous: string,
  max: number = MAX_DURATION_MIN,
): TypedDuration {
  const digits = text.replace(/\D/g, '').replace(/^0+/, '');
  if (digits.length > 4) return { digits: previous, minutes: minutesOf(previous, max) };
  return { digits, minutes: minutesOf(digits, max) };
}

function minutesOf(digits: string, max: number): number | null {
  const value = Number(digits || '0');
  const hours = Math.floor(value / 100);
  const minutes = value % 100;
  if (minutes > 59) return null;
  const total = hours * 60 + minutes;
  return total > max ? null : total;
}

/** The digits that show `minutes` as hh:mm (90 → "130"). */
export function durationDigits(minutes: number): string {
  const value = Math.floor(minutes / 60) * 100 + (minutes % 60);
  return value === 0 ? '' : String(value);
}

/** "01:30" from the digits "130". */
export function hhmm(digits: string): string {
  const padded = digits.padStart(4, '0');
  return `${padded.slice(0, -2)}:${padded.slice(-2)}`;
}
