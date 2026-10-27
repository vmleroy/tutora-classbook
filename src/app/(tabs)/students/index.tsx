import { useRouter } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import type { Group } from '@/domain/places';
import { useGroups, usePlaces, useStudents, type StudentWithLinks } from '@/state/catalog';
import { initials, placeName, rateMoney, useLanguage } from '@/ui/catalog';
import { Chip } from '@/ui/components/Chip';
import { EmptyState } from '@/ui/components/EmptyState';
import { ListGroup } from '@/ui/components/ListGroup';
import { ListRow } from '@/ui/components/ListRow';
import { Screen } from '@/ui/components/Screen';
import { Text } from '@/ui/components/Text';
import { TextField } from '@/ui/components/TextField';

/** Accent- and case-insensitive name match, so "ana" finds "Ána". */
const fold = (s: string) =>
  s
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();

export default function StudentsScreen() {
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [byPlace, setByPlace] = useState(false);
  const { data: students = [] } = useStudents();
  const { data: groups = [] } = useGroups();
  const { data: places = [] } = usePlaces();
  const nameOf = new Map(places.map((p) => [p.id, placeName(p, t)]));

  const q = fold(query.trim());
  const shownStudents = students.filter((s) => fold(s.name).includes(q));
  const shownGroups = groups.filter((g) => fold(g.name).includes(q));

  const studentRow = (s: StudentWithLinks) => (
    <ListRow
      key={s.id}
      title={s.name}
      avatar={{ initials: initials(s.name), tag: s.color }}
      subtitle={[
        s.subject,
        s.links.length > 0
          ? s.links.map((l) => nameOf.get(l.placeId)).join(', ')
          : t('students.noPlace'),
      ]
        .filter(Boolean)
        .join(' · ')}
      onPress={() => router.push(`/catalog/student/${s.id}`)}
    />
  );
  const groupRow = (g: Group) => (
    <ListRow
      key={g.id}
      title={g.name}
      icon="users"
      subtitle={[
        g.subject,
        nameOf.get(g.placeId),
        t('groups.size', { count: g.memberIds.length }),
        g.rateCents !== null
          ? t('places.perLesson', { price: rateMoney(g.rateCents, language) })
          : null,
      ]
        .filter(Boolean)
        .join(' · ')}
      onPress={() => router.push(`/catalog/group/edit?id=${g.id}`)}
    />
  );

  const nothing = students.length === 0 && groups.length === 0;
  return (
    <Screen testID="students-screen">
      {nothing ? (
        <EmptyState
          icon="users"
          title={t('empty.students.title')}
          body={t('empty.students.body')}
        />
      ) : (
        <>
          <TextField label={t('students.search')} value={query} onChangeText={setQuery} />
          <View className="flex-row gap-2">
            <Chip
              label={t('students.all', { count: students.length + groups.length })}
              selected={!byPlace}
              onPress={() => setByPlace(false)}
            />
            <Chip
              label={t('students.byPlace')}
              selected={byPlace}
              onPress={() => setByPlace(true)}
            />
          </View>
          {shownStudents.length + shownGroups.length === 0 ? (
            <Text variant="body" className="text-ink-muted">
              {t('students.noSearchResults')}
            </Text>
          ) : byPlace ? (
            <>
              {places.map((p) => {
                const here = shownStudents.filter((s) => s.links.some((l) => l.placeId === p.id));
                const groupsHere = shownGroups.filter((g) => g.placeId === p.id);
                if (here.length + groupsHere.length === 0) return null;
                return (
                  <ListGroup key={p.id} title={placeName(p, t)}>
                    {here.map(studentRow)}
                    {groupsHere.map(groupRow)}
                  </ListGroup>
                );
              })}
              {shownStudents.some((s) => s.links.length === 0) ? (
                <ListGroup title={t('students.noPlace')}>
                  {shownStudents.filter((s) => s.links.length === 0).map(studentRow)}
                </ListGroup>
              ) : null}
            </>
          ) : (
            <ListGroup>
              {shownStudents.map(studentRow)}
              {shownGroups.map(groupRow)}
            </ListGroup>
          )}
        </>
      )}
      <ListGroup>
        <ListRow
          title={t('catalog.archived')}
          icon="archive"
          onPress={() => router.push('/catalog/archived')}
        />
      </ListGroup>
    </Screen>
  );
}
