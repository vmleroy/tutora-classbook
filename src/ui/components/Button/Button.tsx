import type { ReactNode } from 'react';
import { ActivityIndicator, View } from 'react-native';

import type { PaletteToken as TokenName } from '@/domain/theme/palette';
import { cn, tv } from '@/ui/cn';
import { Icon, type IconName } from '@/ui/components/Icon';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';
import { sizes, touchSlop } from '@/ui/theme/sizes';
import { useTheme } from '@/ui/theme/ThemeProvider';

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
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
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

const MIN_LABEL_SCALE = 0.85;

const heights = { sm: sizes.controlSm, md: sizes.controlMd, lg: sizes.controlLg } as const;

export function Button({
  className,
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
      className={cn(button({ variant, size, fullWidth, inactive }), className)}
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
        {/* A label never wraps: with Large text in a narrow slot it shrinks a little instead. */}
        <Text
          variant={size === 'lg' ? 'section' : 'button'}
          numberOfLines={1}
          adjustsFontSizeToFit
          minimumFontScale={MIN_LABEL_SCALE}
          className={labelClass[variant]}
        >
          {children}
        </Text>
      </View>
    </Touchable>
  );
}
