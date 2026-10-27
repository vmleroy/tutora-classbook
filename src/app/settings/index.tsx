import { useTranslation } from 'react-i18next';

import { useAppVersion } from '@/state/appInfo';
import { Card } from '@/ui/components/Card';
import { Screen } from '@/ui/components/Screen';
import { Text } from '@/ui/components/Text';

export default function SettingsScreen() {
  const { t } = useTranslation();
  const { version, build, schemaVersion } = useAppVersion();
  return (
    <Screen testID="settings-screen">
      <Card label={t('settings.about')}>
        <Text variant="row-title">{t('settings.about')}</Text>
        <Text variant="caption" tabular className="text-ink-muted">
          {t('settings.aboutValue', { version, build, schema: schemaVersion })}
        </Text>
      </Card>
    </Screen>
  );
}
