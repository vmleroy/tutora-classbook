import { themes, type Palette, type Scheme, type ThemeId } from './themes';

export type ThemeMode = Scheme | 'system';

/** The scheme the app shows: the chosen mode, or the phone's when the mode is "system". */
export function resolveScheme(mode: ThemeMode, systemScheme: Scheme | null | undefined): Scheme {
  return mode === 'system' ? (systemScheme ?? 'light') : mode;
}

export function resolvePalette(
  theme: ThemeId,
  mode: ThemeMode,
  systemScheme: Scheme | null | undefined,
): Palette {
  return themes[theme][resolveScheme(mode, systemScheme)];
}
