# Tasks 014: Reminders, confirming classes and first-run setup

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain `Confirm.kt`, `Reminders.kt`, `Teaching.kt`; new `Settings` fields with defaults in `SettingsRepository`. Test: `core/domain/src/test/kotlin/app/tutora/domain/ConfirmRulesTest.kt`, `ReminderRulesTest.kt`, `TeachingRulesTest.kt`. (AC-014-1, AC-014-12, AC-014-13, AC-014-14, AC-014-19, AC-014-20)
- [x] 2. Migration 5→6 (`lesson.auto_done`), schema 6, backup step 5→6, DAO reads. Test: `core/data/src/test/kotlin/app/tutora/data/MigrationsTest.kt`. (AC-014-2)
- [x] 3. Use cases `Confirming` (confirm, confirmAll, undoAll, autoConfirm). Test: `app/src/test/kotlin/app/tutora/ConfirmingTest.kt`. (AC-014-2, AC-014-6, AC-014-7, AC-014-8, AC-014-9, AC-014-10, AC-014-11)
- [x] 4. Design system: `UndoBar`, `TabItem.count`, `MonthDay.pending`, `BlockState.TO_CONFIRM`, with README and gallery (42 components). Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt`. (AC-014-3, AC-014-5)
- [x] 5. Screens: "A confirmar" marks, Hoje's alert, the Agenda tab count, Mês' ring, the confirm list with Desfazer, the Ajustes rows. Test: `app/src/test/kotlin/app/tutora/ConfirmScreensTest.kt`. (AC-014-3, AC-014-4, AC-014-5, AC-014-6, AC-014-9, AC-014-24)
- [x] 6. Reminders: `ReminderAlarm` and its receiver, channel, boot, permission, "Marcar todas" from the notification, `Reminders.plan` and its collector. Test: `app/src/test/kotlin/app/tutora/RemindersTest.kt`. (AC-014-12, AC-014-13, AC-014-14, AC-014-15, AC-014-16)
- [x] 7. First-run setup: the four steps, summary and ways out; `setupDone` for teachers with data; hiding by teaching kind. Test: `app/src/test/kotlin/app/tutora/SetupScreensTest.kt`, `SetupTest.kt`. (AC-014-17, AC-014-18, AC-014-19, AC-014-20, AC-014-21, AC-014-22, AC-014-23)
- [x] 8. `./gradlew check`, roadmap status, device checks pending in the PR.

## Pending on a device

- A reminder before class arriving with Tutora closed, and again after the phone restarts (AC-014-15); alarms are inexact, so a few minutes late is expected.
- "Quem te deve" on Monday at 08:00 and "Aulas para confirmar" at 20:00, each opening the right page when tapped (AC-014-13, 14).
- "Marcar todas como dadas" from the notification, with the app closed (AC-014-14).
- The notification permission prompt on Android 13+, and "Abrir ajustes" opening Tutora's page in the phone's settings (AC-014-16).
- The setup on a fresh install: the language switch redrawing the setup in English without losing the steps, and the folder picker in step 4 (AC-014-17, 18, 22).
- Large text on the confirm list's three buttons and the setup's price steppers.

