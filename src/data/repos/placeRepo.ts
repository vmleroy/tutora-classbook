import { and, asc, eq, isNotNull, isNull, sql } from 'drizzle-orm';

import { builtinPlace, type BuiltinPlace, type Place } from '@/domain/places';

import { place, studentPlace } from '../schema';
import type { Db } from './settingsRepo';

type Row = typeof place.$inferSelect;

function toPlace({ createdAt: _c, updatedAt: _u, ...row }: Row): Place {
  return row;
}

export type PlaceRow = Place & { studentCount: number };

/** Built-ins first, then the teacher's places by name; archived ones only when asked. */
export function listPlaces(db: Db, archived = false): PlaceRow[] {
  const count = sql<number>`(SELECT count(*) FROM ${studentPlace} WHERE ${studentPlace.placeId} = ${place.id})`;
  return db
    .select({ row: place, studentCount: count })
    .from(place)
    .where(archived ? isNotNull(place.archivedAt) : isNull(place.archivedAt))
    .orderBy(
      // Casa do Aluno, then Online / Remoto, as in the mockup; then the teacher's by name.
      sql`CASE ${place.builtin} WHEN 'student_home' THEN 0 WHEN 'online' THEN 1 ELSE 2 END`,
      asc(sql`lower(${place.name})`),
    )
    .all()
    .map(({ row, studentCount }) => ({ ...toPlace(row), studentCount }));
}

export function getPlace(db: Db, id: string): Place | undefined {
  const row = db.select().from(place).where(eq(place.id, id)).get();
  return row && toPlace(row);
}

export function insertPlace(db: Db, p: Place, now: string): void {
  db.insert(place)
    .values({ ...p, createdAt: now, updatedAt: now })
    .run();
}

export function updatePlace(db: Db, p: Place, now: string): void {
  const { id, ...values } = p;
  db.update(place)
    .set({ ...values, updatedAt: now })
    .where(eq(place.id, id))
    .run();
}

export function setPlaceArchived(db: Db, id: string, archivedAt: string | null, now: string) {
  db.update(place).set({ archivedAt, updatedAt: now }).where(eq(place.id, id)).run();
}

export function deletePlace(db: Db, id: string): void {
  db.delete(place)
    .where(and(eq(place.id, id), isNull(place.builtin)))
    .run();
}

/** Adds the built-in places that are missing; saved ones are never touched. */
export function ensureBuiltinPlaces(db: Db, now: string): void {
  for (const key of ['student_home', 'online'] satisfies BuiltinPlace[]) {
    db.insert(place)
      .values({ ...builtinPlace(key), createdAt: now, updatedAt: now })
      .onConflictDoNothing()
      .run();
  }
}
