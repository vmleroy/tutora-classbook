import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import type { Language } from '@/domain/settings';
import { useAppVersion } from '@/state/appInfo';
import { useSettingsStore } from '@/state/settingsStore';
import { Card } from '@/ui/components/Card';
import { ListGroup } from '@/ui/components/ListGroup';
import { Screen } from '@/ui/components/Screen';
import { SegmentedControl } from '@/ui/components/SegmentedControl';
import { Text } from '@/ui/components/Text';
import { formatDayMonth, formatMoney } from '@/ui/format';

const languages: Language[] = ['pt-BR', 'en'];
// A fixed sample, so each option shows how its dates and amounts are written.
const sampleDate = new Date(2026, 9, 26);
const sampleCents = 123456;

function LanguageRow() {
  const { t } = useTranslation();
  const language = useSettingsStore((s) => s.settings?.language ?? 'pt-BR');
  const setLanguage = useSettingsStore((s) => s.setLanguage);
  return (
    <View className="gap-2 px-4 py-2.5">
      <View>
        <Text variant="body-strong">{t('settings.language')}</Text>
        <Text variant="meta" className="text-ink-muted">
          {t('settings.languageHint')}
        </Text>
      </View>
      <SegmentedControl
        label={t('settings.language')}
        size="sm"
        value={language}
        onChange={(next) => void setLanguage(next)}
        options={languages.map((l) => ({ value: l, label: t(`languages.${l}`) }))}
      />
      <View className="flex-row gap-1 px-1">
        {languages.map((l) => (
          <Text key={l} variant="meta" tabular className="flex-1 text-center text-ink-muted">
            {t('settings.languageSample', {
              lng: l,
              date: formatDayMonth(sampleDate, l),
              money: formatMoney(sampleCents, l),
            })}
          </Text>
        ))}
      </View>
    </View>
  );
}

export default function SettingsScreen() {
  const { t } = useTranslation();
  const { version, build, schemaVersion } = useAppVersion();
  return (
    <Screen testID="settings-screen">
      <ListGroup title={t('settings.general')}>
        <LanguageRow />
      </ListGroup>
      <Card label={t('settings.about')}>
        <Text variant="row-title">{t('settings.about')}</Text>
        <Text variant="caption" tabular className="text-ink-muted">
          {t('settings.aboutValue', { version, build, schema: schemaVersion })}
        </Text>
      </Card>
    </Screen>
  );
}
