import { useTranslation } from 'react-i18next';

import { EmptyState } from '@/ui/components/EmptyState';
import { Screen } from '@/ui/components/Screen';

export default function StudentsScreen() {
  const { t } = useTranslation();
  return (
    <Screen testID="students-screen">
      <EmptyState icon="users" title={t('empty.students.title')} body={t('empty.students.body')} />
    </Screen>
  );
}
