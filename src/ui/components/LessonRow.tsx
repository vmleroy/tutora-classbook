import { View } from 'react-native';

import { Badge, type BadgeProps } from './Badge';
import { TagDot, type Tag } from './TagDot';
import { Text } from './Text';
import { Touchable } from './Touchable';

export interface LessonRowProps {
  time: string;
  tag: Tag;
  title: string;
  subtitle?: string;
  status?: { tone: BadgeProps['tone']; label: string };
  trailing?: string;
  onPress?: () => void;
}

/** One lesson in a day list: time, tag, who and where, and its state or value. */
export function LessonRow({
  time,
  tag,
  title,
  subtitle,
  status,
  trailing,
  onPress,
}: LessonRowProps) {
  return (
    <Touchable
      accessibilityRole="button"
      onPress={onPress}
      className="min-h-control-lg flex-row items-center gap-3 rounded-xl bg-surface px-3.5 py-3"
    >
      <View className="w-11">
        <Text variant="row-title" tabular className="text-ink-muted">
          {time}
        </Text>
      </View>
      <TagDot tag={tag} />
      <View className="min-w-0 flex-1">
        <Text variant="row-title" numberOfLines={1}>
          {title}
        </Text>
        {subtitle ? (
          <Text variant="meta" numberOfLines={1} className="text-ink-muted">
            {subtitle}
          </Text>
        ) : null}
      </View>
      {status ? (
        <Badge tone={status.tone}>{status.label}</Badge>
      ) : trailing ? (
        <Text variant="caption" weight={700} tabular className="text-ink-muted">
          {trailing}
        </Text>
      ) : null}
    </Touchable>
  );
}
