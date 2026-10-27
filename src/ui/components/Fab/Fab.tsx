import { useTranslation } from 'react-i18next';

import { Icon, type IconName } from '@/ui/components/Icon';
import { Touchable } from '@/ui/components/Touchable';
import { shadowStyle } from '@/ui/theme/shadow';
import { useTheme } from '@/ui/theme/ThemeProvider';

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
    <Touchable
      accessibilityRole="button"
      accessibilityLabel={label ?? t('ui.newLesson')}
      onPress={onPress}
      className="h-fab w-fab items-center justify-center rounded-2xl bg-accent"
      style={shadowStyle('fab', setId)}
    >
      <Icon name={icon} size={26} strokeWidth={2.5} tone="on-accent" />
    </Touchable>
  );
}
