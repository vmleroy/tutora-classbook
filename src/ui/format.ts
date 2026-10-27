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
