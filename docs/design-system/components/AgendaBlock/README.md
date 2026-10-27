# AgendaBlock

A lesson placed on the Agenda day timeline, sized by its duration (44px per hour).

## States
`default`, `done` (check icon), `next` (2px accent outline and "Próxima"). Missed and cancelled blocks use the neutral fill with the title struck through.

## Rules
- Fill = the student's `tag-*-soft`, text = `tag-*-ink`.
- Placed by the screen in a positioned view: top from the start time, height from the duration, and side by side when classes overlap. Moving a class goes through its edit form (no drag).
- The now-line is a 2px `danger` rule with a dot at the left.

## Props

```ts
interface AgendaBlockProps {
  tag: Tag;
  title: string;
  subtitle?: string;
  state?: 'default' | 'done' | 'next';
  height?: number;
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
