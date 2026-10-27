import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { Card } from '@/ui/components/Card';
import { Money, type MoneyProps } from '@/ui/components/Money';
import { ProgressBar } from '@/ui/components/ProgressBar';
import { Text } from '@/ui/components/Text';

export interface EarningsSummaryProps {
  title: string;
  earnedCents: number;
  expectedCents?: number;
  expectedLabel?: string;
  stats?: { label: string; cents: number; tone?: MoneyProps['tone'] }[];
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

/** The money card on Hoje: earned, progress to expected, and up to three compact stats. */
export function EarningsSummary({
  className,
  title,
  earnedCents,
  expectedCents,
  expectedLabel,
  stats,
}: EarningsSummaryProps) {
  const { t } = useTranslation();
  return (
    <Card className={className} elevation="raised" label={title}>
      <View className="flex-row items-baseline justify-between">
        <Text variant="field-label" className="text-ink-muted">
          {title}
        </Text>
        {expectedLabel ? (
          <Text variant="caption" className="text-ink-muted">
            {expectedLabel}
          </Text>
        ) : null}
      </View>
      <Money cents={earnedCents} size="xl" tone="earnings" />
      {expectedCents ? (
        <ProgressBar value={earnedCents / expectedCents} label={t('ui.earnedVsExpected')} />
      ) : null}
      {stats?.length ? (
        <View className="flex-row gap-2 border-t border-line-soft pt-2">
          {stats.slice(0, 3).map((s) => (
            <View key={s.label} className="flex-1">
              <Text variant="meta" className="text-ink-muted">
                {s.label}
              </Text>
              <Money cents={s.cents} size="md" compact tone={s.tone ?? 'default'} />
            </View>
          ))}
        </View>
      ) : null}
    </Card>
  );
}
