import { useQuery, useQueryClient } from '@tanstack/react-query';

import { newId } from '@/adapters/random';
import { listGroups, getGroup } from '@/data/repos/groupRepo';
import { getPlace, listPlaces } from '@/data/repos/placeRepo';
import { getStudent, listStudents, studentsAtPlace } from '@/data/repos/studentRepo';
import * as catalog from '@/usecases/catalog';

import { currentDatabase } from './settingsStore';

export type { PlaceRow } from '@/data/repos/placeRepo';
export type { StudentWithLinks } from '@/data/repos/studentRepo';

// Any write to places, students or groups can change what the others show (counts, links,
// groups), so each one invalidates all three.
const tables = ['places', 'students', 'groups'] as const;

export function usePlaces(archived = false) {
  return useQuery({
    queryKey: ['places', { archived }],
    queryFn: () => listPlaces(currentDatabase(), archived),
  });
}

export function usePlace(id: string | undefined) {
  return useQuery({
    queryKey: ['places', id],
    enabled: !!id,
    queryFn: () => {
      const db = currentDatabase();
      const place = getPlace(db, id!);
      if (!place) return null;
      return {
        place,
        students: studentsAtPlace(db, id!),
        groups: listGroups(db, { placeId: id }),
      };
    },
  });
}

export function useStudents(archived = false) {
  return useQuery({
    queryKey: ['students', { archived }],
    queryFn: () => listStudents(currentDatabase(), archived),
  });
}

export function useStudent(id: string | undefined) {
  return useQuery({
    queryKey: ['students', id],
    enabled: !!id,
    queryFn: () => getStudent(currentDatabase(), id!) ?? null,
  });
}

export function useGroups(archived = false) {
  return useQuery({
    queryKey: ['groups', { archived }],
    queryFn: () => listGroups(currentDatabase(), { archived }),
  });
}

export function useGroup(id: string | undefined) {
  return useQuery({
    queryKey: ['groups', id],
    enabled: !!id,
    queryFn: () => getGroup(currentDatabase(), id!) ?? null,
  });
}

/** The catalog use cases, bound to the open database; each write refreshes the lists. */
export function useCatalogActions() {
  const client = useQueryClient();
  const run =
    <A extends unknown[], R>(fn: (ctx: catalog.Ctx, ...args: A) => R) =>
    (...args: A): R => {
      const result = fn({ db: currentDatabase(), now: new Date().toISOString(), newId }, ...args);
      for (const key of tables) void client.invalidateQueries({ queryKey: [key] });
      return result;
    };
  return {
    savePlace: run(catalog.savePlace),
    saveStudent: run(catalog.saveStudent),
    saveGroup: run(catalog.saveGroup),
    linkStudentToPlace: run(catalog.linkStudentToPlace),
    archive: run(catalog.archive),
    restore: run(catalog.restore),
    removePermanently: run(catalog.removePermanently),
  };
}
