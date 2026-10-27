import { builtinPlace, type Place } from './places';
import { previewLesson, resolvePrice } from './pricing';

const place = (over: Partial<Place>): Place => ({
  ...builtinPlace('online'),
  id: 'p',
  builtin: null,
  name: 'Salinha Aprender+',
  kind: 'salinha',
  ...over,
});
const hourly = { rateType: 'hour' as const };

describe('pricing', () => {
  it('AC-004-7: the preview shows what a 60-minute lesson earns', () => {
    expect(previewLesson(place({ payer: 'place', rateCents: 6000 }), 60)).toEqual({
      kind: 'placePays',
      earned: 6000,
    });
    expect(previewLesson(place({ rateCents: 10000, commissionBp: 3000 }), 60)).toEqual({
      kind: 'studentPays',
      paid: 10000,
      kept: 7000,
    });
  });

  it("AC-004-18: the student's own price wins, unless the place pays", () => {
    const studentPays = place({ rateCents: 10000 });
    expect(
      resolvePrice(studentPays, { student: hourly, link: { rateCents: 12000 } }),
    ).toMatchObject({ cents: 12000 });
    expect(resolvePrice(studentPays, { student: hourly, link: { rateCents: null } })).toMatchObject(
      { cents: 10000 },
    );
    const placePays = place({ payer: 'place', rateCents: 6000 });
    expect(resolvePrice(placePays, { student: hourly, link: { rateCents: 12000 } })).toMatchObject({
      cents: 6000,
    });
  });

  it("AC-004-19: a group lesson is the group's price; at an Escola the salary covers it", () => {
    expect(resolvePrice(place({}), { group: { rateCents: 18000 } })).toMatchObject({
      cents: 18000,
      rateType: 'lesson',
    });
    expect(resolvePrice(place({ kind: 'school' }), { group: { rateCents: 18000 } })).toEqual({
      kind: 'salary',
    });
  });

  it('AC-004-20: the price says where it came from', () => {
    const p = place({ rateCents: 10000 });
    expect(resolvePrice(p, { student: hourly })).toMatchObject({ source: 'place' });
    expect(resolvePrice(p, { student: hourly, link: { rateCents: 1 } })).toMatchObject({
      source: 'student',
    });
    expect(resolvePrice(p, { group: { rateCents: 1 } })).toMatchObject({ source: 'group' });
  });

  it('AC-004-21: no price anywhere is unset, never zero', () => {
    expect(resolvePrice(builtinPlace('online'), { student: hourly })).toEqual({ kind: 'unset' });
  });

  it("AC-004-22: a place's new price reaches Do local students only", () => {
    const after = place({ rateCents: 15000 });
    expect(resolvePrice(after, { student: hourly, link: { rateCents: null } })).toMatchObject({
      cents: 15000,
    });
    expect(resolvePrice(after, { student: hourly, link: { rateCents: 12000 } })).toMatchObject({
      cents: 12000,
    });
  });
});
