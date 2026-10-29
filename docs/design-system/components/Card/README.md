# Card

White (surface) container with `radius-2xl` and 16px padding that groups one subject: today's earnings, the next lesson, a summary.

## Rules
- `flat` (shadow-card) by default; `raised` only for the earnings card that overlaps the hero header; `outline` for selectable cards in sheets (PDF / Excel).
- Cards do not nest. Lists of rows use ListGroup or separate rows, not cards inside cards.

## Props

```ts
interface CardProps {
  elevation?: 'flat' | 'raised' | 'outline';
  label?: string;
  children: React.ReactNode;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```

## 2026 look (spec 016)
Corners `radius-3xl` (28), 12 dp between its children; flat.
