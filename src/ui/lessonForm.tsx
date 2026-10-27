// The class form: Nova aula (Agendar) and Editar aula (spec 005).
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import type { Cents } from '@/domain/money';
import type { Group, Place, Student } from '@/domain/places';
import { utcToLocal, type LocalDate, type LocalTime, type Repeat } from '@/domain/recurrence';
import { lessonWorth } from '@/domain/schedule';
import type { StudentWithLinks } from '@/state/catalog';
import { DurationStepper, initials, placeName, PriceStepper, useLanguage } from '@/ui/catalog';
import { BottomSheet } from '@/ui/components/BottomSheet';
import { Button } from '@/ui/components/Button';
import { Card } from '@/ui/components/Card';
import { Chip } from '@/ui/components/Chip';
import { ListGroup } from '@/ui/components/ListGroup';
import { ListRow } from '@/ui/components/ListRow';
import { SegmentedControl } from '@/ui/components/SegmentedControl';
import { SelectField } from '@/ui/components/SelectField';
import { Text } from '@/ui/components/Text';
import { TextField } from '@/ui/components/TextField';
import { formatShortDate, formatTime, formatWeekdayLong } from '@/ui/format';
import { DateSheet, worthText } from '@/ui/schedule';

export type Who = { kind: 'student'; id: string } | { kind: 'group'; id: string };

export type LessonValues = {
  who: Who | null;
  placeId: string | null;
  date: LocalDate;
  time: LocalTime;
  plannedMin: number;
  valueCents: Cents | null;
  travelFeeCents: Cents | null;
  repeat: Repeat | null;
  notes: string | null;
};

export type Scope = 'one' | 'following';

