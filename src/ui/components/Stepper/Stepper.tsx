import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { cn } from '@/ui/cn';
import { Icon } from '@/ui/components/Icon';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';
import { touchSlop } from '@/ui/theme/sizes';

export interface StepperProps {
  value?: number;
  step?: number;
  min?: number;
  max?: number;
  format?: (value: number) => string;
  onChange?: (value: number) => void;
  /** Tapping the value opens exact entry (a money field in a sheet, for prices). */
  onPressValue?: () => void;
  valueLabel?: string;
  decrementLabel?: string;
  incrementLabel?: string;
  size?: 'sm' | 'md';
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const BUTTON = { md: 44, sm: 32 } as const;
const HEIGHT = { md: 50, sm: 40 } as const;

/** Minus / value / plus, for durations and small amounts. */
export function Stepper({
  className,
  value,
  step = 15,
  min = 0,
  max = Number.MAX_SAFE_INTEGER,
  format = String,
  onChange,
  onPressValue,
  valueLabel,
  decrementLabel,
  incrementLabel,
  size = 'md',
}: StepperProps) {
  const { t } = useTranslation();
  const [own, setOwn] = useState(value ?? min);
  const current = value ?? own;
  const go = (next: number) => {
    const clamped = Math.min(max, Math.max(min, next));
    setOwn(clamped);
    onChange?.(clamped);
  };
  const button = (kind: 'minus' | 'plus') => {
    const disabled = kind === 'minus' ? current <= min : current >= max;
    return (
      <Touchable
        accessibilityRole="button"
        accessibilityLabel={
          kind === 'minus'
            ? (decrementLabel ?? t('ui.decrease'))
            : (incrementLabel ?? t('ui.increase'))
        }
        accessibilityState={{ disabled }}
        disabled={disabled}
        hitSlop={touchSlop(BUTTON[size])}
        onPress={() => go(kind === 'minus' ? current - step : current + step)}
        className={cn(
          'items-center justify-center rounded-md bg-surface-sunken',
          disabled && 'opacity-40',
        )}
        style={{ width: BUTTON[size], height: BUTTON[size] }}
      >
        <Icon name={kind} size={18} strokeWidth={2.6} />
      </Touchable>
    );
  };
  return (
    <View
      className={cn(
        'flex-row items-center justify-between rounded-xl border border-line bg-surface px-1',
        className,
      )}
      style={{ height: HEIGHT[size] }}
    >
      {button('minus')}
      {onPressValue ? (
        <Touchable
          accessibilityRole="button"
          accessibilityLabel={valueLabel ? `${valueLabel}, ${format(current)}` : format(current)}
          onPress={onPressValue}
          className="h-full flex-1 items-center justify-center"
        >
          <Text variant="money-md" accessibilityLiveRegion="polite">
            {format(current)}
          </Text>
        </Touchable>
      ) : (
        <Text variant="money-md" accessibilityLiveRegion="polite">
          {format(current)}
        </Text>
      )}
      {button('plus')}
    </View>
  );
}
