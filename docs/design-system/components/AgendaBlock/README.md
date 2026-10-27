# AgendaBlock

A lesson placed on the Agenda day timeline, sized by its duration (44px per hour).

## States
`default`, `done` (check icon), `next` (2px accent outline and "Próxima"). Missed and cancelled blocks use the neutral fill with the title struck through.

## Rules
- Fill = the student's `tag-*-soft`, text = `tag-*-ink`.
- Long-press and drag moves the lesson in 15-minute steps (spec: Schedule).
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
}
```
