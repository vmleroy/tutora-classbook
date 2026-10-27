import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { HeaderIconButton, useStackOptions } from '@/ui/navigation';

/** Details and forms for places, students and groups, above the tabs so links cross freely. */
export default function CatalogStack() {
  const { t } = useTranslation();
  const router = useRouter();
  return (
    <Stack
      screenOptions={{
        ...useStackOptions(),
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
  );
}
