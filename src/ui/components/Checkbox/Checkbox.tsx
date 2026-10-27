import { useState } from 'react';
import { View } from 'react-native';

import { cn } from '@/ui/cn';
import { Icon } from '@/ui/components/Icon';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';

export interface CheckboxProps {
  label: string;
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const BOX = 22;

/** Independent yes/no option in a list; the whole 44px row is pressable. */
export function Checkbox({ className, label, checked, onChange }: CheckboxProps) {
  const [own, setOwn] = useState(!!checked);
  const on = checked ?? own;
  return (
    <Touchable
      accessibilityRole="checkbox"
      accessibilityLabel={label}
      accessibilityState={{ checked: on }}
      onPress={() => {
        setOwn(!on);
        onChange?.(!on);
      }}
      className={cn('min-h-touch flex-row items-center gap-2.5', className)}
    >
      <View
        className={cn(
          'items-center justify-center rounded-xs border-2',
          on ? 'border-btn-primary bg-btn-primary' : 'border-line-strong bg-surface',
        )}
        style={{ width: BOX, height: BOX }}
      >
        {on ? <Icon name="check" size={14} strokeWidth={3} tone="on-btn-primary" /> : null}
      </View>
      <Text variant="body-strong">{label}</Text>
    </Touchable>
  );
}
