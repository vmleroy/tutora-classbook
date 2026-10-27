import { getLocales } from 'expo-localization';

import { languageBeforeMigration, openAppDatabase } from '@/data/appDatabase';
import { MigrationError } from '@/data/db';
import { ensureDefaultSettings, loadSettings, type Db } from '@/data/repos/settingsRepo';
import type { Language, Settings } from '@/domain/settings';
import { firstRunLanguage, startupLanguage } from '@/i18n/language';

export type StartResult =
  | { ok: true; db: Db; settings: Settings; schemaVersion: number }
  | { ok: false; language: Language; backupPath: string | null; message: string };

export type StartDeps = {
  openDatabase: typeof openAppDatabase;
  savedLanguage: typeof languageBeforeMigration;
  phoneLocales: typeof getLocales;
  now: () => string;
};

const defaultDeps: StartDeps = {
  openDatabase: openAppDatabase,
  savedLanguage: languageBeforeMigration,
  phoneLocales: getLocales,
  now: () => new Date().toISOString(),
};

/**
 * Opens and migrates the database, then makes sure every setting exists. First run picks
 * the language from the phone; afterwards the saved setting rules (AC-001-6).
 */
export function startApp(deps: StartDeps = defaultDeps): StartResult {
  const phone = deps.phoneLocales();
  try {
    const opened = deps.openDatabase();
    ensureDefaultSettings(opened.db, firstRunLanguage(phone), deps.now());
    return {
      ok: true,
      db: opened.db,
      settings: loadSettings(opened.db),
      schemaVersion: opened.schemaVersion,
    };
  } catch (error) {
    let language = firstRunLanguage(phone);
    try {
      language = startupLanguage(deps.savedLanguage(), phone);
    } catch {
      // The database cannot even be read; the phone language will do.
    }
    return {
      ok: false,
      language,
      backupPath: error instanceof MigrationError ? error.backupPath : null,
      message: error instanceof Error ? error.message : String(error),
    };
  }
}
