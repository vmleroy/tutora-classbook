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

- [ ] 9. Domain: `periodBars`, `compareMonths`; `SetupChoices.travelCents`. Test: `EarningsRulesTest.kt` (AC-014-30, AC-014-31), `SetupTest.kt` (AC-014-25).
- [ ] 10. Design system: typable `Stepper` (`StepperInput`), scrolling `MonthBars`, `size-bar-min`; guidelines and gallery. Test: `DesignSystemTest.kt` (AC-014-29).
- [ ] 11. Exact reminders: build flag, manifest overlay, `setExactAndAllowWhileIdle`, `ExactAlarmPermission` and the Lembretes row; days as one row. Test: `RemindersTest.kt` (AC-014-26, AC-014-27).
- [ ] 12. Ajustes in pages: Geral, Aulas, Lembretes, Pagamentos and Sobre, with their summaries; update the screen tests that open Ajustes. Test: `SettingsPagesTest.kt` (AC-014-28).
- [ ] 13. Ganhos: bars by period, Evolução with ranges and the comparison; update AC-009-12's test. Test: `EarningsScreensTest.kt` (AC-014-30, AC-014-31).
- [ ] 14. Setup's fixed travel amount; `./gradlew check`; the new screens on the emulator (Ajustes pages, Evolução, typing in steppers, an exact reminder).

## Checked on a device (5 Nov 2026, emulator, Android 16)

The app's data was copied aside first and put back afterwards. Clock jumps used root on the emulator.

- [x] Migration 5→6 on real data: Hoje shows "2 aulas para confirmar", and the Agenda tab shows 2. The setup doesn't appear for a teacher with data.
- [x] Turning a reminder on asks for notifications. Without permission, Lembretes shows "Notificações desligadas no celular · Abrir ajustes".
- [x] With Tutora closed, "Aulas para confirmar" (20:00), "Quem te deve" (Monday) and the reminder before class arrive. Tapping each opens the list, Cadastro on Devendo, or the class.
- [x] "Marcar todas como dadas" from the notification, with the app closed, marks the classes and clears the notification.
- [x] After the phone restarts, the three alarms are set again without opening the app.
- [x] Fresh install: the setup opens, switching language mid-setup keeps the steps, the folder picker in step 4 turns on Backup automático, and "Cadastrar meu primeiro aluno" goes back to Hoje.
- [x] Largest text: the confirm list's buttons, the setup's price steppers and the Lembretes hours fit.

Fixed along the way:
- The hour and price steppers squeezed their labels to one letter per line.
- A language switch opened a second setup, losing the choices.
- Finishing the setup emptied Hoje's stack, so going back closed the app.
- "Cancelada" was cut at Large text.
- The Lembretes row said "Desligado" while other reminders were on.
- The class reminder said "15 minutos" when it came late.
- "Quem te deve" called places and groups "alunos"; it now says "pagamentos pendentes" and opens Hoje.

## Still open

- Android gives these inexact alarms a window of up to an hour (`window=+1h` in `dumpsys alarm`), so a reminder before class can arrive after the class has started. Exact alarms would need `USE_EXACT_ALARM` (Play allows it for calendar-like apps) or `SCHEDULE_EXACT_ALARM` (the teacher grants it). Victor to decide.
