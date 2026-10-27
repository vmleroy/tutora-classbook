import { useTranslation } from 'react-i18next';
import { TextInput, View, type TextInputProps } from 'react-native';

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
  /** `money` shows `cents` in reais and fills from the right as digits are typed. */
  kind?: 'text' | 'money';
  cents?: number;
  maxCents?: number;
  onChangeCents?: (cents: number) => void;
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
}: TextFieldProps) {
  const { palette, textScale } = useTheme();
  const { i18n } = useTranslation();
  const language: Language = i18n.language === 'en' ? 'en' : 'pt-BR';
  const money = kind === 'money';
  const shown = money ? formatMoney(cents, language) : value;
  const body = typeStyles.body;
  const help = error ?? helper;
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
        aria-invalid={!!error}
        value={shown}
        placeholder={placeholder}
        inputMode={money ? 'numeric' : inputMode}
        // The caret stays at the end, where digits are added and removed.
        selection={money ? { start: shown?.length ?? 0, end: shown?.length ?? 0 } : undefined}
        onChangeText={
          money ? (text) => onChangeCents?.(typedCents(text, cents, maxCents)) : onChangeText
        }
        placeholderTextColor={
          tone === 'onPrimary' ? palette['on-primary-muted'] : palette['ink-muted']
        }
        className={cn(input({ tone, error: !!error }), money && 'text-right')}
        style={{ fontSize: body.fontSize * textScale }}
      />
      {help ? (
        <Text
          variant="meta"
          weight={error ? 700 : undefined}
          className={error ? 'text-danger-ink' : 'text-ink-muted'}
        >
          {help}
        </Text>
      ) : null}
    </View>
  );
}
