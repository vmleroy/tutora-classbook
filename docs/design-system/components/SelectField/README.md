# SelectField

Field that opens a picker (bottom sheet, date or time picker) instead of taking typed text.

## Use
Student or group, place, date, start time, calendar target.

## Consumer provides
`label`, `value`, optional `sub`, `avatar` or `icon`, `actionLabel`, `onPress` that opens the picker.

## Rules
- Shows the current choice, never an empty box: default to the most likely value (today, the student's default place).
- Dates read "Hoje, 26 out" / "Seg, 2 nov"; times are 24-hour "14:00" in Portuguese and "2:00 PM" in English.
- A date is a `DateField`: it opens the month calendar (with the keyboard toggle to type a date), confirmed with OK.
- A time of day is a `TimeField`: it opens the clock in a bottom sheet, with "Digitar" to type hours and minutes instead; OK applies it. Never type a time into a text field.

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
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
