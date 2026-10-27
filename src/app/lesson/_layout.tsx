import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { HeaderIconButton, useStackOptions } from '@/ui/navigation';

/** Nova aula and Editar aula, above the tabs (006 adds the lesson detail). */
export default function LessonStack() {
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
