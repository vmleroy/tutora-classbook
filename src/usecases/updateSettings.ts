import { loadSettings, saveSettings, type Db } from '@/data/repos/settingsRepo';
import type { Settings } from '@/domain/settings';
import type { ThemeId } from '@/domain/theme/colorSets';
import { checkCustomColor, type CustomPart, type Scheme } from '@/domain/theme/palette';

/** Saves a settings change in one transaction and returns what is now stored. */
export function updateSettings(db: Db, patch: Partial<Settings>, now: string): Settings {
  db.transaction((tx) => saveSettings(tx, patch, now));
  return loadSettings(db);
}

export type CustomColorResult = { saved: boolean; ratio: number };

/**
 * Repaints one part. A color that leaves text under 4.5:1 is only saved once the teacher
 * confirms it (AC-002-8); the ratio comes back either way so the screen can show it.
 */
export function setCustomColor(
  db: Db,
  input: { theme: ThemeId; scheme: Scheme; part: CustomPart; hex: string; confirmed: boolean },
  now: string,
): CustomColorResult & { settings: Settings } {
  const { ok, ratio } = checkCustomColor(input.theme, input.scheme, input.part, input.hex);
  const current = loadSettings(db);
  if (!ok && !input.confirmed) return { saved: false, ratio, settings: current };
  const settings = updateSettings(
    db,
    { customColors: { ...current.customColors, [input.part]: input.hex } },
    now,
  );
  return { saved: true, ratio, settings };
}

/** Restaurar cores: back to the theme's own colors. */
export function resetCustomColors(db: Db, now: string): Settings {
  return updateSettings(db, { customColors: {} }, now);
}
