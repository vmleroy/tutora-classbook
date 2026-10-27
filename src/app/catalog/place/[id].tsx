import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { payMode } from '@/domain/places';
import { useCatalogActions, usePlace, useStudents } from '@/state/catalog';
import {
  initials,
  placeName,
  placePriceText,
  rateMoney,
  travelText,
  useLanguage,
} from '@/ui/catalog';
import { BottomSheet } from '@/ui/components/BottomSheet';
import { Button } from '@/ui/components/Button';
import { Card } from '@/ui/components/Card';
import { ListGroup } from '@/ui/components/ListGroup';
import { ListRow } from '@/ui/components/ListRow';
import { Screen } from '@/ui/components/Screen';
import { SectionHeader } from '@/ui/components/SectionHeader';
import { Text } from '@/ui/components/Text';

export default function PlaceDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const { data } = usePlace(id);
  const { data: allStudents = [] } = useStudents();
  const { linkStudentToPlace } = useCatalogActions();
  const [picking, setPicking] = useState(false);
  if (!data) return <Screen testID="place-detail">{null}</Screen>;
  const { place, students, groups } = data;
  const name = placeName(place, t);
  const linked = new Set(students.map((s) => s.id));
  const unlinked = allStudents.filter((s) => !linked.has(s.id));
  const mode = payMode(place.kind);

  const model = [
    mode === 'choose'
      ? t('places.whoPaysValue', {
          who: t(place.payer === 'place' ? 'places.whoPlace' : 'places.whoStudent'),
        })
      : null,
    placePriceText(place, t, language),
    place.payer === 'student' && place.commissionBp > 0
      ? `${t('places.commission')} ${place.commissionBp / 100}%`
      : null,
    t('places.minutes', { count: place.defaultDurationMin }),
    mode === 'choose' && place.payer === 'place' ? t(`places.cycles.${place.payCycle}`) : null,
    place.kind === 'student_home' ? travelText(place, t, language) : null,
  ].filter(Boolean);

  return (
    <Screen testID="place-detail">
      <Stack.Screen
        options={{
          title: t('places.detail'),
          headerRight: () => (
            <Button
              variant="ghost"
              size="sm"
              onPress={() => router.push(`/catalog/place/edit?id=${place.id}`)}
            >
              {t('catalog.edit')}
            </Button>
          ),
        }}
      />
      <Card>
        <Text variant="title-lg">{name}</Text>
        <Text variant="meta" className="text-ink-muted">
          {[t(`places.kind.${place.kind}`), place.address].filter(Boolean).join(' · ')}
        </Text>
      </Card>
      <Card label={t('places.model')}>
        <Text variant="caption" className="text-ink-muted">
          {t('places.model')}
        </Text>
        {model.map((line) => (
          <Text key={line} variant="body">
            {line}
          </Text>
        ))}
      </Card>

      <SectionHeader title={`${t('places.studentsHere')} · ${students.length}`} />
      {students.length > 0 ? (
        <ListGroup>
          {students.map((s) => (
            <ListRow
              key={s.id}
              title={s.name}
              avatar={{ initials: initials(s.name), tag: s.color }}
              subtitle={s.subject ?? undefined}
              onPress={() => router.push(`/catalog/student/${s.id}`)}
            />
          ))}
        </ListGroup>
      ) : null}
      <Button variant="outline" onPress={() => setPicking(true)}>
        {t('places.linkStudent')}
      </Button>

      <SectionHeader title={t('places.groups')} />
      {groups.length > 0 ? (
        <ListGroup>
          {groups.map((g) => (
            <ListRow
              key={g.id}
              title={g.name}
              subtitle={[
                g.subject,
                t('groups.size', { count: g.memberIds.length }),
                g.rateCents !== null
                  ? t('places.perLesson', { price: rateMoney(g.rateCents, language) })
                  : null,
              ]
                .filter(Boolean)
                .join(' · ')}
              onPress={() => router.push(`/catalog/group/edit?id=${g.id}`)}
            />
          ))}
        </ListGroup>
      ) : null}
      <Button
        variant="outline"
        onPress={() => router.push(`/catalog/group/edit?placeId=${place.id}`)}
      >
        {t('places.newGroup')}
      </Button>

      {picking ? (
        <BottomSheet title={t('places.linkWhich')} onClose={() => setPicking(false)}>
          {unlinked.length === 0 ? (
            <Text variant="body" className="text-ink-muted">
              {t('places.noStudentsToLink')}
            </Text>
          ) : (
            <ListGroup>
              {unlinked.map((s) => (
                <ListRow
                  key={s.id}
                  title={s.name}
                  avatar={{ initials: initials(s.name), tag: s.color }}
                  trailing="none"
                  onPress={() => {
                    linkStudentToPlace(s.id, place.id);
                    setPicking(false);
                  }}
                />
              ))}
            </ListGroup>
          )}
        </BottomSheet>
      ) : null}
    </Screen>
  );
}
