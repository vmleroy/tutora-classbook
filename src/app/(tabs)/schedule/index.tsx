import { useRouter } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ScrollView, View, type ScrollViewInstance } from 'react-native';

import { addDays, dayRange, utcToLocal, weekStart } from '@/domain/recurrence';
import { expected, lessonWorth, nextLesson } from '@/domain/schedule';
import { deviceZone, useLessons } from '@/state/schedule';
import { useLanguage } from '@/ui/catalog';
import { EmptyState } from '@/ui/components/EmptyState';
import { SegmentedControl } from '@/ui/components/SegmentedControl';
import { Text } from '@/ui/components/Text';
import { formatMoney, formatShortDate } from '@/ui/format';
import { byLocalDay, DayGrid, HOUR, WeekPicker } from '@/ui/schedule';

export default function ScheduleScreen() {
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const [zone] = useState(deviceZone);
  const [now] = useState(() => new Date().toISOString());
  const today = utcToLocal(now, zone).date;
  const [selected, setSelected] = useState(today);
  const [view, setView] = useState<'day' | 'week'>('day');
  const monday = weekStart(selected);
  const [from, to] = dayRange(monday, addDays(monday, 6), zone);
  const { data: lessons = [] } = useLessons(from, to);
  const byDay = byLocalDay(lessons, zone);
  const dayLessons = byDay.get(selected) ?? [];
  const next = nextLesson(lessons, now);
  const previsto = expected(dayLessons.map(lessonWorth));

  // The grid opens at the day's first class, or at the current hour.
  const scroll = useRef<ScrollViewInstance>(null);
  const first = (view === 'day' ? dayLessons : lessons)[0];
  const startHour = first ? utcToLocal(first.startAt, zone).hour : utcToLocal(now, zone).hour;
  useEffect(() => {
    scroll.current?.scrollTo({ y: Math.max(0, startHour - 1) * HOUR, animated: false });
  }, [startHour, selected, view]);

  const dayTitle = selected === today ? t('schedule.today') : formatShortDate(selected, language);
  const open = (id: string) => router.push(`/lesson/${id}/edit`);

  return (
    <View testID="schedule-screen" className="flex-1 bg-background">
      <View className="gap-3 px-5 pb-2 pt-1">
        <WeekPicker selected={selected} today={today} byDay={byDay} onSelect={setSelected} />
        <SegmentedControl
          label={t('tabs.schedule')}
          size="sm"
          value={view}
          onChange={setView}
          options={[
            { value: 'day', label: t('schedule.day') },
            { value: 'week', label: t('schedule.week') },
          ]}
        />
        {view === 'day' ? (
          <View className="flex-row items-baseline justify-between">
            <Text variant="section">
              {`${dayTitle} · ${t('schedule.count', { count: dayLessons.length })}`}
            </Text>
            {previsto.cents > 0 || previsto.salary ? (
              <Text variant="caption" tabular className="text-ink-muted">
                {[
                  previsto.cents > 0
                    ? t('schedule.expected', { money: formatMoney(previsto.cents, language, true) })
                    : null,
                  previsto.salary ? t('schedule.plusSchool') : null,
                ]
                  .filter(Boolean)
                  .join(' ')}
              </Text>
            ) : null}
          </View>
        ) : null}
      </View>
      {lessons.length === 0 ? (
        <View className="px-5">
          <EmptyState
            icon="calendar"
            title={t('empty.schedule.title')}
            body={t('empty.schedule.body')}
          />
        </View>
      ) : (
        <ScrollView ref={scroll} className="flex-1" contentContainerClassName="px-5 pb-6">
          <DayGrid
            days={
              view === 'day' ? [selected] : Array.from({ length: 7 }, (_, i) => addDays(monday, i))
            }
            byDay={byDay}
            zone={zone}
            nextId={next?.id}
            compact={view === 'week'}
            onPress={(l) => open(l.id)}
          />
        </ScrollView>
      )}
    </View>
  );
}
