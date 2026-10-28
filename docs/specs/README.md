# Specs

Tutora is built spec by spec. Each folder holds `spec.md` (what and why), `plan.md` (how) and `tasks.md` (ordered steps with tests). The rules every spec follows are in [`constitution.md`](constitution.md). Templates are in [`_templates/`](_templates/).

## Roadmap

Each spec ships on its own APK. On 27 Oct 2026 Tutora moved to native Kotlin + Compose: the Expo version of 001–005 is archived at the `expo-final` tag, and those specs are re-planned (`/plan`) and implemented again in Kotlin. Only 001 to 007 are written so far; write the next one with `/spec` when its turn comes.

| # | Spec | Status | Mockups |
| --- | --- | --- | --- |
| 001 | [Foundation](001-foundation/spec.md): Kotlin project, 5 tabs, theme tokens, i18n, SQLite + migrations, CI | done | NavMap, Settings |
| 002 | [Design system](002-design-system/spec.md): tokens for 4 themes × light/dark, 30 components, custom colors, Large text | done | Palette, Themes |
| 003 | [Money and language](003-money-and-language/spec.md): R$ money rules, amount entry, language switch in Ajustes | done | Settings |
| 004 | [Places and students](004-places-and-students/spec.md): built-in places, own places, students, groups, student–place links, price resolution, archive | done | Students, StudentDetail, StudentEdit, Places, PlaceDetail, PlaceEdit |
| 005 | [Schedule](005-schedule/spec.md): recurring and one-off classes, 8 weeks ahead, this-only vs this-and-following edits, Agenda and Hoje | done | Main, Schedule, AddLesson, EditLesson |
| 006 | [Lessons](006-lessons/spec.md): log a past lesson, mark done, cancel with a reason and a charge | done | AddLesson, LessonDetail |
| 007 | [Live timer](007-live-timer/spec.md): pause and resume, end with the real duration, discard, ongoing notification, Ajustes switch | in progress | Live |
| 008 | Payments and packages: record payment, covered lessons, balances | not written | StudentDetail |
| 009 | Earnings dashboard: period and type filters, 6-month trend | not written | Earnings |
| 010 | Reports: per student or place, PDF, XLSX, copy as text | not written | Report, ReportPdf, EarningsPdf |
| 011 | Backup: manual JSON and restore, automatic backup to a folder (after the SAF spike) | not written | Backup |
| 012 | Calendar sync and .ics | not written | Calendar |
| 013 | Appearance screens: theme picker, Suas cores, text size | not written | Theme, ColorPicker |
| 014 | Reminders and first-run setup | not written | Setup |

Status moves draft → ready → in progress → done. Update this table in the same PR that changes a status.
