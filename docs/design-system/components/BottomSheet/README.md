# BottomSheet

Panel that rises from the bottom for choices, confirmations and pickers, over a `scrim`.

## Consumer provides
`title`, optional `subtitle`, content, and `actions` (outline + primary, 1fr : 2fr).

## Rules
- Use for: report options, record payment, confirm restore or delete, student and place pickers.
- Never stack two sheets. Destructive confirmations name what will be lost ("Os dados atuais serão substituídos").
- In the app: a Reanimated panel with the grabber that slides up (instantly when the phone asks for reduced motion) and closes on scrim tap. Swipe-to-dismiss comes with the first screen that needs a sheet.

## Props

```ts
interface BottomSheetProps {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
  children?: React.ReactNode;
  /** Whether the sheet is up. Default true. */
  open?: boolean;
  /** Tapping the scrim dismisses the sheet. */
  onClose?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
