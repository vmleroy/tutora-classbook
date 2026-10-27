import { useState } from 'react';
import { View } from 'react-native';
import Animated, { useAnimatedStyle } from 'react-native-reanimated';

import { cn } from '@/ui/cn';
import { Touchable } from '@/ui/components/Touchable';
import { useMotion } from '@/ui/theme/motion';
import { touchSlop } from '@/ui/theme/sizes';

export interface SwitchProps {
  value?: boolean;
  onChange?: (value: boolean) => void;
  label: string;
  disabled?: boolean;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

// Track 52 × 32, knob 26 with 3px inset: the knob travels 20px.
const TRACK = { width: 52, height: 32 };
const KNOB = 26;
const INSET = (TRACK.height - KNOB) / 2;

/** On/off that applies at once. On = main-buttons color, off = line-strong; never green. */
export function Switch({ className, value, onChange, label, disabled }: SwitchProps) {
  const [own, setOwn] = useState(!!value);
  const on = value ?? own;
  const x = useMotion(on ? TRACK.width - KNOB - INSET : INSET);
  const knob = useAnimatedStyle(() => ({ transform: [{ translateX: x.value }] }));
  return (
    <Touchable
      accessibilityRole="switch"
      accessibilityLabel={label}
      accessibilityState={{ checked: on, disabled: !!disabled }}
      disabled={disabled}
      hitSlop={touchSlop(TRACK.height)}
      onPress={() => {
        setOwn(!on);
        onChange?.(!on);
      }}
      className={cn(
        'justify-center rounded-full',
        on ? 'bg-btn-primary' : 'bg-line-strong',
        disabled && 'opacity-40',
        className,
      )}
      style={TRACK}
    >
      <Animated.View style={[{ position: 'absolute', width: KNOB, height: KNOB }, knob]}>
        <View
          testID="switch-knob"
          className={cn('flex-1 rounded-full', on ? 'bg-on-btn-primary' : 'bg-surface')}
        />
      </Animated.View>
    </Touchable>
  );
}
