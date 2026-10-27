import { useState, type ReactNode } from 'react';
import { View } from 'react-native';

import { cn } from '@/ui/cn';
import { Icon, type IconName } from '@/ui/components/Icon';
import { Text } from '@/ui/components/Text';

export interface EmptyStateProps {
  icon?: IconName;
  title: string;
  body?: string;
  action?: ReactNode;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const RULE_GAP = 28; // ruled-paper lines every 28px (design system: empty states only)

export function EmptyState({ className, icon = 'calendar', title, body, action }: EmptyStateProps) {
  const [height, setHeight] = useState(0);
  const rules = Math.floor(height / RULE_GAP);
  return (
    <View
      className={cn('items-center gap-2 overflow-hidden rounded-2xl px-5 py-6', className)}
      onLayout={(e) => setHeight(e.nativeEvent.layout.height)}
    >
      {Array.from({ length: rules }, (_, i) => (
        <View
          key={i}
          className="absolute inset-x-0 h-px bg-line-soft"
          style={{ top: (i + 1) * RULE_GAP - 1 }}
        />
      ))}
      <View className="h-14 w-14 items-center justify-center rounded-full bg-primary-soft">
        <Icon name={icon} size={28} tone="primary" />
      </View>
      <Text variant="section" accessibilityRole="header" className="text-center">
        {title}
      </Text>
      {body ? (
        <Text variant="caption" className="max-w-xs text-center text-ink-muted">
          {body}
        </Text>
      ) : null}
      {action ?? null}
    </View>
  );
}
