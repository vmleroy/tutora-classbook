import { sqliteTable, text } from 'drizzle-orm/sqlite-core';

/** App settings as key → JSON value (docs/product.md, data model). */
export const settings = sqliteTable('settings', {
  key: text('key').primaryKey(),
  value: text('value').notNull(),
  updatedAt: text('updated_at').notNull(),
});
