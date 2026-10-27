import { useTranslation } from 'react-i18next';

import { EmptyState } from '@/ui/components/EmptyState';
import { Screen } from '@/ui/components/Screen';

export default function EarningsScreen() {
  const { t } = useTranslation();
  return (
    <Screen testID="earnings-screen">
      <EmptyState icon="wallet" title={t('empty.earnings.title')} body={t('empty.earnings.body')} />
    </Screen>
  );
}
