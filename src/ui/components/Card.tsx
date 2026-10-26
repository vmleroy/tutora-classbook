import type { ReactNode } from 'react';
import { Platform, View, type ViewStyle } from 'react-native';
import { tv } from 'tailwind-variants';

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

// shadow-card and shadow-raised from the tokens; dark themes drop shadows.
function shadow(elevation: CardProps['elevation'], ink: string): ViewStyle | undefined {
  if (elevation === 'outline') return undefined;
  const raised = elevation === 'raised';
  return Platform.select<ViewStyle>({
    android: { elevation: raised ? 6 : 2, shadowColor: ink },
    default: {
      shadowColor: ink,
      shadowOffset: { width: 0, height: raised ? 6 : 2 },
      shadowOpacity: raised ? 0.08 : 0.05,
      shadowRadius: raised ? 10 : 5,
    },
  });
}

export function Card({ elevation = 'flat', label, children }: CardProps) {
  const { palette, scheme } = useTheme();
  return (
    <View
      accessibilityLabel={label}
      className={card({ elevation })}
      style={scheme === 'light' ? shadow(elevation, palette.ink) : undefined}
    >
      {children}
    </View>
  );
}
