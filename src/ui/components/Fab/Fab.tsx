import { useTranslation } from 'react-i18next';

import { cn } from '@/ui/cn';
import { Icon, type IconName } from '@/ui/components/Icon';
import { Touchable } from '@/ui/components/Touchable';
import { shadowStyle } from '@/ui/theme/shadow';
import { useTheme } from '@/ui/theme/ThemeProvider';

export interface FabProps {
  icon?: IconName;
  label?: string;
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

/** The floating + on Agenda: 60 px, accent, the only thing in the app that floats. */
export function Fab({ className, icon = 'plus', label, onPress }: FabProps) {
  const { t } = useTranslation();
  const { setId } = useTheme();
  return (
    <Touchable
      accessibilityRole="button"
      accessibilityLabel={label ?? t('ui.newLesson')}
      onPress={onPress}
      className={cn('h-fab w-fab items-center justify-center rounded-2xl bg-accent', className)}
      style={shadowStyle('fab', setId)}
    >
      <Icon name={icon} size={26} strokeWidth={2.5} tone="on-accent" />
    </Touchable>
  );
}
