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

/** A repeating class (spec 005): wall-clock time in its own zone, filled 8 weeks ahead. */
export const scheduleRule = sqliteTable(
  'schedule_rule',
  {
    id: text('id').primaryKey(),
    studentId: text('student_id').references(() => student.id, { onDelete: 'cascade' }),
    groupId: text('group_id').references(() => groupClass.id, { onDelete: 'cascade' }),
    placeId: text('place_id')
      .notNull()
      .references(() => place.id, { onDelete: 'cascade' }),
    weekday: integer('weekday').notNull(),
    startTime: text('start_time').notNull(),
    durationMin: integer('duration_min').notNull(),
    repeat: text('repeat', { enum: ['weekly', 'biweekly'] }).notNull(),
    startsOn: text('starts_on').notNull(),
    endsOn: text('ends_on'),
    timeZone: text('time_zone').notNull(),
    valueCents: integer('value_cents'),
    travelFeeCents: integer('travel_fee_cents'),
    generatedUntil: text('generated_until').notNull(),
    ...stamps,
  },
  (t) => [check('schedule_rule_who', sql`(${t.studentId} IS NULL) <> (${t.groupId} IS NULL)`)],
);

/** A date removed from a series with "Só esta aula", so filling never brings it back. */
export const ruleSkip = sqliteTable(
  'rule_skip',
  {
    ruleId: text('rule_id')
      .notNull()
      .references(() => scheduleRule.id, { onDelete: 'cascade' }),
    occurrenceDate: text('occurrence_date').notNull(),
  },
  (t) => [primaryKey({ columns: [t.ruleId, t.occurrenceDate] })],
);

/** A class. While scheduled it stores no price: its value follows the current price. */
export const lesson = sqliteTable(
  'lesson',
  {
    id: text('id').primaryKey(),
    ruleId: text('rule_id').references(() => scheduleRule.id, { onDelete: 'set null' }),
    occurrenceDate: text('occurrence_date'),
    studentId: text('student_id').references(() => student.id, { onDelete: 'cascade' }),
    groupId: text('group_id').references(() => groupClass.id, { onDelete: 'cascade' }),
    placeId: text('place_id')
      .notNull()
      .references(() => place.id, { onDelete: 'cascade' }),
    startAt: text('start_at').notNull(),
    plannedMin: integer('planned_min').notNull(),
    status: text('status', { enum: ['scheduled'] })
      .notNull()
      .default('scheduled'),
    valueCents: integer('value_cents'),
    travelFeeCents: integer('travel_fee_cents'),
    isDetached: integer('is_detached', { mode: 'boolean' }).notNull().default(false),
    notes: text('notes'),
    ...stamps,
  },
  (t) => [
    check('lesson_who', sql`(${t.studentId} IS NULL) <> (${t.groupId} IS NULL)`),
    uniqueIndex('lesson_occurrence').on(t.ruleId, t.occurrenceDate),
    index('lesson_start').on(t.startAt),
    index('lesson_student_start').on(t.studentId, t.startAt),
    index('lesson_place_start').on(t.placeId, t.startAt),
  ],
);
