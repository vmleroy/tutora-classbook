# NavRail

The five tabs in a column on the left when the phone is sideways (spec 017). Upright, the `TabBar` shows them at the bottom instead; the shell picks one by `TutoraTheme.landscape`.

## Consumer provides
The same `tabs` (`TabItem` with key, label, icon and count), `active` key and `onSelect` as `TabBar`.

## Rules
- Same five tabs, same order, same icons, labels, `tab-active-soft` pill and count as `TabBar` (AC-017-2); both draw each tab with one shared piece.
- `size-rail` (88 dp) wide on `surface-sunken`, the tabs centered top to bottom; it scrolls if the phone is too short for five.
- Pads for the start, top and bottom safe-drawing insets, so a camera cut-out or navigation buttons on the left never cover a tab (AC-017-5). The screens beside it don't pad that side again.
- Shown only on the tabs; details and forms take the whole width, as upright.
