# Plan 012: Calendar sync and .ics

Architecture reference: docs/architecture.md (layers, feature libraries › Calendar sync and .ics export, theming and i18n, testing pyramid). Mockups: docs/mockups/Calendar.dc.html, docs/mockups/Settings.dc.html.

## Approach

One-way sync from Tutora's classes into one calendar on the phone, driven by the data rather than by each write:

- **Domain (`:core:domain`, `Calendar.kt`).** Plain Kotlin rules, no Android:
  - `eventFor(lesson, words, includeValue)` builds a `ClassEvent`:
    - the title "Ana Souza · Matemática", with "Cancelada · " or "Faltou · " in front for those classes;
    - the place's name and address;
    - the notes, the value when asked for, and the last line "Criado pelo Tutora";
    - start and end in UTC milliseconds.
  - `syncWindow(today, zone)` is from today 00:00 to the 8-week horizon (`agendaHorizon`).
  - `diffEvents(desired, existing)` compares by lesson id and returns which events to create, update and delete. An event changed or deleted in the calendar app is simply different from what's desired, so the next diff puts it back (AC-012-11).
  - `icsFile(events, stamp)` is a hand-written .ics, one VEVENT per class with `UID = <lesson id>@tutora.app`, so importing it again updates instead of doubling (AC-012-21). Times are in UTC, as the architecture doc says.
  - `BusyEvent` and `busyLayout(events, day)` split timed blocks from all-day bands (AC-012-18).
  - `CalendarWords` is the texts the rules need (prefixes, "Valor: %s", the footer). The adapter fills it from string resources in the app language, so rule 14 holds.
- **Data (`:core:data`).** No table and no migration. The calendar setup lives in settings keys read and written by a small `CalendarStore`:
  - the keys are `calendar_on`, `calendar_id`, `calendar_name`, `calendar_value`, `calendar_busy`, `calendar_synced_at` and `calendar_count`;
  - they join spec 011's `DEVICE_KEYS`, since a calendar id means nothing on another phone, so a backup never carries them and a restore keeps them (AC-012-14).
  - **Departure from the architecture doc:** it planned a `calendar_link` table (lesson id → event id). Tutora's mark on each event already says which class it belongs to, so the phone's calendar is the only record and nothing can drift after a restore, a reinstall or an edit in the calendar app. The architecture doc is updated in this PR.
