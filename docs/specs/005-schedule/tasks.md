# Tasks 005: Schedule

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Recurrence and time: `src/domain/recurrence.ts`. Test: `src/domain/recurrence.test.ts`. (AC-005-6, AC-005-7, AC-005-9)
- [x] 2. Schedule rules: value, expected, day layout, greeting, next class. Test: `src/domain/schedule.test.ts`. (AC-005-4, AC-005-16, AC-005-19, AC-005-21, AC-005-22, AC-005-23)
- [x] 3. Migration 0002 (`schedule_rule`, `rule_skip`, `lesson`), `seed-v2.sql`, repo. Test: `src/data/migrations.test.ts`.
- [x] 4. Use cases: schedule, fill, edit and remove by scope, travel suggestion; fill at start-up and on foreground. Test: `src/usecases/schedule.test.ts`. (AC-005-2, AC-005-3, AC-005-5, AC-005-8, AC-005-10, AC-005-11, AC-005-12, AC-005-13, AC-005-14, AC-005-15)
- [x] 5. Design system: WeekStrip `today` (README, gallery); AgendaBlock guideline: placed by the screen, no drag. Agenda, Hoje and the form's shared parts in `src/ui/schedule.tsx` and `src/ui/lessonForm.tsx`.
- [x] 6. State hooks and i18n keys.
- [x] 7. Nova aula / Editar aula screens. Test: `tests/app/schedule.test.tsx`. (AC-005-1)
- [x] 8. Agenda (Dia, Semana) and Hoje. Test: `tests/app/schedule.test.tsx`. (AC-005-17, AC-005-18, AC-005-20, AC-005-24, AC-005-25, AC-005-26)
- [x] 9. Docs: architecture lesson table and time rules as built; full checks.

## Pending on a device

- The Dia grid scrolls to the first class and feels right at 24 hours.
- Date sheet and hh:mm start time with the keyboard open.
- Filling when the app returns to the foreground on a new day.
- Agenda block sizes at 44 px per hour with short (30 min) classes.
