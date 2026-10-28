# Tasks 006: Lessons (Kotlin)

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain: status, frozen value, `counted`, cancel reasons and charges. Test: `core/domain/src/test/kotlin/app/tutora/domain/LessonRulesTest.kt`. (AC-006-2, AC-006-3, AC-006-12, AC-006-16, AC-006-19, AC-006-20, AC-006-21)
- [x] 2. Data: migration 1 → 2 with the new lesson columns; next class only scheduled. Test: `core/data/src/test/kotlin/app/tutora/data/MigrationsTest.kt` (rule 12).
- [x] 3. Use cases: `markDone`, `logLesson`, `cancel`, `undo`. Test: `app/src/test/kotlin/app/tutora/LessonsTest.kt`. (AC-006-4, AC-006-6, AC-006-8, AC-006-10, AC-006-11, AC-006-13, AC-006-17, AC-006-22)
- [x] 4. Design system: `BlockState.MUTED` for `AgendaBlock` and `statusIcon` for `LessonRow`, with their guidelines and gallery. Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt` (gallery lists it).
- [x] 5. Screens: detail buttons, done sheet, cancel sheet and Desfazer; Registrar mode; badges on Hoje and muted blocks on Agenda; no rule in Ajustes or on students and places; strings in both languages. Test: `app/src/test/kotlin/app/tutora/LessonScreensTest.kt`. (AC-006-1, AC-006-5, AC-006-7, AC-006-9, AC-006-14, AC-006-15, AC-006-18, AC-006-23, AC-006-24)

## Pending on a device

- The done and cancel sheets at Large text, with the keyboard open on Motivo and Outro valor.
- Hoje and Agenda with a mix of done, cancelled and missed classes.
- Upgrading an installed 1.5.0 build: the database moves to version 2 and keeps its classes.
