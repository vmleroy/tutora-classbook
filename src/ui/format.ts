import { cents, wholeReais } from '@/domain/money';
import type { Language } from '@/domain/settings';

const localeOf: Record<Language, string> = { 'pt-BR': 'pt-BR', en: 'en-US' };

/**
 * Reais from integer cents, formatted with Intl for the app language. The language changes
 * only how the amount is written, never the amount (constitution rule 4). `compact` shows
 * whole reais, rounded in domain/money.ts, the only place money is rounded.
 */
export function formatMoney(amount: number, language: Language = 'pt-BR', compact = false): string {
  const format = new Intl.NumberFormat(localeOf[language], {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: compact ? 0 : 2,
    maximumFractionDigits: compact ? 0 : 2,
  });
  // Dividing integer cents by 100 gives at most two decimals, so Intl prints them exactly.
  return format.format(compact ? wholeReais(amount) : cents(amount) / 100);
}

/** A day and month in the app language: 26/10 or 10/26. */
export function formatDayMonth(date: Date, language: Language = 'pt-BR'): string {
  return new Intl.DateTimeFormat(localeOf[language], { day: '2-digit', month: '2-digit' }).format(
    date,
  );
}

// Local calendar dates (`YYYY-MM-DD`) are formatted as UTC noon, so no zone can shift them.
const atNoon = (date: string) => new Date(`${date}T12:00:00.000Z`);

function dateFormat(date: string, language: Language, options: Intl.DateTimeFormatOptions) {
  return new Intl.DateTimeFormat(localeOf[language], { ...options, timeZone: 'UTC' }).format(
    atNoon(date),
  );
}

const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);

/** "Segunda, 26 de outubro" / "Monday, October 26". */
export function formatLongDate(date: string, language: Language): string {
  const weekday = dateFormat(date, language, { weekday: 'long' }).replace('-feira', '');
  const dayMonth = dateFormat(date, language, { day: 'numeric', month: 'long' });
  return `${capitalize(weekday)}, ${dayMonth}`;
}

/** "Outubro 2026" / "October 2026". */
export function formatMonthYear(date: string, language: Language): string {
  return capitalize(
    dateFormat(date, language, { month: 'long', year: 'numeric' }).replace(' de ', ' '),
  );
}

/** "Seg" / "Mon". */
export function formatWeekdayShort(date: string, language: Language): string {
  return capitalize(dateFormat(date, language, { weekday: 'short' }).replace('.', ''));
}

/** "segunda-feira" / "Monday". */
export function formatWeekdayLong(date: string, language: Language): string {
  return dateFormat(date, language, { weekday: 'long' });
}

/** "Seg, 26 out" / "Mon, Oct 26". */
export function formatShortDate(date: string, language: Language): string {
  return capitalize(
    dateFormat(date, language, { weekday: 'short', day: 'numeric', month: 'short' })
      .replace(/\./g, '')
      .replace(' de ', ' '),
  );
}

/** An hour mark for the Agenda grid: "08:00" / "8 AM". */
export function formatHour(hour: number, language: Language): string {
  if (language === 'pt-BR') return `${String(hour).padStart(2, '0')}:00`;
  return new Intl.DateTimeFormat(localeOf[language], { hour: 'numeric', timeZone: 'UTC' }).format(
    new Date(Date.UTC(2026, 0, 1, hour)),
  );
}

/** A local `HH:MM` time in the app language: "14:00" / "2:00 PM". */
export function formatTime(time: string, language: Language): string {
  return new Intl.DateTimeFormat(localeOf[language], {
    hour: 'numeric',
    minute: '2-digit',
    timeZone: 'UTC',
  }).format(new Date(`2026-01-01T${time}:00.000Z`));
}
