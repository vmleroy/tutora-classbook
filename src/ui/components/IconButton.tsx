import { Pressable } from 'react-native';
import { tv } from 'tailwind-variants';

import type { PaletteToken } from '@/domain/theme/palette';

import { opacity } from '../theme/metrics';
import { Icon, type IconName } from './Icon';

export interface IconButtonProps {
  icon: IconName;
  label: string;
  variant?: 'sunken' | 'plain' | 'onPrimary' | 'accent';
  disabled?: boolean;
  onPress?: () => void;
}

const button = tv({
  base: 'h-touch w-touch items-center justify-center rounded-full',
  variants: {
    variant: {
      sunken: 'bg-surface-sunken',
      plain: 'bg-transparent',
      onPrimary: 'bg-on-header/15',
      accent: 'bg-accent',
    },
    disabled: { true: 'opacity-40' },
  },
});

const tone: Record<NonNullable<IconButtonProps['variant']>, PaletteToken> = {
  sunken: 'ink',
  plain: 'ink',
  onPrimary: 'on-header',
  accent: 'on-accent',
};

/** Round 44 px button with one icon; `label` is its accessible name. */
export function IconButton({
  icon,
  label,
  variant = 'sunken',
  disabled,
  onPress,
}: IconButtonProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: !!disabled }}
      disabled={disabled}
      onPress={onPress}
      className={button({ variant, disabled })}
      style={({ pressed }) => (pressed ? { opacity: opacity.pressed } : undefined)}
    >
      <Icon name={icon} size={20} strokeWidth={2.2} tone={tone[variant]} />
    </Pressable>
  );
}
