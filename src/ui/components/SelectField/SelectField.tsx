import { View } from 'react-native';

import { cn } from '@/ui/cn';
import { Avatar } from '@/ui/components/Avatar';
import { Icon, type IconName } from '@/ui/components/Icon';
import type { Tag } from '@/ui/components/TagDot';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';

const MIN_HEIGHT = 52;

export interface SelectFieldProps {
  label?: string;
  value: string;
  sub?: string;
  avatar?: { initials: string; tag: Tag };
  icon?: IconName;
  actionLabel?: string;
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

/** A field that opens a picker; always shows the current choice, never an empty box. */
export function SelectField({
  className,
  label,
  value,
  sub,
  avatar,
  icon,
  actionLabel,
  onPress,
}: SelectFieldProps) {
  return (
    <View className={cn('gap-1.5', className)}>
      {label ? (
        <Text variant="field-label" className="text-ink-muted">
          {label}
        </Text>
      ) : null}
      <Touchable
        accessibilityRole="button"
        accessibilityLabel={label ? `${label}: ${value}` : value}
        onPress={onPress}
        className="flex-row items-center gap-3 rounded-xl border border-line bg-surface px-3.5"
        style={{ minHeight: MIN_HEIGHT }}
      >
        {avatar ? (
          <Avatar initials={avatar.initials} tag={avatar.tag} size="sm" />
        ) : icon ? (
          <Icon name={icon} size={18} tone="ink-muted" />
        ) : null}
        <View className="min-w-0 flex-1">
          <Text variant="body-strong" numberOfLines={1}>
            {value}
          </Text>
          {sub ? (
            <Text variant="meta" className="text-ink-muted" numberOfLines={1}>
              {sub}
            </Text>
          ) : null}
        </View>
        {actionLabel ? (
          <Text variant="badge" className="text-primary">
            {actionLabel}
          </Text>
        ) : null}
      </Touchable>
    </View>
  );
}
