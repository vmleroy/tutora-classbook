import type { FileOps, SqlDriver } from './driver';
import type { Migration } from './migrations';

export class MigrationError extends Error {
  constructor(
    readonly failedTag: string,
    readonly backupPath: string | null,
    readonly cause: unknown,
  ) {
    super(
      `Migration ${failedTag} failed: ${cause instanceof Error ? cause.message : String(cause)}`,
    );
    this.name = 'MigrationError';
  }
}

/** Schema version = number of migrations applied, kept in SQLite's user_version. */
export function schemaVersion(driver: SqlDriver): number {
  return driver.get<{ user_version: number }>('PRAGMA user_version')?.user_version ?? 0;
}

export function backupPathFor(dbPath: string, targetVersion: number): string {
  return `${dbPath.slice(0, dbPath.lastIndexOf('/') + 1)}tutora-pre-v${targetVersion}.db`;
}

/**
 * Sets the pragmas, then brings the schema up to date. Before touching an existing
 * database it writes a consistent copy next to it (constitution rule 3). Migrations only
 * go forward, each in its own transaction.
 */
export function prepareDatabase(
  driver: SqlDriver,
  files: FileOps,
  all: readonly Migration[],
  onApplied?: (migration: Migration) => void,
): { schemaVersion: number; backupPath: string | null } {
  driver.exec('PRAGMA journal_mode = WAL');
  driver.exec('PRAGMA foreign_keys = ON');

  const from = schemaVersion(driver);
  if (from > all.length) {
    throw new Error(`Database is at v${from}, newer than this app (v${all.length})`);
  }
  const pending = all.slice(from);
  if (pending.length === 0) return { schemaVersion: from, backupPath: null };

  let backupPath: string | null = null;
  if (from > 0) {
    backupPath = backupPathFor(driver.path, all.length);
    // A copy already there comes from an earlier failed attempt at this same upgrade and
    // holds the data from before any of it ran, so it is kept, never overwritten.
    if (!files.exists(backupPath)) driver.run('VACUUM INTO ?', backupPath);
  }

  let version = from;
  for (const migration of pending) {
    try {
      driver.exec('BEGIN');
      driver.exec(migration.sql);
      driver.exec(`PRAGMA user_version = ${version + 1}`);
      driver.exec('COMMIT');
    } catch (cause) {
      try {
        driver.exec('ROLLBACK');
      } catch {
        // The failed statement may already have ended the transaction.
      }
      throw new MigrationError(migration.tag, backupPath, cause);
    }
    version += 1;
    onApplied?.(migration);
  }
  return { schemaVersion: version, backupPath };
}
