import { useTranslation } from 'react-i18next';

import { touchSlop } from '../theme/metrics';
import { Icon } from './Icon';
import { TagDot, tagInk, tagSoft, type Tag } from './TagDot';
import { Text } from './Text';
import { Touchable } from './Touchable';

export interface AgendaBlockProps {
  tag: Tag;
  title: string;
  subtitle?: string;
  state?: 'default' | 'done' | 'next';
  height?: number;
  onPress?: () => void;
}

const MIN_HEIGHT = 42;

/** A lesson on the Agenda timeline, sized by its duration (44 px per hour). */
export function AgendaBlock({
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
      className={`w-full flex-row flex-wrap items-center gap-x-2 gap-y-0.5 rounded-md px-3 py-2 ${tagSoft[tag]} ${state === 'next' ? 'border-2 border-accent' : ''} ${state === 'done' ? 'opacity-85' : ''}`}
      style={{ minHeight: h }}
    >
      <TagDot tag={tag} size="sm" />
      <Text variant="row-title" className={tagInk[tag]}>
        {title}
      </Text>
      {subtitle ? (
        <Text variant="meta" className={`flex-1 ${tagInk[tag]}`}>
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
