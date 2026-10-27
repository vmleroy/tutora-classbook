import fs from 'node:fs';
import path from 'node:path';

import { nodeFiles, tempDatabase } from '@tests/sqliteNode';

import { prepareDatabase, schemaVersion } from './db';
import { migrations } from './migrations';
import journal from './migrations/meta/_journal.json';

// tests/fixtures/migrations/seed-v{n}.sql fills a database at schema version n with typical
// rows; migration n+1 is applied on top of it. Add one for every new schema version.
const fixtures = path.join(__dirname, '@tests/fixtures/migrations');

describe('migrations', () => {
  it('AC-001-15: every drizzle-kit journal entry is bundled, in order', () => {
    expect(migrations.map((m) => m.tag)).toEqual(journal.entries.map((e) => e.tag));
    for (const m of migrations) expect(m.sql.trim().length).toBeGreaterThan(0);
  });

  migrations.forEach((migration, index) => {
    it(`AC-001-15: ${migration.tag} applies to a database seeded at v${index}`, () => {
      const t = tempDatabase();
      prepareDatabase(t.driver, nodeFiles, migrations.slice(0, index));

      // A database at the previous version holds data; v0 is a fresh install.
      const seed = path.join(fixtures, `seed-v${index}.sql`);
      if (index > 0) {
        expect(fs.existsSync(seed)).toBe(true);
        t.raw.exec(fs.readFileSync(seed, 'utf8'));
      }
      const rowsBefore = index > 0 ? t.raw.prepare('SELECT * FROM settings').all() : [];

      prepareDatabase(t.driver, nodeFiles, migrations.slice(0, index + 1));

      expect(schemaVersion(t.driver)).toBe(index + 1);
      expect(t.raw.pragma('foreign_key_check')).toEqual([]);
      expect(t.raw.pragma('integrity_check', { simple: true })).toBe('ok');
      if (index > 0) {
        expect(t.raw.prepare('SELECT key, value FROM settings').all()).toEqual(
          rowsBefore.map((r) => ({
            key: (r as { key: string }).key,
            value: (r as { value: string }).value,
          })),
        );
      }
      t.close();
    });
  });
});
