# Plan 005: Schedule (Kotlin)

Architecture reference: "Money and time" (time rules), "Database schema". Mockups: Main, Schedule, AddLesson (Agendar), EditLesson.

## Approach

- **Domain.**
  - `Recurrence.kt` uses `kotlinx-datetime` for occurrences, local ↔ UTC in the rule's zone, ISO text and day ranges, with weeks starting on Monday.
  - `Schedule.kt` holds `lessonWorth` (a typed value, or the current price plus travel), `expected`, `layoutDay`, `greeting` and `nextLesson`.
- **Data.**
  - `schedule_rule`, `rule_skip` and `lesson`, with a unique `(rule_id, occurrence_date)` so filling is idempotent.
  - `ScheduleRepository` assembles lessons with who and where. Its queries join place, student and link, so price changes refresh the lists.
- **Use cases** (`app/usecases/Schedule.kt`):
  - `scheduleLesson`, `fillSchedule`, `suggestedTravel`.
  - `editLesson` and `removeLesson` by scope. Any class can change on its own, past ones included; "Esta e as próximas" only from a class still ahead.
  - Filling runs at start-up and on every return to the app.
- **Screens.**
  - Hoje: greeting, next class, today's list.
  - Agenda: week picker, Dia and Semana grids, day header with Previsto.
  - Nova aula and Editar aula: one form with the who picker, place chips, date sheet, hh:mm start, duration, travel, typed value, Repetir, scope and Remover.
  - Times never wrap ("2:00 PM").
- **Navigation:** five tabs — Hoje, Agenda, Cadastro, Ganhos, Ajustes (AC-005-27).

## Test strategy

- **Domain:** `ScheduleRulesTest` (AC-005-4, 6, 7, 9, 16, 19, 21–23).
- **Use cases:** `ScheduleTest` on a real database, with a frozen clock and São Paulo time (AC-005-2, 3, 5, 8, 10–15).
- **Screens:** `FeatureScreensTest` and `TabsTest` (AC-005-1, 17, 18, 20, 24–27).
