import { View } from 'react-native';

import { cn } from '@/ui/cn';
import { Text } from '@/ui/components/Text';

export interface SectionHeaderProps {
  title: string;
  aside?: string;
  asideTone?: 'muted' | 'primary';
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

/** Section title with an optional count or hint on the right. */
export function SectionHeader({
  className,
  title,
  aside,
  asideTone = 'muted',
}: SectionHeaderProps) {
  return (
    <View className={cn('flex-row items-baseline justify-between px-0.5', className)}>
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
