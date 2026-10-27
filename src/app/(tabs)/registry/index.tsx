import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import type { Group } from '@/domain/places';
import {
  useGroups,
  usePlaces,
  useStudents,
  type PlaceRow,
  type StudentWithLinks,
} from '@/state/catalog';
import { useSettingsStore } from '@/state/settingsStore';
import {
  initials,
  placeName,
  placePriceText,
  rateMoney,
  travelText,
  useLanguage,
} from '@/ui/catalog';
import { Chip } from '@/ui/components/Chip';
import { EmptyState } from '@/ui/components/EmptyState';
import { IconButton } from '@/ui/components/IconButton';
import { ListGroup } from '@/ui/components/ListGroup';
import { ListRow } from '@/ui/components/ListRow';
import { Screen } from '@/ui/components/Screen';
import { SegmentedControl } from '@/ui/components/SegmentedControl';
import { Text } from '@/ui/components/Text';
import { TextField } from '@/ui/components/TextField';

type Pane = 'students' | 'places';

/**
 * Cadastro: students and places share one tab, since each links to the other. Alunos and
 * Locais are its two sub-pages; `?view=places` opens the second.
 */
export default function RegistryScreen() {
  const { view: param } = useLocalSearchParams<{ view?: Pane }>();
  const view: Pane = param === 'places' ? 'places' : 'students';
  const { t } = useTranslation();
  const router = useRouter();
  return (
    <Screen testID="registry-screen">
      <Stack.Screen
        options={{
          headerRight: () => (
            <View className="flex-row items-center gap-1">
              <IconButton
                icon="archive"
                variant="plain"
                label={t('catalog.archived')}
                onPress={() => router.push('/catalog/archived')}
              />
              <IconButton
                icon="plus"
                variant="accent"
                label={t(view === 'places' ? 'places.new' : 'students.new')}
                onPress={() =>
                  router.push(view === 'places' ? '/catalog/place/edit' : '/catalog/student/edit')
                }
              />
            </View>
          ),
        }}
      />
      <SegmentedControl
        label={t('tabs.registry')}
        value={view}
        onChange={(next) => router.setParams({ view: next })}
        options={[
          { value: 'students', label: t('tabs.students') },
          { value: 'places', label: t('tabs.places') },
        ]}
      />
      {view === 'places' ? <PlacesPane /> : <StudentsPane />}
    </Screen>
  );
}

/** Accent- and case-insensitive name match, so "ana" finds "Ána". */
const fold = (s: string) =>
  s
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();

function StudentsPane() {
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
    <View testID="students-screen" className="gap-4">
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
    </View>
  );
}

function PlacesPane() {
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const { data: places = [] } = usePlaces();
  const showDefaults = useSettingsStore((s) => s.settings?.showDefaultPlaces ?? true);
  const builtins = showDefaults ? places.filter((p) => p.builtin) : [];
  const own = places.filter((p) => !p.builtin);

  const summary = (p: PlaceRow) =>
    [
      placePriceText(p, t, language),
      t('places.minutes', { count: p.defaultDurationMin }),
      t('places.students', { count: p.studentCount }),
      p.kind === 'student_home' ? travelText(p, t, language) : null,
    ]
      .filter(Boolean)
      .join(' · ');

  const row = (p: PlaceRow) => (
    <ListRow
      key={p.id}
      title={placeName(p, t)}
      subtitle={summary(p)}
      badge={p.builtin ? undefined : { tone: 'secondary', label: t(`places.kind.${p.kind}`) }}
      onPress={() => router.push(`/catalog/place/${p.id}`)}
    />
  );

  return (
    <View testID="places-screen" className="gap-4">
      <Text variant="meta" className="text-ink-muted">
        {t('places.subtitle')}
      </Text>
      {builtins.length > 0 ? (
        <ListGroup title={`${t('places.builtinGroup')} · ${t('places.builtinHint')}`}>
          {builtins.map(row)}
        </ListGroup>
      ) : null}
      {own.length > 0 ? <ListGroup title={t('places.own')}>{own.map(row)}</ListGroup> : null}
      {/* Only when nothing at all is listed: the default places are places too. */}
      {builtins.length + own.length === 0 ? (
        <EmptyState icon="pin" title={t('places.empty')} body={t('places.emptyBody')} />
      ) : null}
    </View>
  );
}
