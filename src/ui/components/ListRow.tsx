import { View } from 'react-native';

import type { PaletteToken } from '@/domain/theme/palette';

import { Badge, type BadgeProps } from './Badge';
import { Icon, type IconName } from './Icon';
import { Switch } from './Switch';
import { Text } from './Text';
import { Touchable } from './Touchable';

type IconTone = 'secondary' | 'danger' | 'success' | 'accent' | 'primary';

export interface ListRowProps {
  title: string;
  subtitle?: string;
  icon?: IconName;
  iconTone?: IconTone;
  trailing?: 'chevron' | 'switch' | 'none';
  switchValue?: boolean;
  badge?: { tone: BadgeProps['tone']; label: string };
  onPress?: () => void;
}

const tile: Record<IconTone, string> = {
  secondary: 'bg-secondary-soft',
  danger: 'bg-danger-soft',
  success: 'bg-success-soft',
  accent: 'bg-accent-soft',
  primary: 'bg-primary-soft',
};

const tileInk: Record<IconTone, PaletteToken> = {
  secondary: 'secondary-ink',
  danger: 'danger-ink',
  success: 'success-ink',
  accent: 'accent-ink',
  primary: 'primary',
};

const ROW = 'min-h-control-lg flex-row items-center gap-3 px-4 py-2.5';

/** A settings or navigation row whose subtitle states the current value. */
export function ListRow({
  title,
  subtitle,
  icon,
  iconTone = 'secondary',
  trailing = 'chevron',
  switchValue,
  badge,
  onPress,
}: ListRowProps) {
  const content = (
    <>
      {icon ? (
        <View className={`h-8 w-8 items-center justify-center rounded-md ${tile[iconTone]}`}>
          <Icon name={icon} size={18} tone={tileInk[iconTone]} />
        </View>
      ) : null}
      <View className="min-w-0 flex-1">
        <Text variant="body-strong">{title}</Text>
        {subtitle ? (
          <Text variant="meta" className="text-ink-muted">
            {subtitle}
          </Text>
        ) : null}
      </View>
      {badge ? <Badge tone={badge.tone}>{badge.label}</Badge> : null}
      {trailing === 'chevron' ? (
        <Icon name="chevron-right" size={18} strokeWidth={2.4} tone="ink-muted" />
      ) : null}
    </>
  );
  // A switch row is not pressable as a whole: only the switch is.
  if (trailing === 'switch') {
    return (
      <View className={ROW}>
        {content}
        <Switch label={title} value={switchValue} />
      </View>
    );
  }
  return (
    <Touchable
      accessibilityRole="button"
      accessibilityLabel={subtitle ? `${title}, ${subtitle}` : title}
      onPress={onPress}
      className={ROW}
    >
      {content}
    </Touchable>
  );
}
