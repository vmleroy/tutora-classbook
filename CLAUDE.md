# Tutora

Offline-first Android app for teachers: today's lessons, who owes money, and what was earned this month. Native Kotlin with Jetpack Compose, data in SQLite through Room on the phone. No backend, no accounts. iOS comes much later; the domain module stays plain Kotlin so it can be shared.

The first version (Expo + React Native, specs 001–005) is archived at the `expo-final` tag and the `archive/expo` branch. Its specs, mockups and design tokens carry over; each spec is re-planned for Kotlin with `/plan` and implemented again.

The owner is Victor. Interface copy is Portuguese first (`pt-BR`), with English equal.

## How work happens here: spec-driven development

Nothing is built without a spec. The cycle for every feature is:

1. `/spec NNN`: write `docs/specs/NNN-name/spec.md` (what and why, acceptance criteria `AC-NNN-n`). Victor reviews it.
2. `/plan NNN`: write `plan.md` (how) and `tasks.md` (ordered tasks, each naming its tests).
3. `/implement NNN`: on branch `spec/NNN-name`, all tasks in one pass with lean tests (one focused test per criterion, full checks once at the end), a few logical commits.
4. `/verify NNN`: every criterion has a passing test whose name contains its id; push the branch and open one PR per spec.

How a session usually runs:

- Victor asks for work in plan mode. Read what's needed, ask about anything only he can decide, write the plan and get it approved before touching code.
- Specs build on each other. When spec N starts before spec N−1 has merged, branch `spec/N-…` from `spec/(N−1)-…` and base its PR on that branch. Retarget the PR to `main` once the earlier one merges.
- Test only what proves a criterion. No emulator or Maestro runs unless Victor asks; device checks (Maestro flows, timing on Android, Large-text screenshots, performance budgets, the real signing key) are listed in the PR as pending.
- Release builds in development use a throwaway keystore kept outside the repo. The real release key belongs to Victor (constitution rule 19).

Start each session by reading, in this order:

1. [`docs/specs/constitution.md`](docs/specs/constitution.md): the 19 non-negotiable rules. If a change needs to break one, stop and ask.
2. [`docs/specs/README.md`](docs/specs/README.md): the roadmap and the status of each spec.
3. The spec you are working on: its `spec.md`, `plan.md` and `tasks.md`.

Read these when the spec points to them, not all up front:

- [`docs/product.md`](docs/product.md): features, screens, flows, data model, visual identity.
- [`docs/architecture.md`](docs/architecture.md): stack, modules and layers, state, schema, money/time rules, libraries, testing pyramid, build.
- [`docs/design-system/`](docs/design-system/): `README.md` (usage rules), `Theming.md` and `components/<Name>/README.md` (rules per component; their props are rewritten for Compose in spec 002).
- `design/tokens.json` is the source of all colors, type, spacing and radius; `:core:designsystem` generates its Kotlin tokens from it.
- [`docs/mockups/`](docs/mockups/): one HTML file per screen with the exact copy, layout and sample data.

## Rules that are easy to get wrong

- Money is `Long` cents of reais. Never floats or `Double`. Rounding lives only in the domain's money rules.
- `:core:domain` is plain Kotlin: no Android, Compose or Room dependencies. Pass the clock in.
- Screens never touch `:core:data`. Every write is a use case in one Room transaction.
- No raw `Color(0x…)`, `sp` or `dp` literals in screens or design-system components; use `TutoraTheme` tokens. Mockup hex values map to tokens.
- Every user-facing string is a resource in both `values/strings.xml` (English) and `values-pt-rBR/strings.xml`.
- New UI pieces come from the design system's components (35 since spec 016). A screen that needs a new one adds it to `:core:designsystem` first, with its guideline in `docs/design-system/components/<Name>/README.md` and a gallery entry.
- All Markdown documentation lives under `docs/`. Only `README.md`, `CLAUDE.md` and `.claude/commands/` stay at the root.
- Test names carry criterion ids: ``fun `AC-001-3 Ajustes is the fifth tab`()``.
- If a spec is ambiguous, write the question under "Open questions" and ask Victor rather than guessing on anything the teacher would see.

## Stack as installed (spec 001, 27 Oct 2026)

- Gradle 9.8.2, Android Gradle Plugin 9.4.1 (built-in Kotlin, so no `kotlin-android` plugin), Kotlin 2.4.21, KSP 2.3.12. Versions live in `gradle/libs.versions.toml`.
- JDK 21 toolchain for compiling and testing, downloaded by the Foojay resolver; any JDK 17+ can run Gradle itself.
- compileSdk and targetSdk 37, minSdk 26. Application id `app.tutora.app`.
- Compose BOM 2026.09.00 with Material 3 under `TutoraTheme`, Navigation Compose 2.10.2 (type-safe routes), Lifecycle 2.11.0, AppCompat 1.8.0 (per-app language), Core SplashScreen 1.2.0.
- Room 2.8.5 with the bundled SQLite driver in the app; kotlinx-datetime 0.8.0, kotlinx.serialization 1.12.0, coroutines 1.11.0.
- Tests: JUnit 4, kotest-property 6.2.6, Robolectric 4.17, Compose UI test (`junit4.v2.createComposeRule`), Espresso 3.7.0, navigation-testing.
- Static analysis: Android Lint (warnings as errors) and ktlint 14.2.0 (`.editorconfig`). Detekt is out until it supports Kotlin 2.4 in a stable release.

## Tooling gotchas

- Robolectric needs JDK 21 for SDK 36+ and two JVM flags (`--add-opens java.base/java.io`, `--add-exports java.base/jdk.internal.access`); the root `build.gradle.kts` sets them for every test task.
- The bundled SQLite driver's native library only loads on Android. JVM tests pass `AndroidSQLiteDriver()` (sqlite-framework, backed by Robolectric) to `openTutoraDatabase`.
- The Compose test rule pulls Espresso 3.5, which crashes on Android 37; modules with UI tests depend on `espresso-core` 3.7.0 explicitly.
- Design tokens are generated from `design/tokens.json` by `:core:designsystem:generateTutoraTokens` into `build/generated`; they are never committed.
- Composable functions are named like types; `.editorconfig` tells ktlint so.
- Room emits and writes on its own IO thread, which Compose's `waitForIdle` does not wait for. Screen tests use `Harness.settle()` / `awaitDb()` (real short sleeps) before looking; without them the tests are flaky.
- Screens read Flows and call use cases through `LocalServices` (`app/state/AppServices`), never `:core:data`; a source check enforces it.
- The app's copy for specs 003–005 is in `strings_features.xml` (both languages); Portuguese plurals need `one`, `many` and `other`.

## Commands

- `./gradlew check` (build, lint, detekt, ktlint, unit tests) must pass before any commit is pushed.
- `./gradlew installDebug` for a dev build on the emulator or phone; `./gradlew assembleRelease` with the `TUTORA_KEYSTORE_*` and `TUTORA_VERSION_CODE` environment variables for a signed APK.
- Maestro flows live in `.maestro/` (run only when Victor asks).

## Git

- One spec per branch (`spec/NNN-name`) and per PR into `main`.
- Commit messages start with the spec number: `001: add settings repo`.
- Docs-only changes go on `docs/<topic>` branches.
- Never commit the release keystore or any `.env`.
