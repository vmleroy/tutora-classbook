import { afterCommission, lessonValue, type Cents, type RateType } from './money';
import type { Group, Place, PlaceLink, Student } from './places';

export type PriceSource = 'place' | 'student' | 'group';

export type Price =
  | { kind: 'set'; cents: Cents; rateType: RateType; source: PriceSource }
  | { kind: 'unset' }
  | { kind: 'salary' };

/**
 * What a lesson charges, and where the price comes from. A place that pays sets its own
 * rate; otherwise the student's own price there wins over the place's. A group has its own
 * price, and at a school the salary covers it. A missing price is "unset", never zero.
 */
export function resolvePrice(
  place: Place,
  who:
    | { student: Pick<Student, 'rateType'>; link?: Pick<PlaceLink, 'rateCents'> }
    | { group: Pick<Group, 'rateCents'> },
): Price {
  if (place.kind === 'school') return { kind: 'salary' };
  if ('group' in who) {
    return who.group.rateCents === null
      ? { kind: 'unset' }
      : { kind: 'set', cents: who.group.rateCents, rateType: 'lesson', source: 'group' };
  }
  const own = place.payer === 'student' ? (who.link?.rateCents ?? null) : null;
  if (own !== null) {
    return { kind: 'set', cents: own, rateType: who.student.rateType, source: 'student' };
  }
  return place.rateCents === null
    ? { kind: 'unset' }
    : { kind: 'set', cents: place.rateCents, rateType: place.rateType, source: 'place' };
}

export type Preview =
  | { kind: 'placePays'; earned: Cents }
  | { kind: 'studentPays'; paid: Cents; kept: Cents }
  | { kind: 'salary' }
  | { kind: 'unset' };

/** What a lesson of `minutes` at this place earns, for the edit form's preview. */
export function previewLesson(place: Place, minutes: number): Preview {
  if (place.kind === 'school') return { kind: 'salary' };
  if (place.rateCents === null) return { kind: 'unset' };
  const value = lessonValue(place.rateCents, place.rateType, minutes);
  if (place.payer === 'place') return { kind: 'placePays', earned: value };
  return { kind: 'studentPays', paid: value, kept: afterCommission(value, place.commissionBp) };
}
