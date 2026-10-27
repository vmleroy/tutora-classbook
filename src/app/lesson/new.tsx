import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { utcToLocal } from '@/domain/recurrence';
import { useGroups, usePlaces, useStudents } from '@/state/catalog';
import { deviceZone, useScheduleActions } from '@/state/schedule';
import { Screen } from '@/ui/components/Screen';
import { LessonForm, type LessonValues } from '@/ui/lessonForm';

/** Nova aula in Agendar mode; Registrar comes with spec 006. */
export default function NewLesson() {
  const { date } = useLocalSearchParams<{ date?: string }>();
  const { t } = useTranslation();
  const router = useRouter();
  const { data: students } = useStudents();
  const { data: groups } = useGroups();
  const { data: places } = usePlaces();
  const actions = useScheduleActions();
  const [error, setError] = useState<string | null>(null);
  const [now] = useState(() => utcToLocal(new Date().toISOString(), deviceZone()));
  if (!students || !groups || !places) return <Screen testID="lesson-new">{null}</Screen>;

  const initial: LessonValues = {
    who: null,
    placeId: null,
    date: date ?? now.date,
    time: `${String(Math.min(now.hour + 1, 23)).padStart(2, '0')}:00`,
    plannedMin: 60,
    valueCents: null,
    travelFeeCents: null,
    repeat: 'weekly',
    notes: null,
  };

  const submit = (v: LessonValues) => {
    if (!v.who) return setError(t('lesson.errors.whoRequired'));
    if (!v.placeId) return setError(t('lesson.errors.placeRequired'));
    const result = actions.scheduleLesson({
      who: v.who.kind === 'student' ? { studentId: v.who.id } : { groupId: v.who.id },
      placeId: v.placeId,
      date: v.date,
      time: v.time,
      plannedMin: v.plannedMin,
      valueCents: v.valueCents,
      travelFeeCents: v.travelFeeCents,
      repeat: v.repeat,
      notes: v.notes,
    });
    if (!result.ok) return setError(t(`lesson.errors.${result.error}`));
    router.back();
  };

  return (
    <Screen testID="lesson-new">
      <Stack.Screen options={{ title: t('lesson.new') }} />
      <LessonForm
        initial={initial}
        today={now.date}
        students={students}
        groups={groups}
        places={places}
        error={error}
        suggestTravel={actions.suggestedTravel}
        onSubmit={submit}
      />
    </Screen>
  );
}
