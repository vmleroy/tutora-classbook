import type { Cents, RateType } from './money';

export type PlaceKind = 'school' | 'salinha' | 'course' | 'private' | 'student_home' | 'online';
export type BuiltinPlace = 'student_home' | 'online';
export type Payer = 'place' | 'student';
export type PayCycle = 'lesson' | 'weekly' | 'monthly';
export type TravelMode = 'varies' | 'fixed' | 'none';
/** The design system's color tags, stored by name. */
export type TagColor = 'amber' | 'green' | 'blue' | 'violet' | 'orange' | 'red' | 'teal';

export type Place = Readonly<{
  id: string;
  builtin: BuiltinPlace | null;
  /** null for built-ins: their name comes from the app language. */
  name: string | null;
  address: string | null;
  kind: PlaceKind;
  payer: Payer;
  rateType: RateType;
  /** null = no price set yet. */
  rateCents: Cents | null;
  /** What the place keeps when the student pays, in basis points (30% = 3000). */
  commissionBp: number;
  payCycle: PayCycle;
  monthlySalaryCents: Cents | null;
  defaultDurationMin: number;
  travelMode: TravelMode;
  travelFeeCents: Cents;
  archivedAt: string | null;
}>;

export type Student = Readonly<{
  id: string;
  name: string;
  subject: string | null;
  phone: string | null;
  notes: string | null;
  color: TagColor;
  rateType: RateType;
  archivedAt: string | null;
}>;

/** A student's link to a place; `rateCents` null = the place's price ("Do local"). */
export type PlaceLink = Readonly<{ placeId: string; isDefault: boolean; rateCents: Cents | null }>;

export type Group = Readonly<{
  id: string;
  placeId: string;
  name: string;
  subject: string | null;
  rateCents: Cents | null;
  memberIds: readonly string[];
  archivedAt: string | null;
}>;

export const DEFAULT_DURATION_MIN = 60;
export const MAX_COMMISSION_BP = 9000;
export const BUILTIN_IDS: Record<BuiltinPlace, string> = {
  student_home: '00000000-0000-7000-8000-000000000001',
  online: '00000000-0000-7000-8000-000000000002',
};

export const tagColors: readonly TagColor[] = [
  'amber',
  'green',
  'blue',
  'violet',
  'orange',
  'red',
  'teal',
];

/** A color tag picked from `random` in [0, 1), so new students don't all look alike. */
export function randomTag(random: number): TagColor {
  return tagColors[Math.floor(random * tagColors.length)] ?? 'amber';
}

export const ownKinds = ['school', 'salinha', 'course', 'private'] as const;

/** Escola and Cursinho count as school and formal job in Ganhos; the rest as private lessons. */
export function earningsType(kind: PlaceKind): 'job' | 'private' {
  return kind === 'school' || kind === 'course' ? 'job' : 'private';
}

/** How a place pays: a monthly salary, a choice of who pays, or always the student. */
export function payMode(kind: PlaceKind): 'salary' | 'choose' | 'student' {
  if (kind === 'school') return 'salary';
  return kind === 'salinha' || kind === 'course' || kind === 'private' ? 'choose' : 'student';
}

/** A built-in place as it is first created: no price until the teacher sets one. */
export function builtinPlace(builtin: BuiltinPlace): Place {
  return {
    id: BUILTIN_IDS[builtin],
    builtin,
    name: null,
    address: null,
    kind: builtin,
    payer: 'student',
    rateType: 'hour',
    rateCents: null,
    commissionBp: 0,
    payCycle: 'lesson',
    monthlySalaryCents: null,
    defaultDurationMin: DEFAULT_DURATION_MIN,
    travelMode: builtin === 'student_home' ? 'varies' : 'none',
    travelFeeCents: 0,
    archivedAt: null,
  };
}

export type PlaceError = 'nameRequired' | 'builtinFixed' | 'commissionRange';

export function checkPlace(place: Place, before?: Place): PlaceError | null {
  if (before?.builtin && (place.name !== null || place.kind !== before.kind)) return 'builtinFixed';
  if (!place.builtin && !place.name?.trim()) return 'nameRequired';
  if (place.commissionBp < 0 || place.commissionBp > MAX_COMMISSION_BP) return 'commissionRange';
  return null;
}

export type StudentError = 'nameRequired' | 'placeRequired';

export function checkStudent(student: Student, links: readonly PlaceLink[]): StudentError | null {
  if (!student.name.trim()) return 'nameRequired';
  if (links.length === 0) return 'placeRequired';
  return null;
}

/** Exactly one starred place: the starred one if any, else the first. */
export function withDefault(links: readonly PlaceLink[]): PlaceLink[] {
  const star = links.find((l) => l.isDefault)?.placeId ?? links[0]?.placeId;
  return links.map((l) => ({ ...l, isDefault: l.placeId === star }));
}

/** Unlinks a place; if it was the default, the star moves to the next one. */
export function unlink(links: readonly PlaceLink[], placeId: string): PlaceLink[] {
  return withDefault(links.filter((l) => l.placeId !== placeId));
}
