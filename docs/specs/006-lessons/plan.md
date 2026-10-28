# Plan 006: Lessons (Kotlin)

Architecture reference: "Database schema and migrations" (frozen value), "Money and time". Mockups: AddLesson (Registrar), LessonDetail.

Revised on 27 Oct 2026, when Victor replaced the fee rules and Situação with a cancel sheet on the lesson detail (spec, Open questions).

## Approach

- **Domain** (`core/domain`).
  - `Lesson` gains `status` (`SCHEDULED`, `DONE`, `CANCELLED`, `MISSED`; `LIVE` arrives with 007), the frozen price (`frozenRate`), and for a cancelled or missed class its `cancelReason`, `cancelNote` and `feeCents` (the charge).
  - A done class is worth its typed value, or its frozen rate for its duration, plus the travel stored on it. This freezes the value (AC-006-3) and recomputes it when the duration changes (AC-006-12).
  - `counted(lesson)` is what Previsto adds: the worth of scheduled and done classes, and the charge of cancelled and missed ones (AC-006-16).
  - `Fees.kt` holds:
    - `CancelReason` (`STUDENT` and `TEACHER` mean Cancelada, `NO_SHOW` means Faltou; `TEACHER` is never chargeable).
    - `Charge` (`Percent(bp)` or `Amount(cents)`).
    - `chargeCents`, which rounds a percentage once through `multiplyRate` and needs a price (AC-006-19 to 21).
- **Data** (`core/data`).
  - Migration 1 → 2, written by hand, adds `lesson.rate_cents`, `rate_type`, `fee_cents`, `cancel_reason` and `cancel_note`. `MigrationsTest` builds a version 1 database from `1.json`, seeds it and opens it.
  - `observeFirstAfter` only returns scheduled classes (AC-006-17).
- **Use cases** (`app/usecases/Lessons.kt`), each in one transaction:
  - `markDone(id, minutes)` refuses a class that hasn't started. It sets Dada and the duration, freezes the rate and travel, and detaches the class from its series (AC-006-1, 2, 4, 11).
  - `logLesson(draft)` refuses a future date, a start still ahead and an unlinked place. It inserts a one-off class that is already Dada (AC-006-6, 8).
  - `cancel(id, Cancellation(reason, note, charge))` refuses "Aluno faltou" before the start. It stores the status, reason, note and the charge in cents, ignores a charge for "Eu cancelei", and detaches the class from its series (AC-006-10, 11, 20, 22).
  - `undo(id)` goes back to Agendada and clears the frozen rate, the reason and the charge (AC-006-13).
  - `Schedule.editLesson` doesn't touch the status.
- **Screens.**
  - **Lesson detail:**
    - The status line ("Agendada · em 25 min", "Cancelada pelo aluno · cobra R$ 60,00"), the note, and Quem paga.
    - A bottom bar: Cancelar aula (outline), plus Marcar como dada once started. A class that is no longer scheduled shows Desfazer instead.
    - Marcar como dada opens a `BottomSheet` with the duration stepper and "Confirmar · R$ …".
    - Cancelar aula opens `CancelSheet` with:
      - reason chips ("Aluno faltou" only once started);
      - a `TextField` for the note;
      - a `ListRow` switch for "Cobrar esta aula?";
      - 50% / 100% / Outro valor chips (the percentages only for a priced class), with a `MoneyField` for Outro valor;
      - Voltar and "Cancelar aula · cobrar R$ …".
  - **Nova aula:** a `SegmentedControl` for Registrar aula dada | Agendar aula, starting on Agendar. Registrar shows "Duração real", hides Repetir, explains the value and labels Save "Registrar aula · R$ …".
  - **Hoje and Agenda:** `LessonRow` shows a status badge (with a check for Dada). `AgendaBlock` gets a `MUTED` state for cancelled and missed classes, already described in its guideline.
- All new copy goes in `strings_features.xml`, in both languages.

## Data

| Table | New column | Default / backfill |
| --- | --- | --- |
| lesson | `rate_cents` INTEGER | null (frozen only when done) |
| lesson | `rate_type` TEXT | null |
| lesson | `fee_cents` INTEGER | null (the charge) |
| lesson | `cancel_reason` TEXT | null (`student`, `no_show`, `teacher`) |
| lesson | `cancel_note` TEXT | null |

`status` already exists, defaulting to `scheduled`. A done class's real duration overwrites `planned_min`.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `Lessons.markDone` | lesson | none | lesson lists (Room) |
| `Lessons.logLesson` | lesson | none | lesson lists |
| `Lessons.cancel` | lesson | none | lesson lists |
| `Lessons.undo` | lesson | none | lesson lists |

## Test strategy

- **Domain:** `LessonRulesTest` (AC-006-2, 3, 12, 16, 19, 20, 21).
- **Use cases:** `LessonsTest` on a real database with a frozen clock (AC-006-4, 6, 8, 10, 11, 13, 17, 22).
- **Data:** `MigrationsTest` covers the 1 → 2 step (rule 12).
- **Screens:** `LessonScreensTest` (AC-006-1, 5, 7, 9, 14, 15, 18, 23, 24).

## Risks

- **Frozen versus following.** Every read goes through `Lesson.worth()`, so no screen prices a class by itself.
- **Time-dependent rules** (started) use `ctx.now()`. Tests freeze it.
