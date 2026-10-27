# SectionHeader

Section title with an optional count or hint on the right.

## Rules
- Title in `section` style, sentence case. Aside in `caption` ink-muted, or primary when it is time-sensitive ("em 25 min").

## Props

```ts
interface SectionHeaderProps {
  title: string;
  aside?: string;
  asideTone?: 'muted' | 'primary';
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
