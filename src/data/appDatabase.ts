import { drizzle } from 'drizzle-orm/expo-sqlite';
import { File } from 'expo-file-system';
import { openDatabaseSync } from 'expo-sqlite';

import { prepareDatabase } from './db';
import type { FileOps, SqlDriver } from './driver';
import { migrations } from './migrations';
import { savedLanguage, type Db } from './repos/settingsRepo';

export const DATABASE_NAME = 'tutora.db';

const fileUri = (path: string) => (path.startsWith('file://') ? path : `file://${path}`);

const expoFiles: FileOps = {
  exists: (path) => new File(fileUri(path)).exists,
};

export type OpenedDatabase = { db: Db; schemaVersion: number; backupPath: string | null };

/**
 * Opens the phone's database and migrates it. `onMigrationError` learns the saved language
 * first, so the error screen can speak it even when the schema is broken.
 */
export function openAppDatabase(): OpenedDatabase {
  const native = openDatabaseSync(DATABASE_NAME);
  const driver: SqlDriver = {
    path: native.databasePath,
    exec: (sql) => native.execSync(sql),
    get: <T>(sql: string, ...params: unknown[]) =>
      native.getFirstSync<T>(sql, params as never) ?? undefined,
    run: (sql, ...params) => void native.runSync(sql, params as never),
  };
  const db = drizzle(native);
  const result = prepareDatabase(driver, expoFiles, migrations);
  return { db, ...result };
}

export function languageBeforeMigration(): ReturnType<typeof savedLanguage> {
  return savedLanguage(drizzle(openDatabaseSync(DATABASE_NAME)));
}
