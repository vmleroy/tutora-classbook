import { vars } from 'nativewind';
import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { View } from 'react-native';

import { hexToChannels } from '@/domain/theme/channels';
import { resolvePalette, resolveScheme, type ThemeMode } from '@/domain/theme/palette';
import type { Palette, Scheme, ThemeId } from '@/domain/theme/themes';

import { useSystemScheme } from './useSystemScheme';

type ThemeValue = Readonly<{ palette: Palette; scheme: Scheme; textScale: number }>;

const ThemeContext = createContext<ThemeValue | null>(null);

type Props = {
  theme: ThemeId;
  mode: ThemeMode;
  textScale?: number;
  children: ReactNode;
};

/** Sets the resolved palette as CSS variables, so every color class follows it at once. */
export function ThemeProvider({ theme, mode, textScale = 1, children }: Props) {
  const systemScheme = useSystemScheme();
  const scheme = resolveScheme(mode, systemScheme);
  const palette = useMemo(
    () => resolvePalette(theme, mode, systemScheme),
    [theme, mode, systemScheme],
  );
  const style = useMemo(
    () =>
      vars(
        Object.fromEntries(
          Object.entries(palette).map(([token, hex]) => [`--${token}`, hexToChannels(hex)]),
        ),
      ),
    [palette],
  );
  const value = useMemo(() => ({ palette, scheme, textScale }), [palette, scheme, textScale]);

  return (
    <ThemeContext.Provider value={value}>
      <View className="flex-1 bg-background" style={style}>
        {children}
      </View>
    </ThemeContext.Provider>
  );
}

/** Raw palette values, for the few places classes cannot reach (SVG, shadows, status bar). */
export function useTheme(): ThemeValue {
  const value = useContext(ThemeContext);
  if (!value) throw new Error('useTheme must be used inside ThemeProvider');
  return value;
}
