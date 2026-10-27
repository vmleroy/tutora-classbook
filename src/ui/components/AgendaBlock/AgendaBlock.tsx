import { useTranslation } from 'react-i18next';

import { cn } from '@/ui/cn';
import { Icon } from '@/ui/components/Icon';
import { TagDot, tagInk, tagSoft, type Tag } from '@/ui/components/TagDot';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';
import { touchSlop } from '@/ui/theme/sizes';

export interface AgendaBlockProps {
  tag: Tag;
  title: string;
  subtitle?: string;
  state?: 'default' | 'done' | 'next';
  height?: number;
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const MIN_HEIGHT = 42;

/** A lesson on the Agenda timeline, sized by its duration (44 px per hour). */
export function AgendaBlock({
  className,
  tag,
  title,
  subtitle,
  state = 'default',
  height,
  onPress,
}: AgendaBlockProps) {
  const { t } = useTranslation();
  const h = Math.max(MIN_HEIGHT, height ?? MIN_HEIGHT);
  return (
    <Touchable
      accessibilityRole="button"
      accessibilityLabel={[title, subtitle, state === 'next' ? t('ui.next') : null]
        .filter(Boolean)
        .join(', ')}
      onPress={onPress}
      hitSlop={touchSlop(h)}
      className={cn(
        'w-full flex-row flex-wrap items-center gap-x-2 gap-y-0.5 rounded-md px-3 py-2',
        tagSoft[tag],
        state === 'next' && 'border-2 border-accent',
        state === 'done' && 'opacity-85',
        className,
      )}
      style={{ minHeight: h }}
    >
      <TagDot tag={tag} size="sm" />
      <Text variant="row-title" className={tagInk[tag]}>
        {title}
      </Text>
      {subtitle ? (
        <Text variant="meta" className={cn('flex-1', tagInk[tag])}>
          {subtitle}
        </Text>
      ) : null}
      {state === 'done' ? (
        <Icon name="check" size={16} strokeWidth={2.6} label={t('ui.done')} />
      ) : null}
      {state === 'next' ? (
        <Text variant="badge" className={tagInk[tag]}>
          {t('ui.next')}
        </Text>
      ) : null}
    </Touchable>
  );
}
