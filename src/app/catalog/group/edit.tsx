import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import type { Group } from '@/domain/places';
import { useCatalogActions, useGroup, usePlace } from '@/state/catalog';
import { ArchiveActions, PriceStepper, placeName, useLanguage, rateText } from '@/ui/catalog';
import { Button } from '@/ui/components/Button';
import { Checkbox } from '@/ui/components/Checkbox';
import { Screen } from '@/ui/components/Screen';
import { Text } from '@/ui/components/Text';
import { TextField } from '@/ui/components/TextField';

export default function GroupEdit() {
  const { id, placeId } = useLocalSearchParams<{ id?: string; placeId?: string }>();
  const { data: saved } = useGroup(id);
  // The form mounts once the saved group is loaded, starting from it.
  if (id && !saved) return <Screen testID="group-edit">{null}</Screen>;
  const initial: Group = saved ?? {
    id: '',
    placeId: placeId ?? '',
    name: '',
    subject: null,
    rateCents: null,
    memberIds: [],
    archivedAt: null,
  };
  return <GroupForm id={id} initial={initial} />;
}

function GroupForm({ id, initial }: { id?: string; initial: Group }) {
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const [draft, setDraft] = useState<Group>(initial);
  const { data: at } = usePlace(draft.placeId || undefined);
  const actions = useCatalogActions();
  const [error, setError] = useState<string | null>(null);
  const set = (patch: Partial<Group>) => setDraft((d) => ({ ...d, ...patch }));
  const school = at?.place.kind === 'school';

  const save = () => {
    const { id: savedId, archivedAt: _a, ...rest } = draft;
    const result = actions.saveGroup({ ...rest, ...(savedId ? { id: savedId } : {}) });
    if (!result.ok) return setError(t(`catalog.errors.${result.error}`));
    router.back();
  };

  return (
    <Screen testID="group-edit">
      <Stack.Screen options={{ title: t(id ? 'groups.edit' : 'groups.new') }} />
      {at ? (
        <Text variant="meta" className="text-ink-muted">
          {placeName(at.place, t)}
        </Text>
      ) : null}
      <TextField
        label={t('groups.name')}
        placeholder={t('groups.namePlaceholder')}
        value={draft.name}
        error={error ?? undefined}
        onChangeText={(name) => set({ name })}
      />
      <TextField
        label={t('groups.subject')}
        value={draft.subject ?? ''}
        onChangeText={(subject) => set({ subject: subject || null })}
      />
      {school ? (
        <Text variant="body" className="text-ink-muted">
          {t('groups.coveredBySalary')}
        </Text>
      ) : (
        <View className="gap-1.5">
          <Text variant="field-label" className="text-ink-muted">
            {t('groups.price')}
          </Text>
          <PriceStepper
            label={t('groups.price')}
            cents={draft.rateCents ?? 0}
            format={(v) =>
              draft.rateCents === null ? t('places.noPrice') : rateText(v, 'lesson', t, language)
            }
            onChange={(rateCents) => set({ rateCents })}
          />
        </View>
      )}
      <View className="gap-2">
        <Text variant="field-label" className="text-ink-muted">
          {t('groups.members')}
        </Text>
        <Text variant="meta" className="text-ink-muted">
          {t('groups.membersHint')}
        </Text>
        {(at?.students ?? []).map((s) => (
          <Checkbox
            key={s.id}
            label={s.name}
            checked={draft.memberIds.includes(s.id)}
            onChange={(on) =>
              set({
                memberIds: on
                  ? [...draft.memberIds, s.id]
                  : draft.memberIds.filter((m) => m !== s.id),
              })
            }
          />
        ))}
      </View>
      <Button fullWidth onPress={save}>
        {t('catalog.save')}
      </Button>
      {id ? (
        <ArchiveActions
          name={draft.name}
          archived={!!draft.archivedAt}
          onArchive={() => {
            actions.archive('group', id);
            router.back();
          }}
          onRestore={() => {
            actions.restore('group', id);
            router.back();
          }}
          onRemove={() => {
            actions.removePermanently('group', id);
            router.back();
          }}
        />
      ) : null}
    </Screen>
  );
}
