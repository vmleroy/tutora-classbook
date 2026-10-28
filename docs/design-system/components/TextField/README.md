# TextField

Labelled single-line input; the label sits above, never only as a placeholder.

## Consumer provides
`label`, `value`, `onChangeText` (or `kind="money"` with `cents` and `onChangeCents`), optional `placeholder` (an example, starting "Ex.:"), `helper`, `error`, `keyboardType`, and `tone="onPrimary"` on the live screen.

## Rules
- `MoneyField` with no amount is empty and shows a muted placeholder on the right: "Sem preço" for a price that may stay unset, "R$ 0,00" otherwise. Typed amounts use the money type (16 sp, heavy, tabular digits), and the field is only as wide as its largest amount (R$ 99.999,99) or its hint, left under its label. Anywhere a price is missing the app says "Sem preço", never "—" or "R$ 0,00".
- A required field ends its label in a red asterisk (`required = true`, read out as "obrigatório"); optional fields carry no mark. Each form with a required field says once, at the top, "Campos com * são obrigatórios". `SelectField`, `SectionHeader` and the app's `Field` take `required` the same way.
- An error makes the input itself blink red (its fill flashes between white and the soft danger red) three times as it appears, and again on each failed Save, then it stays red with a strong red border and the message below; with reduced motion it is red at once. A `SelectField` left empty takes `isError` the same way. An error that belongs to no field (a missing choice, a clash) is a `FieldError` next to the action that failed.
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
