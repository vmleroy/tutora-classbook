import type { ReactNode } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { tv } from 'tailwind-variants';
import { Touchable } from './Touchable';

import type { PaletteToken as TokenName } from '@/domain/theme/palette';

import { size as sizes, touchSlop } from '../theme/metrics';
import { useTheme } from '../theme/ThemeProvider';
import { Icon, type IconName } from './Icon';
import { Text } from './Text';

type Variant = 'primary' | 'accent' | 'outline' | 'ghost' | 'danger';

export interface ButtonProps {
  variant?: Variant;
  size?: 'sm' | 'md' | 'lg';
  icon?: IconName;
  fullWidth?: boolean;
  loading?: boolean;
  disabled?: boolean;
  onPress?: () => void;
  children: ReactNode;
}

const button = tv({
  base: 'flex-row items-center justify-center gap-1.5 rounded-lg px-4',
  variants: {
    variant: {
      primary: 'bg-btn-primary',
      accent: 'bg-accent',
      outline: 'border border-line-strong bg-surface',
      ghost: 'bg-transparent',
      danger: 'bg-danger-soft',
    },
    size: { sm: 'h-control-sm px-3.5', md: 'h-control-md', lg: 'h-control-lg rounded-xl' },
    fullWidth: { true: 'w-full' },
    inactive: { true: 'opacity-40' },
  },
});

const labelTone: Record<Variant, TokenName> = {
  primary: 'on-btn-primary',
  accent: 'on-accent',
  outline: 'ink',
  ghost: 'primary',
  danger: 'danger-ink',
};

const labelClass: Record<Variant, string> = {
  primary: 'text-on-btn-primary',
  accent: 'text-on-accent',
  outline: 'text-ink',
  ghost: 'text-primary',
  danger: 'text-danger-ink',
};

const heights = { sm: sizes.controlSm, md: sizes.controlMd, lg: sizes.controlLg } as const;

export function Button({
  variant = 'primary',
  size = 'md',
  icon,
  fullWidth,
  loading,
  disabled,
  onPress,
  children,
}: ButtonProps) {
  const { palette } = useTheme();
  const inactive = !!(disabled || loading);
  return (
    <Touchable
      accessibilityRole="button"
      accessibilityState={{ disabled: inactive, busy: !!loading }}
      disabled={inactive}
      onPress={onPress}
      hitSlop={touchSlop(heights[size])}
      className={button({ variant, size, fullWidth, inactive })}
    >
      {loading ? (
        <ActivityIndicator size="small" color={palette[labelTone[variant]]} />
      ) : icon ? (
        <Icon
          name={icon}
          size={size === 'sm' ? 16 : 18}
          strokeWidth={2.4}
          tone={labelTone[variant]}
        />
      ) : null}
      <View>
        <Text variant={size === 'lg' ? 'section' : 'button'} className={labelClass[variant]}>
          {children}
        </Text>
      </View>
    </Touchable>
  );
}
