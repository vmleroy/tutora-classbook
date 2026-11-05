# Plan 014: Reminders, confirming classes and first-run setup

Architecture reference: docs/architecture.md (layers, feature libraries › Reminders, background work, testing pyramid). Mockups: docs/mockups/Setup.dc.html, docs/mockups/Settings.dc.html.

## Approach

- **Domain (`:core:domain`):**
  - **`Confirm.kt`:** `ConfirmWhen` (END, HOUR, NEXT_DAY) and `PastClasses` (ASK, AUTO). `toConfirm(lesson, now, zone, when)` is true for a scheduled, non-forecast class whose end (or end plus 1 h, or the next day's 00:00) has passed.
  - **`Reminders.kt`:**
    - `nextClassReminder(lessons, now, minutes)`: the next class still ahead and its reminder time.
    - `nextWeekly(now, day, hour)` and `nextDaily(now, hour)`.
    - `owedSummary(balances)`, the count and total for the "who owes" message.
  - **`Teaching.kt`:** the three kinds of teaching and `shown(kind, chosen, existing)`. Something hidden comes back when data of that kind already exists (AC-014-20).
  - **`Settings` gains:**
    - `confirmWhen`, `pastClasses`;
    - `remindBefore` (0, 5, 15, 30 or 60), `owedWeekly` with its day and hour, `confirmDaily` with its hour;
    - `teaching: Set<Teaching>` (all three for existing users).
- **Data (`:core:data`), migration 5→6:**
  - `lesson.auto_done INTEGER NOT NULL DEFAULT 0` holds "Dada sozinha". It gets an exported schema, a `MigrationsTest` case and a backup upgrade step 5→6 (`backupUpgrades`, AC-011-15).
  - New DAO reads: scheduled classes that started before now (as a Flow), and whether places or classes of a kind exist.
- **Use cases (`:app usecases/`):**
  - **`Confirming`:** `confirm(id)` (as `markDone` with the planned minutes), `confirmAll()`, which returns the ids, so `undoAll(ids)` can reuse `Lessons.undo`. `autoConfirm()` marks due classes done with `auto_done` set when Aulas passadas is AUTO. Cancelled and missed go through the existing `Lessons.cancel`.
  - **`Reminders`:** `plan()` sets three single inexact alarms through a `ReminderAlarm` adapter: the next class reminder, the next Monday 08:00, the next 20:00. It's called after any change, by a collector like the calendar sync's, and on start and boot.
  - **`Setup`:** saves the four steps: language, teaching, built-in place prices and travel mode, and backup and reminders through `Backups` and `Settings`.
- **Adapters (`:app reminders/`):**
  - `AndroidReminderAlarm`, with `AlarmManager.setAndAllowWhileIdle` like `AutoStartAlarm`.
  - `ReminderReceiver` posts a notification on a "Lembretes" channel and sets the next alarm. "Marcar todas" calls `Confirming.confirmAll`, and boot sets the alarms again.
  - `NotificationPermission` reuses spec 007's request.
- **Screens:**
  - **"Aulas para confirmar"** (route `ConfirmClasses`): rows with Dada, Cancelada and Faltou; Cancelada and Faltou reuse spec 006's cancel sheet. "Confirmar todas como dadas" asks first, then shows an `UndoBar`.
  - **Hoje:** a `ConfirmAlert` next to `PendingAlert`.
  - **"A confirmar" in Hoje, Dia and Semana:** the state `BlockState.TO_CONFIRM`, dimmed with its badge.
  - **Mês:** a ring on the day (`MonthDay.pending`).
  - **Agenda tab:** a count (`TabItem.count`).
  - **Ajustes rows:** Aulas passadas, Pedir confirmação and Lembretes under Aulas; Como você dá aula under Geral.
  - **Setup** (route `Setup`, 4 steps after Setup.dc.html, without the currency): shown when `setupDone` is false and there are no places of the teacher's own, students or classes. StartApp sets `setupDone` for teachers who already have data.
  - **Hiding by teaching kind:** Ganhos' filters, PlaceEdit's kinds, salary and payer follow `shown(...)`.
- **Design system:** `UndoBar` (a new component, with its README and gallery entry, 42 components), `TabItem.count`, `MonthDay.pending`, and `BlockState.TO_CONFIRM`.

## Data

Migration 5→6 adds `lesson.auto_done` (default 0), with schema 6 exported. New settings keys get defaults:
- `confirm_when` (end), `past_classes` (ask);
- `remind_before` (15, off until setup or Ajustes turns it on), `owed_weekly` (on, Monday, 08:00), `confirm_daily` (on, 20:00);
- `teaching` (all three).

These are teacher's data, so backups carry them.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `Confirming.confirm` / `confirmAll` / `undoAll` | lesson status, value, `auto_done` | `Reminders.plan`, calendar sync | lessons, balances, earnings |
| `Confirming.autoConfirm` | the same, `auto_done` = 1 | the same | the same |
| `Reminders.plan` | nothing | `ReminderAlarm` | — |
| `Setup.save` | settings, built-in places' prices and travel | `Backups.setAuto` | settings, places |

## Test strategy

Lean, one test per criterion, `./gradlew check` once.

- **Domain** (`ConfirmRulesTest`, `ReminderRulesTest`, `TeachingRulesTest`): AC-014-1, 12, 13, 14, 19, 20.
- **Use cases** (`ConfirmingTest`, `RemindersTest`, `SetupTest`), with a fake `ReminderAlarm`: AC-014-2, 6–11, 15, 17, 21–23.
- **Data:** the 5→6 migration in `MigrationsTest`.
- **Screens** (`ConfirmScreensTest`, `SetupScreensTest`): AC-014-3, 4, 5, 9, 16, 18, 24.
- **Device checks pending:** notifications arriving with the app closed and after a reboot, "Marcar todas" from the notification, and the setup on a fresh install.

## Risks

- **Inexact alarms** can be minutes late. That's fine for reminders (the architecture doc accepts it).
- **"Marcar como dadas sozinhas" with the app closed** marks classes at the next open or at the 20:00 alarm, not at the exact minute. The page says "Dada sozinha" either way.
- **Hiding parts by teaching kind** must never hide data that exists. `shown` always checks existing places and classes first.
