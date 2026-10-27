# ProgressBar

Horizontal bar for earned vs expected, received vs owed, and each origin's share on Ganhos.

## Pairs
- Earned vs expected: `accent` fill on `accent-soft`.
- Received vs owed: `success` fill on `danger-soft` (the red part is what is still owed).
- Share of total: the origin's tag color on `surface-sunken`, `thin`.

## Rules
- Always next to the numbers it shows; the bar alone never carries the value.

## Props

```ts
interface ProgressBarProps {
  value: number;
  tone?: 'accent' | 'success' | 'primary' | 'secondary';
  track?: 'accent' | 'danger' | 'sunken';
  thin?: boolean;
  label: string;
}
```
