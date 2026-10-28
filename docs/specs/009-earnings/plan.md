# Plan 009: Earnings (Kotlin)

Architecture reference: "State management and data flow" (aggregates), "Money and time" (rounding once, UTC ranges from local days), "Feature libraries" (charts drawn with Compose `Canvas`). Mockups: Earnings, Main.

## Approach

Earnings are worked out in the domain from a light list of the period's done and charged classes, the period's payments and the schools' salaries. Nothing new is stored.

- **Domain** (`core/domain/Earnings.kt`, new).
  - `Period(kind: MONTH | QUARTER | YEAR, anchor: LocalDate)` with `range()` (first and last local day), `previous()`, `next()`, `months()` (each month in it, cut at the current month for the year) and `isCurrent(today)` (AC-009-9).
  - `TypeFilter` is `ALL`, `PRIVATE` or `JOB`, matched through `earningsType(place.kind)` from spec 004 (AC-009-10).
  - `EarnedClass(lessonId, day, type, payer, gross, commission, bonus)`, made by `earnedOf(row, bonusCover)`:
    - **Gross.** A done class is worth its typed value, or its frozen price × minutes, plus travel. A charged cancellation is worth its charge (AC-009-1).
    - **Commission.** It is taken only where the student pays and the place keeps one: `gross − travel − afterCommission(gross − travel, bp)`. It comes off the class value, never the travel, and is rounded once (AC-009-2).
    - **Package.** A package class's frozen price is already the package price per class (AC-009-3).
    - **Bonus.** `bonus` is what bonuses covered on the class (AC-009-4).
    - Earned = gross − commission − bonus.
  - `bonusCover(uses, bonusMoney)`, per payer:
    - Minutes uses always come from a bonus.
    - Money uses are counted as bonus up to the money bonuses given, oldest uses first; the rest came from paid credit.
    - It returns the bonus part of each class.
  - `salaryMonths(place, months)`: a school with a salary earns it in each month from the month it was created to the month it was archived, if it was (AC-009-5).
  - `summarize(classes, payments, salaries, toReceive, filter)` returns `Summary(earned, received, toReceive, commission, bonus)` (AC-009-6, 7, 11).
  - `trend(...)` gives the six months ending at the period's last month (AC-009-12). `sources(...)` gives the rows of "De onde veio": per payer, plus one row per school for its salary, largest first (AC-009-13).
- **Data** (`core/data/EarningsData.kt`).
  - `EarningsDao.observeClasses(from, to)` is one query: the done, cancelled and missed classes in the range, joined with their place (kind, payer, commission) and the student's or group's name. It replaces the per-class reads that `ScheduleRepository.between` does, which would be slow over a year.
  - `observeReceived(from, to)` lists payments by `paid_at`, each with the type of the place it comes from: a place as its type, a group as its place's, a student as private. `observeSchools()` gives salary, created and archived.
  - The credit uses on those classes and the money bonuses come from the `credit` table.
  - `EarningsRepository.summary(period, filter)` combines these and, for A receber, `PaymentRepository`'s per-class `left` (the same ledgers as the balances). It returns `Flow<EarningsView>`: summary, trend and sources.
  - No migration. A new index on `payment(paid_at)` would need version 6. With a year of payments being a few hundred rows, a scan is fine, so this is left out.
- **State** (`app/state`). `AppServices` gets `earningsRepo`. Hoje uses `summary` for today, the week and the month.
- **Screens.**
  - `EarningsScreen` (`ui/earnings/Earnings.kt`) replaces the placeholder:
    - The period `SegmentedControl` (Mês / Trimestre / Ano) with ‹ › `IconButton`s and the range label.
    - The type `Chip`s.
    - `EarningsSummary` with Recebido / A receber `stats` and the received-share bar, plus the commission and bonus line (AC-009-9 to 11).
    - The trend, `MonthBars`.
    - "De onde veio" as a `ListGroup` of rows with a `ProgressBar` each and "deve R$ 240" from the current balances, opening the student, group or place (AC-009-13).
    - "Nada ganho neste período" as an `EmptyState` (AC-009-14).
  - **New design-system component `MonthBars`.** It draws six bars on a `Canvas` from theme tokens: earned in `accent`, the received part in `earnings`. It has month labels and a content description reading every month. Its guideline goes in `docs/design-system/components/MonthBars/README.md`, with a gallery entry (AC-009-12).
  - `EarningsSummary` gains `receivedShare` (the success-on-danger bar its guideline describes for Ganhos) and `footnote` (the commission and bonus line).
  - **Hoje.** An `EarningsSummary` card above Próxima aula: Ganho hoje, "de R$ 380,00 previstos", and the stats Semana, the month's name, and Alunos devem (the sum of positive balances, AC-008-15's source). Tapping it opens Ganhos (AC-009-8).
  - **Previsto on Hoje's card** uses the earned rules: scheduled and done classes at what the teacher keeps. So the bar compares like with like. The Agenda's Previsto (AC-005-17) is unchanged.
- **Copy** in `strings_features.xml`, both languages. Month names are formatted for the app language (AC-009-15).
- Version 1.9.0.

## Data

No new tables or columns; database version 5 stays.

| Read | Source |
| --- | --- |
| Classes in a range | `lesson` (done / cancelled / missed, `start_at` in the UTC range of the local days) ⋈ `place` ⋈ `student` / `group_class` names |
| Payments in a range | `payment` by `paid_at` |
| Bonus cover | `credit` (uses on those classes, money bonuses per payer) |
| Salaries | `place` where `kind = school` and `monthly_salary_cents` set |
| To receive | `PaymentRepository` ledgers (`allocation.left`) |

## Use cases

None: 009 only reads.

## Screens and components

| Screen | Components |
| --- | --- |
| Ganhos | ScreenScaffold, SegmentedControl, IconButton, Chip, EarningsSummary, MonthBars (new), SectionHeader, ListGroup, ListRow, ProgressBar, EmptyState |
| Hoje | EarningsSummary (above Próxima aula) |

## Test strategy

- **Domain:** `EarningsRulesTest`: gross, commission, package and bonus cover; salary months; periods and their ranges; summary; trend; sources (AC-009-1 to 7, 9, 10, 12, 13).
- **Screens:** `EarningsScreensTest` under Robolectric: Hoje's card, Ganhos filters and moving periods, summary line, list opening a detail, empty state, English (AC-009-8, 9, 11, 13, 14, 15).
- `MonthBars` gets the design system's hit-area and label check like every component.
- Pending on a device: tab switch to Ganhos under 300 ms with a year of data (rule 18), and Large-text screenshots of Ganhos.

## Risks

- **A year of classes on every change.** The one query plus the domain work is linear in the period's classes, a few hundred to fifteen hundred. The balances already run the same ledgers.
- **Bonus cover is an attribution.** Credit is one pool, so "bonus first" is a rule, not a record. It only decides how a class's covered part splits between bonus and paid credit, and the totals stay exact.
- **Salary months** depend on the school's creation date. A school added late in the year doesn't earn for the months before it existed, which matches what the teacher entered.
