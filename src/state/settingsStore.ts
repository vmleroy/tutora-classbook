import type { i18n } from 'i18next';
import { create } from 'zustand';

import type { Db } from '@/data/repos/settingsRepo';
import type { Language, Settings } from '@/domain/settings';
import { createI18n } from '@/i18n';
import { startApp } from '@/usecases/startApp';

type Failure = { backupPath: string | null; message: string };

type State = {
  status: 'loading' | 'ready' | 'error';
  settings: Settings | null;
  schemaVersion: number;
  failure: Failure | null;
  i18n: i18n | null;
  boot: () => Promise<void>;
};

let database: Db | null = null;

/** The open database, for use cases. Only valid once the store is ready. */
export function currentDatabase(): Db {
  if (!database) throw new Error('Database is not open yet');
  return database;
}

const initial = {
  status: 'loading' as const,
  settings: null,
  schemaVersion: 0,
  failure: null,
  i18n: null,
};

export const useSettingsStore = create<State>()((set, get) => ({
  ...initial,
  boot: async () => {
    if (get().status !== 'loading' || get().i18n) return;
    const result = startApp();
    const language: Language = result.ok ? result.settings.language : result.language;
    const i18n = await createI18n(language);
    if (result.ok) {
      database = result.db;
      set({
        status: 'ready',
        settings: result.settings,
        schemaVersion: result.schemaVersion,
        i18n,
      });
    } else {
      set({
        status: 'error',
        failure: { backupPath: result.backupPath, message: result.message },
        i18n,
      });
    }
  },
}));

/** Tests only: back to a cold start. */
export function resetSettingsStore() {
  database = null;
  useSettingsStore.setState(initial);
}
