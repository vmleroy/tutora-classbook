# Plan 019: Evolução, any period and tidier filters

Built on the spec 017 branch, as Victor asked (5 Nov 2026). Architecture reference: docs/architecture.md.

## Approach

- **Domain (`Earnings.kt`):** `rangeSpans(from, to)` gives the months of a gap of dates, the first and last cut to it (AC-019-3). `evolutionPeriodOk(from, to, today)` holds the rules: De ≤ Até ≤ today, at most `EVOLUTION_MAX_YEARS` (5) (AC-019-2). `monthDetails` and the bars already work on any spans, so the money rules don't change; a salary counts when its month's first day is inside.
- **Data (`EarningsData.evolution`):** takes `from` and `to` instead of a first month and a count, and builds its spans with `rangeSpans`.
- **Screen (`Evolution.kt`):**
  - `EvolutionRange` gains `CUSTOM`. 6 meses, 12 meses and Ano become a (from, to) pair; Personalizado starts from the period on screen and shows De and Até `DateField`s, as the report's "Outro…".
  - A pick that breaks the rules keeps the last valid period and shows `evo_tooLong` (or clamps Até to today and to De).
  - Bars spanning more than one year carry the year in their label ("out 25").
- **Dropdowns:** a shared `TypeFilterField` in `ui/earnings` (a `SelectField` "Tipo" opening a `BottomSheet` of choices) replaces the type chips on Ganhos and Evolução (AC-019-7, 11). `PlaceKindField` does the same for "Local" (AC-019-8). Both are built from existing components.
- **Strings:** `evo_custom`, `evo_tooLong`, `evo_type`, `evo_place` in both languages (AC-019-10).

## Data

No table or migration.

## Test strategy

Lean, one test per criterion: `EarningsRulesTest` (rangeSpans, the period rules) and `EarningsScreensTest` (the screen), plus updating the tests that tapped the old chips (AC-014-19, AC-014-35).
