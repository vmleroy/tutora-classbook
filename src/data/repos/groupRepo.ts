import { asc, eq, isNotNull, isNull, sql } from 'drizzle-orm';

import type { Group } from '@/domain/places';

import { groupClass, groupMember, place } from '../schema';
import type { Db } from './settingsRepo';

type Row = typeof groupClass.$inferSelect;

function withMembers(db: Db, rows: Row[]): Group[] {
  return rows.map(({ createdAt: _c, updatedAt: _u, ...row }) => ({
    ...row,
    memberIds: db
      .select({ id: groupMember.studentId })
      .from(groupMember)
      .where(eq(groupMember.groupId, row.id))
      .all()
      .map((m) => m.id),
  }));
}

/** Groups at places that are not archived, optionally at one place. */
export function listGroups(db: Db, opts: { placeId?: string; archived?: boolean } = {}): Group[] {
  const rows = db
    .select({ g: groupClass })
    .from(groupClass)
    .innerJoin(place, eq(place.id, groupClass.placeId))
    .where(
      sql`${opts.archived ? isNotNull(groupClass.archivedAt) : isNull(groupClass.archivedAt)}
        AND ${isNull(place.archivedAt)}
        ${opts.placeId ? sql`AND ${groupClass.placeId} = ${opts.placeId}` : sql``}`,
    )
    .orderBy(asc(sql`lower(${groupClass.name})`))
    .all()
    .map(({ g }) => g);
  return withMembers(db, rows);
}

export function getGroup(db: Db, id: string): Group | undefined {
  const row = db.select().from(groupClass).where(eq(groupClass.id, id)).get();
  return row && withMembers(db, [row])[0];
}

export function upsertGroup(db: Db, g: Group, now: string): void {
  const { id, memberIds, ...values } = g;
  db.insert(groupClass)
    .values({ id, ...values, createdAt: now, updatedAt: now })
    .onConflictDoUpdate({ target: groupClass.id, set: { ...values, updatedAt: now } })
    .run();
  db.delete(groupMember).where(eq(groupMember.groupId, id)).run();
  if (memberIds.length > 0) {
    db.insert(groupMember)
      .values(memberIds.map((studentId) => ({ groupId: id, studentId })))
      .run();
  }
}

export function setGroupArchived(db: Db, id: string, archivedAt: string | null, now: string) {
  db.update(groupClass).set({ archivedAt, updatedAt: now }).where(eq(groupClass.id, id)).run();
}

export function deleteGroup(db: Db, id: string): void {
  db.delete(groupClass).where(eq(groupClass.id, id)).run();
}
