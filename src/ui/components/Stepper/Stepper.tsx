import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { Icon } from '@/ui/components/Icon';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';
import { touchSlop } from '@/ui/theme/metrics';

export interface StepperProps {
  value?: number;
  step?: number;
  min?: number;
  max?: number;
  format?: (value: number) => string;
  onChange?: (value: number) => void;
  decrementLabel?: string;
  incrementLabel?: string;
  size?: 'sm' | 'md';
}

const BUTTON = { md: 44, sm: 32 } as const;
const HEIGHT = { md: 50, sm: 40 } as const;

/** Minus / value / plus, for durations and small amounts. */
export function Stepper({
  value,
  step = 15,
  min = 0,
  max = Number.MAX_SAFE_INTEGER,
  format = String,
  onChange,
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
        className={`items-center justify-center rounded-md bg-surface-sunken ${disabled ? 'opacity-40' : ''}`}
        style={{ width: BUTTON[size], height: BUTTON[size] }}
      >
        <Icon name={kind} size={18} strokeWidth={2.6} />
      </Touchable>
    );
  };
  return (
    <View
      className="flex-row items-center justify-between rounded-xl border border-line bg-surface px-1"
      style={{ height: HEIGHT[size] }}
    >
      {button('minus')}
      <Text variant="money-md" accessibilityLiveRegion="polite">
        {format(current)}
      </Text>
      {button('plus')}
    </View>
  );
}
