# Tasks 007: Live timer (Kotlin)

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain: `LessonTimer`, elapsed, pause and resume, `liveMinutes`, `earnedSoFar`, `LIVE` status, `liveTimer` setting. Test: `core/domain/src/test/kotlin/app/tutora/domain/TimerRulesTest.kt`. (AC-007-5, AC-007-6, AC-007-7, AC-007-9)
- [x] 2. Data: `lesson_timer` table, migration 2 → 3, `live_timer` setting, live and timer reads. Test: `core/data/src/test/kotlin/app/tutora/data/MigrationsTest.kt` (rule 12).
- [x] 3. Use cases: `Live` (start, pause, resume, setNote, end, undoEnd, discard), notifier adapter on `Ctx`, removing a live class. Test: `app/src/test/kotlin/app/tutora/LiveTest.kt`. (AC-007-1, AC-007-2, AC-007-8, AC-007-10, AC-007-11, AC-007-12, AC-007-14, AC-007-16)
- [x] 4. Notification: channel, ongoing notification with chronometer and actions, receiver, re-post on start-up, permission request. Test: `app/src/test/kotlin/app/tutora/LiveScreensTest.kt`. (AC-007-15)
- [x] 5. Design system: `BlockState.LIVE` and its guideline and gallery entry. Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt` (gallery lists it).
- [x] 6. Screens: live screen and Aula concluída, shared `DoneSheet`, Iniciar aula on the detail and Hoje, live card on Hoje, Ao vivo on Agenda and lists, Ajustes › Aulas; strings in both languages. Test: `app/src/test/kotlin/app/tutora/LiveScreensTest.kt`. (AC-007-3, AC-007-4, AC-007-13, AC-007-17, AC-007-18)

## Pending on a device

- The notification ticking with the app in the background, its Pausar / Retomar and Encerrar actions, and its return after a reboot.
- The permission prompt on Android 13+ and the timer when notifications are denied.
- The live screen at Large text, and the battery cost of a 1-hour class on screen.
