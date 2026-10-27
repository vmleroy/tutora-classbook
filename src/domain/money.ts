// Money is integer cents of reais (constitution rules 8 and 9).
// This is the only place in the app that rounds money.

/** An amount in centavos. Always a safe integer. */
export type Cents = number;

/** Largest amount a money field accepts: R$ 99.999,99. */
export const MAX_FIELD_CENTS: Cents = 9_999_999;

export function cents(value: number): Cents {
  if (!Number.isSafeInteger(value)) throw new RangeError(`Money needs integer cents, got ${value}`);
  return value;
}

/** Exact sum of stored amounts; never rounds, so totals never drift. */
export function sum(amounts: readonly Cents[]): Cents {
  return cents(amounts.reduce((total, a) => total + cents(a), 0));
}

/** numerator / denominator rounded half away from zero, computed exactly. */
export function roundHalfAwayFromZero(numerator: number, denominator: number): number {
  if (!Number.isSafeInteger(numerator) || !Number.isSafeInteger(denominator) || denominator <= 0) {
    throw new RangeError('roundHalfAwayFromZero needs integers and a positive denominator');
  }
  return Number(roundBig(BigInt(numerator), BigInt(denominator)));
}

function roundBig(n: bigint, d: bigint): bigint {
  const negative = n < 0n;
  const abs = negative ? -n : n;
  const q = (2n * abs + d) / (2n * d); // floor(abs/d + 1/2)
  return negative ? -q : q;
}

/** Multiplies by the ratio `numerator / denominator` and rounds once. */
export function multiplyRate(amount: Cents, numerator: number, denominator: number): Cents {
  if (!Number.isSafeInteger(numerator) || !Number.isSafeInteger(denominator) || denominator <= 0) {
    throw new RangeError('multiplyRate needs integers and a positive denominator');
  }
  return cents(Number(roundBig(BigInt(cents(amount)) * BigInt(numerator), BigInt(denominator))));
}

export type RateType = 'hour' | 'lesson';

/** What one lesson is worth: hourly prices scale by minutes ÷ 60, per-lesson prices do not. */
export function lessonValue(price: Cents, rateType: RateType, minutes: number): Cents {
  return rateType === 'hour' ? multiplyRate(price, minutes, 60) : cents(price);
}

/** What the teacher keeps after a commission in basis points (30% = 3000). */
export function afterCommission(amount: Cents, commissionBp: number): Cents {
  return multiplyRate(amount, 10_000 - commissionBp, 10_000);
}

/** Whole reais for compact summaries, rounded half away from zero. */
export function wholeReais(amount: Cents): number {
  return roundHalfAwayFromZero(cents(amount), 100);
}

/**
 * The amount a money field holds after the teacher edits its text. The digits of the
 * formatted text are the cents, so typing a digit appends it on the right and deleting
 * drops the last one. Anything past `max` keeps the previous amount.
 */
export function typedCents(text: string, previous: Cents, max: Cents = MAX_FIELD_CENTS): Cents {
  const digits = text.replace(/\D/g, '').replace(/^0+/, '');
  if (digits.length > String(max).length) return previous;
  const value = digits === '' ? 0 : Number(digits);
  return value > max ? previous : value;
}
