import en from './en.json';
import ptBR from './pt-BR.json';

// Same shape, both ways: a key missing from either file fails `npm run typecheck`.
type Shape<T> = T extends string ? string : { [K in keyof T]: Shape<T[K]> };
export const enMatchesPtBR: Shape<typeof ptBR> = en satisfies Shape<typeof ptBR>;
export const ptBRMatchesEn: Shape<typeof en> = ptBR satisfies Shape<typeof en>;

export const resources = {
  'pt-BR': { translation: ptBR },
  en: { translation: en },
} as const;
