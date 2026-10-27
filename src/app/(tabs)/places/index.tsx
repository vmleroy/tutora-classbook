import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { usePlaces, type PlaceRow } from '@/state/catalog';
import { useSettingsStore } from '@/state/settingsStore';
import { placeName, placePriceText, travelText, useLanguage } from '@/ui/catalog';
import { EmptyState } from '@/ui/components/EmptyState';
import { ListGroup } from '@/ui/components/ListGroup';
import { ListRow } from '@/ui/components/ListRow';
import { Screen } from '@/ui/components/Screen';
import { Text } from '@/ui/components/Text';

export default function PlacesScreen() {
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
    <Screen testID="places-screen">
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
    </Screen>
  );
}
