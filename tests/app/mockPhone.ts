// Import first in a screen test: a phone with fonts loaded, a Portuguese locale and a
// fresh SQLite database in Node behind the app's open function.
import { registerCSS, setupAllComponents } from 'react-native-css-interop/test';

import { resetSettingsStore } from '@/state/settingsStore';

import { appCss } from '../nativewind';

export const mockPhone = { languageCode: 'pt', languageTag: 'pt-BR' };

jest.mock('expo-font', () => ({ useFonts: () => [true, null] }));
jest.mock('expo-splash-screen', () => ({
  preventAutoHideAsync: jest.fn(() => Promise.resolve(true)),
  hideAsync: jest.fn(() => Promise.resolve()),
}));
jest.mock('expo-localization', () => ({ getLocales: () => [mockPhone] }));
jest.mock('expo-application', () => ({
  nativeApplicationVersion: '1.1.0',
  nativeBuildVersion: '7',
}));
jest.mock('@/data/appDatabase', () => {
  const { tempDatabase, nodeFiles } = jest.requireActual('../sqliteNode');
  const { prepareDatabase } = jest.requireActual('@/data/db');
  const { migrations } = jest.requireActual('@/data/migrations');
  return {
    openAppDatabase: () => {
      const t = tempDatabase();
      return { db: t.db, ...prepareDatabase(t.driver, nodeFiles, migrations) };
    },
    languageBeforeMigration: () => undefined,
  };
});

beforeEach(async () => {
  setupAllComponents();
  registerCSS(await appCss(), { inlineRem: 16, ignorePropertyWarningRegex: ['^--tw-'] });
  resetSettingsStore();
});
