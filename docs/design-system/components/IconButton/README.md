# IconButton

Round 44px button with one icon, for back, close, settings and period arrows.

## Variants
`sunken` (default, on background), `plain` (period arrows), `onPrimary` (on the hero header and the live screen), `accent` (the + in the hero header).

## Consumer provides
`icon`, `label` (required: it is the accessible name, since there is no visible text), `onPress`.

## Rules
- The hit area is always 44×44 even when the drawn circle is smaller.
- One `accent` icon button per screen at most.

## Props

```ts
interface IconButtonProps {
  icon: IconName;
  label: string;
  variant?: 'sunken' | 'plain' | 'onPrimary' | 'accent';
  disabled?: boolean;
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
