import { useState } from 'react';
import { Touchable } from './Touchable';

import Animated, { useAnimatedStyle } from 'react-native-reanimated';

import { touchSlop } from '../theme/metrics';
import { useMotion } from '../theme/motion';

export interface SwitchProps {
  value?: boolean;
  onChange?: (value: boolean) => void;
  label: string;
  disabled?: boolean;
}

// Track 52 × 32, knob 26 with 3px inset: the knob travels 20px.
const TRACK = { width: 52, height: 32 };
const KNOB = 26;
const INSET = (TRACK.height - KNOB) / 2;

/** On/off that applies at once. On = main-buttons color, off = line-strong; never green. */
export function Switch({ value, onChange, label, disabled }: SwitchProps) {
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
      className={`justify-center rounded-full ${on ? 'bg-btn-primary' : 'bg-line-strong'} ${disabled ? 'opacity-40' : ''}`}
      style={TRACK}
    >
      <Animated.View
        testID="switch-knob"
        className={`absolute rounded-full ${on ? 'bg-on-btn-primary' : 'bg-surface'}`}
        style={[{ width: KNOB, height: KNOB }, knob]}
      />
    </Touchable>
  );
}
