import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { HeaderIconButton, useStackOptions } from '@/ui/navigation';

export default function SettingsStack() {
  const { t } = useTranslation();
  const router = useRouter();
  return (
    <Stack screenOptions={useStackOptions()}>
      <Stack.Screen
        name="index"
        options={{
          title: t('settings.title'),
          headerLeft: () => (
            <HeaderIconButton
              icon="chevron-left"
              testID="back-button"
              label={t('nav.back')}
              onPress={() => (router.canGoBack() ? router.back() : router.replace('/today'))}
            />
          ),
        }}
      />
    </Stack>
  );
}
