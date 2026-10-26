import type { Currency } from './money';
import type { ThemeMode } from './theme/palette';
import type { ThemeId } from './theme/themes';

export type Language = 'pt-BR' | 'en';

export type Settings = Readonly<{
  language: Language;
  currency: Currency;
  themeId: ThemeId;
  themeMode: ThemeMode;
  setupDone: boolean;
}>;

/** First-run settings. Language comes from the phone; everything else is fixed. */
export function defaultSettings(language: Language): Settings {
  return { language, currency: 'BRL', themeId: 'quadro', themeMode: 'system', setupDone: false };
}
