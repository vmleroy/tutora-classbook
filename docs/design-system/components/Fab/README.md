# Fab

Floating 60px + button for adding a lesson on Agenda; sits 20px from the right edge, above the tab bar.

## Rules
- Only on Agenda. Hoje uses the accent IconButton in the header instead.
- `label` defaults to "Nova aula"; set it when the action differs.
- Uses `shadow-fab`; nothing else in the app floats.

## Props

```ts
interface FabProps {
  icon?: IconName;
  label?: string;
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
