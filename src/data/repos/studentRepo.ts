import { asc, eq, inArray, isNotNull, isNull, sql } from 'drizzle-orm';

import type { PlaceLink, Student } from '@/domain/places';

import { place, student, studentPlace } from '../schema';
import type { Db } from './settingsRepo';

type Row = typeof student.$inferSelect;

function toStudent({ createdAt: _c, updatedAt: _u, color, ...row }: Row): Student {
  return { ...row, color: color as Student['color'] };
}

export type StudentWithLinks = Student & { links: PlaceLink[] };

/** Links to places that are not archived; an archived place's links stay hidden. */
function linksOf(db: Db, ids: readonly string[]): Map<string, PlaceLink[]> {
  const byStudent = new Map<string, PlaceLink[]>();
  if (ids.length === 0) return byStudent;
  const rows = db
    .select({
      studentId: studentPlace.studentId,
      placeId: studentPlace.placeId,
      isDefault: studentPlace.isDefault,
      rateCents: studentPlace.rateCents,
    })
    .from(studentPlace)
    .innerJoin(place, eq(place.id, studentPlace.placeId))
    .where(sql`${inArray(studentPlace.studentId, [...ids])} AND ${isNull(place.archivedAt)}`)
    .orderBy(sql`${studentPlace.isDefault} DESC`, asc(studentPlace.createdAt))
    .all();
  for (const { studentId, ...link } of rows) {
    byStudent.set(studentId, [...(byStudent.get(studentId) ?? []), link]);
  }
  return byStudent;
}

export function listStudents(db: Db, archived = false): StudentWithLinks[] {
  const rows = db
    .select()
    .from(student)
    .where(archived ? isNotNull(student.archivedAt) : isNull(student.archivedAt))
    .orderBy(asc(sql`lower(${student.name})`))
    .all()
    .map(toStudent);
  const links = linksOf(
    db,
    rows.map((s) => s.id),
  );
  return rows.map((s) => ({ ...s, links: links.get(s.id) ?? [] }));
}

export function getStudent(db: Db, id: string): StudentWithLinks | undefined {
  const row = db.select().from(student).where(eq(student.id, id)).get();
  return row && { ...toStudent(row), links: linksOf(db, [id]).get(id) ?? [] };
}

export function studentsAtPlace(db: Db, placeId: string): Student[] {
  return db
    .select({ s: student })
    .from(studentPlace)
    .innerJoin(student, eq(student.id, studentPlace.studentId))
    .where(sql`${studentPlace.placeId} = ${placeId} AND ${isNull(student.archivedAt)}`)
    .orderBy(asc(sql`lower(${student.name})`))
    .all()
    .map(({ s }) => toStudent(s));
}

export function upsertStudent(db: Db, s: Student, now: string): void {
  const { id, ...values } = s;
  db.insert(student)
    .values({ ...s, createdAt: now, updatedAt: now })
    .onConflictDoUpdate({ target: student.id, set: { ...values, updatedAt: now } })
    .run();
}

/** Replaces the student's links to places that are not archived. */
export function replaceLinks(db: Db, studentId: string, links: readonly PlaceLink[], now: string) {
  const hidden = linksOfArchivedPlaces(db, studentId);
  db.delete(studentPlace).where(eq(studentPlace.studentId, studentId)).run();
  const all = [...links, ...hidden.map((l) => ({ ...l, isDefault: false }))];
  if (all.length === 0) return;
  db.insert(studentPlace)
    .values(all.map((l) => ({ ...l, studentId, createdAt: now, updatedAt: now })))
    .run();
}

function linksOfArchivedPlaces(db: Db, studentId: string): PlaceLink[] {
  return db
    .select({
      placeId: studentPlace.placeId,
      isDefault: studentPlace.isDefault,
      rateCents: studentPlace.rateCents,
    })
    .from(studentPlace)
    .innerJoin(place, eq(place.id, studentPlace.placeId))
    .where(sql`${studentPlace.studentId} = ${studentId} AND ${isNotNull(place.archivedAt)}`)
    .all();
}

export function addLink(db: Db, studentId: string, link: PlaceLink, now: string): void {
  db.insert(studentPlace)
    .values({ ...link, studentId, createdAt: now, updatedAt: now })
    .onConflictDoNothing()
    .run();
}

export function setStudentArchived(db: Db, id: string, archivedAt: string | null, now: string) {
  db.update(student).set({ archivedAt, updatedAt: now }).where(eq(student.id, id)).run();
}

export function deleteStudent(db: Db, id: string): void {
  db.delete(student).where(eq(student.id, id)).run();
}
