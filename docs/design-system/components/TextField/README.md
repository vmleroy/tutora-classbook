# TextField

Labelled single-line input; the label sits above, never only as a placeholder.

## Consumer provides
`label`, `value`, `onChangeText` (or `kind="money"` with `cents` and `onChangeCents`), optional `placeholder` (an example, starting "Ex.:"), `helper`, `error`, `keyboardType`, and `tone="onPrimary"` on the live screen.

## Rules
- Errors say what is wrong and how to fix it, in one line, replacing the helper.
- Money fields (`kind="money"`) show reais in the app language and fill from the right: typing 1, 2, 0, 0, 0 reads `R$ 120,00`. Only digits count, deleting drops the last digit, and digits past `maxCents` (R$ 99.999,99 by default) are refused. The value is integer cents; the rules live in `typedCents` in `@/domain/money`.
- Duration fields (`kind="duration"`) show `minutes` as hh:mm and fill from the right the same way (1, 3, 0 → `01:30`). Minutes over 59 or a total over `maxMinutes` (5 hours by default) show an error and report `null`. Rules in `typedDuration` in `@/domain/duration`.
- Height 48px, `radius-lg`, `line` border; error = 2px `danger` border plus `danger-ink` text.

## Props

```ts
interface TextFieldProps {
  label: string;
  value?: string;
  placeholder?: string;
  helper?: string;
  error?: string;
  inputMode?: TextInputProps['inputMode'];
  tone?: 'default' | 'onPrimary';
  onChangeText?: (text: string) => void;
  kind?: 'text' | 'money' | 'duration';
  cents?: number;
  maxCents?: number;
  onChangeCents?: (cents: number) => void;
  minutes?: number;
  maxMinutes?: number;
  onChangeMinutes?: (minutes: number | null) => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
