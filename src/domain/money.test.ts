import * as fc from 'fast-check';

import {
  afterCommission,
  cents,
  lessonValue,
  multiplyRate,
  roundHalfAwayFromZero,
  sum,
  typedCents,
  wholeReais,
} from './money';

describe('money', () => {
  it('AC-001-17: money is integer cents and rounding is half away from zero', () => {
    expect(() => cents(10.5)).toThrow();
    expect(() => cents(Number.NaN)).toThrow();
    expect(roundHalfAwayFromZero(5, 2)).toBe(3);
    expect(roundHalfAwayFromZero(-5, 2)).toBe(-3);
    expect(roundHalfAwayFromZero(7, 3)).toBe(2);
    expect(multiplyRate(10000, 50, 60)).toBe(8333);
  });

  it('AC-003-1: an hourly lesson is price × minutes ÷ 60, rounded once', () => {
    expect(lessonValue(10000, 'hour', 50)).toBe(8333);
    expect(lessonValue(10000, 'hour', 45)).toBe(7500);
    expect(lessonValue(5, 'hour', 30)).toBe(3); // 2,5 centavos → 3
  });

  it('AC-003-2: a per-lesson price is the same whatever the duration', () => {
    for (const minutes of [30, 50, 60, 90])
      expect(lessonValue(12000, 'lesson', minutes)).toBe(12000);
  });

  it('AC-003-3: with a commission the teacher keeps price × (100% − commission), rounded once', () => {
    expect(afterCommission(10000, 3000)).toBe(7000);
    expect(afterCommission(8333, 3000)).toBe(5833); // 5833,1
  });

  it('AC-003-4: a total is the exact sum of stored values, with no drift', () => {
    const value = fc.integer({ min: -10_000_000, max: 10_000_000 });
    fc.assert(
      fc.property(fc.array(value, { maxLength: 500 }), (values) => {
        expect(sum(values)).toBe(values.reduce((a, b) => a + b, 0));
      }),
    );
    expect(sum(Array(1000).fill(8333))).toBe(8_333_000);
  });

  it('AC-003-7: compact amounts are whole reais, half away from zero', () => {
    expect(wholeReais(118050)).toBe(1181);
    expect(wholeReais(118049)).toBe(1180);
    expect(wholeReais(-118050)).toBe(-1181);
  });

  it('AC-003-8: a money field fills from the right as digits are typed', () => {
    let value = 0;
    for (const digit of '12000') value = typedCents(`R$ ${value}${digit}`, value);
    expect(value).toBe(12000);
  });

  it('AC-003-9: only digits count and deleting drops the last digit', () => {
    expect(typedCents('R$ 120,0a', 12000)).toBe(1200);
    expect(typedCents('R$ 120,0', 12000)).toBe(1200);
    expect(typedCents('R$ ,', 1)).toBe(0);
  });

  it('AC-003-10: digits past R$ 99.999,99 are refused', () => {
    expect(typedCents('R$ 99.999,99', 999999)).toBe(9999999);
    expect(typedCents('R$ 99.999,990', 9999999)).toBe(9999999);
    expect(typedCents('R$ 10.000,000', 1000000)).toBe(1000000);
  });
});
