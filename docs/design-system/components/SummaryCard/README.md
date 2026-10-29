# SummaryCard

The one card on a screen painted with the Topo color (the "header" part of Suas cores), holding that screen's main numbers: today's earnings on Hoje, the period on Ganhos, a balance on a student, group or place (spec 016). It replaces the colored band that used to sit at the top of Hoje.

## Consumer provides
Its content, and an optional `label` read aloud for the card. Inside it, `SummaryTile`s hold the small figures: `label`, `cents`, a `tone` and an optional `onClick`; `compact` writes whole reais.

## Rules
- One per screen, near the top. Never a second Topo-colored surface.
- Text on it is `onHeader` or `onHeaderMuted`; the headline amount is `Money` with `MoneyTone.ON_HEADER` (earnings-on-header, the highlight color when it holds 3:1).
- `SummaryTone.OWED` sits in the red tile and `PAID` in the green, the same in every theme. `PLAIN` tiles use `onHeaderFill()`: the Topo color moved away from its text, so contrast only goes up.
- A control on it (a progress bar, the first of `SplitButtons`, an `IconButton` `ON_PRIMARY`) uses the same `onHeaderFill()`.
- Corners 28, padding 16, no shadow.
