// Money is integer minor units with its currency (constitution rule 8).
// This is the only place in the app that rounds money.

export type Currency = 'BRL' | 'USD';

export type Money = Readonly<{ cents: number; currency: Currency }>;

export function money(cents: number, currency: Currency): Money {
  if (!Number.isSafeInteger(cents)) throw new RangeError(`Money needs integer cents, got ${cents}`);
  return { cents, currency };
}

export function add(a: Money, b: Money): Money {
  if (a.currency !== b.currency) {
    throw new TypeError(`Cannot add ${a.currency} to ${b.currency}; convert first`);
  }
  return money(a.cents + b.cents, a.currency);
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

/**
 * Multiplies by the ratio `numerator / denominator` and rounds once.
 * Hourly lesson: `multiplyRate(rate, minutes, 60)`; commission kept: `(10000 − bp) / 10000`;
 * FX: `multiplyRate(usd, usd_brl_e4, 10000, 'BRL')`.
 */
export function multiplyRate(
  m: Money,
  numerator: number,
  denominator: number,
  currency: Currency = m.currency,
): Money {
  if (!Number.isSafeInteger(numerator) || !Number.isSafeInteger(denominator) || denominator <= 0) {
    throw new RangeError('multiplyRate needs integers and a positive denominator');
  }
  const cents = roundBig(BigInt(m.cents) * BigInt(numerator), BigInt(denominator));
  return money(Number(cents), currency);
}
