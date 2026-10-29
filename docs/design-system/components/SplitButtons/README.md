# SplitButtons

Two actions that belong together, joined: Marcar como dada with Iniciar aula on Hoje's next class (spec 016).

## Consumer provides
`first` and `second` labels with their `onFirst` and `onSecond`, optional `firstIcon` and `secondIcon`, and `onSummary` when the pair sits on a `SummaryCard`.

## Rules
- The second is the main action, in `btnPrimary`; the first sits on `primarySoft` (on a summary card, `onHeaderFill()`).
- Outer corners round (28), inner corners small (6), 4 dp apart, both at least 56 dp tall and the same height.
- Only for two actions on the same thing. A single action is a `Button`; a destructive one is never part of a pair.
