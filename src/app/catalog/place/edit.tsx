import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import {
  builtinPlace,
  earningsType,
  MAX_COMMISSION_BP,
  ownKinds,
  payMode,
  type Place,
} from '@/domain/places';
import { previewLesson } from '@/domain/pricing';
import { useCatalogActions, usePlace } from '@/state/catalog';
import {
  ArchiveActions,
  DurationStepper,
  PriceStepper,
  placeName,
  rateMoney,
  useLanguage,
} from '@/ui/catalog';
import { Button } from '@/ui/components/Button';
import { Card } from '@/ui/components/Card';
import { Chip } from '@/ui/components/Chip';
import { Screen } from '@/ui/components/Screen';
import { SegmentedControl } from '@/ui/components/SegmentedControl';
import { Stepper } from '@/ui/components/Stepper';
import { Text } from '@/ui/components/Text';
import { TextField } from '@/ui/components/TextField';
import { formatMoney } from '@/ui/format';

const newPlace: Place = {
  ...builtinPlace('online'),
  id: '',
  builtin: null,
  name: '',
  kind: 'salinha',
  payer: 'place',
  payCycle: 'monthly',
  travelMode: 'none',
};

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <View className="gap-1.5">
      <Text variant="field-label" className="text-ink-muted">
        {label}
      </Text>
      {children}
    </View>
  );
}

export default function PlaceEdit() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const { data } = usePlace(id);
  // The form mounts once the saved place is loaded, starting from it.
  if (id && !data) return <Screen testID="place-edit">{null}</Screen>;
  return <PlaceForm id={id} initial={data?.place ?? newPlace} />;
}