- **Adapter (`:app adapters/calendar/`).** `PhoneCalendar`, behind an interface with an in-memory fake:
  - Reading: `hasPermission()`; `calendars()` (the writable calendars of each account, plus Tutora's own phone calendar if it exists); `ownCalendar(name, color)`, which creates or finds Tutora's phone-only calendar; `marked(calendarId)` and `markedEverywhere()`.
  - Writing: `insert`, `update`, `delete` and `deleteCalendar`.
  - Busy time: `busy(from, to)` is a Flow from a content observer, already leaving out marked events.
  - The Android version uses the phone's calendar store (`CalendarContract`). **The mark** is the event's custom-app fields, set to Tutora's package and `tutora://lesson/<id>`. They are hidden in calendar apps, and an app may set them on events in an account's calendar, unlike extended properties, which only the account's own syncing may write. The visible footer is only for people: AC-012-16 says typed text never counts.
- **Use cases (`:app usecases/CalendarSync.kt`).** `CalendarSync(ctx, phone, words)` writes the setup in one settings transaction, then calls the adapter:
  - `sync()` runs when sync is on and the permission is granted. It reads the classes in the window, diffs them against the marked events in the chosen calendar, applies the diff, and saves the time and count (AC-012-10).
  - `turnOn(calendarId)` and `choose(calendarId)` deletes the marked events in the old calendar, then syncs into the new one (AC-012-12).
  - `turnOff()` deletes every marked event, plus Tutora's own phone calendar if it made one (AC-012-13).
  - `cleanCount()` and `clean()` work through `markedEverywhere()`, then turn sync off (AC-012-15, 16).
  - `ics()` builds the file for the share sheet.
- **When it syncs.** `AppViewModel` keeps one collector for the app's lifetime. It combines the classes Flow for the window with the calendar setup and the language, waits about a second after the last change, and calls `sync()`. Every write therefore reaches the calendar within moments without touching each use case (AC-012-8), and so do a restore (Room emits again, AC-012-14) and a new day (the window moves on start and resume, AC-012-9).
- **Screens.**
  - **Calendário page** (`ui/calendar/Calendar.kt`, route `CalendarHome`): the sync switch with its status line, "Em qual calendário?" with "Só neste aparelho" and the account calendars, the two option switches, the note, "Exportar agenda como .ics" and "Limpar eventos do Tutora" with its confirmation.
  - **Ajustes:** the row in Geral.
  - **Agenda:** `BusyBlock`s behind the classes on Dia and Semana when busy time is on.
  - **Permissions:** `READ_CALENDAR` and `WRITE_CALENDAR`, asked through an injectable `CalendarPermission`, so tests answer it like spec 011's pickers.

## Data

No new table and no migration; the schema stays at version 5. Seven settings keys, inserted with defaults, are added to `DEVICE_KEYS`. Two new permissions go in the manifest.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `CalendarSync.turnOn` / `choose` | `calendar_on`, `calendar_id`, `calendar_name` | `PhoneCalendar.ownCalendar`, `delete` (old calendar), then `sync` | calendar state |
| `CalendarSync.sync` | `calendar_synced_at`, `calendar_count` | `PhoneCalendar.marked`, `insert`, `update`, `delete` | calendar state |
| `CalendarSync.setOptions` | `calendar_value`, `calendar_busy` | `sync` (value changes the descriptions) | calendar state |
| `CalendarSync.turnOff` | `calendar_on` | `PhoneCalendar.delete`, `deleteCalendar` | calendar state |
| `CalendarSync.clean` | `calendar_on` off | `PhoneCalendar.markedEverywhere`, `delete`, `deleteCalendar` | calendar state |
| `CalendarSync.ics` | nothing | none (the screen shares) | — |

## Screens and components

- **Calendário page** after Calendar.dc.html in the 016 look:
  - a `ListGroup` with the switch row, whose line is "Atualizado às 10:42 · 24 aulas nas próximas 8 semanas" or "Desligado";
  - "Em qual calendário?" as a `ListGroup` of selectable `ListRow`s, each with its letter tile and account;
  - a `ListGroup` with "Mostrar outros compromissos" and "Incluir valor na descrição";
  - the note as meta text;
  - an outlined `Button` for "Exportar agenda como .ics" and a ghost one for "Limpar eventos do Tutora";
  - a `BottomSheet` confirming the cleanup, and an `AlertBanner` when the permission is refused, with "Abrir configurações".
- **New design-system component `BusyBlock`:** a grey block on the sunken surface color with the event's time and title, not pressable. It has an all-day `band` variant, a thin bar at the top of a day. It comes with its guideline in `docs/design-system/components/BusyBlock/README.md` and a gallery entry. It is placed by `DayGrid` behind the `AgendaBlock`s.
- **Ajustes:** a `ListRow` "Calendário" in Geral, with an icon.
- **Copy:** a new `strings_calendar.xml` in both languages. The event texts come from it through `CalendarWords`, with the resources read in the app language.

## Test strategy

Lean, JVM only, one focused test per criterion; `./gradlew check` once at the end.

- **Domain** (`core/domain/.../CalendarRulesTest.kt`):
  - the footer (5), title, place, notes and the prefixes (6), the value line (7);
  - the diff for a moved, retitled and deleted class (8), the window (9), an edited event put back (11);
  - all-day versus timed (18), marked events left out of busy time (19);
  - the .ics with stable UIDs (20, 21), the words in English (22).
- **Use cases with a real Room database and `FakePhoneCalendar`** (`app/src/test/.../CalendarSyncTest.kt`):
  - Tutora's phone calendar is made and only it is written (4);
  - "Só esta aula" against "Esta e as próximas" (8), the count and time (10);
  - moving to another calendar (12), off removing only marked events (13), a restore (14);
  - the cleanup counting and deleting only marked events, a hand-typed footer included (15, 16).
- **Screens** (`app/src/test/.../CalendarScreensTest.kt`, Robolectric + Compose, `Harness.settle()`):
  - the Ajustes row and the page (1), a refused permission (2), the calendar choices (3);
  - busy blocks on Dia and Semana and none on Mês (17), the English page (22).
- **Device checks, pending in the PR:**
  - the custom-app mark surviving a Google and an Outlook round trip;
  - classes reaching calendar.google.com from an account calendar;
  - the .ics importing in Google Calendar, Outlook and Apple Calendar;
  - busy time with real calendars.

## Risks

- **The mark in a Google or Microsoft calendar.** Whether the custom-app fields survive the account's own syncing is checked on a device first (task 1).
  - If the cloud keeps them, nothing changes.
  - If the cloud drops them but the phone keeps them, cleaning works on that phone, but not for events coming back on a new phone; the PR says so.
  - If the phone drops them too, the plan comes back to Victor before going on: the fallback is an id written in the description, which the spec doesn't allow for cleaning (AC-012-16).
- **Calendar apps differ.** The phone's own calendar must be visible in the calendar app the teacher uses. Some apps hide phone-only calendars until they're turned on. The note on the page and the PR say so.
- **Big diffs.** Eight weeks is at most a few hundred events, and the diff only writes what changed. Writes run off the main thread, and the collector waits for changes to settle so a fill of eight weeks syncs once.
- **The permission can be revoked later.** `sync()` checks it every time. When it's gone, the status line says "Sem acesso ao calendário" and the switch shows off without losing the setup.
