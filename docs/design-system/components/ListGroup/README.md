# ListGroup

Titled white block of ListRows separated by hairlines, as in Ajustes and detail screens.

## Rules
- Group title in `badge` style, ink-muted, sentence case ("Seus dados"), 4px inset.
- Rows inside are separated by `line-soft`; the group has `radius-xl` and no shadow.

## Props

```ts
interface ListGroupProps {
  title?: string;
  children: React.ReactNode;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```

## 2026 look (spec 016)
Every list is a ListGroup: one `surface` card with corners 28 and hairlines between rows, its title above it in the `section` style. Lists of classes put their `LessonRow`s in one.
