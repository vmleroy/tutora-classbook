# Plan 016: Visual refresh 2026 and the month calendar

Architecture reference: docs/architecture.md (layers, design system, testing). Mockups: docs/mockups/DirA.dc.html, A_Agenda, A_AgendaMes, A_AgendaAno, A_Aluno, A_Ganhos, A_Ajustes, A_Estilo.

## Approach

Most of the look lives in `:core:designsystem`, so the refresh starts there and every screen follows. Screens change only where a layout has to: Hoje, Agenda, the detail balances and lists that become grouped cards.

- **Tokens** (`design/tokens.json`): touch target 44 → 48, tab bar 72 → 80, Fab 60 → 56; headings and money at weight 900 with title-xl 28, section 18 and money-xl 40. No color changes: the four themes and Suas cores (`Palette.kt`) stay as they are, except that a custom Topo color keeps its muted text at 4.5:1 (AC-016-9).
- **Changed components:** `ScreenHeader` (no band in any variant; HERO is now the date and greeting on the background), `TabBar` (on surface-sunken, bold active label), `Button` (pill), `IconButton` (ACCENT is a rounded square, the Nova aula button), `Avatar` (rounded square), `Card` and `ListGroup` (28 corners, section titles), `SegmentedControl` (joined segments, the chosen one filled with the main button color), `EarningsSummary` (now on a `SummaryCard`).
- **New components:** `SummaryCard` (the Topo-colored card with `SummaryTile`s), `SplitButtons` (two joined actions), `MonthGrid` (a month of days with dots) and `MonthPicker` (the year and its twelve months). That makes 35 components, each with its README and a gallery entry.
- **Domain:** `Month.kt` in `:core:domain`, plain Kotlin: `monthCells(year, month)` (Monday-first weeks with the neighbors' days flagged), `agendaHorizon(today)` = today + 8 weeks, `canShowMonth`, and `DayKind` for the dots (school, done, private, group; cancelled and missed give none).
- **Agenda:** the header shows "Agenda" over the month and year (a button that opens the picker sheet), Hoje and the + button. The view switch is Dia / Semana / Mês. Mês reads the month's classes through the existing `scheduleRepo.between`, and the picker counts classes per month for the year in one more `between` read.

## Data

No tables, columns or migrations.

## Use cases

None: this spec writes nothing.

## Screens and components

Hoje (TodayScreen, TodayEarnings), Agenda (AgendaScreen, WeekPicker), Ganhos, student, group and place detail balances (Payments.kt), Ajustes, and the gallery. Lists of classes become `ListGroup`s of `LessonRow`s.

## Test strategy

- Domain (`MonthRulesTest`): the grid, the horizon and the dot kinds (AC-016-16, 17, 18).
- Design system (`RefreshTest`, `RefreshColorsTest`, `RefreshGalleryTest`; Robolectric with native graphics, the view drawn in software onto a bitmap): pixel checks for the header without a band, the summary card and its tiles, the joined buttons, the tab pill, the 48 dp targets and the contrast over every theme and mode (AC-016-1 to 10, 12).
- Screens (`AgendaMonthTest`): the month title, Hoje, Dia/Semana/Mês, the grid, choosing a day, the picker and the limit, English (AC-016-13 to 15, 19 to 23).
- AC-016-11 is covered by the existing screen tests passing unchanged, plus a Large-text check at 360 dp on the month view.

## Risks

- Existing tests that look for header colors or exact sizes may need updating; they change only where the spec changes the look.
- Pixel checks under Robolectric depend on native graphics; they read token colors, not hard-coded values.
