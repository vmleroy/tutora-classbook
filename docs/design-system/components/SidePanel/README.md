# SidePanel

A panel over the right side of the screen when the phone is sideways (spec 017, option B chosen by Victor on 5 Nov 2026). Upright, the same tap opens the page full screen instead.

## Consumer provides
`visible`, `onDismiss` (a tap on the dimmed side) and the `content`, usually a detail screen whose back arrow closes the panel.

## Rules
- The screen keeps its upright structure under a `scrim`; nothing reflows when the panel opens or closes.
- The panel is at most 55% of the width and never wider than `size-content-max` (600 dp), full height, on `background`, sliding in from the right (fading with reduce motion).
- Back, the content's ✕ or back arrow, and a tap on the scrim close it, the panel before anything else (AC-017-11).
- Opened from a list row, a day or a class: one panel at a time; opening another replaces what it shows.
- The app shell hosts it, so it covers the tab's content but never the rail; when the phone turns upright, what it showed opens full screen.

## The readable width
`Modifier.readableWidth()` takes the whole width up to `size-content-max` and centers past it, with the background on both sides (AC-017-4). `ScreenScaffold`'s body and bottom bar use it, so cards and forms never stretch; `BottomSheet` opens at the same maximum width (AC-017-15).
