import type { ThemeId } from './theme/colorSets';
import type { CustomColors, ThemeMode } from './theme/palette';

export type Language = 'pt-BR' | 'en';

export type Settings = Readonly<{
  language: Language;
  themeId: ThemeId;
  themeMode: ThemeMode;
  setupDone: boolean;
  customColors: CustomColors;
  textSize: TextSize;
  /** Whether Casa do Aluno and Online / Remoto show in Locais and the place pickers. */
  showDefaultPlaces: boolean;
}>;

export type TextSize = 'normal' | 'large';

/** First-run settings. Language comes from the phone; everything else is fixed. */
export function defaultSettings(language: Language): Settings {
  return {
    language,
    themeId: 'quadro',
    themeMode: 'system',
    setupDone: false,
    customColors: {},
    textSize: 'normal',
    showDefaultPlaces: true,
  };
}
