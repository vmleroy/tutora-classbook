import { act, renderRouter, screen } from 'expo-router/testing-library';
import * as SplashScreen from 'expo-splash-screen';
import { Text, View } from 'react-native';
import { registerCSS, setupAllComponents } from 'react-native-css-interop/test';

import RootLayout from '@/app/_layout';

import { appCss } from '../nativewind';

// --- Phone: fonts load on demand, locale is Portuguese, splash is observable. ---
const mockFonts: { loaded: boolean; resolve: (() => void) | null } = {
  loaded: false,
  resolve: null,
};
jest.mock('expo-font', () => {
  const React = jest.requireActual('react');
  return {
    useFonts: () => {
      const [loaded, setLoaded] = React.useState(mockFonts.loaded);
      React.useEffect(() => {
        mockFonts.resolve = () => {
          mockFonts.loaded = true;
          setLoaded(true);
        };
      }, []);
      return [loaded, null];
    },
  };
});
jest.mock('expo-splash-screen', () => ({
  preventAutoHideAsync: jest.fn(() => Promise.resolve(true)),
  hideAsync: jest.fn(() => Promise.resolve()),
  setOptions: jest.fn(),
}));
jest.mock('expo-localization', () => ({
  getLocales: () => [{ languageCode: 'pt', languageTag: 'pt-BR' }],
}));

// --- Database: a real SQLite file in Node, behind the app's open function. ---
const mockState: { savedMode?: 'dark'; failMigration?: boolean } = {};
jest.mock('@/data/appDatabase', () => {
  const { tempDatabase, nodeFiles } = jest.requireActual('../sqliteNode');
  const { prepareDatabase } = jest.requireActual('@/data/db');
  const { migrations } = jest.requireActual('@/data/migrations');
  const repo = jest.requireActual('@/data/repos/settingsRepo');
  let t: ReturnType<typeof tempDatabase>;
  return {
    openAppDatabase: () => {
      t = tempDatabase();
      prepareDatabase(t.driver, nodeFiles, migrations);
      if (mockState.savedMode) {
        repo.saveSettings(t.db, { language: 'en', themeMode: mockState.savedMode }, 'x');
      }
      if (mockState.failMigration) {
        return prepareDatabase(t.driver, nodeFiles, [
          ...migrations,
          { tag: '0001_broken', sql: 'ALTER TABLE nope ADD x text;' },
        ]);
      }
      return { db: t.db, schemaVersion: migrations.length, backupPath: null };
    },
    languageBeforeMigration: () => (mockState.savedMode ? 'en' : undefined),
  };
});

beforeEach(async () => {
  mockFonts.loaded = false;
  mockFonts.resolve = null;
  delete mockState.savedMode;
  delete mockState.failMigration;
  jest.mocked(SplashScreen.hideAsync).mockClear();
  setupAllComponents();
  registerCSS(await appCss(), { inlineRem: 16, ignorePropertyWarningRegex: ['^--tw-'] });
  const { resetSettingsStore } = jest.requireActual('@/state/settingsStore');
  resetSettingsStore();
});

function Home() {
  return (
    <View testID="home" className="flex-1 bg-background">
      <Text>home</Text>
    </View>
  );
}

const routes = { _layout: RootLayout, index: Home };

describe('start-up', () => {
  it('AC-001-4: the splash stays until fonts, settings and migrations are ready', async () => {
    await renderRouter(routes);
    expect(SplashScreen.preventAutoHideAsync).toHaveBeenCalled();
    // Settings and migrations are done, fonts are not: still nothing on screen.
    expect(screen.queryByTestId('home')).toBeNull();
    expect(SplashScreen.hideAsync).not.toHaveBeenCalled();

    await act(async () => mockFonts.resolve?.());
    expect(screen.getByTestId('home')).toBeTruthy();
    expect(SplashScreen.hideAsync).toHaveBeenCalledTimes(1);
  });

  it('AC-001-4: the first screen already has the saved theme, never the default one', async () => {
    mockState.savedMode = 'dark';
    mockFonts.loaded = true;
    await renderRouter(routes);
    // Quadro-negro dark background on the very first frame shown.
    expect(screen.getByTestId('home')).toHaveStyle({ backgroundColor: 'rgba(20, 28, 25, 1)' });
  });

  it('AC-001-4: on first run the settings come from the phone language', async () => {
    mockFonts.loaded = true;
    await renderRouter(routes);
    const { useSettingsStore } = jest.requireActual('@/state/settingsStore');
    expect(useSettingsStore.getState().settings).toMatchObject({
      language: 'pt-BR',
      themeMode: 'system',
    });
  });

  it('AC-001-14: a failed migration shows the error screen instead of the app', async () => {
    mockState.failMigration = true;
    mockFonts.loaded = true;
    await renderRouter(routes);
    expect(screen.queryByTestId('home')).toBeNull();
    expect(screen.getByText('Não foi possível atualizar seus dados')).toBeTruthy();
    expect(SplashScreen.hideAsync).toHaveBeenCalledTimes(1);
  });
});
