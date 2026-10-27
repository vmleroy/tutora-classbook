import m0000 from './0000_init.sql';
import m0001 from './0001_places_and_students.sql';
import m0002 from './0002_schedule.sql';
import journal from './meta/_journal.json';

export type Migration = Readonly<{ tag: string; sql: string }>;

// One import per drizzle-kit migration; migrations.test.ts fails if one is missing.
const sqlByTag: Record<string, string> = {
  '0000_init': m0000,
  '0001_places_and_students': m0001,
  '0002_schedule': m0002,
};

export const migrations: readonly Migration[] = journal.entries.map((entry) => ({
  tag: entry.tag,
  sql: sqlByTag[entry.tag] ?? '',
}));
