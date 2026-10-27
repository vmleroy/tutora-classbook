import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { IconButton } from '@/ui/components/IconButton';
import { useStackOptions } from '@/ui/navigation';

export default function PlacesStack() {
  const { t } = useTranslation();
  const router = useRouter();
  return (
    <Stack screenOptions={useStackOptions()}>
      <Stack.Screen
        name="index"
        options={{
          title: t('tabs.places'),
          headerRight: () => (
            <View className="flex-row items-center gap-1">
              <IconButton
                icon="archive"
                variant="plain"
                label={t('catalog.archived')}
                onPress={() => router.push('/catalog/archived')}
              />
              <IconButton
                icon="plus"
                variant="accent"
                label={t('places.new')}
                onPress={() => router.push('/catalog/place/edit')}
              />
            </View>
          ),
        }}
      />
    </Stack>
  );
}
