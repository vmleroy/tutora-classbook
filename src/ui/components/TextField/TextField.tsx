import { TextInput, View, type TextInputProps } from 'react-native';

import { cn, tv } from '@/ui/cn';
import { Text } from '@/ui/components/Text';
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
}: TextFieldProps) {
  const { palette, textScale } = useTheme();
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
        value={value}
        placeholder={placeholder}
        inputMode={inputMode}
        onChangeText={onChangeText}
        placeholderTextColor={
          tone === 'onPrimary' ? palette['on-primary-muted'] : palette['ink-muted']
        }
        className={input({ tone, error: !!error })}
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
