import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { AppState } from 'react-native';

import { newId } from '@/adapters/random';
import { firstLessonAfter, getLesson, listLessons } from '@/data/repos/scheduleRepo';
import * as schedule from '@/usecases/schedule';

import { currentDatabase } from './settingsStore';

export type { Lesson } from '@/domain/schedule';

/** The phone's time zone; classes are scheduled in it. */
export const deviceZone = () => Intl.DateTimeFormat().resolvedOptions().timeZone;

const ctx = (): schedule.Ctx => ({
  db: currentDatabase(),
  now: new Date().toISOString(),
  zone: deviceZone(),
  newId,
});

export function useLessons(from: string, to: string) {
  return useQuery({
    queryKey: ['lessons', from, to],
    queryFn: () => listLessons(currentDatabase(), from, to),
  });
}

export function useLesson(id: string | undefined) {
  return useQuery({
    queryKey: ['lessons', id],
    enabled: !!id,
    queryFn: () => getLesson(currentDatabase(), id!) ?? null,
  });
}

export function useNextLesson(now: string) {
  return useQuery({
    queryKey: ['lessons', 'next', now],
    queryFn: () => firstLessonAfter(currentDatabase(), now) ?? null,
  });
}

export function useScheduleActions() {
  const client = useQueryClient();
  const run =
    <A extends unknown[], R>(fn: (c: schedule.Ctx, ...args: A) => R) =>
    (...args: A): R => {
      const result = fn(ctx(), ...args);
      void client.invalidateQueries({ queryKey: ['lessons'] });
      return result;
    };
  return {
    scheduleLesson: run(schedule.scheduleLesson),
    editLesson: run(schedule.editLesson),
    removeLesson: run(schedule.removeLesson),
    suggestedTravel: (studentId: string, placeId: string) =>
      schedule.suggestedTravel({ db: currentDatabase() }, studentId, placeId),
  };
}

/** Keeps repeating classes filled 8 weeks ahead: at start-up and on every return to the app. */
export function useKeepScheduleFilled(ready: boolean) {
  const client = useQueryClient();
  useEffect(() => {
    if (!ready) return;
    const fill = () => {
      schedule.fillSchedule(ctx());
      void client.invalidateQueries({ queryKey: ['lessons'] });
    };
    fill();
    const sub = AppState.addEventListener('change', (state) => state === 'active' && fill());
    return () => sub.remove();
  }, [ready, client]);
}
