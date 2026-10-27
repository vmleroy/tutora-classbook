import type { Language } from '@/domain/settings';

export type { Language };

type PhoneLocale = { languageCode: string | null; languageTag: string };

/** Portuguese from any region opens in pt-BR; every other language in English. */
export function firstRunLanguage(phoneLocales: readonly PhoneLocale[]): Language {
  return phoneLocales[0]?.languageCode === 'pt' ? 'pt-BR' : 'en';
}

/** The saved setting rules once it exists (AC-001-6). */
export function startupLanguage(
  saved: Language | undefined,
  phoneLocales: readonly PhoneLocale[],
): Language {
  return saved ?? firstRunLanguage(phoneLocales);
}
