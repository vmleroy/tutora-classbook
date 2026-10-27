import type { ReactNode } from 'react';
import { View } from 'react-native';

import { cn, tv } from '@/ui/cn';
import { shadowStyle } from '@/ui/theme/shadow';
import { useTheme } from '@/ui/theme/ThemeProvider';

export interface CardProps {
  elevation?: 'flat' | 'raised' | 'outline';
  label?: string;
  children: ReactNode;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const card = tv({
  base: 'gap-2.5 rounded-2xl bg-surface p-4',
  variants: { elevation: { flat: '', raised: '', outline: 'border border-line' } },
});

export function Card({ className, elevation = 'flat', label, children }: CardProps) {
  const { setId } = useTheme();
  const shadow =
    elevation === 'outline'
      ? undefined
      : shadowStyle(elevation === 'raised' ? 'raised' : 'card', setId);
  return (
    <View accessibilityLabel={label} className={cn(card({ elevation }), className)} style={shadow}>
      {children}
    </View>
  );
}
