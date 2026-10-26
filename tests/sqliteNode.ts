import Database from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import type { FileOps, SqlDriver } from '@/data/driver';

/** A real SQLite file in a fresh temp folder, behind the same driver interface the app uses. */
export function tempDatabase(name = 'tutora.db') {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'tutora-'));
  const file = path.join(dir, name);
  const raw = new Database(file);
  const driver: SqlDriver = {
    path: file,
    exec: (sql) => void raw.exec(sql),
    get: <T>(sql: string, ...params: unknown[]) => raw.prepare(sql).get(...params) as T | undefined,
    run: (sql, ...params) => void raw.prepare(sql).run(...params),
  };
  return { dir, file, raw, driver, db: drizzle(raw), close: () => raw.close() };
}

export const nodeFiles: FileOps = {
  exists: (p) => fs.existsSync(p),
};
