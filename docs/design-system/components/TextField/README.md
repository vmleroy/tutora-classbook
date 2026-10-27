# TextField

Labelled single-line input; the label sits above, never only as a placeholder.

## Consumer provides
`label`, `value`, `onChangeText`, optional `placeholder` (an example, starting "Ex.:"), `helper`, `error`, `keyboardType`, and `tone="onPrimary"` on the live screen.

## Rules
- Errors say what is wrong and how to fix it, in one line, replacing the helper.
- Money fields format as you type in the app currency (`R$ 120,00`) and store cents.
- Height 48px, `radius-lg`, `line` border; error = 2px `danger` border plus `danger-ink` text.

## Props

```ts
interface TextFieldProps {
  label: string;
  value?: string;
  placeholder?: string;
  helper?: string;
  error?: string;
  inputMode?: string;
  tone?: 'default' | 'onPrimary';
  onChangeText?: (text: string) => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
