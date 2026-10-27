import { sql } from 'drizzle-orm';
import {
  check,
  index,
  integer,
  primaryKey,
  sqliteTable,
  text,
  uniqueIndex,
} from 'drizzle-orm/sqlite-core';

/** App settings as key → JSON value (docs/product.md, data model). */
export const settings = sqliteTable('settings', {
  key: text('key').primaryKey(),
  value: text('value').notNull(),
  updatedAt: text('updated_at').notNull(),
});

const stamps = {
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull(),
};

/** Where the teacher teaches; two built-ins plus the teacher's own (spec 004). */
export const place = sqliteTable(
  'place',
  {
    id: text('id').primaryKey(),
    builtin: text('builtin', { enum: ['student_home', 'online'] }).unique(),
    name: text('name'),
    address: text('address'),
    kind: text('kind', {
      enum: ['school', 'salinha', 'course', 'private', 'student_home', 'online'],
    }).notNull(),
    payer: text('payer', { enum: ['place', 'student'] }).notNull(),
    rateType: text('rate_type', { enum: ['hour', 'lesson'] }).notNull(),
    rateCents: integer('rate_cents'),
    commissionBp: integer('commission_bp').notNull().default(0),
    payCycle: text('pay_cycle', { enum: ['lesson', 'weekly', 'monthly'] }).notNull(),
    monthlySalaryCents: integer('monthly_salary_cents'),
    defaultDurationMin: integer('default_duration_min').notNull().default(60),
    travelMode: text('travel_mode', { enum: ['varies', 'fixed', 'none'] })
      .notNull()
      .default('none'),
    travelFeeCents: integer('travel_fee_cents').notNull().default(0),
    archivedAt: text('archived_at'),
    ...stamps,
  },
  (t) => [check('place_commission', sql`${t.commissionBp} BETWEEN 0 AND 9000`)],
);

export const student = sqliteTable('student', {
  id: text('id').primaryKey(),
  name: text('name').notNull(),
  subject: text('subject'),
  phone: text('phone'),
  notes: text('notes'),
  color: text('color').notNull(),
  rateType: text('rate_type', { enum: ['hour', 'lesson'] }).notNull(),
  archivedAt: text('archived_at'),
  ...stamps,
});

/** A student's link to a place; rate_cents null = the place's price. */
export const studentPlace = sqliteTable(
  'student_place',
  {
    studentId: text('student_id')
      .notNull()
      .references(() => student.id, { onDelete: 'cascade' }),
    placeId: text('place_id')
      .notNull()
      .references(() => place.id, { onDelete: 'cascade' }),
    isDefault: integer('is_default', { mode: 'boolean' }).notNull().default(false),
    rateCents: integer('rate_cents'),
    ...stamps,
  },
  (t) => [
    primaryKey({ columns: [t.studentId, t.placeId] }),
    index('student_place_place').on(t.placeId),
    uniqueIndex('student_place_one_default')
      .on(t.studentId)
      .where(sql`${t.isDefault} = 1`),
  ],
);

export const groupClass = sqliteTable(
  'group_class',
  {
    id: text('id').primaryKey(),
    placeId: text('place_id')
      .notNull()
      .references(() => place.id, { onDelete: 'cascade' }),
    name: text('name').notNull(),
    subject: text('subject'),
    rateCents: integer('rate_cents'),
    archivedAt: text('archived_at'),
    ...stamps,
  },
  (t) => [index('group_class_place').on(t.placeId)],
);

export const groupMember = sqliteTable(
  'group_member',
  {
    groupId: text('group_id')
      .notNull()
      .references(() => groupClass.id, { onDelete: 'cascade' }),
    studentId: text('student_id')
      .notNull()
      .references(() => student.id, { onDelete: 'cascade' }),
  },
  (t) => [primaryKey({ columns: [t.groupId, t.studentId] })],
);
