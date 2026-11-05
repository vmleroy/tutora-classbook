# TwoPane

Two columns side by side, for a phone held sideways (spec 017): Hoje, Ganhos, the Agenda's Mês, Cadastro with its page, and the live class.

## Consumer provides
`start` and `end` content, the `startWeight` (Hoje and Ganhos 0.45, Mês and the live class 0.5, Cadastro 0.4), whether each pane scrolls (`scrollStart`, `scrollEnd`) and the `gap` between items.

## Rules
- A `space-5` gutter between the panes. Each scrolls on its own, unless it holds its own list or a grid fitted to the height.
- Neither pane grows past `size-content-max`; a wider pane centers its column.
- When a pane would be narrower than `size-pane-min` (260 dp) and both panes scroll, they stack into one scrolling column at the readable width.
- Only screens read `TutoraTheme.landscape` to choose it; upright they keep their one column.

## The readable width
`Modifier.readableWidth()` takes the whole width up to `size-content-max` (600 dp) and centers past it, with the background on both sides (AC-017-4). `ScreenScaffold`'s body and bottom bar use it, so cards and forms never stretch; `BottomSheet` opens at the same maximum width (AC-017-15).
