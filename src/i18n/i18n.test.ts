import en from './en.json';
import { createI18n, firstRunLanguage, startupLanguage } from './index';
import ptBR from './pt-BR.json';

function keys(obj: object, prefix = ''): string[] {
  return Object.entries(obj).flatMap(([k, v]) =>
    typeof v === 'object' && v !== null ? keys(v, `${prefix}${k}.`) : [`${prefix}${k}`],
  );
}

describe('i18n', () => {
  it('AC-001-5: Portuguese in any region opens in pt-BR on first run', () => {
    for (const tag of ['pt-BR', 'pt-PT', 'pt-AO', 'pt']) {
      expect(firstRunLanguage([{ languageCode: 'pt', languageTag: tag }])).toBe('pt-BR');
    }
  });

  it('AC-001-5: any other phone language opens in English', () => {
    expect(firstRunLanguage([{ languageCode: 'en', languageTag: 'en-US' }])).toBe('en');
    expect(firstRunLanguage([{ languageCode: 'es', languageTag: 'es-AR' }])).toBe('en');
    expect(firstRunLanguage([{ languageCode: 'fr', languageTag: 'fr-FR' }])).toBe('en');
    expect(firstRunLanguage([])).toBe('en');
  });

  it('AC-001-5: only the first preferred language counts', () => {
    expect(
      firstRunLanguage([
        { languageCode: 'en', languageTag: 'en-GB' },
        { languageCode: 'pt', languageTag: 'pt-BR' },
      ]),
    ).toBe('en');
  });

  it('AC-001-6: a saved language wins over the phone language', () => {
    const phone = [{ languageCode: 'pt', languageTag: 'pt-BR' }];
    expect(startupLanguage('en', phone)).toBe('en');
    expect(startupLanguage('pt-BR', [{ languageCode: 'en', languageTag: 'en-US' }])).toBe('pt-BR');
    expect(startupLanguage(undefined, phone)).toBe('pt-BR');
  });

  it('AC-001-7: pt-BR and en have exactly the same keys, none empty', () => {
    expect(keys(en).sort()).toEqual(keys(ptBR).sort());
    for (const k of keys(ptBR)) {
      const get = (o: object) => k.split('.').reduce<unknown>((x, p) => (x as never)[p], o);
      expect([k, get(ptBR)]).not.toEqual([k, '']);
      expect([k, get(en)]).not.toEqual([k, '']);
    }
  });

  it('AC-001-7: strings render in the chosen language', async () => {
    const pt = await createI18n('pt-BR');
    expect(pt.t('tabs.today')).toBe('Hoje');
    const english = await createI18n('en');
    expect(english.t('tabs.today')).toBe('Today');
    expect(english.t('tabs.earnings')).toBe('Earnings');
  });
});
