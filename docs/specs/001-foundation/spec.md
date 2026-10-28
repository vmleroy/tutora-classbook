# Spec 001: Foundation

Status: done
Depends on: nothing

## Goal

An installable Tutora app shell that every later spec builds on: the five tabs from the mockups, the default theme, both languages, a local database that upgrades itself safely, and a test and build pipeline. No teaching features yet.

## User stories

- **US-1.** As a teacher, I install Tutora from an APK and open it to a home screen with five tabs: Hoje, Agenda, Cadastro, Ganhos, Ajustes.
- **US-2.** As a teacher, I see the app in my phone's language (Portuguese or English) on first open.
- **US-3.** As a teacher, I see the Quadro-negro theme, in light or dark following my phone.
- **US-4.** As a teacher, when I install a newer version over the old one, my data is kept.
- **US-5.** As the developer, I can run every test with one command and build a signed APK with one command.

## Acceptance criteria

### App shell

- **AC-001-1.** Given a fresh install, when the app opens, then the Hoje tab is shown with a bottom bar of five tabs in this order: Hoje, Agenda, Cadastro, Ganhos, Ajustes.
- **AC-001-2.** Given any tab, when the teacher taps another tab, then it opens in under 300 ms on the reference phone and keeps its own navigation stack.
- **AC-001-3.** Ajustes is the fifth tab, so it opens from anywhere. It shows the Sobre row (AC-001-22) and nothing else yet.
- **AC-001-4.** Given the app is opening, then the splash screen stays until fonts, settings and migrations are ready, and the first screen never flashes unstyled or in the wrong theme.

### Language

- **AC-001-5.** Given the phone language is Portuguese (any region), when the app opens for the first time, then all text is in pt-BR; for any other language it is in English.
- **AC-001-6.** Given the language setting exists, when the phone language changes later, then the app keeps the saved setting.
- **AC-001-7.** Every user-facing string in the shell comes from the string resources, and a string missing from either language fails the build checks.

### Theme

- **AC-001-8.** Given theme mode "system" (the default), when the phone is in dark mode, then the app uses Quadro-negro dark colors; in light mode, Quadro-negro light, matching the product doc's hex values.
- **AC-001-9.** Given the phone switches between light and dark while the app is open, then the app follows within one second without restarting.
- **AC-001-10.** Screens and components use only semantic theme colors; a check fails on a raw color value in screen or design-system code.
- **AC-001-11.** Text uses Nunito; numbers styled as money use tabular figures.

### Database

- **AC-001-12.** Given a fresh install, when the app opens, then the database is created with WAL mode and foreign keys on, and the `settings` table exists with defaults (language from the phone, theme Quadro-negro, mode system, setup_done false).
- **AC-001-13.** Given a database at an older schema version, when a newer app opens, then the database file is first copied to `tutora-pre-v{n}.db`, then pending migrations run in order, and the app opens normally.
- **AC-001-14.** Given a migration fails, then the app shows an error screen in the current language with the path of the copied database, and does not open the tabs.
- **AC-001-15.** Each migration has a test that applies it to a database seeded at the previous version.

### Domain core boundaries

- **AC-001-16.** The build fails if the domain code depends on Android, the UI toolkit or the database: it is plain Kotlin, ready to share with iOS later.
- **AC-001-17.** The domain has money as integer cents, exact addition, multiplying by a ratio, and half-away-from-zero rounding, proven by property tests (full money rules come in spec 003).

### Build and quality

- **AC-001-18.** One command runs domain, data and UI tests on the computer, with no emulator, in under 60 seconds.
- **AC-001-19.** The build, static analysis and formatting checks pass with zero errors.
- **AC-001-20.** One documented command produces a signed release APK, and installing a second build with a higher `versionCode` over the first keeps the database.
- **AC-001-21.** A Maestro flow opens the app on an emulator, taps through all five tabs, and passes.
- **AC-001-22.** Ajustes shows a Sobre (About) row with app version, build number and schema version.

## Out of scope

Students, places, lessons, money screens, custom colors, other themes, first-run setup, backups. The tabs show an empty state only.

## Decisions

- Large text scales type styles only; spacing never changes (decided 26 Oct 2026, kept for Kotlin).
- 27 Oct 2026: rebuilt in Kotlin + Jetpack Compose (constitution amendment). The Expo + React Native version is archived at the `expo-final` tag and the `archive/expo` branch.
- 27 Oct 2026: tabs are Hoje, Agenda, Cadastro, Ganhos, Ajustes from the start (decided in 005 review; the gear on Hoje is gone).
