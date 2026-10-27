import { createInstance, type i18n } from 'i18next';

import type { Language } from './language';
import ptBR from './pt-BR.json';
import { resources } from './resources';

export { firstRunLanguage, startupLanguage, type Language } from './language';

declare module 'i18next' {
  interface CustomTypeOptions {
    resources: { translation: typeof ptBR };
  }
}

export async function createI18n(language: Language): Promise<i18n> {
  const instance = createInstance();
  await instance.init({
    resources,
    lng: language,
    fallbackLng: false,
    interpolation: { escapeValue: false },
    returnNull: false,
  });
  return instance;
}
