import type { BaseSQLiteDatabase } from 'drizzle-orm/sqlite-core';

import { defaultSettings, type Language, type Settings } from '@/domain/settings';

import { settings } from '../schema';

// Both drivers (expo-sqlite in the app, better-sqlite3 in tests) are synchronous.
export type Db = BaseSQLiteDatabase<'sync', unknown>;

// Column names follow the product doc's settings keys.
const keyOf: Record<keyof Settings, string> = {
  language: 'language',
  currency: 'currency',
  themeId: 'theme_id',
  themeMode: 'theme_mode',
  setupDone: 'setup_done',
  customColors: 'custom_colors',
  textSize: 'text_scale',
};

function rows(values: Partial<Settings>, now: string) {
  return Object.entries(values).map(([field, value]) => ({
    key: keyOf[field as keyof Settings],
    value: JSON.stringify(value),
    updatedAt: now,
  }));
}

/** Inserts any setting that is not saved yet; saved values are never touched. */
export function ensureDefaultSettings(db: Db, phoneLanguage: Language, now: string): void {
  db.insert(settings)
    .values(rows(defaultSettings(phoneLanguage), now))
    .onConflictDoNothing()
    .run();
}

export function loadSettings(db: Db): Settings {
  const saved = new Map(
    db
      .select()
      .from(settings)
      .all()
      .map((r) => [r.key, r.value]),
  );
  const fallback = defaultSettings('en');
  return Object.fromEntries(
    (Object.keys(keyOf) as (keyof Settings)[]).map((field) => {
      const raw = saved.get(keyOf[field]);
      return [field, raw === undefined ? fallback[field] : JSON.parse(raw)];
    }),
  ) as Settings;
}

/** The saved language, or undefined on a first run or when settings cannot be read yet. */
export function savedLanguage(db: Db): Language | undefined {
  try {
    const row = db
      .select()
      .from(settings)
      .all()
      .find((r) => r.key === keyOf.language);
    return row ? (JSON.parse(row.value) as Language) : undefined;
  } catch {
    return undefined;
  }
}

export function saveSettings(db: Db, patch: Partial<Settings>, now: string): void {
  for (const row of rows(patch, now)) {
    db.insert(settings)
      .values(row)
      .onConflictDoUpdate({ target: settings.key, set: { value: row.value, updatedAt: now } })
      .run();
  }
}
