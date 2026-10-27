import { useRouter } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { resolvePrice } from '@/domain/pricing';
import { dayRange, utcToLocal } from '@/domain/recurrence';
import { greeting, lessonWorth, type Lesson } from '@/domain/schedule';
import { deviceZone, useLessons, useNextLesson } from '@/state/schedule';
import { durationText, initials, rateText, useLanguage } from '@/ui/catalog';
import { Avatar } from '@/ui/components/Avatar';
import { Card } from '@/ui/components/Card';
import { EmptyState } from '@/ui/components/EmptyState';
import { LessonRow } from '@/ui/components/LessonRow';
import { Screen } from '@/ui/components/Screen';
import { SectionHeader } from '@/ui/components/SectionHeader';
import { Text } from '@/ui/components/Text';
import { formatLongDate, formatShortDate, formatTime } from '@/ui/format';
import { lessonSubtitle, whoName, whoTag, worthText } from '@/ui/schedule';

export default function TodayScreen() {
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const [zone] = useState(deviceZone);
  const [now] = useState(() => new Date().toISOString());
  const local = utcToLocal(now, zone);
  const [from, to] = dayRange(local.date, local.date, zone);
  const { data: lessons = [] } = useLessons(from, to);
  const { data: next } = useNextLesson(now);
  const open = (l: Lesson) => router.push(`/lesson/${l.id}/edit`);

  const when = (l: Lesson) => {
    const at = utcToLocal(l.startAt, zone);
    if (at.date !== local.date) {
      return `${formatShortDate(at.date, language)}, ${formatTime(at.time, language)}`;
    }
    const minutes = Math.round((Date.parse(l.startAt) - Date.parse(now)) / 60_000);
    if (minutes < 60) return t('home.inMinutes', { count: minutes });
    const rest = minutes % 60;
    return t('home.inHours', {
      time: `${Math.floor(minutes / 60)}h${rest ? String(rest).padStart(2, '0') : ''}`,
    });
  };

  const rate = (l: Lesson) => {
    const price =
      l.who.kind === 'student'
        ? resolvePrice(l.place, { student: l.who.student, link: l.who.link })
        : resolvePrice(l.place, { group: l.who.group });
    return price.kind === 'set' ? rateText(price.cents, price.rateType, t, language) : null;
  };

  return (
    <Screen testID="today-screen">
      <View>
        <Text variant="caption" className="text-ink-muted">
          {formatLongDate(local.date, language)}
        </Text>
        <Text variant="title-xl">{t(`home.greeting.${greeting(local.hour)}`)}</Text>
      </View>

      {next ? (
        <Card label={t('home.next')}>
          <Text variant="caption" className="text-ink-muted">
            {`${t('home.next')} · ${when(next)}`}
          </Text>
          <View className="flex-row items-center gap-3">
            <Avatar initials={initials(whoName(next))} tag={whoTag(next)} />
            <View className="min-w-0 flex-1">
              <Text variant="body-strong">{whoName(next)}</Text>
              <Text variant="meta" className="text-ink-muted">
                {[lessonSubtitle(next, t), rate(next)].filter(Boolean).join(' · ')}
              </Text>
            </View>
            <View className="items-end">
              <Text variant="section" tabular numberOfLines={1}>
                {formatTime(utcToLocal(next.startAt, zone).time, language)}
              </Text>
              <Text variant="meta" className="text-ink-muted">
                {durationText(next.plannedMin, t)}
              </Text>
            </View>
          </View>
        </Card>
      ) : null}

      {lessons.length === 0 ? (
        <EmptyState icon="calendar" title={t('empty.today.title')} body={t('empty.today.body')} />
      ) : (
        <>
          <SectionHeader
            title={t('home.lessonsToday')}
            aside={t('schedule.count', { count: lessons.length })}
          />
          <View className="gap-2">
            {lessons.map((l) => (
              <LessonRow
                key={l.id}
                time={formatTime(utcToLocal(l.startAt, zone).time, language)}
                tag={whoTag(l)}
                title={whoName(l)}
                subtitle={`${lessonSubtitle(l, t)} · ${t('places.minutes', { count: l.plannedMin })}`}
                trailing={worthText(lessonWorth(l), t, language)}
                onPress={() => open(l)}
              />
            ))}
          </View>
        </>
      )}
    </Screen>
  );
}
