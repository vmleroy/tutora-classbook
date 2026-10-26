import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { HeaderIconButton, useStackOptions } from '@/ui/navigation';

export default function TodayStack() {
  const { t } = useTranslation();
  const router = useRouter();
  return (
    <Stack screenOptions={useStackOptions()}>
      <Stack.Screen
        name="index"
        options={{
          title: t('tabs.today'),
          headerRight: () => (
            <HeaderIconButton
              icon="gear"
              label={t('nav.settings')}
              onPress={() => router.push('/settings')}
            />
          ),
        }}
      />
    </Stack>
  );
}
