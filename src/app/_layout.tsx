import '../../global.css';

import {
  Nunito_400Regular,
  Nunito_600SemiBold,
  Nunito_700Bold,
  Nunito_800ExtraBold,
} from '@expo-google-fonts/nunito';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { I18nextProvider } from 'react-i18next';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { useSettingsStore } from '@/state/settingsStore';
import { StartupError } from '@/ui/StartupError';
import { ThemeProvider } from '@/ui/theme/ThemeProvider';

void SplashScreen.preventAutoHideAsync();

const queryClient = new QueryClient();

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts({
    Nunito_400Regular,
    Nunito_600SemiBold,
    Nunito_700Bold,
    Nunito_800ExtraBold,
  });
  const { status, settings, failure, i18n, boot } = useSettingsStore();

  useEffect(() => {
    void boot();
  }, [boot]);

  const ready = (fontsLoaded || !!fontError) && status !== 'loading' && !!i18n;
  useEffect(() => {
    if (ready) void SplashScreen.hideAsync();
  }, [ready]);

  // The splash covers everything until the first frame can be drawn in the right theme.
  if (!ready || !i18n) return null;

  return (
    <SafeAreaProvider>
      <I18nextProvider i18n={i18n}>
        <ThemeProvider
          theme={settings?.themeId ?? 'quadro'}
          mode={settings?.themeMode ?? 'system'}
          customColors={settings?.customColors}
          textSize={settings?.textSize}
        >
          {status === 'error' && failure ? (
            <StartupError t={i18n.t} backupPath={failure.backupPath} message={failure.message} />
          ) : (
            <QueryClientProvider client={queryClient}>
              <Stack screenOptions={{ headerShown: false }}>
                <Stack.Screen name="(tabs)" />
                <Stack.Screen name="settings" />
              </Stack>
            </QueryClientProvider>
          )}
        </ThemeProvider>
      </I18nextProvider>
    </SafeAreaProvider>
  );
}
