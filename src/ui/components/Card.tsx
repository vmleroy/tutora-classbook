import type { ReactNode } from 'react';
import { View } from 'react-native';
import { tv } from 'tailwind-variants';

import { shadowStyle } from '../theme/shadow';
import { useTheme } from '../theme/ThemeProvider';

export interface CardProps {
  elevation?: 'flat' | 'raised' | 'outline';
  label?: string;
  children: ReactNode;
}

const card = tv({
  base: 'gap-2.5 rounded-2xl bg-surface p-4',
  variants: { elevation: { flat: '', raised: '', outline: 'border border-line' } },
});

export function Card({ elevation = 'flat', label, children }: CardProps) {
  const { setId } = useTheme();
  const shadow =
    elevation === 'outline'
      ? undefined
      : shadowStyle(elevation === 'raised' ? 'raised' : 'card', setId);
  return (
    <View accessibilityLabel={label} className={card({ elevation })} style={shadow}>
      {children}
    </View>
  );
}
