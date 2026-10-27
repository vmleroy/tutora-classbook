import { View } from 'react-native';

import type { PaletteToken } from '@/domain/theme/palette';
import { cn } from '@/ui/cn';
import { Avatar } from '@/ui/components/Avatar';
import { Badge, type BadgeProps } from '@/ui/components/Badge';
import { Icon, type IconName } from '@/ui/components/Icon';
import { Switch } from '@/ui/components/Switch';
import type { Tag } from '@/ui/components/TagDot';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';

type IconTone = 'secondary' | 'danger' | 'success' | 'accent' | 'primary';

export interface ListRowProps {
  title: string;
  subtitle?: string;
  icon?: IconName;
  iconTone?: IconTone;
  /** A person's initials in their color tag, in place of the icon tile. */
  avatar?: { initials: string; tag: Tag };
  trailing?: 'chevron' | 'switch' | 'none';
  switchValue?: boolean;
  badge?: { tone: BadgeProps['tone']; label: string };
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
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
  className,
  title,
  subtitle,
  icon,
  iconTone = 'secondary',
  avatar,
  trailing = 'chevron',
  switchValue,
  badge,
  onPress,
}: ListRowProps) {
  const content = (
    <>
      {avatar ? (
        <Avatar initials={avatar.initials} tag={avatar.tag} size="sm" />
      ) : icon ? (
        <View className={cn('h-8 w-8 items-center justify-center rounded-md', tile[iconTone])}>
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
      <View className={cn(ROW, className)}>
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
      className={cn(ROW, className)}
    >
      {content}
    </Touchable>
  );
}
