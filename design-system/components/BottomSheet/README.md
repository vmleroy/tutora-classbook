# BottomSheet

Panel that rises from the bottom for choices, confirmations and pickers, over a `scrim`.

## Consumer provides
`title`, optional `subtitle`, content, and `actions` (outline + primary, 1fr : 2fr).

## Rules
- Use for: report options, record payment, confirm restore or delete, student and place pickers.
- Never stack two sheets. Destructive confirmations name what will be lost ("Os dados atuais serão substituídos").
- In RN: `@gorhom/bottom-sheet` with the grabber, dismiss on swipe down and on scrim tap.
