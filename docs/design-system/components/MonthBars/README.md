# MonthBars

A few months side by side as bars: the 6-month trend on Ganhos (spec 009).

## Consumer provides
`bars` (each with its month `label`, the bar's `total` and the `filled` part, both in cents) and a `description` read aloud for the whole chart.

## Rules
- Six months at most, oldest on the left; labels are the short month names in the app language ("mai", "May").
- The bar height follows its total against the largest month; an empty month shows no bar, only its label.
- The bar is `accentSoft` (earned) with the received part filled in `success`, bottom up, as on the summary card's bar.
- The chart is one element for screen readers: the description lists every month with its earned and received amounts.
- No numbers on the bars; the summary card above carries the amounts.

## Periods, scrolling and choosing (spec 014)
Ganhos shows the bars of the period chosen (weeks of a month, months of a quarter or a year), never another period's. Bars share the width down to `size-bar-min` (48, a full touch target); with more, the chart scrolls sideways and opens at its latest bar. With `onSelect`, each bar is a button and the `selected` ones are outlined in `ink` with a bold label, as Evolução does to compare two months.
