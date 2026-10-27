import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { utcToLocal } from '@/domain/recurrence';
import { useGroups, usePlaces, useStudents } from '@/state/catalog';
import { deviceZone, useLesson, useScheduleActions } from '@/state/schedule';
import { Screen } from '@/ui/components/Screen';
import { LessonForm, type LessonValues, type Scope } from '@/ui/lessonForm';

/** Editar aula: date, time, duration, value and notes, for this class or the series. */
export default function EditLesson() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { t } = useTranslation();
  const router = useRouter();
  const { data: lesson } = useLesson(id);
  const { data: students } = useStudents();
  const { data: groups } = useGroups();
  const { data: places } = usePlaces();
  const actions = useScheduleActions();
  const [error, setError] = useState<string | null>(null);
  const [zone] = useState(deviceZone);
  if (!lesson || !students || !groups || !places) {
    return <Screen testID="lesson-edit">{null}</Screen>;
  }
  const local = utcToLocal(lesson.startAt, zone);
  const today = utcToLocal(new Date().toISOString(), zone).date;
  const initial: LessonValues = {
    who:
      lesson.who.kind === 'student'
        ? { kind: 'student', id: lesson.who.student.id }
        : { kind: 'group', id: lesson.who.group.id },
    placeId: lesson.place.id,
    date: local.date,
    time: local.time,
    plannedMin: lesson.plannedMin,
    valueCents: lesson.valueCents,
    travelFeeCents: lesson.travelFeeCents,
    repeat: lesson.repeat,
    notes: lesson.notes,
  };

  const done = <E extends string>(result: { ok: true } | { ok: false; error: E }) => {
    if (!result.ok) return setError(t(`lesson.errors.${result.error}` as 'lesson.errors.notFound'));
    router.back();
  };

  return (
    <Screen testID="lesson-edit">
      <Stack.Screen options={{ title: t('lesson.edit') }} />
      <LessonForm
        initial={initial}
        editing
        series={
          lesson.repeat && !lesson.isDetached
            ? { repeat: lesson.repeat, startAt: lesson.startAt, zone }
            : undefined
        }
        today={today}
        students={students}
        groups={groups}
        places={places}
        error={error}
        suggestTravel={actions.suggestedTravel}
        onSubmit={(v: LessonValues, scope: Scope) =>
          done(
            actions.editLesson(
              lesson.id,
              {
                date: v.date,
                time: v.time,
                plannedMin: v.plannedMin,
                valueCents: v.valueCents,
                travelFeeCents: v.travelFeeCents,
                notes: v.notes,
              },
              scope,
            ),
          )
        }
        onRemove={(scope: Scope) => done(actions.removeLesson(lesson.id, scope))}
      />
    </Screen>
  );
}
