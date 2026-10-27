import type { Currency } from './money';
import type { ThemeId } from './theme/colorSets';
import type { CustomColors, ThemeMode } from './theme/palette';

export type Language = 'pt-BR' | 'en';

export type Settings = Readonly<{
  language: Language;
  currency: Currency;
  themeId: ThemeId;
  themeMode: ThemeMode;
  setupDone: boolean;
  customColors: CustomColors;
  textSize: TextSize;
}>;

export type TextSize = 'normal' | 'large';

/** First-run settings. Language comes from the phone; everything else is fixed. */
export function defaultSettings(language: Language): Settings {
  return {
    language,
    currency: 'BRL',
    themeId: 'quadro',
    themeMode: 'system',
    setupDone: false,
    customColors: {},
    textSize: 'normal',
  };
}
