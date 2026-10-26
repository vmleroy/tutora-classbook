import m0000 from './0000_init.sql';
import journal from './meta/_journal.json';

export type Migration = Readonly<{ tag: string; sql: string }>;

// One import per drizzle-kit migration; migrations.test.ts fails if one is missing.
const sqlByTag: Record<string, string> = {
  '0000_init': m0000,
};

export const migrations: readonly Migration[] = journal.entries.map((entry) => ({
  tag: entry.tag,
  sql: sqlByTag[entry.tag] ?? '',
}));
