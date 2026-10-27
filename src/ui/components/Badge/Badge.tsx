import type { ReactNode } from 'react';
import { View } from 'react-native';

import type { PaletteToken } from '@/domain/theme/palette';
import { cn } from '@/ui/cn';
import { Icon, type IconName } from '@/ui/components/Icon';
import { Text } from '@/ui/components/Text';

type Tone = 'neutral' | 'success' | 'danger' | 'accent' | 'secondary' | 'violet';

export interface BadgeProps {
  tone?: Tone;
  icon?: IconName;
  children: ReactNode;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const ground: Record<Tone, string> = {
  neutral: 'bg-surface-sunken',
  success: 'bg-success-soft',
  danger: 'bg-danger-soft',
  accent: 'bg-accent-soft',
  secondary: 'bg-secondary-soft',
  violet: 'bg-tag-violet-soft',
};

const ink: Record<Tone, PaletteToken> = {
  neutral: 'ink-muted',
  success: 'success-ink',
  danger: 'danger-ink',
  accent: 'accent-ink',
  secondary: 'secondary-ink',
  violet: 'tag-violet-ink',
};

const textClass: Record<Tone, string> = {
  neutral: 'text-ink-muted',
  success: 'text-success-ink',
  danger: 'text-danger-ink',
  accent: 'text-accent-ink',
  secondary: 'text-secondary-ink',
  violet: 'text-tag-violet-ink',
};

/** A short status word on a tint; the word carries the meaning, color only reinforces it. */
export function Badge({ className, tone = 'neutral', icon, children }: BadgeProps) {
  return (
    <View
      testID="badge"
      className={cn(
        'flex-row items-center gap-1 self-start rounded-sm px-2 py-1',
        ground[tone],
        className,
      )}
    >
      {icon ? <Icon name={icon} size={12} strokeWidth={3} tone={ink[tone]} /> : null}
      <Text variant="badge" numberOfLines={1} className={textClass[tone]}>
        {children}
      </Text>
    </View>
  );
}
