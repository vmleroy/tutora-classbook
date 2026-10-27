import { Icon } from '@/ui/components/Icon';
import { Touchable } from '@/ui/components/Touchable';
import { size, touchSlop } from '@/ui/theme/metrics';
import { useTheme } from '@/ui/theme/ThemeProvider';
import { typeStyles } from '@/ui/theme/type';

/** Header and tab-bar options shared by every stack, painted from the palette. */
export function useStackOptions() {
  const { palette } = useTheme();
  return {
    headerStyle: { backgroundColor: palette.background },
    headerShadowVisible: false,
    headerTintColor: palette.ink,
    headerTitleStyle: {
      fontFamily: 'Nunito_800ExtraBold',
      fontSize: typeStyles['title-lg'].fontSize,
    },
    contentStyle: { backgroundColor: palette.background },
  };
}

export function HeaderIconButton({
  icon,
  label,
  onPress,
  testID,
}: {
  icon: 'gear' | 'chevron-left';
  label: string;
  onPress: () => void;
  testID?: string;
}) {
  return (
    <Touchable
      testID={testID}
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={onPress}
      hitSlop={touchSlop(size.touch)}
      className="h-touch w-touch items-center justify-center rounded-full"
    >
      <Icon name={icon} tone="ink" />
    </Touchable>
  );
}
