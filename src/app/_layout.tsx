import '../../global.css';

import {
  Nunito_400Regular,
  Nunito_600SemiBold,
  Nunito_700Bold,
  Nunito_800ExtraBold,
} from '@expo-google-fonts/nunito';
import { QueryClientProvider } from '@tanstack/react-query';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { I18nextProvider } from 'react-i18next';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { queryClient } from '@/state/queryClient';
import { useKeepScheduleFilled } from '@/state/schedule';
import { useSettingsStore } from '@/state/settingsStore';
import { StartupError } from '@/ui/StartupError';
import { ThemeProvider } from '@/ui/theme/ThemeProvider';

void SplashScreen.preventAutoHideAsync();

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
              <ScheduleKeeper ready={status === 'ready'} />
              <Stack screenOptions={{ headerShown: false }}>
                <Stack.Screen name="(tabs)" />
                <Stack.Screen name="settings" />
                <Stack.Screen name="catalog" />
                <Stack.Screen name="lesson" />
                <Stack.Screen name="dev/components" />
              </Stack>
            </QueryClientProvider>
          )}
        </ThemeProvider>
      </I18nextProvider>
    </SafeAreaProvider>
  );
}

/** Fills repeating classes 8 weeks ahead at start-up and whenever the app comes back. */
function ScheduleKeeper({ ready }: { ready: boolean }) {
  useKeepScheduleFilled(ready);
  return null;
}
