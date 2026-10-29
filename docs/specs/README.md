# Specs

Tutora is built spec by spec. Each folder holds `spec.md` (what and why), `plan.md` (how) and `tasks.md` (ordered steps with tests). The rules every spec follows are in [`constitution.md`](constitution.md). Templates are in [`_templates/`](_templates/).

## Roadmap

Each spec ships on its own APK. On 27 Oct 2026 Tutora moved to native Kotlin + Compose: the Expo version of 001–005 is archived at the `expo-final` tag, and those specs are re-planned (`/plan`) and implemented again in Kotlin. 001 to 012 and 016 are written so far; write the next one with `/spec` when its turn comes.

| # | Spec | Status | Mockups |
| --- | --- | --- | --- |
| 001 | [Foundation](001-foundation/spec.md): Kotlin project, 5 tabs, theme tokens, i18n, SQLite + migrations, CI | done | NavMap, Settings |
| 002 | [Design system](002-design-system/spec.md): tokens for 4 themes × light/dark, 30 components, custom colors, Large text | done | Palette, Themes |
| 003 | [Money and language](003-money-and-language/spec.md): R$ money rules, amount entry, language switch in Ajustes | done | Settings |
| 004 | [Places and students](004-places-and-students/spec.md): built-in places, own places, students, groups, student–place links, price resolution, archive | done | Students, StudentDetail, StudentEdit, Places, PlaceDetail, PlaceEdit |
| 005 | [Schedule](005-schedule/spec.md): recurring and one-off classes, 8 weeks ahead, this-only vs this-and-following edits, Agenda and Hoje | done | Main, Schedule, AddLesson, EditLesson |
| 006 | [Lessons](006-lessons/spec.md): log a past lesson, mark done, cancel with a reason and a charge | done | AddLesson, LessonDetail |
| 007 | [Live timer](007-live-timer/spec.md): pause and resume, end with the real duration, discard, ongoing notification, Ajustes switch | done | Live |
| 008 | [Payments and packages](008-payments/spec.md): who owes, record payment (oldest first or chosen classes), credit, balances on lists and Hoje, packages | done | StudentDetail, PlaceDetail, Students, Places, Main, LessonDetail, Live, AddLesson, StudentEdit |
| 009 | [Earnings](009-earnings/spec.md): Hoje's earnings card, Ganhos with period and type filters, earned / received / to receive, where it came from, 6-month trend; commission, package, bonus and salary rules | done | Earnings, Main |
| 010 | [Reports](010-reports/spec.md): per student, group or place with periods; PDF, Excel (own small writer) and copy as text; earnings report from Ganhos; name and Pix key in Ajustes | done | Report, ReportPdf, EarningsPdf |
| 011 | [Backup and restore](011-backup/spec.md): full backup file shared anywhere, automatic backup to a folder, restore with undo, readable spreadsheet | done | Backup, Settings |
| 012 | [Calendar sync and .ics](012-calendar/spec.md): classes of the next 8 weeks in a "Tutora – Aulas" calendar, updated on every change; other appointments as busy time on the Agenda; .ics export | ready | Calendar, Settings |
| 013 | Appearance screens: theme picker, Suas cores, text size | not written | Theme, ColorPicker |
| 014 | Reminders and first-run setup | not written | Setup |
| 016 | [Visual refresh 2026](016-visual-refresh/spec.md): the "Quadro refinado" look on every screen, built only from theme colors; Agenda titled by month and year, a Mês view and a month/year picker | done | DirA, A_Agenda, A_AgendaMes, A_AgendaAno, A_Aluno, A_Ganhos, A_Ajustes, A_Estilo |

Status moves draft → ready → in progress → done. Update this table in the same PR that changes a status.
