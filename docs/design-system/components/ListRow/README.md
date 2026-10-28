# ListRow

Settings and navigation row: optional icon tile, title, subtitle with the current value, and a chevron, switch, badge or remove (×).

## Consumer provides
`title`, `subtitle` (the current value, so the user knows the state without opening it), optional `icon` + `iconTone` or `avatar` (initials + color tag, for people), `trailing` (chevron | switch | remove | none), `badge`, `onPress`, `onRemove`.

## Rules
- Subtitles state the current setting ("Automática · US$ 1 = R$ 5,42 · hoje"), not a description of the feature.
- A person's row always shows their `avatar` in their color tag, never a generic icon.
- Switch rows are not pressable as a whole except through the switch.
- A remove row is a connection (a student at a place, a member of a group): the row opens the item and the × takes it out of the list, after the screen confirms. The × reads "Remover {title}" to screen readers.

## Props

```ts
interface ListRowProps {
  title: string;
  subtitle?: string;
  icon?: IconName;
  iconTone?: 'secondary' | 'danger' | 'success' | 'accent' | 'primary';
  avatar?: { initials: string; tag: Tag };
  trailing?: 'chevron' | 'switch' | 'remove' | 'none';
  switchValue?: boolean;
  onSwitchChange?: (value: boolean) => void;
  badge?: { tone: BadgeProps['tone']; label: string };
  onPress?: () => void;
  onRemove?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
