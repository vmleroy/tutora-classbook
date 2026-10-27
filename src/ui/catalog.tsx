// Text and small composites shared by the places, students and groups screens (spec 004).
// Built only from design-system components.
import type { TFunction } from 'i18next';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { MAX_DURATION_MIN } from '@/domain/duration';
import type { Cents, RateType } from '@/domain/money';
import type { Place } from '@/domain/places';
import type { Language } from '@/domain/settings';
import { BottomSheet } from '@/ui/components/BottomSheet';
import { Button } from '@/ui/components/Button';
import { Stepper } from '@/ui/components/Stepper';
import { TextField } from '@/ui/components/TextField';
import { formatMoney } from '@/ui/format';

export function useLanguage(): Language {
  const { i18n } = useTranslation();
  return i18n.language === 'en' ? 'en' : 'pt-BR';
}

/** Built-in names follow the app language; names the teacher typed never change. */
export function placeName(place: Pick<Place, 'builtin' | 'name'>, t: TFunction): string {
  return place.builtin ? t(`places.builtin.${place.builtin}`) : (place.name ?? '');
}

/** Whole reais read "R$ 120"; anything with centavos keeps them. */
export function rateMoney(cents: Cents, language: Language): string {
  return formatMoney(cents, language, cents % 100 === 0);
}

export function rateText(cents: Cents, rateType: RateType, t: TFunction, language: Language) {
  const price = rateMoney(cents, language);
  return rateType === 'hour' ? t('places.perHour', { price }) : t('places.perLesson', { price });
}

/** "R$ 60/h pagos pelo local", "Salário R$ 2.400/mês", "Sem preço"… */
export function placePriceText(place: Place, t: TFunction, language: Language): string {
  if (place.kind === 'school') {
    return place.monthlySalaryCents === null
      ? t('places.noPrice')
      : t('places.salary', { price: rateMoney(place.monthlySalaryCents, language) });
  }
  if (place.rateCents === null) return t('places.noPrice');
  const rate = rateText(place.rateCents, place.rateType, t, language);
  return place.payer === 'place' ? t('places.paidByPlace', { price: rate }) : rate;
}

export function travelText(place: Place, t: TFunction, language: Language): string {
  return t(`places.travel.${place.travelMode}`, {
    price: rateMoney(place.travelFeeCents, language),
  });
}

export function initials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);
  const first = words[0]?.[0] ?? '';
  const last = words.length > 1 ? (words[words.length - 1]?.[0] ?? '') : (words[0]?.[1] ?? '');
  return (first + last).toUpperCase();
}

const STEP = 500; // R$ 5

/** A price stepper in R$ 5 steps; tapping the amount opens a money field for the exact value. */
export function PriceStepper({
  label,
  cents,
  onChange,
  format,
}: {
  label: string;
  cents: Cents;
  onChange: (cents: Cents) => void;
  format?: (cents: Cents) => string;
}) {
  const { t } = useTranslation();
  const language = useLanguage();
  const [exact, setExact] = useState<Cents | null>(null);
  return (
    <>
      <Stepper
        value={cents}
        step={STEP}
        min={0}
        valueLabel={label}
        format={format ?? ((v) => rateMoney(v, language))}
        onChange={onChange}
        onPressValue={() => setExact(cents)}
      />
      {exact !== null ? (
        <BottomSheet
          title={label}
          subtitle={t('catalog.exactAmount')}
          onClose={() => setExact(null)}
          actions={
            <Button
              onPress={() => {
                onChange(exact);
                setExact(null);
              }}
            >
              {t('catalog.done')}
            </Button>
          }
        >
          <TextField kind="money" label={label} cents={exact} onChangeCents={setExact} />
        </BottomSheet>
      ) : null}
    </>
  );
}

/** "45 min", or "90 min · 1h30" from an hour on, as in the mockups. */
export function durationText(minutes: number, t: TFunction): string {
  const label = t('places.minutes', { count: minutes });
  if (minutes < 60) return label;
  const rest = minutes % 60;
  return `${label} · ${Math.floor(minutes / 60)}h${rest ? String(rest).padStart(2, '0') : ''}`;
}

/** A duration stepper in 5-minute steps; tapping the time opens hh:mm entry. */
export function DurationStepper({
  label,
  minutes,
  onChange,
  min = 15,
  max = MAX_DURATION_MIN,
}: {
  label: string;
  minutes: number;
  onChange: (minutes: number) => void;
  min?: number;
  max?: number;
}) {
  const { t } = useTranslation();
  const [exact, setExact] = useState<number | null | undefined>(undefined);
  const valid = typeof exact === 'number' && exact >= min;
  return (
    <>
      <Stepper
        value={minutes}
        step={5}
        min={min}
        max={max}
        valueLabel={label}
        format={(v) => durationText(v, t)}
        onChange={onChange}
        onPressValue={() => setExact(minutes)}
      />
      {exact !== undefined ? (
        <BottomSheet
          title={label}
          subtitle={t('catalog.exactDuration')}
          onClose={() => setExact(undefined)}
          actions={
            <Button
              disabled={!valid}
              onPress={() => {
                if (typeof exact === 'number') onChange(exact);
                setExact(undefined);
              }}
            >
              {t('catalog.done')}
            </Button>
          }
        >
          <TextField
            kind="duration"
            label={label}
            minutes={minutes}
            maxMinutes={max}
            onChangeMinutes={setExact}
          />
        </BottomSheet>
      ) : null}
    </>
  );
}

/**
 * Arquivar for an active item; Restaurar and Remover para sempre once archived. Removing
 * asks first, naming the item (constitution rule 3).
 */
export function ArchiveActions({
  name,
  archived,
  removeBody,
  onArchive,
  onRestore,
  onRemove,
}: {
  name: string;
  archived: boolean;
  removeBody?: string;
  onArchive: () => void;
  onRestore: () => void;
  onRemove: () => void;
}) {
  const { t } = useTranslation();
  const [confirming, setConfirming] = useState(false);
  if (!archived) {
    return (
      <Button variant="ghost" icon="archive" onPress={onArchive}>
        {t('catalog.archive')}
      </Button>
    );
  }
  return (
    <View className="gap-2">
      <Button variant="outline" onPress={onRestore}>
        {t('catalog.restore')}
      </Button>
      <Button variant="danger" onPress={() => setConfirming(true)}>
        {t('catalog.remove')}
      </Button>
      {confirming ? (
        <BottomSheet
          title={t('catalog.removeTitle', { name })}
          subtitle={removeBody ?? t('catalog.removeBody')}
          onClose={() => setConfirming(false)}
          actions={
            <>
              <Button variant="outline" onPress={() => setConfirming(false)}>
                {t('catalog.cancel')}
              </Button>
              <Button
                variant="danger"
                onPress={() => {
                  setConfirming(false);
                  onRemove();
                }}
              >
                {t('catalog.remove')}
              </Button>
            </>
          }
        />
      ) : null}
    </View>
  );
}
