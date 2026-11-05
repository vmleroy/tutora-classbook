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

- [x] 9. Domain: `periodBars`, `compareMonths`; `SetupChoices.travelCents`. Test: `EarningsRulesTest.kt` (AC-014-30, AC-014-31), `SetupTest.kt` (AC-014-25).
- [x] 10. Design system: typable `Stepper` (`StepperInput`), scrolling `MonthBars`, `size-bar-min`; guidelines and gallery. Test: `DesignSystemTest.kt` (AC-014-29).
- [x] 11. Exact reminders: build flag, manifest overlay, `setExactAndAllowWhileIdle`, `ExactAlarmPermission` and the Lembretes row; days as one row. Test: `RemindersTest.kt` (AC-014-26, AC-014-27).
- [x] 12. Ajustes in pages: Geral, Aulas, Lembretes, Pagamentos and Sobre, with their summaries; update the screen tests that open Ajustes. Test: `SettingsPagesTest.kt` (AC-014-28).
- [x] 13. Ganhos: bars by period, Evolução with ranges and the comparison; update AC-009-12's test. Test: `EarningsScreensTest.kt` (AC-014-30, AC-014-31).
- [x] 14. Setup's fixed travel amount; `./gradlew check`; the new screens on the emulator (Ajustes pages, Evolução, typing in steppers, an exact reminder).

- [ ] 15. Design system: `NumberField`, `DurationField` with − +, bell and chart icons, `IconButton.dot`, paired `MonthBars`; guidelines, gallery (43 components). Test: `DesignSystemTest.kt` (AC-014-37).
- [ ] 16. Fields per kind on every form (Lembretes' hours, the setup's prices, counts, percent, minutes, durations). Update the tests that pressed − and + or the duration sheet. Test: `FieldsTest.kt` (AC-014-37).
- [ ] 17. Bell on Hoje and the Agenda; Ganhos' header icons; custom aviso. Test: `RemindersTest.kt` (AC-014-32, 36), `EarningsScreensTest.kt` (AC-014-33).
- [ ] 18. Evolução: place kind and payer on earnings, filters, month details, paired bars. Test: `EarningsRulesTest.kt` (AC-014-34, 35), `EarningsScreensTest.kt` (AC-014-34, 35).
- [ ] 19. `./gradlew check`; the new screens on the emulator.

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

## Checked on a device, second round (5 Nov 2026)

- [x] Ajustes lists its pages with their lines; Lembretes' days fit as Seg … Dom; typing 9 in the hour gives 09:00.
- [x] With the default build, the class reminder is exact (`window=0`, `exactAllowReason=policy_permission`), and no "Ativar" row shows.
- [x] Ganhos: November (empty) shows only its empty state; October shows its five weeks. Evolução scrolls through 12 months and compares October with September ("+R$ 640,20", no percent since September is zero).

Fixed along the way: small segments wrapped "Seg" to "Se g" (now 4 dp a side), and the chevron on "Ver evolução" was dropped.

## Decided

- Exact reminders: both permissions, chosen by `-Ptutora.exactAlarm=use|schedule` (Victor, 5 Nov 2026). "use" is the default.
