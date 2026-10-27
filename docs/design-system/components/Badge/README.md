# Badge

Short status word on a tinted pill: paid, owed, live, school, group, cancelled.

## Tones
`success` paid, done, ligado · `danger` owed, missed, overdue · `accent` live, next, package ending · `secondary` school and formal job · `violet` group class · `neutral` cancelled, inactive.

## Rules
- The word carries the meaning; color only reinforces it, so status is readable in grayscale.
- One badge per row. Amounts in badges are compact ("Deve R$ 240").

## Props

```ts
interface BadgeProps {
  tone?: 'neutral' | 'success' | 'danger' | 'accent' | 'secondary' | 'violet';
  icon?: IconName;
  children: React.ReactNode;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
