# Tasks 016: Visual refresh 2026 and the month calendar

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Tokens: touch 48, tab bar 80, Fab 56, heavier headings and money, Nunito 900. Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/RefreshGalleryTest.kt`. (AC-016-8)
- [x] 2. Changed components: ScreenHeader, TabBar, Button, IconButton, Avatar, Card, ListGroup, SegmentedControl. Test: `RefreshTest.kt`, `RefreshColorsTest.kt`. (AC-016-1, 4, 6, 7)
- [x] 3. New components SummaryCard, SummaryTile, SplitButtons; EarningsSummary on SummaryCard; READMEs and gallery. Test: `RefreshTest.kt`, `RefreshColorsTest.kt`, `RefreshGalleryTest.kt`. (AC-016-2, 3, 5, 9, 10, 12)
- [x] 4. Domain: `MonthRules.kt` (month cells, horizon, dot kinds). Test: `core/domain/src/test/kotlin/app/tutora/domain/MonthRulesTest.kt`. (AC-016-16, 17, 23)
- [x] 5. New components MonthGrid and MonthPicker, READMEs and gallery. Test: `RefreshGalleryTest.kt`, `AgendaMonthTest.kt`. (AC-016-12, 19)
- [x] 6. Screens: Hoje with the summary card, joined actions and a grouped list; balances on the summary card; grouped lists. Test: existing screen tests, `AgendaMonthTest.kt`. (AC-016-2, 4, 5, 11)
- [x] 7. Screens: Agenda title, Hoje, Dia / Semana / Mês, the month view, the picker and the limit, in both languages. Test: `app/src/test/kotlin/app/tutora/AgendaMonthTest.kt`. (AC-016-13 to 23)
- [x] 8. Docs: design-system READMEs and README count, CLAUDE.md (35 components), architecture note; version 1.11.0; `./gradlew check` once; mark the spec done.

## Pending on a device

- Look through every screen in the four themes, light and dark, and with Suas cores.
- Large-text screenshots at 360 dp of Hoje, Agenda Mês and Ganhos.
- Tab switch under 300 ms with a year of data on the month view.
