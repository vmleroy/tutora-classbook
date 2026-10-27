import { prepareDatabase } from '@/data/db';
import { migrations } from '@/data/migrations';
import { getGroup, listGroups } from '@/data/repos/groupRepo';
import { ensureBuiltinPlaces, getPlace, listPlaces } from '@/data/repos/placeRepo';
import { getStudent, listStudents } from '@/data/repos/studentRepo';
import { BUILTIN_IDS, builtinPlace, type Place, type Student } from '@/domain/places';
import { nodeFiles, tempDatabase } from '@tests/sqliteNode';

import {
  archive,
  linkStudentToPlace,
  removePermanently,
  restore,
  saveGroup,
  savePlace,
  saveStudent,
  type Ctx,
} from './catalog';

const NOW = '2026-10-27T12:00:00.000Z';
let n = 0;

function setup(): Ctx {
  const t = tempDatabase();
  prepareDatabase(t.driver, nodeFiles, migrations);
  ensureBuiltinPlaces(t.db, NOW);
  return { db: t.db, now: NOW, newId: () => `id-${++n}` };
}

const salinha = (over: Partial<Place> = {}) => {
  const {
    id: _id,
    archivedAt: _a,
    ...base
  } = {
    ...builtinPlace('online'),
    builtin: null,
    name: 'Salinha Aprender+',
    kind: 'salinha' as const,
    rateCents: 10000,
    ...over,
  };
  return base;
};

const ana: Omit<Student, 'id' | 'archivedAt'> = {
  name: 'Ana Souza',
  subject: 'Matemática',
  phone: '(11) 90000-0000',
  notes: null,
  color: 'amber',
  rateType: 'hour',
};

function placeId(ctx: Ctx, over: Partial<Place> = {}) {
  const r = savePlace(ctx, salinha(over));
  if (!r.ok) throw new Error(r.error);
  return r.value.id;
}

describe('places', () => {
  it('AC-004-2: built-ins start with no price, can be priced, never renamed or archived', () => {
    const ctx = setup();
    const online = getPlace(ctx.db, BUILTIN_IDS.online)!;
    expect(online.rateCents).toBeNull();
    expect(savePlace(ctx, { ...online, rateCents: 10000, defaultDurationMin: 50 }).ok).toBe(true);
    expect(getPlace(ctx.db, online.id)).toMatchObject({ rateCents: 10000, defaultDurationMin: 50 });
    expect(savePlace(ctx, { ...online, name: 'Zoom' })).toEqual({
      ok: false,
      error: 'builtinFixed',
    });
    expect(archive(ctx, 'place', online.id)).toEqual({ ok: false, error: 'builtinFixed' });
  });

  it('AC-004-3: a place needs a name', () => {
    const ctx = setup();
    expect(savePlace(ctx, salinha({ name: '  ' }))).toEqual({ ok: false, error: 'nameRequired' });
    expect(savePlace(ctx, salinha({ address: 'Rua das Flores, 120' })).ok).toBe(true);
  });

  it('AC-004-9: Casa do Aluno starts with travel "varies"', () => {
    const ctx = setup();
    expect(getPlace(ctx.db, BUILTIN_IDS.student_home)?.travelMode).toBe('varies');
  });
});

