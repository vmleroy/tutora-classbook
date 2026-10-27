import { useState } from 'react';

import { cn, tv } from '@/ui/cn';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';
import { sizes, touchSlop } from '@/ui/theme/sizes';

export interface ChipProps {
  label: string;
  selected?: boolean;
  onPress?: (selected: boolean) => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
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
export function Chip({ className, label, selected, onPress }: ChipProps) {
  const [own, setOwn] = useState(!!selected);
  const on = selected ?? own;
  return (
    <Touchable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ selected: on }}
      hitSlop={touchSlop(sizes.controlSm)}
      onPress={() => {
        setOwn(!on);
        onPress?.(!on);
      }}
      className={cn(chip({ on }), className)}
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
