import type { i18n } from 'i18next';
import { create } from 'zustand';

import type { Db } from '@/data/repos/settingsRepo';
import type { Language, Settings } from '@/domain/settings';
import type { CustomPart, Scheme } from '@/domain/theme/palette';
import { createI18n } from '@/i18n';
import { startApp } from '@/usecases/startApp';
import * as settingsUseCases from '@/usecases/updateSettings';

type Failure = { backupPath: string | null; message: string };

type State = {
  status: 'loading' | 'ready' | 'error';
  settings: Settings | null;
  schemaVersion: number;
  failure: Failure | null;
  i18n: i18n | null;
  boot: () => Promise<void>;
  updateSettings: (patch: Partial<Settings>) => void;
  setLanguage: (language: Language) => Promise<void>;
  setCustomColor: (
    part: CustomPart,
    hex: string,
    scheme: Scheme,
    options?: { confirmed?: boolean },
  ) => settingsUseCases.CustomColorResult;
  resetCustomColors: () => void;
};

const now = () => new Date().toISOString();

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
  // Each action writes SQLite first, then updates the store, so the screen follows at once.
  updateSettings: (patch) => {
    set({ settings: settingsUseCases.updateSettings(currentDatabase(), patch, now()) });
  },
  // Saved first, then every mounted text repaints through react-i18next, with no restart.
  setLanguage: async (language) => {
    set({ settings: settingsUseCases.updateSettings(currentDatabase(), { language }, now()) });
    await get().i18n?.changeLanguage(language);
  },
  setCustomColor: (part, hex, scheme, options = {}) => {
    const theme = get().settings?.themeId ?? 'quadro';
    const { settings, ...result } = settingsUseCases.setCustomColor(
      currentDatabase(),
      { theme, scheme, part, hex, confirmed: options.confirmed ?? false },
      now(),
    );
    if (result.saved) set({ settings });
    return result;
  },
  resetCustomColors: () => {
    set({ settings: settingsUseCases.resetCustomColors(currentDatabase(), now()) });
  },
}));

/** Tests only: back to a cold start. */
export function resetSettingsStore() {
  database = null;
  useSettingsStore.setState(initial);
}
