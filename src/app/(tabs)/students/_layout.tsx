import { Stack } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { useStackOptions } from '@/ui/navigation';

export default function StudentsStack() {
  const { t } = useTranslation();
  return (
    <Stack screenOptions={useStackOptions()}>
      <Stack.Screen name="index" options={{ title: t('tabs.students') }} />
    </Stack>
  );
}
