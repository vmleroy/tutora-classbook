# WeekStrip

Seven-day selector at the top of Agenda; a dot marks days that have lessons.

## Rules
- Swipe left or right to change week; the selected day is a primary fill.
- The dot shows the first lesson's tag color; no dot means a free day.

## Props

- `today` marks today's number in `primary` when another day is selected.

```ts
interface WeekStripProps {
  days: { weekday: string; day: number; dot?: Tag; a11y?: string }[];
  selected?: number;
  today?: number;
  label?: string;
  onSelect?: (index: number) => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```

## 2026 look (spec 016)
Days sit on `surface` pills (`radius-2xl`), the chosen one filled with `btnPrimary`.
