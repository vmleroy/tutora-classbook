import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import {
  randomTag,
  tagColors,
  unlink,
  withDefault,
  type PlaceLink,
  type Student,
} from '@/domain/places';
import { useCatalogActions, usePlaces, useStudent } from '@/state/catalog';
import {
  ArchiveActions,
  PriceStepper,
  placeName,
  placePriceText,
  rateText,
  useLanguage,
} from '@/ui/catalog';
import { cn } from '@/ui/cn';
import { BottomSheet } from '@/ui/components/BottomSheet';
import { Button } from '@/ui/components/Button';
import { Card } from '@/ui/components/Card';
import { IconButton } from '@/ui/components/IconButton';
import { ListGroup } from '@/ui/components/ListGroup';
import { ListRow } from '@/ui/components/ListRow';
import { Screen } from '@/ui/components/Screen';
import { SectionHeader } from '@/ui/components/SectionHeader';
import { SegmentedControl } from '@/ui/components/SegmentedControl';
import { TagDot } from '@/ui/components/TagDot';
import { Text } from '@/ui/components/Text';
import { TextField } from '@/ui/components/TextField';
import { Touchable } from '@/ui/components/Touchable';

const blank: Student & { links?: PlaceLink[] } = {
  id: '',
  name: '',
  subject: null,
  phone: null,
  notes: null,
  color: 'amber',
  rateType: 'hour',
  archivedAt: null,
};

export default function StudentEdit() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const { data } = useStudent(id);
  // A new student starts with a random color; the teacher can still pick another.
  const [fresh] = useState(() => ({ ...blank, color: randomTag(Math.random()) }));
  // The form mounts once the saved student is loaded, starting from it.
  if (id && !data) return <Screen testID="student-edit">{null}</Screen>;
  const { links = [], ...student } = data ?? fresh;
  return <StudentForm id={id} initial={student} initialLinks={links} />;
}

