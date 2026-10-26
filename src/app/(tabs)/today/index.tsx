import { useTranslation } from 'react-i18next';

import { EmptyState } from '@/ui/components/EmptyState';
import { Screen } from '@/ui/components/Screen';

export default function TodayScreen() {
  const { t } = useTranslation();
  return (
    <Screen testID="today-screen">
      <EmptyState icon="calendar" title={t('empty.today.title')} body={t('empty.today.body')} />
    </Screen>
  );
}
