import Database from 'better-sqlite3';

import { MigrationError, prepareDatabase, schemaVersion } from './db';
import { migrations, type Migration } from './migrations';
import { ensureDefaultSettings, loadSettings } from './repos/settingsRepo';
import { nodeFiles, tempDatabase } from '../../tests/sqliteNode';

const NOW = '2026-10-26T20:40:00.000Z';
const v2: Migration = { tag: '9999_test_add_note', sql: 'ALTER TABLE `settings` ADD `note` text;' };
const broken: Migration = { tag: '9999_test_broken', sql: 'ALTER TABLE `nope` ADD `x` text;' };

function atVersion1() {
  const t = tempDatabase();
  prepareDatabase(t.driver, nodeFiles, migrations);
  ensureDefaultSettings(t.db, 'en', NOW);
  return t;
}

describe('prepareDatabase', () => {
  it('AC-001-12: a fresh install gets WAL, foreign keys and the settings table with defaults', () => {
    const t = tempDatabase();
    const result = prepareDatabase(t.driver, nodeFiles, migrations);
    expect(result).toEqual({ schemaVersion: migrations.length, backupPath: null });

    expect(t.raw.pragma('journal_mode', { simple: true })).toBe('wal');
    expect(t.raw.pragma('foreign_keys', { simple: true })).toBe(1);
    expect(
      t.raw.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name='settings'").get(),
    ).toEqual({ name: 'settings' });

    ensureDefaultSettings(t.db, 'pt-BR', NOW);
    expect(loadSettings(t.db)).toEqual({
      language: 'pt-BR',
      currency: 'BRL',
      themeId: 'quadro',
      themeMode: 'system',
      setupDone: false,
      customColors: {},
      textSize: 'normal',
    });
    t.close();
  });

  it('AC-001-12: defaults never overwrite saved settings', () => {
    const t = atVersion1();
    ensureDefaultSettings(t.db, 'pt-BR', NOW);
    expect(loadSettings(t.db).language).toBe('en');
    t.close();
  });

  it('AC-001-12: a fresh install makes no pre-migration copy', () => {
    const t = tempDatabase();
    prepareDatabase(t.driver, nodeFiles, migrations);
    expect(nodeFiles.exists(`${t.dir}/tutora-pre-v1.db`)).toBe(false);
    t.close();
  });

  it('AC-001-13: an older database is copied to tutora-pre-v{n}.db, then migrated in order, keeping data', () => {
    const t = atVersion1();
    const applied: string[] = [];
    const result = prepareDatabase(t.driver, nodeFiles, [...migrations, v2], (m) =>
      applied.push(m.tag),
    );

    const backupPath = `${t.dir}/tutora-pre-v2.db`;
    expect(result).toEqual({ schemaVersion: 2, backupPath });
    expect(applied).toEqual([v2.tag]);
    expect(schemaVersion(t.driver)).toBe(2);
    expect(loadSettings(t.db).language).toBe('en');

    const copy = new Database(backupPath, { readonly: true });
    expect(copy.pragma('user_version', { simple: true })).toBe(1);
    expect(copy.prepare("SELECT value FROM settings WHERE key='language'").get()).toEqual({
      value: '"en"',
    });
    copy.close();
    t.close();
  });

  it('AC-001-13: an up-to-date database opens without a copy', () => {
    const t = atVersion1();
    expect(prepareDatabase(t.driver, nodeFiles, migrations)).toEqual({
      schemaVersion: 1,
      backupPath: null,
    });
    t.close();
  });

  it('AC-001-14: a failed migration rolls back, keeps the copy and reports its path', () => {
    const t = atVersion1();
    let error: unknown;
    try {
      prepareDatabase(t.driver, nodeFiles, [...migrations, v2, broken]);
    } catch (e) {
      error = e;
    }
    expect(error).toBeInstanceOf(MigrationError);
    const failure = error as MigrationError;
    expect(failure.backupPath).toBe(`${t.dir}/tutora-pre-v3.db`);
    expect(failure.failedTag).toBe(broken.tag);
    expect(nodeFiles.exists(failure.backupPath!)).toBe(true);
    // v2 committed, the broken step rolled back.
    expect(schemaVersion(t.driver)).toBe(2);
    expect(loadSettings(t.db).language).toBe('en');
    t.close();
  });

  it('AC-001-14: retrying a failed upgrade keeps the first copy, taken before any step ran', () => {
    const t = atVersion1();
    const upgrade = [...migrations, v2, broken];
    expect(() => prepareDatabase(t.driver, nodeFiles, upgrade)).toThrow(MigrationError);
    expect(schemaVersion(t.driver)).toBe(2);
    expect(() => prepareDatabase(t.driver, nodeFiles, upgrade)).toThrow(MigrationError);

    const copy = new Database(`${t.dir}/tutora-pre-v3.db`, { readonly: true });
    expect(copy.pragma('user_version', { simple: true })).toBe(1);
    copy.close();
    t.close();
  });
});
