import { useTranslation } from 'react-i18next';
import { Pressable } from 'react-native';

import { opacity } from '../theme/metrics';
import { useTheme } from '../theme/ThemeProvider';
import { shadowStyle } from '../theme/shadow';
import { Icon, type IconName } from './Icon';

export interface FabProps {
  icon?: IconName;
  label?: string;
  onPress?: () => void;
}

/** The floating + on Agenda: 60 px, accent, the only thing in the app that floats. */
export function Fab({ icon = 'plus', label, onPress }: FabProps) {
  const { t } = useTranslation();
  const { setId } = useTheme();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label ?? t('ui.newLesson')}
      onPress={onPress}
      className="h-fab w-fab items-center justify-center rounded-2xl bg-accent"
      style={({ pressed }) => [shadowStyle('fab', setId), pressed && { opacity: opacity.pressed }]}
    >
      <Icon name={icon} size={26} strokeWidth={2.5} tone="on-accent" />
    </Pressable>
  );
}
