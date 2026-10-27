import { roundHalfAwayFromZero, type Currency } from '@/domain/money';
import type { Language } from '@/domain/settings';

const localeOf: Record<Language, string> = { 'pt-BR': 'pt-BR', en: 'en-US' };

/**
 * Money from integer cents, formatted with Intl for the app language; the currency is
 * independent of the language (constitution rule 4). `compact` drops the cents, rounding
 * whole units half away from zero in domain/money.ts, the only place money is rounded.
 */
export function formatMoney(
  cents: number,
  currency: Currency = 'BRL',
  language: Language = 'pt-BR',
  compact = false,
): string {
  if (!Number.isSafeInteger(cents)) throw new RangeError(`Money needs integer cents, got ${cents}`);
  const format = new Intl.NumberFormat(localeOf[language], {
    style: 'currency',
    currency,
    minimumFractionDigits: compact ? 0 : 2,
    maximumFractionDigits: compact ? 0 : 2,
  });
  // Dividing integer cents by 100 gives at most two decimals, so Intl prints them exactly.
  return format.format(compact ? roundHalfAwayFromZero(cents, 100) : cents / 100);
}
