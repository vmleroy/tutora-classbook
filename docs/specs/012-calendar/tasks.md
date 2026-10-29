# Tasks 012: Calendar sync and .ics

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [ ] 1. Device spike (with Victor's phone or the emulator with a Google account): write one event with the custom-app mark into an account calendar and into a phone-only calendar; check it reaches calendar.google.com, and that the mark is still there after the account syncs. Outcome noted in plan.md › Risks. (AC-012-4, AC-012-5)
- [ ] 2. Domain `Calendar.kt`: `CalendarWords`, `ClassEvent`, `eventFor` (title, prefixes, place, notes, value, footer), `syncWindow`. Test: `core/domain/src/test/kotlin/app/tutora/domain/CalendarRulesTest.kt`. (AC-012-5, AC-012-6, AC-012-7, AC-012-9, AC-012-22)
- [ ] 3. Domain `diffEvents`, `BusyEvent` / `busyLayout`, the busy filter, `icsFile`. Test: `CalendarRulesTest.kt`. (AC-012-8, AC-012-11, AC-012-18, AC-012-19, AC-012-20, AC-012-21)
- [ ] 4. Data `CalendarStore` with its keys and defaults, added to `DEVICE_KEYS`. Test: `app/src/test/kotlin/app/tutora/CalendarSyncTest.kt`. (AC-012-14)
- [ ] 5. Adapter `PhoneCalendar` (interface, `FakePhoneCalendar`, the Android version with the custom-app mark and a content-observer Flow for busy time); permissions in the manifest; `CalendarPermission` with a fake. Test: `CalendarSyncTest.kt`. (AC-012-4, AC-012-5)
- [ ] 6. Use case `CalendarSync`: `turnOn`, `choose`, `sync`, `setOptions`, `turnOff`, `cleanCount`, `clean`, `ics`; wire into `AppContainer` and `AppServices`; the collector in `AppViewModel`. Test: `CalendarSyncTest.kt`. (AC-012-4, AC-012-8, AC-012-10, AC-012-12, AC-012-13, AC-012-14, AC-012-15, AC-012-16)
- [ ] 7. Design system: `BusyBlock` (block and all-day band), its README and gallery entry. Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt` (the gallery lists it). (AC-012-18)
- [ ] 8. Strings in `strings_calendar.xml` (English and Portuguese) and the Calendário page: switch and status, calendar choices, options, note, .ics, cleanup sheet, permission banner; the Ajustes row in Geral. Test: `app/src/test/kotlin/app/tutora/CalendarScreensTest.kt`. (AC-012-1, AC-012-2, AC-012-3, AC-012-10, AC-012-15, AC-012-20, AC-012-22)
- [ ] 9. Busy time on the Agenda's Dia and Semana, none on Mês. Test: `CalendarScreensTest.kt`. (AC-012-17, AC-012-18, AC-012-19)
- [ ] 10. Update `docs/architecture.md` (no `calendar_link`: the mark is the link), run `./gradlew check`, update the roadmap status, and list the device checks as pending in the PR.
