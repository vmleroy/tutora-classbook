import { useState } from 'react';
import { Touchable } from './Touchable';

import { tv } from 'tailwind-variants';

import { size, touchSlop } from '../theme/metrics';
import { Text } from './Text';

export interface ChipProps {
  label: string;
  selected?: boolean;
  onPress?: (selected: boolean) => void;
}

const chip = tv({
  base: 'h-control-sm items-center justify-center rounded-full border px-3',
  variants: {
    on: {
      true: 'border-btn-primary bg-btn-primary',
      false: 'border-line bg-surface',
    },
  },
});

/** Pill-shaped choice for filters and short pick lists; never wraps. */
export function Chip({ label, selected, onPress }: ChipProps) {
  const [own, setOwn] = useState(!!selected);
  const on = selected ?? own;
  return (
    <Touchable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ selected: on }}
      hitSlop={touchSlop(size.controlSm)}
      onPress={() => {
        setOwn(!on);
        onPress?.(!on);
      }}
      className={chip({ on })}
    >
      <Text
        variant="caption"
        weight={700}
        numberOfLines={1}
        className={on ? 'text-on-btn-primary' : 'text-ink'}
      >
        {label}
      </Text>
    </Touchable>
  );
}
