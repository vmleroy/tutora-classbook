import { useState } from 'react';
import { View } from 'react-native';
import { Touchable } from './Touchable';

import { Icon } from './Icon';
import { Text } from './Text';

export interface CheckboxProps {
  label: string;
  checked?: boolean;
  onChange?: (checked: boolean) => void;
}

const BOX = 22;

/** Independent yes/no option in a list; the whole 44px row is pressable. */
export function Checkbox({ label, checked, onChange }: CheckboxProps) {
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
      className="min-h-touch flex-row items-center gap-2.5"
    >
      <View
        className={`items-center justify-center rounded-xs border-2 ${on ? 'border-btn-primary bg-btn-primary' : 'border-line-strong bg-surface'}`}
        style={{ width: BOX, height: BOX }}
      >
        {on ? <Icon name="check" size={14} strokeWidth={3} tone="on-btn-primary" /> : null}
      </View>
      <Text variant="body-strong">{label}</Text>
    </Touchable>
  );
}
