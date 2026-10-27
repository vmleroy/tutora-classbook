import { Children, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { View, type LayoutChangeEvent } from 'react-native';
import Animated, { useAnimatedStyle } from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useMotion } from '../theme/motion';
import { useTheme } from '../theme/ThemeProvider';
import { spacing } from '../theme/tokens';
import { Text } from './Text';
import { Touchable } from './Touchable';

export interface BottomSheetProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  children?: ReactNode;
  /** Whether the sheet is up. Defaults to true, so a sheet can be rendered only while needed. */
  open?: boolean;
  /** Tapping the scrim or the close target dismisses the sheet. */
  onClose?: () => void;
}

const GRABBER = { width: 36, height: 4 };

/** The scrim token is #rrggbbaa: its last two digits are the overlay's opacity. */
function alphaOf(hex: string): number {
  return hex.length === 9 ? Math.round((parseInt(hex.slice(7), 16) / 255) * 100) / 100 : 1;
}

/** A panel from the bottom over a scrim, for choices, confirmations and pickers. */
export function BottomSheet({
  title,
  subtitle,
  actions,
  children,
  open = true,
  onClose,
}: BottomSheetProps) {
  const { t } = useTranslation();
  const { palette } = useTheme();
  const insets = useSafeAreaInsets();
  const [height, setHeight] = useState(0);
  const y = useMotion(open ? 0 : height);
  const slide = useAnimatedStyle(() => ({ transform: [{ translateY: y.value }] }));
  if (!open) return null;

  const [cancel, confirm] = Children.toArray(actions);
  return (
    <View className="absolute inset-0 justify-end">
      <Touchable
        testID="sheet-scrim"
        accessibilityRole="button"
        accessibilityLabel={t('ui.close')}
        onPress={onClose}
        pressedFeedback={false}
        className="absolute inset-0 bg-scrim"
        style={{ opacity: alphaOf(palette.scrim) }}
      />
      {/* Animated.View carries only the motion: NativeWind styles plain views. */}
      <Animated.View
        onLayout={(e: LayoutChangeEvent) => setHeight(e.nativeEvent.layout.height)}
        style={slide}
      >
        <View
          testID="sheet"
          accessibilityViewIsModal
          accessibilityLabel={title}
          className="gap-3.5 rounded-t-3xl bg-surface px-5 pt-3"
          style={{ paddingBottom: insets.bottom + spacing['7'] }}
        >
          <View className="self-center rounded-full bg-line-strong" style={GRABBER} />
          <View className="gap-0.5">
            <Text variant="title-lg" accessibilityRole="header">
              {title}
            </Text>
            {subtitle ? (
              <Text variant="caption" className="text-ink-muted">
                {subtitle}
              </Text>
            ) : null}
          </View>
          {children}
          {actions ? (
            <View className="flex-row gap-2.5">
              {confirm ? (
                <>
                  <View className="flex-1">{cancel}</View>
                  <View className="flex-[2]">{confirm}</View>
                </>
              ) : (
                <View className="flex-1">{cancel}</View>
              )}
            </View>
          ) : null}
        </View>
      </Animated.View>
    </View>
  );
}
