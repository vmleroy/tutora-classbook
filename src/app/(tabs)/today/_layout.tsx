import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { IconButton } from '@/ui/components/IconButton';
import { useStackOptions } from '@/ui/navigation';

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
            <IconButton
              icon="plus"
              variant="accent"
              label={t('lesson.new')}
              onPress={() => router.push('/lesson/new')}
            />
          ),
        }}
      />
    </Stack>
  );
}
