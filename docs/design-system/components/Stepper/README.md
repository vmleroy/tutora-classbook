# Stepper

Minus / value / plus control for durations (15-minute steps) and small money adjustments such as the travel fee.

## Consumer provides
`value`, `onChange`, `step`, `min`, `max`, `format` (value → label), and labels for both buttons ("Menos 15 minutos").

## Rules
- Durations step by 5 minutes and read "90 min · 1h30"; with `onPressValue` the value opens hh:mm entry.
- Prices step by R$ 5; with `onPressValue` the value is a button that opens a sheet with a money field for the exact amount.
- The value uses tabular figures and announces changes politely to screen readers.
- Compact everywhere (Victor, 29 Oct 2026): one height (`size-control-md`), − and + drawn as small circles (`size-tile`) while each still has the full `size-touch` to press; the value in `bodyStrong`. `size` is kept for callers but no longer changes the height.
- Long-press repeats the step in the app (not in this preview).
- Tapping the value opens a number pad for exact entry.

## Props

```ts
interface StepperProps {
  value?: number;
  step?: number;
  min?: number;
  max?: number;
  format?: (value: number) => string;
  onChange?: (value: number) => void;
  /** Tapping the value opens exact entry (a money field in a sheet, for prices). */
  onPressValue?: () => void;
  valueLabel?: string;
  decrementLabel?: string;
  incrementLabel?: string;
  size?: 'sm' | 'md';
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
