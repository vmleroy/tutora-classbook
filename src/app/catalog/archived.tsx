import { Stack, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { useGroups, usePlaces, useStudents } from '@/state/catalog';
import { initials, placeName } from '@/ui/catalog';
import { ListGroup } from '@/ui/components/ListGroup';
import { ListRow } from '@/ui/components/ListRow';
import { Screen } from '@/ui/components/Screen';
import { Text } from '@/ui/components/Text';

/** Arquivados: everything hidden, each opening its form to restore or remove it. */
export default function Archived() {
  const { t } = useTranslation();
  const router = useRouter();
  const { data: places = [] } = usePlaces(true);
  const { data: students = [] } = useStudents(true);
  const { data: groups = [] } = useGroups(true);
  const empty = places.length + students.length + groups.length === 0;
  return (
    <Screen testID="archived">
      <Stack.Screen options={{ title: t('catalog.archived') }} />
      {empty ? (
        <Text variant="body" className="text-ink-muted">
          {t('catalog.noArchived')}
        </Text>
      ) : null}
      {students.length > 0 ? (
        <ListGroup title={t('tabs.students')}>
          {students.map((s) => (
            <ListRow
              key={s.id}
              title={s.name}
              avatar={{ initials: initials(s.name), tag: s.color }}
              onPress={() => router.push(`/catalog/student/edit?id=${s.id}`)}
            />
          ))}
        </ListGroup>
      ) : null}
      {groups.length > 0 ? (
        <ListGroup title={t('places.groups')}>
          {groups.map((g) => (
            <ListRow
              key={g.id}
              title={g.name}
              onPress={() => router.push(`/catalog/group/edit?id=${g.id}`)}
            />
          ))}
        </ListGroup>
      ) : null}
      {places.length > 0 ? (
        <ListGroup title={t('tabs.places')}>
          {places.map((p) => (
            <ListRow
              key={p.id}
              title={placeName(p, t)}
              onPress={() => router.push(`/catalog/place/edit?id=${p.id}`)}
            />
          ))}
        </ListGroup>
      ) : null}
    </Screen>
  );
}
