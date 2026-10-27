// Text and composites shared by Agenda, Hoje and the class form (spec 005).
// Built only from design-system components.
import type { TFunction } from 'i18next';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { addDays, utcToLocal, weekStart, type LocalDate } from '@/domain/recurrence';
import { layoutDay, type Lesson, type LessonWorth } from '@/domain/schedule';
import type { Language } from '@/domain/settings';
import { AgendaBlock } from '@/ui/components/AgendaBlock';
import { BottomSheet } from '@/ui/components/BottomSheet';
import { IconButton } from '@/ui/components/IconButton';
import type { Tag } from '@/ui/components/TagDot';
import { Text } from '@/ui/components/Text';
import { WeekStrip } from '@/ui/components/WeekStrip';
import { formatMoney, formatMonthYear, formatTime, formatWeekdayShort } from '@/ui/format';

import { placeName } from './catalog';

/** Groups have no color of their own; they all use one tag. */
const GROUP_TAG: Tag = 'teal';

export const whoName = (l: Lesson) =>
  l.who.kind === 'student' ? l.who.student.name : l.who.group.name;
export const whoTag = (l: Lesson): Tag =>
  l.who.kind === 'student' ? l.who.student.color : GROUP_TAG;
export const whoSubject = (l: Lesson) =>
  l.who.kind === 'student' ? l.who.student.subject : l.who.group.subject;

export function worthText(worth: LessonWorth, t: TFunction, language: Language): string {
  if (worth.kind === 'set') return formatMoney(worth.cents, language);
  return worth.kind === 'salary' ? t('groups.coveredBySalary') : t('lesson.noPrice');
}

export function lessonSubtitle(l: Lesson, t: TFunction): string {
  return [whoSubject(l), placeName(l.place, t)].filter(Boolean).join(' · ');
}

/** The week strip's seven days from Monday, each with a dot in its first class's color. */
export function weekDays(monday: LocalDate, byDay: Map<LocalDate, Lesson[]>, language: Language) {
  return Array.from({ length: 7 }, (_, i) => {
    const date = addDays(monday, i);
    const first = byDay.get(date)?.[0];
    return {
      date,
      weekday: formatWeekdayShort(date, language),
      day: Number(date.slice(8)),
      dot: first ? whoTag(first) : undefined,
    };
  });
}

/** A month label, ‹ › weeks and the strip; tapping a day picks it. */
export function WeekPicker({
  selected,
  today,
  byDay = new Map(),
  onSelect,
}: {
  selected: LocalDate;
  today: LocalDate;
  byDay?: Map<LocalDate, Lesson[]>;
  onSelect: (date: LocalDate) => void;
}) {
  const { t, i18n } = useTranslation();
  const language: Language = i18n.language === 'en' ? 'en' : 'pt-BR';
  const monday = weekStart(selected);
  const days = weekDays(monday, byDay, language);
  const index = days.findIndex((d) => d.date === selected);
  const todayIndex = days.findIndex((d) => d.date === today);
  return (
    <View className="gap-2">
      <View className="flex-row items-center justify-between">
        <IconButton
          icon="chevron-left"
          variant="plain"
          label={t('schedule.prevWeek')}
          onPress={() => onSelect(addDays(selected, -7))}
        />
        <Text variant="section">{formatMonthYear(selected, language)}</Text>
        <IconButton
          icon="chevron-right"
          variant="plain"
          label={t('schedule.nextWeek')}
          onPress={() => onSelect(addDays(selected, 7))}
        />
      </View>
      <WeekStrip
        key={monday}
        days={days}
        selected={index}
        today={todayIndex >= 0 ? todayIndex : undefined}
        onSelect={(i) => onSelect(days[i]!.date)}
      />
    </View>
  );
}

/** A sheet with the week picker, for the form's Data field. */
export function DateSheet({
  title,
  value,
  today,
  onPick,
  onClose,
}: {
  title: string;
  value: LocalDate;
  today: LocalDate;
  onPick: (date: LocalDate) => void;
  onClose: () => void;
}) {
  const [shown, setShown] = useState(value);
  return (
    <BottomSheet title={title} onClose={onClose}>
      <WeekPicker
        selected={shown}
        today={today}
        onSelect={(date) => {
          // Moving weeks keeps the sheet open; tapping a day of the shown week picks it.
          if (weekStart(date) === weekStart(shown) && date !== shown) {
            onPick(date);
            onClose();
          } else setShown(date);
        }}
      />
    </BottomSheet>
  );
}

/** Pixels per hour on the grid, as in the AgendaBlock guideline. */
export const HOUR = 44;
const LABEL = 48;

/**
 * The 24-hour grid of one or more days side by side, with a block per class. Overlapping
 * classes share the width of their day (AC-005-21).
 */
export function DayGrid({
  days,
  byDay,
  zone,
  nextId,
  compact,
  onPress,
}: {
  days: LocalDate[];
  byDay: Map<LocalDate, Lesson[]>;
  zone: string;
  nextId?: string;
  compact?: boolean;
  onPress: (lesson: Lesson) => void;
}) {
  const { t, i18n } = useTranslation();
  const language: Language = i18n.language === 'en' ? 'en' : 'pt-BR';
  return (
    <View
      accessibilityLabel={t('schedule.grid')}
      className="flex-row"
      style={{ height: 24 * HOUR }}
    >
      <View style={{ width: LABEL }}>
        {Array.from({ length: 24 }, (_, h) => (
          <Text
            key={h}
            variant="meta"
            tabular
            className="text-ink-muted"
            style={{ position: 'absolute', top: h * HOUR - 6 }}
          >
            {formatTime(`${String(h).padStart(2, '0')}:00`, language)}
          </Text>
        ))}
      </View>
      {days.map((date) => {
        const lessons = byDay.get(date) ?? [];
        const spans = lessons.map((l) => {
          const { hour, minute } = utcToLocal(l.startAt, zone);
          const start = hour * 60 + minute;
          return { id: l.id, start, end: start + l.plannedMin };
        });
        const layout = layoutDay(spans);
        return (
          <View key={date} className="flex-1 border-l border-line-soft">
            {Array.from({ length: 24 }, (_, h) => (
              <View
                key={h}
                className="border-t border-line-soft"
                style={{ position: 'absolute', top: h * HOUR, left: 0, right: 0 }}
              />
            ))}
            {lessons.map((l, i) => {
              const span = spans[i]!;
              const place = layout.get(l.id) ?? { column: 0, columns: 1 };
              return (
                <View
                  key={l.id}
                  testID={`block-${l.id}`}
                  style={{
                    position: 'absolute',
                    top: (span.start * HOUR) / 60,
                    left: `${(place.column / place.columns) * 100}%`,
                    width: `${100 / place.columns}%`,
                    paddingHorizontal: 1,
                  }}
                >
                  <AgendaBlock
                    tag={whoTag(l)}
                    title={whoName(l)}
                    subtitle={compact ? undefined : lessonSubtitle(l, t)}
                    state={l.id === nextId ? 'next' : 'default'}
                    height={(l.plannedMin * HOUR) / 60}
                    onPress={() => onPress(l)}
                  />
                </View>
              );
            })}
          </View>
        );
      })}
    </View>
  );
}

/** Classes by their local date, in time order. */
export function byLocalDay(lessons: readonly Lesson[], zone: string): Map<LocalDate, Lesson[]> {
  const map = new Map<LocalDate, Lesson[]>();
  for (const l of lessons) {
    const date = utcToLocal(l.startAt, zone).date;
    map.set(date, [...(map.get(date) ?? []), l]);
  }
  return map;
}
