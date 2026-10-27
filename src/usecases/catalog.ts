// Places, students and groups (spec 004). Every write runs in one transaction.
import * as groups from '@/data/repos/groupRepo';
import * as places from '@/data/repos/placeRepo';
import type { Db } from '@/data/repos/settingsRepo';
import * as students from '@/data/repos/studentRepo';
import {
  checkPlace,
  checkStudent,
  withDefault,
  type Group,
  type Place,
  type PlaceError,
  type PlaceLink,
  type Student,
  type StudentError,
} from '@/domain/places';

export type Ctx = { db: Db; now: string; newId: () => string };
export type Result<T, E extends string> = { ok: true; value: T } | { ok: false; error: E };

const fail = <E extends string>(error: E) => ({ ok: false, error }) as const;

type Draft<T extends { id: string; archivedAt: string | null }> = Omit<T, 'id' | 'archivedAt'> & {
  id?: string;
};

/** Creates or updates a place. A built-in keeps its name and type. */
export function savePlace({ db, now, newId }: Ctx, draft: Draft<Place>): Result<Place, PlaceError> {
  return db.transaction((tx) => {
    const before = draft.id ? places.getPlace(tx, draft.id) : undefined;
    const next: Place = {
      ...draft,
      name: draft.name?.trim() || null,
      id: before?.id ?? newId(),
      builtin: before?.builtin ?? null,
      archivedAt: before?.archivedAt ?? null,
    };
    const error = checkPlace(next, before);
    if (error) return fail(error);
    if (before) places.updatePlace(tx, next, now);
    else places.insertPlace(tx, next, now);
    return { ok: true, value: next };
  });
}

/**
 * Creates or updates a student with their places. The star lands on exactly one place, and
 * a place that pays the teacher keeps no student price (AC-004-13).
 */
export function saveStudent(
  { db, now, newId }: Ctx,
  draft: Draft<Student>,
  links: readonly PlaceLink[],
): Result<Student, StudentError> {
  return db.transaction((tx) => {
    const before = draft.id ? students.getStudent(tx, draft.id) : undefined;
    const next: Student = {
      ...draft,
      name: draft.name.trim(),
      id: before?.id ?? newId(),
      archivedAt: before?.archivedAt ?? null,
    };
    const error = checkStudent(next, links);
    if (error) return fail(error);
    const priced = withDefault(links).map((l) =>
      places.getPlace(tx, l.placeId)?.payer === 'place' ? { ...l, rateCents: null } : l,
    );
    students.upsertStudent(tx, next, now);
    students.replaceLinks(tx, next.id, priced, now);
    return { ok: true, value: next };
  });
}

/** "+ Ligar aluno" on a place: links with the place's price; first place gets the star. */
export function linkStudentToPlace({ db, now }: Ctx, studentId: string, placeId: string): void {
  db.transaction((tx) => {
    const current = students.getStudent(tx, studentId)?.links ?? [];
    students.addLink(
      tx,
      studentId,
      { placeId, isDefault: current.length === 0, rateCents: null },
      now,
    );
  });
}

export type GroupError = 'nameRequired';

/** Members must be linked to the group's place; at a school the salary covers the price. */
export function saveGroup({ db, now, newId }: Ctx, draft: Draft<Group>): Result<Group, GroupError> {
  return db.transaction((tx) => {
    if (!draft.name.trim()) return fail('nameRequired' as const);
    const before = draft.id ? groups.getGroup(tx, draft.id) : undefined;
    const atPlace = new Set(students.studentsAtPlace(tx, draft.placeId).map((s) => s.id));
    const school = places.getPlace(tx, draft.placeId)?.kind === 'school';
    const next: Group = {
      ...draft,
      name: draft.name.trim(),
      id: before?.id ?? newId(),
      rateCents: school ? null : draft.rateCents,
      memberIds: draft.memberIds.filter((id) => atPlace.has(id)),
      archivedAt: before?.archivedAt ?? null,
    };
    groups.upsertGroup(tx, next, now);
    return { ok: true, value: next };
  });
}

export type Kind = 'place' | 'student' | 'group';
export type ArchiveError = 'builtinFixed' | 'notArchived' | 'notFound';

const archivedAtOf: Record<Kind, (db: Db, id: string) => string | null | undefined> = {
  place: (db, id) => places.getPlace(db, id)?.archivedAt,
  student: (db, id) => students.getStudent(db, id)?.archivedAt,
  group: (db, id) => groups.getGroup(db, id)?.archivedAt,
};

const setArchived = {
  place: places.setPlaceArchived,
  student: students.setStudentArchived,
  group: groups.setGroupArchived,
};

/** Arquivar hides an item and keeps everything about it. Built-in places stay. */
export function archive({ db, now }: Ctx, kind: Kind, id: string): Result<null, ArchiveError> {
  return db.transaction((tx) => {
    if (archivedAtOf[kind](tx, id) === undefined) return fail('notFound' as const);
    if (kind === 'place' && places.getPlace(tx, id)?.builtin) return fail('builtinFixed' as const);
    setArchived[kind](tx, id, now, now);
    return { ok: true, value: null };
  });
}

export function restore({ db, now }: Ctx, kind: Kind, id: string): void {
  db.transaction((tx) => setArchived[kind](tx, id, null, now));
}

/**
 * Removes an archived item for good, after the screen's confirmation. A place takes its
 * links and groups with it, never its students.
 */
export function removePermanently({ db }: Ctx, kind: Kind, id: string): Result<null, ArchiveError> {
  return db.transaction((tx) => {
    const archivedAt = archivedAtOf[kind](tx, id);
    if (archivedAt === undefined) return fail('notFound' as const);
    if (archivedAt === null) return fail('notArchived' as const);
    ({ place: places.deletePlace, student: students.deleteStudent, group: groups.deleteGroup })[
      kind
    ](tx, id);
    return { ok: true, value: null };
  });
}
