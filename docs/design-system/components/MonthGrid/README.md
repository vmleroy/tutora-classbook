# MonthGrid

A month as weeks of seven days, with a dot per class: the Agenda's Mês view (spec 016).

## Consumer provides
`days` (each `MonthDay` with its `day` number, `inMonth`, up to two `dots`, how many classes the dots leave out as `more`, an `a11y` reading such as "28 de outubro, 2 aulas", and `today`), the `selected` index, `onSelect`, the seven weekday initials as `heads` (from the app language, Monday first), a `label` for the grid and an optional `legend`.

## Rules
- Weeks start on Monday. The neighbors' days fill the first and last week, greyed out; tapping one opens its month, unless it is past the Agenda's horizon (`enabled` false).
- Dots use the fixed status colors through `DotTone`: school `secondary`, done `success`, private `accent`, group `tagViolet`. A cancelled or missed class has no dot. Past two, "+N", so the dots stay easy to see.
- Today is filled with `btnPrimary` and its dots turn `onBtnPrimary`; the chosen day is tinted `primarySoft`.
- Each day is one element for screen readers, read as its date and number of classes.
- The grid sits on a `surface` card with corners 28; a day is 52 dp tall.

## Sizes since 4 Nov 2026
Days are square tiles with `radius-sm` corners and `spacing-2` between them both ways (Victor, 4 Nov 2026).

## Classes to confirm (spec 014)
`MonthDay.pending` puts a `secondary` ring (`border-strong`) around a day that holds a class waiting to be confirmed (AC-014-5), so a forgotten week stands out. It sits with today's fill and the selected tint.

## Sideways (spec 017)
With the phone sideways each day is a `size-month-day` (52 dp) row instead of a square, so the weeks don't take the whole height; the grid sits at the readable width.
