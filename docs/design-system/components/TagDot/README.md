# TagDot

Small round mark in a student's or place's color, used beside names in rows, timelines and charts.

## Rules
- 10px in rows, 8px inside agenda blocks, 5px under week-strip days.
- Never the only signal: the name is always next to it.

## Props

```ts
interface TagDotProps {
  tag?: Tag;
  size?: 'sm' | 'md';
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
