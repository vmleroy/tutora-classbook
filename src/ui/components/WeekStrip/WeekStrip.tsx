import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { cn } from '@/ui/cn';
import { tagFill, type Tag } from '@/ui/components/TagDot';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';

export interface WeekStripProps {
  days: { weekday: string; day: number; dot?: Tag; a11y?: string }[];
  selected?: number;
  /** Index of today, whose number is shown in primary when it is not the selected day. */
  today?: number;
  label?: string;
  onSelect?: (index: number) => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const DAY_HEIGHT = 64;
const DOT = 5;

/** Seven days; the selected one is a main-buttons fill, a dot marks days with lessons. */
export function WeekStrip({ className, days, selected, today, label, onSelect }: WeekStripProps) {
  const { t } = useTranslation();
  const [own, setOwn] = useState(selected ?? 0);
  const current = selected ?? own;
  return (
    <View
      accessibilityRole="tablist"
      accessibilityLabel={label ?? t('ui.weekDays')}
      className={cn('flex-row gap-1', className)}
    >
      {days.map((d, i) => {
        const on = i === current;
        return (
          <Touchable
            key={i}
            accessibilityRole="tab"
            accessibilityLabel={d.a11y ?? `${d.weekday} ${d.day}`}
            accessibilityState={{ selected: on }}
            onPress={() => {
              setOwn(i);
              onSelect?.(i);
            }}
            className={cn(
              'flex-1 items-center justify-center gap-0.5 rounded-xl',
              on && 'bg-btn-primary',
            )}
            style={{ height: DAY_HEIGHT }}
          >
            <Text
              variant="badge"
              weight={700}
              className={on ? 'text-on-primary-muted' : 'text-ink-muted'}
            >
              {d.weekday}
            </Text>
            <Text
              variant="section"
              tabular
              className={on ? 'text-on-btn-primary' : i === today ? 'text-primary' : 'text-ink'}
            >
              {d.day}
            </Text>
            <View
              className={cn('rounded-full', d.dot ? (on ? 'bg-accent' : tagFill[d.dot]) : '')}
              style={{ width: DOT, height: DOT }}
            />
          </Touchable>
        );
      })}
    </View>
  );
}
