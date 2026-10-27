import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { IconButton } from '@/ui/components/IconButton';
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
            <View className="flex-row items-center gap-1">
              <IconButton
                icon="plus"
                variant="accent"
                label={t('lesson.new')}
                onPress={() => router.push('/lesson/new')}
              />
              <HeaderIconButton
                icon="gear"
                testID="settings-button"
                label={t('nav.settings')}
                onPress={() => router.push('/settings')}
              />
            </View>
          ),
        }}
      />
    </Stack>
  );
}