function PlaceForm({ id, initial }: { id?: string; initial: Place }) {
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const actions = useCatalogActions();
  const [draft, setDraft] = useState<Place>(initial);
  const [error, setError] = useState<string | null>(null);

  const set = (patch: Partial<Place>) => setDraft((d) => ({ ...d, ...patch }));
  const builtin = !!draft.builtin;
  const mode = payMode(draft.kind);
  const money = (cents: number) => formatMoney(cents, language);
  const preview = previewLesson(draft, 60);
  const previewText =
    preview.kind === 'placePays'
      ? t('places.preview.placePays', {
          earned: money(preview.earned),
          cycle: t(`places.cycleWords.${draft.payCycle}`),
        })
      : preview.kind === 'studentPays'
        ? t('places.preview.studentPays', { paid: money(preview.paid), kept: money(preview.kept) })
        : t(`places.preview.${preview.kind}`);

  const save = () => {
    const { id: savedId, archivedAt: _a, ...rest } = draft;
    const result = actions.savePlace({ ...rest, ...(savedId ? { id: savedId } : {}) });
    if (!result.ok) return setError(t(`catalog.errors.${result.error}`));
    router.back();
  };

  const rateFormat = (v: number) =>
    draft.rateCents === null ? t('places.noPrice') : rateMoney(v, language);

  return (
    <Screen testID="place-edit">
      <Stack.Screen options={{ title: t(id ? 'places.edit' : 'places.new') }} />
      {builtin ? (
        <Text variant="title-lg">{placeName(draft, t)}</Text>
      ) : (
        <>
          <TextField
            label={t('places.name')}
            placeholder={t('places.namePlaceholder')}
            value={draft.name ?? ''}
            error={error ?? undefined}
            onChangeText={(name) => set({ name })}
          />
          <TextField
            label={t('places.address')}
            value={draft.address ?? ''}
            onChangeText={(address) => set({ address: address || null })}
          />
          <Field label={t('places.type')}>
            <View className="flex-row flex-wrap gap-2">
              {ownKinds.map((kind) => (
                <Chip
                  key={kind}
                  label={t(`places.kind.${kind}`)}
                  selected={draft.kind === kind}
                  onPress={() =>
                    set({ kind, payer: payMode(kind) === 'choose' ? draft.payer : 'student' })
                  }
                />
              ))}
            </View>
            <Text variant="meta" className="text-ink-muted">
              {t(`places.earnings.${earningsType(draft.kind)}`)}
            </Text>
          </Field>
        </>
      )}

      {mode === 'salary' ? (
        <Field label={t('places.salaryLabel')}>
          <PriceStepper
            label={t('places.salaryLabel')}
            cents={draft.monthlySalaryCents ?? 0}
            onChange={(monthlySalaryCents) => set({ monthlySalaryCents })}
          />
        </Field>
      ) : (
        <>
          {mode === 'choose' ? (
            <Field label={t('places.whoPays')}>
              <SegmentedControl
                label={t('places.whoPays')}
                value={draft.payer}
                onChange={(payer) => set({ payer })}
                options={[
                  { value: 'place', label: t('places.payerPlace') },
                  { value: 'student', label: t('places.payerStudent') },
                ]}
              />
            </Field>
          ) : null}
          <Field label={t('places.charging')}>
            <SegmentedControl
              label={t('places.charging')}
              size="sm"
              value={draft.rateType}
              onChange={(rateType) => set({ rateType })}
              options={[
                { value: 'hour', label: t('places.perHourOption') },
                { value: 'lesson', label: t('places.perLessonOption') },
              ]}
            />
          </Field>
          <Field label={t(draft.payer === 'place' ? 'places.placePays' : 'places.studentPays')}>
            <PriceStepper
              label={t(draft.payer === 'place' ? 'places.placePays' : 'places.studentPays')}
              cents={draft.rateCents ?? 0}
              format={rateFormat}
              onChange={(rateCents) => set({ rateCents })}
            />
          </Field>
          {draft.payer === 'student' && mode === 'choose' ? (
            <Field label={t('places.commission')}>
              <Stepper
                value={draft.commissionBp}
                step={500}
                min={0}
                max={MAX_COMMISSION_BP}
                format={(v) => `${v / 100}%`}
                onChange={(commissionBp) => set({ commissionBp })}
              />
            </Field>
          ) : null}
          {draft.payer === 'place' ? (
            <Field label={t('places.cycle')}>
              <View className="flex-row flex-wrap gap-2">
                {(['lesson', 'weekly', 'monthly'] as const).map((payCycle) => (
                  <Chip
                    key={payCycle}
                    label={t(`places.cycles.${payCycle}`)}
                    selected={draft.payCycle === payCycle}
                    onPress={() => set({ payCycle })}
                  />
                ))}
              </View>
            </Field>
          ) : null}
        </>
      )}

      <Field label={t('places.duration')}>
        <DurationStepper
          label={t('places.duration')}
          minutes={draft.defaultDurationMin}
          onChange={(defaultDurationMin) => set({ defaultDurationMin })}
        />
      </Field>

      {draft.kind === 'student_home' ? (
        <Field label={t('places.travelLabel')}>
          <SegmentedControl
            label={t('places.travelLabel')}
            size="sm"
            value={draft.travelMode}
            onChange={(travelMode) => set({ travelMode })}
            options={(['varies', 'fixed', 'none'] as const).map((value) => ({
              value,
              label: t(`places.travelOptions.${value}`),
            }))}
          />
          {draft.travelMode === 'fixed' ? (
            <PriceStepper
              label={t('places.travelFee')}
              cents={draft.travelFeeCents}
              onChange={(travelFeeCents) => set({ travelFeeCents })}
            />
          ) : null}
        </Field>
      ) : null}

      <Card label={t('places.previewTitle')}>
        <Text variant="caption" className="text-ink-muted">
          {t('places.previewTitle')}
        </Text>
        <Text variant="body-strong" testID="place-preview">
          {previewText}
        </Text>
      </Card>

      {error && builtin ? (
        <Text variant="meta" className="text-danger-ink">
          {error}
        </Text>
      ) : null}
      <Button fullWidth onPress={save}>
        {t('places.save')}
      </Button>
      {id && !builtin ? (
        <ArchiveActions
          name={placeName(draft, t)}
          archived={!!draft.archivedAt}
          removeBody={t('catalog.removePlaceBody')}
          onArchive={() => {
            actions.archive('place', id);
            router.back();
          }}
          onRestore={() => {
            actions.restore('place', id);
            router.back();
          }}
          onRemove={() => {
            actions.removePermanently('place', id);
            router.dismissTo('/places');
          }}
        />
      ) : null}
    </Screen>
  );
}
