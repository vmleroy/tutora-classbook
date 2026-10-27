# Tasks 005: Schedule

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [ ] 1. Recurrence and time: `src/domain/recurrence.ts`. Test: `src/domain/recurrence.test.ts`. (AC-005-6, AC-005-7, AC-005-9)
- [ ] 2. Schedule rules: value, expected, day layout, greeting, next class. Test: `src/domain/schedule.test.ts`. (AC-005-4, AC-005-16, AC-005-19, AC-005-21, AC-005-22, AC-005-23)
- [ ] 3. Migration 0002 (`schedule_rule`, `rule_skip`, `lesson`), `seed-v2.sql`, repo. Test: `src/data/migrations.test.ts`.
- [ ] 4. Use cases: schedule, fill, edit and remove by scope, travel suggestion; fill at start-up and on foreground. Test: `src/usecases/schedule.test.ts`. (AC-005-2, AC-005-3, AC-005-5, AC-005-8, AC-005-10, AC-005-11, AC-005-12, AC-005-13, AC-005-14, AC-005-15)
- [ ] 5. Design system: WeekStrip `today`, AgendaBlock placement; READMEs and gallery.
- [ ] 6. State hooks and i18n keys.
- [ ] 7. Nova aula / Editar aula screens. Test: `tests/app/schedule.test.tsx`. (AC-005-1)
- [ ] 8. Agenda (Dia, Semana) and Hoje. Test: `tests/app/schedule.test.tsx`. (AC-005-17, AC-005-18, AC-005-20, AC-005-24, AC-005-25, AC-005-26)
- [ ] 9. Docs: architecture lesson table and time rules as built; full checks.

## Pending on a device

- The Dia grid scrolls to the first class and feels right at 24 hours.
- Date sheet and hh:mm start time with the keyboard open.
