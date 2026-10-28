# Plan 006: Lessons (Kotlin)

Architecture reference: "Database schema and migrations" (fee rule columns, frozen value), "Money and time". Mockups: AddLesson (Registrar), LessonDetail, EditLesson (Situação, fee switch), Fees.

## Approach

- **Domain** (`core/domain`).
  - `Lesson` gains `status` (`SCHEDULED`, `DONE`, `CANCELLED`, `MISSED`; `LIVE` arrives with 007), the frozen price (`frozenRate`: cents and rate type), and the fee (`feeCents`, `feeWaived`, `noticeAt`).
  - `lessonWorth` keeps working for scheduled classes. A done class is worth its typed value, or its frozen rate for its duration, plus the travel stored on it; with no frozen rate it is "Sem preço" (or the salary at a school). This is what freezes the value (AC-006-3) and recomputes it when a done class's duration changes (AC-006-12).
  - `counted(lesson)` is what Previsto adds: the worth of scheduled and done classes, the kept fee of cancelled and missed ones, nothing otherwise (AC-006-16).
  - `Fees.kt`:
    - `FeeAmount` (`Percent(bp)` or `Fixed(cents)`), `FeeRule` (late on/off, window in hours, late amount, no-show on/off, no-show amount) and `FeeChoice` (`Default`, `Own(rule)`, `Never`).
    - `ruleFor(student, place, default)` resolves student → place → default (AC-006-21).
    - `suggestFee(rule, status, valueWithoutTravel, startAt, noticeAt)` returns `Charge(cents, amount, windowH)`, `EnoughNotice`, `RuleOff` or `NoPrice` (AC-006-22 to 26).
    - Percentages round once through `multiplyRate`.
  - `Student` and `Place` gain `feeChoice`, default `FeeChoice.Default`, so existing call sites keep compiling.
  - `Settings` gains `feeRule`. For a new teacher it is off in both parts, with 24h / 50% / 100% ready for when a part is turned on (AC-006-19).
- **Data** (`core/data`).
  - Migration 1 → 2, written by hand: `ALTER TABLE ADD COLUMN` for each new column. The `MigrationsTest` helper test seeds version 1 and checks a lesson and a student survive with their defaults.
  - Fee rules are stored as JSON text in the same shape everywhere: `student.fee_rule`, `place.fee_rule` (null = Padrão, `"never"`, or the rule object) and settings key `fee_rule`. Encoding lives in `Mapping.kt` with `org.json`, as settings already do.
  - `observeFirstAfter` only returns scheduled classes (AC-006-17).
- **Use cases** (`app/usecases/Lessons.kt`, class `Lessons`), each in one transaction:
  - `markDone(id, minutes)`: refuses a class that hasn't started yet. It sets Dada and the duration, freezes the rate and the travel, and detaches the class from its series (AC-006-1, 2, 4).
  - `logLesson(draft)`: refuses a future date or a start that hasn't happened yet, and an unlinked place. It inserts a one-off class that is already Dada, with its rate frozen (AC-006-6, 8).
  - `Schedule.editLesson` takes an optional `StatusChange(status, noticeAt, keepFee)` in `LessonChange`. The status applies to the one class being edited, even when the rest goes to "Esta e as próximas" (AC-006-10). Dada and Faltou are refused before the start (AC-006-11). Moving to Dada freezes; moving back to Agendada clears the frozen rate (AC-006-13). The fee is worked out by `suggestFee` in the same transaction from the class's rule and stored as cents, or recorded as waived (AC-006-27).
  - The default rule is saved with the other settings (`AppServices.saveSettings`), like the language. `savePlace` and `saveStudent` carry `feeChoice` with the rest.
- **Screens.**
  - Lesson detail gets the status line ("Agendada · em 25 min", "Cancelada · taxa R$ 60,00"), Quem paga, and Marcar como dada (only when scheduled and started). Marcar como dada opens a `BottomSheet` with the duration stepper, the value and "Confirmar · R$ …".
  - Nova aula gets a `SegmentedControl` at the top, Registrar aula dada | Agendar aula, starting on Agendar. Registrar shows "Duração real", hides Repetir, limits the date to today or earlier, shows how the value was worked out, and labels Save "Registrar aula · R$ …".
  - Editar aula gets Situação as chips with its hint line. Under Cancelada it shows "Avisou em" (date and time fields) and the fee switch or the reason there is none.
  - Hoje and Agenda: `LessonRow` shows a status badge for done, cancelled and missed classes, as its guideline already says. `AgendaBlock` gets a `MUTED` state for cancelled and missed classes; it is added to its guideline and the gallery.
  - Ajustes gets "Aulas e cobrança › Cancelamentos e faltas", which opens a new `FeeRule` screen. That screen has two `ListRow` switches, chips for the window and the amount, a `MoneyField` for a fixed amount, the live example and Salvar regra.
  - Student and place edit forms get "Cancelamentos e faltas" with `SegmentedControl` Padrão | Própria | Não cobrar. Própria shows the same rule editor, shared as a composable.
- All new copy goes in `strings_features.xml`, in both languages.

## Data

| Table | New column | Default / backfill |
| --- | --- | --- |
| lesson | `rate_cents` INTEGER | null (frozen only when done) |
| lesson | `rate_type` TEXT | null |
| lesson | `fee_cents` INTEGER | null |
| lesson | `fee_waived` INTEGER NOT NULL | 0 |
| lesson | `notice_at` TEXT | null |
| student | `fee_rule` TEXT | null = Padrão |
| place | `fee_rule` TEXT | null = Padrão |
| settings | key `fee_rule` | inserted by `ensureDefaults`, both parts off |

`status` already exists, with `scheduled` as its default. A done class's real duration overwrites `planned_min`, so no extra column is needed; 007 adds the timer's own timestamps.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `Lessons.markDone` | lesson | none | lesson lists (Room) |
| `Lessons.logLesson` | lesson | none | lesson lists |
| `Schedule.editLesson` (+ status) | lesson, schedule_rule | none | lesson lists |
| `AppServices.saveSettings` (fee rule) | settings | none | settings |
| `Catalog.savePlace` / `saveStudent` (+ fee choice) | place / student | none | catalog |

## Test strategy

- **Domain:** `LessonRulesTest`: done worth and freezing, recompute on duration change, `counted`, `ruleFor`, `suggestFee` and its rounding (AC-006-2, 3, 12, 16, 19, 21, 24, 25, 26).
- **Use cases:** `LessonsTest` on a real database with a frozen clock: mark done, log, status changes, fees stored, series untouched, next class (AC-006-4, 6, 8, 10, 11, 13, 17, 22, 23, 27).
- **Data:** `MigrationsTest` gains the 1 → 2 step (no criterion; rule 12).
- **Screens:** `LessonScreensTest`: the done sheet, the Registrar mode and button, Situação and its hint, the fee switch, the detail status line, the badges on Hoje, the fee rule screen, the student fee choice, and language (AC-006-1, 5, 7, 9, 14, 15, 18, 20, 28).

## Risks

- **The edit form grows.** Situação, notice and fee add three blocks. They only show for the statuses that need them.
- **Frozen versus following.** Every read must go through `Lesson.worth()`. Hoje, Agenda, the detail and the form already do, so no screen prices a class by itself.
- **Time-dependent rules** (started, late) use `ctx.now()`. Tests freeze it.
