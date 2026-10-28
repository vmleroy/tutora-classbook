# Tasks 009: Earnings

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain: `earnedOf` (gross, commission on the value without travel, package price per class), `bonusCover`. Test: `core/domain/src/test/kotlin/app/tutora/domain/EarningsRulesTest.kt`. (AC-009-1, 2, 3, 4)
- [x] 2. Domain: `Period` (ranges, ‹ ›, stop at the current one), `TypeFilter`, `salaryMonths`. Test: `EarningsRulesTest.kt`. (AC-009-5, 9, 10)
- [x] 3. Domain: `summarize` (earned, received with salary, to receive, commission and bonus lines), `trend`, `sources`. Test: `EarningsRulesTest.kt`. (AC-009-6, 7, 12, 13)
- [x] 4. Data: `EarningsDao` (classes with place and name in a range, payments by date, schools, bonus uses) and `EarningsRepository.summary(period, filter)`; `AppServices.earningsRepo`. Test: covered through the screens (no new rule here).
- [x] 5. Design system: `MonthBars` (Canvas, theme tokens, labels, content description), guideline and gallery entry. Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt` (existing hit-area and label check).
- [x] 6. Screens: Ganhos (period, ‹ ›, type chips, summary with commission and bonus line, MonthBars, De onde veio opening details, empty state). Test: `app/src/test/kotlin/app/tutora/EarningsScreensTest.kt`. (AC-009-9, 11, 13, 14)
- [x] 7. Screens: Hoje's earnings card (Ganho hoje, previstos, Semana, month, Alunos devem), opening Ganhos. Test: `EarningsScreensTest.kt`. (AC-009-8)
- [x] 8. Copy in both languages; English test; version 1.9.0. Test: `EarningsScreensTest.kt`. (AC-009-15)
- [x] 9. Update `docs/architecture.md` (earnings in the domain over one range query) and the design-system component list; run `./gradlew check` once; mark the spec done.
- [x] 10. Durações em HH:MM in Ajustes, read by every duration through `minutesText` / `durationText`. Test: `LessonScreensTest.kt`. (AC-009-16)

## Pending on a device

- Tab switch to Ganhos under 300 ms with a year of classes and payments (constitution rule 18).
- Large-text screenshots of Ganhos and Hoje's card at 360 px.
