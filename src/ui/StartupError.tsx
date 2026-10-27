import type { TFunction } from 'i18next';
import { View } from 'react-native';

import { Icon } from '@/ui/components/Icon';
import { Screen } from '@/ui/components/Screen';
import { Text } from '@/ui/components/Text';

type Props = { t: TFunction; backupPath: string | null; message: string };

/** Shown instead of the tabs when the database could not be migrated (AC-001-14). */
export function StartupError({ t, backupPath, message }: Props) {
  return (
    <Screen safeTop testID="startup-error">
      <View className="mt-6 h-14 w-14 items-center justify-center rounded-full bg-danger-soft">
        <Icon name="alert" size={28} tone="danger-ink" />
      </View>
      <Text variant="title-lg" accessibilityRole="header">
        {t('startupError.title')}
      </Text>
      {backupPath ? (
        <>
          <Text>{t('startupError.body')}</Text>
          <Text variant="caption" selectable className="text-ink-muted">
            {t('startupError.backupPath', { path: backupPath })}
          </Text>
        </>
      ) : (
        <Text>{t('startupError.noBackup')}</Text>
      )}
      <Text variant="meta" selectable className="text-ink-muted">
        {t('startupError.detail', { message })}
      </Text>
    </Screen>
  );
}
