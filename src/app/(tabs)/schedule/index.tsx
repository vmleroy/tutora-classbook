import { useTranslation } from 'react-i18next';

import { EmptyState } from '@/ui/components/EmptyState';
import { Screen } from '@/ui/components/Screen';

export default function ScheduleScreen() {
  const { t } = useTranslation();
  return (
    <Screen testID="schedule-screen">
      <EmptyState
        icon="calendar"
        title={t('empty.schedule.title')}
        body={t('empty.schedule.body')}
      />
    </Screen>
  );
}
