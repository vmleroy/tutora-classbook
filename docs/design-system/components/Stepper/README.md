# Stepper

Minus / value / plus control for durations (15-minute steps) and small money adjustments such as the travel fee.

## Consumer provides
`value`, `onChange`, `step`, `min`, `max`, `format` (value → label), and labels for both buttons ("Menos 15 minutos").

## Rules
- The value uses tabular figures and announces changes politely to screen readers.
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
  decrementLabel?: string;
  incrementLabel?: string;
  size?: 'sm' | 'md';
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
