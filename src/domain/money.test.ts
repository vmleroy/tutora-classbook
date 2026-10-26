import * as fc from 'fast-check';

import { add, money, multiplyRate, roundHalfAwayFromZero, type Currency } from './money';

const currency = fc.constantFrom<Currency>('BRL', 'USD');
const cents = fc.integer({ min: -1_000_000_000, max: 1_000_000_000 });
const amount = (c: Currency) => cents.map((v) => money(v, c));

describe('money', () => {
  it('AC-001-17: a Money is integer cents with a currency', () => {
    expect(money(12345, 'BRL')).toEqual({ cents: 12345, currency: 'BRL' });
    expect(() => money(10.5, 'BRL')).toThrow();
    expect(() => money(Number.NaN, 'USD')).toThrow();
  });

  it('AC-001-17: add is exact, commutative and associative within one currency', () => {
    fc.assert(
      fc.property(currency, cents, cents, cents, (c, a, b, d) => {
        const [x, y, z] = [money(a, c), money(b, c), money(d, c)];
        expect(add(x, y)).toEqual(money(a + b, c));
        expect(add(x, y)).toEqual(add(y, x));
        expect(add(add(x, y), z)).toEqual(add(x, add(y, z)));
      }),
    );
  });

  it('AC-001-17: add refuses to mix currencies', () => {
    expect(() => add(money(100, 'BRL'), money(100, 'USD'))).toThrow();
  });

  it('AC-001-17: rounding is half away from zero', () => {
    expect(roundHalfAwayFromZero(5, 2)).toBe(3); // 2.5
    expect(roundHalfAwayFromZero(-5, 2)).toBe(-3); // -2.5
    expect(roundHalfAwayFromZero(7, 3)).toBe(2); // 2.33
    expect(roundHalfAwayFromZero(-7, 3)).toBe(-2);
    expect(roundHalfAwayFromZero(3, 2)).toBe(2); // 1.5
    expect(roundHalfAwayFromZero(1, 3)).toBe(0);
  });

  it('AC-001-17: rounding lands within half a unit, ties away from zero, symmetric around zero', () => {
    const den = fc.integer({ min: 1, max: 1_000_000 });
    fc.assert(
      fc.property(cents, den, (n, d) => {
        const r = roundHalfAwayFromZero(n, d);
        expect(Number.isSafeInteger(r)).toBe(true);
        const twiceError = Math.abs(2 * n - 2 * r * d); // |n/d − r| × 2d
        expect(twiceError).toBeLessThanOrEqual(d);
        if (twiceError === d) expect(Math.abs(r)).toBeGreaterThan(Math.abs(n / d));
        expect(roundHalfAwayFromZero(-n, d)).toBe(-r || 0);
      }),
    );
  });

  it('AC-001-17: multiplyRate scales by a ratio and rounds once at the end', () => {
    // R$ 100,00/h for 50 min → R$ 83,33
    expect(multiplyRate(money(10000, 'BRL'), 50, 60)).toEqual(money(8333, 'BRL'));
    // 30% commission: keep 7000 of 10000 basis points
    expect(multiplyRate(money(10000, 'BRL'), 10000 - 3000, 10000)).toEqual(money(7000, 'BRL'));
    // US$ 1,00 at 5,4321 → R$ 5,43 (ratio applied to cents, currency set by caller)
    expect(multiplyRate(money(100, 'USD'), 54321, 10000, 'BRL')).toEqual(money(543, 'BRL'));
  });

  it('AC-001-17: multiplyRate keeps the currency and always returns integer cents', () => {
    fc.assert(
      fc.property(
        currency.chain((c) => amount(c)),
        fc.integer({ min: 0, max: 100_000 }),
        fc.integer({ min: 1, max: 100_000 }),
        (m, num, den) => {
          const r = multiplyRate(m, num, den);
          expect(r.currency).toBe(m.currency);
          expect(Number.isSafeInteger(r.cents)).toBe(true);
          expect(r.cents).toBe(roundHalfAwayFromZero(m.cents * num, den));
        },
      ),
    );
  });
});
