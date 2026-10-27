# SelectField

Field that opens a picker (bottom sheet, date or time picker) instead of taking typed text.

## Use
Student or group, place, date, start time, calendar target.

## Consumer provides
`label`, `value`, optional `sub`, `avatar` or `icon`, `actionLabel`, `onPress` that opens the picker.

## Rules
- Shows the current choice, never an empty box: default to the most likely value (today, the student's default place).
- Dates read "Hoje, 26 out" / "Seg, 2 nov"; times are 24-hour "14:00".

## Props

```ts
interface SelectFieldProps {
  label?: string;
  value: string;
  sub?: string;
  avatar?: { initials: string; tag: Tag };
  icon?: IconName;
  actionLabel?: string;
  onPress?: () => void;
}
```
