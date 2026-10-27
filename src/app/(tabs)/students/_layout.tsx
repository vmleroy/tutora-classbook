import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { IconButton } from '@/ui/components/IconButton';
import { useStackOptions } from '@/ui/navigation';

export default function StudentsStack() {
  const { t } = useTranslation();
  const router = useRouter();
  return (
    <Stack screenOptions={useStackOptions()}>
      <Stack.Screen
        name="index"
        options={{
          title: t('tabs.students'),
          headerRight: () => (
            <IconButton
              icon="plus"
              variant="accent"
              label={t('students.new')}
              onPress={() => router.push('/catalog/student/edit')}
            />
          ),
        }}
      />
    </Stack>
  );
}
