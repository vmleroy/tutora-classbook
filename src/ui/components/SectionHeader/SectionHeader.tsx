import { View } from 'react-native';

import { Text } from '@/ui/components/Text';

export interface SectionHeaderProps {
  title: string;
  aside?: string;
  asideTone?: 'muted' | 'primary';
}

/** Section title with an optional count or hint on the right. */
export function SectionHeader({ title, aside, asideTone = 'muted' }: SectionHeaderProps) {
  return (
    <View className="flex-row items-baseline justify-between px-0.5">
      <Text variant="section" accessibilityRole="header">
        {title}
      </Text>
      {aside ? (
        <Text
          variant="caption"
          weight={asideTone === 'primary' ? 800 : undefined}
          className={asideTone === 'primary' ? 'text-primary' : 'text-ink-muted'}
        >
          {aside}
        </Text>
      ) : null}
    </View>
  );
}
