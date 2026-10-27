import { vars } from 'nativewind';
import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { View } from 'react-native';

import type { TextSize } from '@/domain/settings';
import { hexToChannels } from '@/domain/theme/channels';
import type { ColorSetId, ThemeId } from '@/domain/theme/colorSets';
import {
  resolvePalette,
  resolveScheme,
  type CustomColors,
  type Palette,
  type Scheme,
  type ThemeMode,
} from '@/domain/theme/palette';
import { LARGE_TEXT_SCALE } from '@/ui/theme/typography';

import { useSystemScheme } from './useSystemScheme';

type ThemeValue = Readonly<{
  palette: Palette;
  scheme: Scheme;
  setId: ColorSetId;
  textScale: number;
}>;

const ThemeContext = createContext<ThemeValue | null>(null);

type Props = {
  theme: ThemeId;
  mode: ThemeMode;
  customColors?: CustomColors;
  textSize?: TextSize;
  children: ReactNode;
};

const NO_CUSTOM: CustomColors = {};

/** Sets the resolved palette as CSS variables, so every color class follows it at once. */
export function ThemeProvider({
  theme,
  mode,
  customColors = NO_CUSTOM,
  textSize = 'normal',
  children,
}: Props) {
  const systemScheme = useSystemScheme();
  const scheme = resolveScheme(mode, systemScheme);
  const palette = useMemo(
    () => resolvePalette(theme, mode, systemScheme, customColors),
    [theme, mode, systemScheme, customColors],
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
  const textScale = textSize === 'large' ? LARGE_TEXT_SCALE : 1;
  const setId: ColorSetId = `${theme}-${scheme}`;
  const value = useMemo(
    () => ({ palette, scheme, setId, textScale }),
    [palette, scheme, setId, textScale],
  );

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
