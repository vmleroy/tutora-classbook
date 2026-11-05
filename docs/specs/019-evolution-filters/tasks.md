# Tasks 019: Evolução, any period and tidier filters

- [x] 1. Domain: `rangeSpans`, `evolutionPeriodOk`, `EVOLUTION_MAX_YEARS`. Test: `core/domain/src/test/kotlin/app/tutora/domain/EarningsRulesTest.kt`. (AC-019-2, AC-019-3, AC-019-4, AC-019-5)
- [x] 2. Data: `evolution(from, to, …)`. Covered by the screen tests.
- [x] 3. Evolução: Personalizado with De and Até, the period kept through a turn, bars per month of the gap, rows and comparison. Test: `app/src/test/kotlin/app/tutora/EarningsScreensTest.kt`. (AC-019-1, AC-019-6)
- [x] 4. `TypeFilterField` and `PlaceKindField` on Evolução and Ganhos; update AC-014-19 and AC-014-35's tests. Test: `EarningsScreensTest.kt`. (AC-019-7, 8, 9, 11)
- [x] 5. Strings in both languages; `./gradlew check`. Test: `EarningsScreensTest.kt`. (AC-019-10)

## Pending on a device

- Picking De and Até on a real phone, a two-year period scrolling sideways.
