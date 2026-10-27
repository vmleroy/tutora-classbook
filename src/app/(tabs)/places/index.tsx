import { useTranslation } from 'react-i18next';

import { EmptyState } from '@/ui/components/EmptyState';
import { Screen } from '@/ui/components/Screen';

export default function PlacesScreen() {
  const { t } = useTranslation();
  return (
    <Screen testID="places-screen">
      <EmptyState icon="pin" title={t('empty.places.title')} body={t('empty.places.body')} />
    </Screen>
  );
}
