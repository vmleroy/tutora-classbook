# Switch

On/off setting that takes effect immediately, with no Save button.

## Rules
- Used inside ListRow (`trailing="switch"`), whose title is the switch's label.
- On = `primary` track, off = `line-strong` track. Never success green.
- If turning it on needs a permission (calendar, notifications), ask first and leave it off when refused.
- In RN use the built-in `Switch` with `trackColor` from the theme, or the custom one here for identical looks on both platforms.

## Props

```ts
interface SwitchProps {
  value?: boolean;
  onChange?: (value: boolean) => void;
  label: string;
  disabled?: boolean;
}
```
