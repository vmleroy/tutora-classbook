import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { TextInput, View, type TextInputProps } from 'react-native';

import { durationDigits, hhmm, MAX_DURATION_MIN, typedDuration } from '@/domain/duration';
import { MAX_FIELD_CENTS, typedCents } from '@/domain/money';
import type { Language } from '@/domain/settings';
import { cn, tv } from '@/ui/cn';
import { Text } from '@/ui/components/Text';
import { formatMoney } from '@/ui/format';
import { useTheme } from '@/ui/theme/ThemeProvider';
import { typeStyles } from '@/ui/theme/tokens';

export interface TextFieldProps {
  label: string;
  value?: string;
  placeholder?: string;
  helper?: string;
  error?: string;
  inputMode?: TextInputProps['inputMode'];
  tone?: 'default' | 'onPrimary';
  onChangeText?: (text: string) => void;
  /**
   * `money` shows `cents` in reais and `duration` shows `minutes` as hh:mm; both fill from
   * the right as digits are typed.
   */
  kind?: 'text' | 'money' | 'duration';
  cents?: number;
  maxCents?: number;
  onChangeCents?: (cents: number) => void;
  minutes?: number;
  maxMinutes?: number;
  /** null while the typed time is not a valid duration (minutes over 59, over the max). */
  onChangeMinutes?: (minutes: number | null) => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const input = tv({
  base: 'h-control-md rounded-lg px-3.5 font-regular',
  variants: {
    tone: {
      default: 'border border-line bg-surface text-ink',
      onPrimary: 'border border-on-primary/30 bg-on-primary/10 text-on-primary',
    },
    error: { true: 'border-2 border-danger' },
  },
});

/** Labelled single-line input: the label sits above, never only as a placeholder. */
export function TextField({
  className,
  label,
  value,
  placeholder,
  helper,
  error,
  inputMode,
  tone = 'default',
  onChangeText,
  kind = 'text',
  cents = 0,
  maxCents = MAX_FIELD_CENTS,
  onChangeCents,
  minutes = 0,
  maxMinutes = MAX_DURATION_MIN,
  onChangeMinutes,
}: TextFieldProps) {
  const { palette, textScale } = useTheme();
  const { t, i18n } = useTranslation();
  const language: Language = i18n.language === 'en' ? 'en' : 'pt-BR';
  const money = kind === 'money';
  const duration = kind === 'duration';
  // An hh:mm field keeps its own digits: "00:90" is mid-typing, not yet a duration.
  const [digits, setDigits] = useState(() => durationDigits(minutes));
  const durationValid = typedDuration(digits, digits, maxMinutes).minutes !== null;
  const shown = money ? formatMoney(cents, language) : duration ? hhmm(digits) : value;
  const invalid = duration && !durationValid;
  const fieldError =
    error ??
    (invalid ? t('ui.durationInvalid', { max: hhmm(durationDigits(maxMinutes)) }) : undefined);
  const numeric = money || duration;
  const body = typeStyles.body;
  const help = fieldError ?? helper;
  return (
    <View className={cn('gap-1.5', className)}>
      <Text
        variant="field-label"
        className={tone === 'onPrimary' ? 'text-on-primary-muted' : 'text-ink-muted'}
      >
        {label}
      </Text>
      <TextInput
        accessibilityLabel={label}
        accessibilityHint={help}
        aria-invalid={!!fieldError}
        value={shown}
        placeholder={placeholder}
        inputMode={numeric ? 'numeric' : inputMode}
        // The caret stays at the end, where digits are added and removed.
        selection={numeric ? { start: shown?.length ?? 0, end: shown?.length ?? 0 } : undefined}
        onChangeText={
          money
            ? (text) => onChangeCents?.(typedCents(text, cents, maxCents))
            : duration
              ? (text) => {
                  const next = typedDuration(text, digits, maxMinutes);
                  setDigits(next.digits);
                  onChangeMinutes?.(next.minutes);
                }
              : onChangeText
        }
        placeholderTextColor={
          tone === 'onPrimary' ? palette['on-primary-muted'] : palette['ink-muted']
        }
        className={cn(input({ tone, error: !!fieldError }), numeric && 'text-right')}
        style={{ fontSize: body.fontSize * textScale }}
      />
      {help ? (
        <Text
          variant="meta"
          weight={fieldError ? 700 : undefined}
          className={fieldError ? 'text-danger-ink' : 'text-ink-muted'}
        >
          {help}
        </Text>
      ) : null}
    </View>
  );
}
