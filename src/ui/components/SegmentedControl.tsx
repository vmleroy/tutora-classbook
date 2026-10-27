import { useState } from 'react';
import { View, type LayoutChangeEvent } from 'react-native';
import Animated, { useAnimatedStyle } from 'react-native-reanimated';
import { Touchable } from './Touchable';

import { useMotion } from '../theme/motion';
import { shadowStyle } from '../theme/shadow';
import { size as sizes, touchSlop } from '../theme/metrics';
import { useTheme } from '../theme/ThemeProvider';
import { Text } from './Text';

export interface SegmentedControlProps<T extends string = string> {
  options: { value: T; label: string }[];
  value?: T;
  onChange?: (value: T) => void;
  label: string;
  size?: 'sm' | 'md';
}

/** Two to four exclusive options; the selected one is a surface thumb with primary text. */
export function SegmentedControl<T extends string = string>({
  options,
  value,
  onChange,
  label,
  size = 'md',
}: SegmentedControlProps<T>) {
  const { setId } = useTheme();
  const [own, setOwn] = useState<T | undefined>(value ?? options[0]?.value);
  const selected = value ?? own;
  const index = Math.max(
    0,
    options.findIndex((o) => o.value === selected),
  );
  const [width, setWidth] = useState(0);
  const height = size === 'sm' ? sizes.controlSm : sizes.touch;

  // Thumb slides between equal-width segments; the track has 4px padding and 4px gaps.
  const gap = 4;
  const segment = width > 0 ? (width - gap * 2 - gap * (options.length - 1)) / options.length : 0;
  const x = useMotion(gap + index * (segment + gap));
  const thumb = useAnimatedStyle(() => ({ transform: [{ translateX: x.value }] }));

  return (
    <View
      accessibilityRole="radiogroup"
      accessibilityLabel={label}
      onLayout={(e: LayoutChangeEvent) => setWidth(e.nativeEvent.layout.width)}
      className="flex-row gap-1 rounded-md bg-surface-sunken p-1"
    >
      {segment > 0 ? (
        <Animated.View
          pointerEvents="none"
          className="absolute top-1 rounded-sm bg-surface"
          style={[{ width: segment, height }, shadowStyle('thumb', setId), thumb]}
        />
      ) : null}
      {options.map((option, i) => {
        const on = i === index;
        return (
          <Touchable
            key={option.value}
            accessibilityRole="radio"
            accessibilityLabel={option.label}
            accessibilityState={{ checked: on }}
            hitSlop={touchSlop(height)}
            onPress={() => {
              setOwn(option.value);
              onChange?.(option.value);
            }}
            className={`flex-1 items-center justify-center rounded-sm ${size === 'sm' ? 'h-control-sm' : 'h-touch'} ${on && segment === 0 ? 'bg-surface' : ''}`}
          >
            <Text
              variant={size === 'sm' ? 'caption' : 'row-title'}
              weight={on ? 800 : 700}
              className={on ? 'text-btn-primary' : 'text-ink-muted'}
            >
              {option.label}
            </Text>
          </Touchable>
        );
      })}
    </View>
  );
}