const toMinutes = (time: LocalTime) => {
  const [h, m] = time.split(':').map(Number) as [number, number];
  return h * 60 + m;
};
const toTime = (minutes: number): LocalTime =>
  `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;

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

export function LessonForm({
  initial,
  editing,
  series,
  today,
  students,
  groups,
  places,
  error,
  suggestTravel,
  onSubmit,
  onRemove,
}: {
  initial: LessonValues;
  /** Editar aula: who and place are fixed; the scope question shows for a series. */
  editing?: boolean;
  series?: { repeat: Repeat; startAt: string; zone: string };
  today: LocalDate;
  students: StudentWithLinks[];
  groups: Group[];
  places: Place[];
  error?: string | null;
  suggestTravel: (studentId: string, placeId: string) => Cents | null;
  onSubmit: (values: LessonValues, scope: Scope) => void;
  onRemove?: (scope: Scope) => void;
}) {
  const { t } = useTranslation();
  const language = useLanguage();
  const [v, setV] = useState<LessonValues>(initial);
  const [scope, setScope] = useState<Scope>('one');
  const [sheet, setSheet] = useState<'who' | 'date' | 'remove' | null>(null);
  const [timeValid, setTimeValid] = useState(true);
  const set = (patch: Partial<LessonValues>) => setV((x) => ({ ...x, ...patch }));

  const placeById = new Map(places.map((p) => [p.id, p]));
  const student = v.who?.kind === 'student' ? students.find((s) => s.id === v.who!.id) : undefined;
  const group = v.who?.kind === 'group' ? groups.find((g) => g.id === v.who!.id) : undefined;
  const placeChoices: Place[] = student
    ? student.links.flatMap((l) => placeById.get(l.placeId) ?? [])
    : group
      ? [placeById.get(group.placeId)].filter((p): p is Place => !!p)
      : [];
  const place = v.placeId ? placeById.get(v.placeId) : undefined;
  const travelVaries = place?.kind === 'student_home' && place.travelMode === 'varies';

  const choosePlace = (p: Place, s?: Student) =>
    set({
      placeId: p.id,
      plannedMin: p.defaultDurationMin,
      travelFeeCents:
        p.kind === 'student_home' && p.travelMode === 'varies' && s
          ? (suggestTravel(s.id, p.id) ?? 0)
          : null,
    });

  const chooseWho = (who: Who) => {
    setSheet(null);
    if (who.kind === 'student') {
      const s = students.find((x) => x.id === who.id);
      const star = s?.links.find((l) => l.isDefault) ?? s?.links[0];
      const p = star ? placeById.get(star.placeId) : undefined;
      set({ who, placeId: null });
      if (p && s) choosePlace(p, s);
    } else {
      const g = groups.find((x) => x.id === who.id);
      const p = g ? placeById.get(g.placeId) : undefined;
      set({ who, placeId: null });
      if (p) choosePlace(p);
    }
  };

  const worth =
    place && (student || group)
      ? lessonWorth({
          place,
          plannedMin: v.plannedMin,
          valueCents: v.valueCents,
          travelFeeCents: v.travelFeeCents,
          who: student
            ? {
                kind: 'student',
                student,
                link: student.links.find((l) => l.placeId === place.id),
              }
            : { kind: 'group', group: group! },
        })
      : null;

  const whoLabel = student?.name ?? group?.name;
  const local = series ? utcToLocal(series.startAt, series.zone) : undefined;

  return (
    <View className="gap-4">
      {series && local ? (
        <Text variant="meta" className="text-ink-muted">
          {t('lesson.series', {
            repeat: t(`lesson.repeats.${series.repeat}`),
            weekday: formatWeekdayLong(local.date, language),
            time: formatTime(local.time, language),
          })}
        </Text>
      ) : null}

      <SelectField
        label={t('lesson.who')}
        value={whoLabel ?? t('lesson.pickWho')}
        avatar={
          student
            ? { initials: initials(student.name), tag: student.color }
            : group
              ? { initials: initials(group.name), tag: 'teal' }
              : undefined
        }
        sub={student?.subject ?? group?.subject ?? undefined}
        actionLabel={editing ? undefined : t('lesson.change')}
        onPress={editing ? undefined : () => setSheet('who')}
      />

      {placeChoices.length > 0 ? (
        <Field label={group ? t('lesson.groupPlace') : t('lesson.place')}>
          <View className="flex-row flex-wrap gap-2">
            {placeChoices.map((p) => (
              <Chip
                key={p.id}
                label={placeName(p, t)}
                selected={v.placeId === p.id}
                onPress={editing ? undefined : () => choosePlace(p, student)}
              />
            ))}
          </View>
        </Field>
      ) : null}

      <View className="flex-row gap-3">
        <View className="flex-1">
          <SelectField
            label={t('lesson.date')}
            value={formatShortDate(v.date, language)}
            icon="calendar"
            onPress={() => setSheet('date')}
          />
        </View>
        <View className="flex-1">
          <TextField
            kind="duration"
            label={t('lesson.start')}
            minutes={toMinutes(v.time)}
            maxMinutes={23 * 60 + 59}
            onChangeMinutes={(m) => {
              setTimeValid(m !== null);
              if (m !== null) set({ time: toTime(m) });
            }}
          />
        </View>
      </View>

      <Field label={t('lesson.duration')}>
        <DurationStepper
          label={t('lesson.duration')}
          minutes={v.plannedMin}
          onChange={(plannedMin) => set({ plannedMin })}
        />
      </Field>

      {travelVaries ? (
        <Field label={t('lesson.travel')}>
          <PriceStepper
            label={t('lesson.travel')}
            cents={v.travelFeeCents ?? 0}
            onChange={(travelFeeCents) => set({ travelFeeCents })}
          />
        </Field>
      ) : null}

      <Card label={t('lesson.value')}>
        <Text variant="caption" className="text-ink-muted">
          {v.valueCents !== null ? t('lesson.handValue') : t('lesson.value')}
        </Text>
        <Text variant="money-lg" testID="lesson-value">
          {worth ? worthText(worth, t, language) : '—'}
        </Text>
        {v.valueCents !== null ? (
          <>
            <PriceStepper
              label={t('lesson.value')}
              cents={v.valueCents}
              onChange={(valueCents) => set({ valueCents })}
            />
            <Button variant="ghost" size="sm" onPress={() => set({ valueCents: null })}>
              {t('lesson.usePrice')}
            </Button>
          </>
        ) : (
          <Button
            variant="ghost"
            size="sm"
            onPress={() =>
              set({
                valueCents: worth?.kind === 'set' ? worth.cents - (v.travelFeeCents ?? 0) : 0,
              })
            }
          >
            {t('lesson.setValue')}
          </Button>
        )}
      </Card>

      {editing ? null : (
        <Field label={t('lesson.repeat')}>
          <View className="flex-row flex-wrap gap-2">
            {([null, 'weekly', 'biweekly'] as const).map((r) => (
              <Chip
                key={r ?? 'none'}
                label={t(`lesson.repeats.${r ?? 'none'}`)}
                selected={v.repeat === r}
                onPress={() => set({ repeat: r })}
              />
            ))}
          </View>
        </Field>
      )}

      {series ? (
        <Field label={t('lesson.scope')}>
          <SegmentedControl
            label={t('lesson.scope')}
            size="sm"
            value={scope}
            onChange={setScope}
            options={[
              { value: 'one', label: t('lesson.scopes.one') },
              { value: 'following', label: t('lesson.scopes.following') },
            ]}
          />
        </Field>
      ) : null}

      <TextField
        label={t('lesson.notes')}
        value={v.notes ?? ''}
        onChangeText={(notes) => set({ notes: notes || null })}
      />

      {error ? (
        <Text variant="meta" weight={700} className="text-danger-ink">
          {error}
        </Text>
      ) : null}
      <Button fullWidth disabled={!timeValid} onPress={() => onSubmit(v, scope)}>
        {editing ? t('lesson.saveEdit') : t('lesson.save')}
      </Button>
      {onRemove ? (
        <Button variant="ghost" onPress={() => (series ? setSheet('remove') : onRemove('one'))}>
          {t('lesson.remove')}
        </Button>
      ) : null}

      {sheet === 'who' ? (
        <BottomSheet title={t('lesson.pickWho')} onClose={() => setSheet(null)}>
          <ListGroup>
            {students
              .filter((s) => s.links.length > 0)
              .map((s) => (
                <ListRow
                  key={s.id}
                  title={s.name}
                  subtitle={s.subject ?? undefined}
                  avatar={{ initials: initials(s.name), tag: s.color }}
                  trailing="none"
                  onPress={() => chooseWho({ kind: 'student', id: s.id })}
                />
              ))}
            {groups.map((g) => (
              <ListRow
                key={g.id}
                title={g.name}
                subtitle={g.subject ?? undefined}
                icon="users"
                trailing="none"
                onPress={() => chooseWho({ kind: 'group', id: g.id })}
              />
            ))}
          </ListGroup>
        </BottomSheet>
      ) : null}
      {sheet === 'date' ? (
        <DateSheet
          title={t('lesson.pickDate')}
          value={v.date}
          today={today}
          onPick={(date) => set({ date })}
          onClose={() => setSheet(null)}
        />
      ) : null}
      {sheet === 'remove' && onRemove ? (
        <BottomSheet
          title={t('lesson.removeTitle')}
          subtitle={t('lesson.removeSeries')}
          onClose={() => setSheet(null)}
          actions={
            <>
              <Button variant="outline" onPress={() => onRemove('one')}>
                {t('lesson.scopes.one')}
              </Button>
              <Button variant="danger" onPress={() => onRemove('following')}>
                {t('lesson.scopes.following')}
              </Button>
            </>
          }
        />
      ) : null}
    </View>
  );
}