function StudentForm({
  id,
  initial,
  initialLinks,
}: {
  id?: string;
  initial: Student;
  initialLinks: PlaceLink[];
}) {
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const { data: places = [] } = usePlaces();
  const actions = useCatalogActions();
  const [draft, setDraft] = useState<Student>(initial);
  const [links, setLinks] = useState<PlaceLink[]>(initialLinks);
  const [picking, setPicking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const set = (patch: Partial<Student>) => setDraft((d) => ({ ...d, ...patch }));
  const setLink = (placeId: string, patch: Partial<PlaceLink>) =>
    setLinks((ls) => ls.map((l) => (l.placeId === placeId ? { ...l, ...patch } : l)));
  const placeById = new Map(places.map((p) => [p.id, p]));
  const available = places.filter((p) => !links.some((l) => l.placeId === p.id));
  const unit = (cents: number) => rateText(cents, draft.rateType, t, language);

  const save = () => {
    const { id: savedId, archivedAt: _a, ...rest } = draft;
    const result = actions.saveStudent({ ...rest, ...(savedId ? { id: savedId } : {}) }, links);
    if (!result.ok) return setError(t(`catalog.errors.${result.error}`));
    router.back();
  };

  return (
    <Screen testID="student-edit">
      <Stack.Screen options={{ title: t(id ? 'students.edit' : 'students.new') }} />
      <TextField
        label={t('students.name')}
        placeholder={t('students.namePlaceholder')}
        value={draft.name}
        error={error === t('catalog.errors.nameRequired') ? error : undefined}
        onChangeText={(name) => set({ name })}
      />
      <TextField
        label={t('students.subject')}
        placeholder={t('students.subjectPlaceholder')}
        value={draft.subject ?? ''}
        onChangeText={(subject) => set({ subject: subject || null })}
      />
      <TextField
        label={t('students.phone')}
        inputMode="tel"
        value={draft.phone ?? ''}
        onChangeText={(phone) => set({ phone: phone || null })}
      />

      <SectionHeader title={t('students.wherePays')} aside={t('students.starHint')} />
      <SegmentedControl
        label={t('students.charging')}
        size="sm"
        value={draft.rateType}
        onChange={(rateType) => set({ rateType })}
        options={[
          { value: 'hour', label: t('students.perHour') },
          { value: 'lesson', label: t('students.perLesson') },
        ]}
      />
      {links.map((link) => {
        const place = placeById.get(link.placeId);
        if (!place) return null;
        const name = placeName(place, t);
        const placePays = place.payer === 'place' || place.kind === 'school';
        const custom = link.rateCents !== null;
        return (
          <Card key={link.placeId} elevation={link.isDefault ? 'raised' : 'outline'}>
            <View className="flex-row items-center gap-2">
              <IconButton
                icon="star"
                variant={link.isDefault ? 'accent' : 'sunken'}
                label={t('students.makeDefault', { place: name })}
                onPress={() =>
                  setLinks((ls) =>
                    withDefault(ls.map((l) => ({ ...l, isDefault: l.placeId === link.placeId }))),
                  )
                }
              />
              <View className="min-w-0 flex-1">
                <Text variant="body-strong">{name}</Text>
                <Text variant="meta" className="text-ink-muted">
                  {placePays
                    ? t('students.paidByPlace', { price: placePriceText(place, t, language) })
                    : custom
                      ? t('students.ownPrice')
                      : t('students.placePrice', { price: placePriceText(place, t, language) })}
                </Text>
              </View>
              <IconButton
                icon="close"
                variant="plain"
                label={t('students.unlink', { place: name })}
                onPress={() => setLinks((ls) => unlink(ls, link.placeId))}
              />
            </View>
            {placePays ? null : (
              <View className="mt-2 gap-2">
                <SegmentedControl
                  label={name}
                  size="sm"
                  value={custom ? 'custom' : 'place'}
                  onChange={(v) =>
                    setLink(link.placeId, {
                      rateCents: v === 'custom' ? (place.rateCents ?? 0) : null,
                    })
                  }
                  options={[
                    { value: 'place', label: t('students.fromPlace') },
                    { value: 'custom', label: t('students.custom') },
                  ]}
                />
                {custom ? (
                  <PriceStepper
                    label={t('students.custom')}
                    cents={link.rateCents ?? 0}
                    format={unit}
                    onChange={(rateCents) => setLink(link.placeId, { rateCents })}
                  />
                ) : null}
              </View>
            )}
          </Card>
        );
      })}
      {error && error !== t('catalog.errors.nameRequired') ? (
        <Text variant="meta" weight={700} className="text-danger-ink">
          {error}
        </Text>
      ) : null}
      <Button variant="outline" onPress={() => setPicking(true)}>
        {t('students.linkPlace')}
      </Button>

      <View className="gap-1.5">
        <Text variant="field-label" className="text-ink-muted">
          {t('students.color')}
        </Text>
        <View accessibilityRole="radiogroup" className="flex-row flex-wrap gap-2">
          {tagColors.map((color) => (
            <Touchable
              key={color}
              accessibilityRole="radio"
              accessibilityLabel={t(`students.colors.${color}`)}
              accessibilityState={{ checked: draft.color === color }}
              onPress={() => set({ color })}
              className={cn(
                'h-touch w-touch items-center justify-center rounded-full border-2',
                draft.color === color ? 'border-primary' : 'border-transparent',
              )}
            >
              <TagDot tag={color} />
            </Touchable>
          ))}
        </View>
      </View>
      <TextField
        label={t('students.notes')}
        value={draft.notes ?? ''}
        onChangeText={(notes) => set({ notes: notes || null })}
      />

      <Button fullWidth onPress={save}>
        {t('catalog.save')}
      </Button>
      {id ? (
        <ArchiveActions
          name={draft.name}
          archived={!!draft.archivedAt}
          onArchive={() => {
            actions.archive('student', id);
            router.back();
          }}
          onRestore={() => {
            actions.restore('student', id);
            router.back();
          }}
          onRemove={() => {
            actions.removePermanently('student', id);
            router.dismissTo('/students');
          }}
        />
      ) : null}

      {picking ? (
        <BottomSheet title={t('students.linkWhich')} onClose={() => setPicking(false)}>
          <ListGroup>
            {available.map((p) => (
              <ListRow
                key={p.id}
                title={placeName(p, t)}
                subtitle={placePriceText(p, t, language)}
                trailing="none"
                onPress={() => {
                  setLinks((ls) =>
                    withDefault([...ls, { placeId: p.id, isDefault: false, rateCents: null }]),
                  );
                  setPicking(false);
                }}
              />
            ))}
          </ListGroup>
        </BottomSheet>
      ) : null}
    </Screen>
  );
}
