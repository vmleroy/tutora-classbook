# Checkbox

Independent yes/no options in a list, such as what a report includes.

## Rules
- 22px box with `radius-xs`; the whole row (44px tall) is pressable.
- Use Switch for settings that apply at once; Checkbox for choices confirmed by a button.

## Props

```ts
interface CheckboxProps {
  label: string;
  checked?: boolean;
  onChange?: (checked: boolean) => void;
}
```
