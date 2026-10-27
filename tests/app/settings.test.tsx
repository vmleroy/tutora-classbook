import './mockPhone';

import { act, fireEvent, renderRouter, screen } from 'expo-router/testing-library';

import { prepareDatabase } from '@/data/db';
import { migrations } from '@/data/migrations';
import { useSettingsStore } from '@/state/settingsStore';
import { startApp } from '@/usecases/startApp';
import { updateSettings } from '@/usecases/updateSettings';

import { nodeFiles, tempDatabase } from '../sqliteNode';

const plain = (s: string) => s.replace(/\u00a0|\u202f/g, ' ');

describe('Ajustes', () => {
  it('AC-001-22: the Sobre row shows app version, build number and schema version', async () => {
    await renderRouter('./src/app', { initialUrl: '/settings' });
    expect(screen.getByText('Sobre')).toBeTruthy();
    expect(screen.getByText('Versão 1.1.0 · build 7 · banco v3')).toBeTruthy();
  });

  it('AC-003-11: Ajustes opens with Geral, whose first row is Idioma do app with both languages', async () => {
    await renderRouter('./src/app', { initialUrl: '/settings' });
    expect(screen.getByLabelText('Geral')).toBeTruthy();
    expect(screen.getByText('Idioma do app')).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'Português' })).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'English' })).toBeTruthy();
    const samples = screen
      .getAllByText(/^(Datas|Dates) /)
      .map((n) => plain(String(n.props.children)));
    expect(samples).toEqual(['Datas 26/10 · R$ 1.234,56', 'Dates 10/26 · R$1,234.56']);
  });

  it('AC-003-12: switching language repaints at once and is saved', async () => {
    await renderRouter('./src/app', { initialUrl: '/settings' });
    await act(async () => fireEvent.press(screen.getByRole('radio', { name: 'English' })));
    expect(screen.getByText('App language')).toBeTruthy();
    expect(screen.getByText('Version 1.1.0 · build 7 · database v3')).toBeTruthy();
    expect(useSettingsStore.getState().settings?.language).toBe('en');
  });

  it('AC-004-29: Ajustes has Mostrar locais padrão, on by default, and saves the choice', async () => {
    await renderRouter('./src/app', { initialUrl: '/settings' });
    const row = screen.getByRole('switch', { name: 'Mostrar locais padrão' });
    expect(row.props.accessibilityState?.checked ?? row.props['aria-checked']).toBe(true);
    await act(async () => fireEvent.press(row));
    expect(useSettingsStore.getState().settings?.showDefaultPlaces).toBe(false);
  });

  it('AC-003-13: a saved language survives a restart and beats the phone language', () => {
    const t = tempDatabase();
    const now = '2026-10-27T12:00:00.000Z';
    const deps = {
      openDatabase: () => ({ db: t.db, ...prepareDatabase(t.driver, nodeFiles, migrations) }),
      savedLanguage: () => undefined,
      phoneLocales: () => [{ languageCode: 'pt', languageTag: 'pt-BR' }] as never,
      now: () => now,
    };
    const first = startApp(deps);
    if (!first.ok) throw new Error(first.message);
    expect(first.settings.language).toBe('pt-BR');
    updateSettings(first.db, { language: 'en' }, now);
    const restart = startApp(deps);
    expect(restart.ok && restart.settings.language).toBe('en');
  });

  it('AC-003-14: no currency is offered and settings hold no currency', async () => {
    await renderRouter('./src/app', { initialUrl: '/settings' });
    expect(screen.queryByText(/Moeda|Currency|US\$|Cotação/)).toBeNull();
    expect(useSettingsStore.getState().settings).not.toHaveProperty('currency');
  });
});
