// Recurring classes: local calendar dates and wall-clock times, converted to UTC instants
// in the rule's own time zone (architecture, time rules).
import { TZDate } from '@date-fns/tz';

/** A local calendar date, `YYYY-MM-DD`. */
export type LocalDate = string;
/** A local wall-clock time, `HH:MM`. */
export type LocalTime = string;
export type Repeat = 'weekly' | 'biweekly';

/** How far ahead repeating classes are kept filled: 8 weeks. */
export const HORIZON_DAYS = 56;

const parse = (date: LocalDate) => date.split('-').map(Number) as [number, number, number];

export function addDays(date: LocalDate, days: number): LocalDate {
  const [y, m, d] = parse(date);
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10);
}

export function daysBetween(from: LocalDate, to: LocalDate): number {
  const [a, b] = [parse(from), parse(to)];
  return Math.round((Date.UTC(b[0], b[1] - 1, b[2]) - Date.UTC(a[0], a[1] - 1, a[2])) / 86_400_000);
}

/** 1 = Monday … 7 = Sunday. */
export function weekdayOf(date: LocalDate): number {
  const [y, m, d] = parse(date);
  return ((new Date(Date.UTC(y, m - 1, d)).getUTCDay() + 6) % 7) + 1;
}

/** The Monday of the week holding `date`; weeks start on Monday in both languages. */
export function weekStart(date: LocalDate): LocalDate {
  return addDays(date, 1 - weekdayOf(date));
}

export type RuleDates = Readonly<{
  startsOn: LocalDate;
  endsOn: LocalDate | null;
  repeat: Repeat;
  skips?: readonly LocalDate[];
}>;

/** The rule's dates in `[from, to]`: every 7 or 14 days from its first date. */
export function occurrences(rule: RuleDates, from: LocalDate, to: LocalDate): LocalDate[] {
  const step = rule.repeat === 'weekly' ? 7 : 14;
  const last = rule.endsOn !== null && rule.endsOn < to ? rule.endsOn : to;
  const skips = new Set(rule.skips ?? []);
  const behind = daysBetween(rule.startsOn, from);
  let date = behind > 0 ? addDays(rule.startsOn, Math.ceil(behind / step) * step) : rule.startsOn;
  const dates: LocalDate[] = [];
  for (; date <= last; date = addDays(date, step)) if (!skips.has(date)) dates.push(date);
  return dates;
}

/** The UTC instant of a local date and time in `zone`, as fixed-width ISO with `Z`. */
export function localToUtc(date: LocalDate, time: LocalTime, zone: string): string {
  const [y, m, d] = parse(date);
  const [h, min] = time.split(':').map(Number) as [number, number];
  return new Date(new TZDate(y, m - 1, d, h, min, zone).getTime()).toISOString();
}

export type Local = { date: LocalDate; time: LocalTime; hour: number; minute: number };

/** The local date and time of a UTC instant in `zone`. */
export function utcToLocal(iso: string, zone: string): Local {
  const t = new TZDate(new Date(iso).getTime(), zone);
  const pad = (n: number) => String(n).padStart(2, '0');
  const date = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`;
  return {
    date,
    time: `${pad(t.getHours())}:${pad(t.getMinutes())}`,
    hour: t.getHours(),
    minute: t.getMinutes(),
  };
}

/** The UTC `[from, to)` range of the local days `[first, last]`. */
export function dayRange(first: LocalDate, last: LocalDate, zone: string): [string, string] {
  return [localToUtc(first, '00:00', zone), localToUtc(addDays(last, 1), '00:00', zone)];
}
