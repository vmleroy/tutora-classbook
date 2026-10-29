# MonthPicker

A year and its twelve months, to jump the Agenda to any month (spec 016). It sits in a `BottomSheet`, opened by tapping the Agenda's month and year.

## Consumer provides
The `year` text, the twelve `months` (each `PickerMonth` with its short `label`, a `count` such as "25 aulas", an `a11y` reading and `enabled`), the `selected` index and `onSelect`, ‹ › labels and handlers for the year with `nextEnabled`, and `todayLabel` with `onToday` ("Voltar para hoje").

## Rules
- Three months a row, each a pill at least 48 dp tall with its number of classes under it; the shown month is filled with `btnPrimary`.
- A month past the Agenda's 8-week horizon is disabled and outlined; › is disabled when the next year is past it too.
- Month names come from the app language.
