import type { ReactNode } from 'react';
import { View } from 'react-native';

import type { PaletteToken } from '@/domain/theme/palette';

import { Icon, type IconName } from './Icon';
import { Text } from './Text';

type Tone = 'neutral' | 'success' | 'danger' | 'accent' | 'secondary' | 'violet';

export interface BadgeProps {
  tone?: Tone;
  icon?: IconName;
  children: ReactNode;
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
export function Badge({ tone = 'neutral', icon, children }: BadgeProps) {
  return (
    <View
      testID="badge"
      className={`flex-row items-center gap-1 self-start rounded-sm px-2 py-1 ${ground[tone]}`}
    >
      {icon ? <Icon name={icon} size={12} strokeWidth={3} tone={ink[tone]} /> : null}
      <Text variant="badge" numberOfLines={1} className={textClass[tone]}>
        {children}
      </Text>
    </View>
  );
}
