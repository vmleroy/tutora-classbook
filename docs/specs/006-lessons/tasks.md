# Tasks 006: Lessons (Kotlin)

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain: status, frozen value, `counted`, fee rules and `suggestFee`; `feeChoice` on student and place; `feeRule` in settings. Test: `core/domain/src/test/kotlin/app/tutora/domain/LessonRulesTest.kt`. (AC-006-2, AC-006-3, AC-006-12, AC-006-16, AC-006-19, AC-006-21, AC-006-24, AC-006-25, AC-006-26)
- [x] 2. Data: migration 1 → 2 with the new lesson, student and place columns; fee rule JSON mapping; settings key; next class only scheduled. Test: `core/data/src/test/kotlin/app/tutora/data/MigrationsTest.kt` (rule 12).
- [x] 3. Use cases: `markDone`, `logLesson`, status change in `editLesson` with the fee, the default rule saved with the settings, fee choice in `savePlace` and `saveStudent`. Test: `app/src/test/kotlin/app/tutora/LessonsTest.kt`. (AC-006-4, AC-006-6, AC-006-8, AC-006-10, AC-006-11, AC-006-13, AC-006-17, AC-006-22, AC-006-23, AC-006-27)
- [x] 4. Design system: `BlockState.MUTED` for `AgendaBlock`, with its guideline and gallery entry. Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt` (gallery lists it).
- [x] 5. Screens: done sheet and status line on the detail; Registrar mode; Situação, notice and fee in the edit form; badges on Hoje and muted blocks on Agenda; Ajustes › Cancelamentos e faltas; fee choice on student and place forms; strings in both languages. Test: `app/src/test/kotlin/app/tutora/LessonScreensTest.kt`. (AC-006-1, AC-006-5, AC-006-7, AC-006-9, AC-006-14, AC-006-15, AC-006-18, AC-006-20, AC-006-28)

## Pending on a device

- The done sheet and the fee switch at Large text.
- Hoje and Agenda with a mix of done, cancelled and missed classes.
- Upgrading an installed 1.5.0 build: the database moves to version 2 and keeps its classes.
