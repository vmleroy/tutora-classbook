# Plan 007: Live timer (Kotlin)

Architecture reference: "Live timer = timestamps" (state), "Native APIs" (live timer rows), "Database schema and migrations". Mockup: Live.

## Approach

- **Domain** (`core/domain/Timer.kt`).
  - `LessonTimer(lessonId, startedAt, pausedMs, pausedAt?, note)` holds the state: `pausedMs` is the time in pauses that are over, and `pausedAt` marks a pause in progress.
  - `elapsedMs(timer, now)` = (`pausedAt` or now − `startedAt`) − `pausedMs`, never negative. `pause` and `resume` return the next timer. Only timestamps are stored, so a restart loses nothing (AC-007-6, 7).
  - `liveMinutes(ms)` rounds to the nearest minute, at least 1 (AC-007-9). `earnedSoFar(price, ms)` gives a per-hour price × ms ÷ 3 600 000, rounded once through `multiplyRate`; it is not computed for a per-lesson price or a salary (AC-007-5).
  - `LessonStatus` gains `LIVE`. A live class counts like a scheduled one in `counted()` and `worth()` (AC-007-14).
  - `Settings` gains `liveTimer = true`.
- **Data** (`core/data`).
  - New table `lesson_timer` (`lesson_id` PK, FK to lesson with cascade, `started_at`, `paused_ms`, `paused_at`, `note`, `notes_before`). Migration 2 → 3, written by hand, creates it.
  - Settings key `live_timer`.
  - `ScheduleRepository` gets `live(): Flow<LiveLesson?>`, the live class with its timer, and `timer(lessonId)`.
- **Use cases** (`app/usecases/Live.kt`, class `Live`), each in one transaction, then the notification adapter:
  - `start(id)` refuses when another class is live (`ALREADY_LIVE`, with its id) or the class isn't scheduled. It sets Ao vivo, writes the timer and shows the notification (AC-007-1, 2).
  - `pause(id)` and `resume(id)` (AC-007-7). `setNote(id, text)` keeps the quick note in the timer row, so it survives the app closing (AC-007-8).
  - `end(id, minutes)` marks the class Dada through the same freeze as `Lessons.markDone`, adds the note to the lesson's notes (keeping the old notes in `notes_before`), leaves the timer paused at the moment of ending, and hides the notification (AC-007-10).
  - `undoEnd(id)` sets Ao vivo again, still paused, restores the notes and shows the notification (AC-007-11).
  - `discard(id)` goes back to Agendada, deletes the timer and hides the notification (AC-007-12).
  - Removing a live class with `Schedule.removeLesson` hides the notification (AC-007-16).
  - `Ctx` gains `live: LiveNotifier` (default: none), the adapter the use cases call after their transaction.
- **Notification** (`app/live/`).
  - `AndroidLiveNotifier` posts one ongoing notification on its own channel. While running it uses `setUsesChronometer` from the moment the class started, minus pauses, so the system ticks it with no app code running. Paused, it shows the frozen time.
  - The actions are Pausar / Retomar, which call `Live` through a `BroadcastReceiver`, and Encerrar, which opens `MainActivity` on the end sheet. Tapping the notification opens the live screen (AC-007-15).
  - `StartApp` posts it again on start-up if a class is still live, for example after a reboot.
  - **Departure from the architecture doc:** a plain ongoing notification, not a foreground service. Elapsed time comes from timestamps and the chronometer is drawn by the system, so no work has to run in the background. A foreground service would add a special-use declaration and a permission for nothing. The architecture doc is updated.
  - `POST_NOTIFICATIONS` is asked for the first time a class is started. If it is denied, the timer still works in the app (AC-007-16).
- **App wiring.** `AppContainer` opens the database once and builds a `Ctx` with the notifier, so the receiver and the screens share the same use cases.
- **Screens.**
  - `LiveScreen` (route `LiveLesson(id, end)`):
    - Ao vivo / Pausada, who, `TimerRing` with hh:mm:ss and "de 60 min planejados", and "Ganho até agora" ticking, or "Valor da aula" / salary / "Sem preço".
    - "Anotação rápida", then Pausar / Retomar, Encerrar aula and Descartar.
    - A 1-second ticker lives in its own composable.
    - After confirming, "Aula concluída" shows with Desfazer and Voltar para Hoje (AC-007-4, 5, 9 to 12).
  - The end sheet is the Marcar como dada sheet, factored into a shared `DoneSheet` with a "Continuar" dismiss (AC-007-9).
  - Lesson detail: a scheduled class gets Iniciar aula (when the setting is on); a live class gets "Abrir aula ao vivo" (AC-007-1, 13).
  - Hoje: a live card at the top with the ticking clock, and Iniciar aula on "Próxima aula" (AC-007-1, 13).
  - Agenda and the day list: `AgendaBlock` gets a `LIVE` state (accent outline and an "Ao vivo" badge), and `LessonRow` an "Ao vivo" badge (AC-007-14). The design system's guideline and gallery are updated.
  - Ajustes: an "Aulas" group with the "Aula ao vivo" switch (AC-007-3, 17).
- All copy goes in `strings_features.xml`, in both languages (AC-007-18).

## Data

| Table | Column | Default / backfill |
| --- | --- | --- |
| lesson_timer (new) | `lesson_id` TEXT PK → lesson, cascade | none; one row per live (or just-ended) class |
| | `started_at` TEXT | when Iniciar aula was tapped |
| | `paused_ms` INTEGER | 0 |
| | `paused_at` TEXT | null while running |
| | `note` TEXT | null |
| | `notes_before` TEXT | the lesson's notes before ending, for Desfazer |
| settings | key `live_timer` | `true` |

`lesson.status` gains the value `live`.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `Live.start` | lesson, lesson_timer | notification: show | lessons, live |
| `Live.pause` / `resume` | lesson_timer | notification: show | live |
| `Live.setNote` | lesson_timer | none | live |
| `Live.end` | lesson, lesson_timer | notification: hide | lessons, live |
| `Live.undoEnd` | lesson, lesson_timer | notification: show | lessons, live |
| `Live.discard` | lesson, lesson_timer | notification: hide | lessons, live |

## Test strategy

- **Domain:** `TimerRulesTest`: elapsed across pauses, `liveMinutes` rounding, `earnedSoFar` (AC-007-5, 6, 7, 9).
- **Use cases:** `LiveTest` on a real database, a frozen clock and a fake notifier (AC-007-1, 2, 8, 10, 11, 12, 14, 16).
- **Data:** `MigrationsTest` covers the 2 → 3 step (rule 12).
- **Screens:** `LiveScreensTest` (AC-007-3, 4, 13, 15, 17, 18). AC-007-15 checks the notification's content and actions with Robolectric's notification shadow.

## Risks

- **Second-by-second recomposition:** only the ticker composable recomposes; the rest of the screen reads stable state.
- **Notification on Android 14+:** the user can swipe away an ongoing notification that has no foreground service. The class stays live in the app, and the notification comes back on the next start or action.
- **Clock changes:** if the user moves the phone's clock while a class runs, elapsed follows the wall clock; `elapsedMs` never goes below 0.