describe('students and groups', () => {
  it('AC-004-10: a student is saved with name, subject, phone, charging, color and notes', () => {
    const ctx = setup();
    expect(saveStudent(ctx, { ...ana, name: '' }, [])).toEqual({
      ok: false,
      error: 'nameRequired',
    });
    expect(saveStudent(ctx, ana, [])).toEqual({ ok: false, error: 'placeRequired' });
    const r = saveStudent(ctx, { ...ana, notes: 'ENEM' }, [
      { placeId: BUILTIN_IDS.online, isDefault: false, rateCents: null },
    ]);
    expect(r.ok && getStudent(ctx.db, r.value.id)).toMatchObject({ ...ana, notes: 'ENEM' });
  });

  it("AC-004-12: at each place the student pays the place's price or their own", () => {
    const ctx = setup();
    const r = saveStudent(ctx, ana, [
      { placeId: BUILTIN_IDS.online, isDefault: false, rateCents: 12000 },
      { placeId: BUILTIN_IDS.student_home, isDefault: true, rateCents: null },
    ]);
    expect(r.ok && getStudent(ctx.db, r.value.id)?.links).toEqual([
      { placeId: BUILTIN_IDS.student_home, isDefault: true, rateCents: null },
      { placeId: BUILTIN_IDS.online, isDefault: false, rateCents: 12000 },
    ]);
  });

  it('AC-004-13: at a place that pays, the student keeps no price of their own', () => {
    const ctx = setup();
    const paying = placeId(ctx, { payer: 'place', rateCents: 6000 });
    const r = saveStudent(ctx, ana, [{ placeId: paying, isDefault: true, rateCents: 9000 }]);
    expect(r.ok && getStudent(ctx.db, r.value.id)?.links[0]?.rateCents).toBeNull();
  });

  it('AC-004-16: a group keeps only members linked to its place; at an Escola it has no price', () => {
    const ctx = setup();
    const p = placeId(ctx);
    const s = saveStudent(ctx, ana, [{ placeId: p, isDefault: true, rateCents: null }]);
    const other = saveStudent(ctx, { ...ana, name: 'Bia' }, [
      { placeId: BUILTIN_IDS.online, isDefault: true, rateCents: null },
    ]);
    if (!s.ok || !other.ok) throw new Error();
    const g = saveGroup(ctx, {
      placeId: p,
      name: 'Turma ENEM',
      subject: 'Redação',
      rateCents: 18000,
      memberIds: [s.value.id, other.value.id],
    });
    expect(g.ok && getGroup(ctx.db, g.value.id)).toMatchObject({
      rateCents: 18000,
      memberIds: [s.value.id],
    });
    const school = placeId(ctx, { kind: 'school', name: 'Colégio Santa Clara' });
    const empty = saveGroup(ctx, {
      placeId: school,
      name: '9º B',
      subject: null,
      rateCents: 5000,
      memberIds: [],
    });
    expect(empty.ok && empty.value).toMatchObject({ rateCents: null, memberIds: [] });
  });
});

describe('archive and remove', () => {
  it('AC-004-23: archiving hides an item and keeps it; restoring brings it back', () => {
    const ctx = setup();
    const r = saveStudent(ctx, ana, [
      { placeId: BUILTIN_IDS.online, isDefault: true, rateCents: null },
    ]);
    if (!r.ok) throw new Error();
    archive(ctx, 'student', r.value.id);
    expect(listStudents(ctx.db)).toEqual([]);
    expect(listStudents(ctx.db, true).map((s) => s.name)).toEqual(['Ana Souza']);
    restore(ctx, 'student', r.value.id);
    expect(listStudents(ctx.db).map((s) => s.links.length)).toEqual([1]);
  });

  it('AC-004-24: only an archived item is removed; a place takes its links and groups, not students', () => {
    const ctx = setup();
    const p = placeId(ctx);
    const s = saveStudent(ctx, ana, [{ placeId: p, isDefault: true, rateCents: null }]);
    if (!s.ok) throw new Error();
    saveGroup(ctx, { placeId: p, name: 'Turma', subject: null, rateCents: null, memberIds: [] });
    expect(removePermanently(ctx, 'place', p)).toEqual({ ok: false, error: 'notArchived' });
    archive(ctx, 'place', p);
    expect(removePermanently(ctx, 'place', p)).toEqual({ ok: true, value: null });
    expect(getPlace(ctx.db, p)).toBeUndefined();
    expect(listGroups(ctx.db, { archived: true })).toEqual([]);
    expect(getStudent(ctx.db, s.value.id)).toMatchObject({ name: 'Ana Souza', links: [] });
  });

  it('AC-004-25: archiving a place keeps its students and hides their links until restored', () => {
    const ctx = setup();
    const p = placeId(ctx);
    const s = saveStudent(ctx, ana, [{ placeId: p, isDefault: true, rateCents: 9000 }]);
    if (!s.ok) throw new Error();
    archive(ctx, 'place', p);
    expect(listPlaces(ctx.db).map((x) => x.id)).not.toContain(p);
    expect(getStudent(ctx.db, s.value.id)?.links).toEqual([]);
    restore(ctx, 'place', p);
    expect(getStudent(ctx.db, s.value.id)?.links).toEqual([
      { placeId: p, isDefault: true, rateCents: 9000 },
    ]);
  });

  it('AC-004-17: + Ligar aluno links a student to a place with its price', () => {
    const ctx = setup();
    const p = placeId(ctx);
    const s = saveStudent(ctx, ana, [
      { placeId: BUILTIN_IDS.online, isDefault: true, rateCents: null },
    ]);
    if (!s.ok) throw new Error();
    linkStudentToPlace(ctx, s.value.id, p);
    expect(getStudent(ctx.db, s.value.id)?.links).toContainEqual({
      placeId: p,
      isDefault: false,
      rateCents: null,
    });
    expect(listPlaces(ctx.db).find((x) => x.id === p)?.studentCount).toBe(1);
  });
});
