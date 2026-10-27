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
- New UI pieces come from the design system's 30 components. A screen that needs a new one adds it to `:core:designsystem` first, with its guideline in `docs/design-system/components/<Name>/README.md` and a gallery entry.
- All Markdown documentation lives under `docs/`. Only `README.md`, `CLAUDE.md` and `.claude/commands/` stay at the root.
- Test names carry criterion ids: ``fun `AC-001-3 Ajustes is the fifth tab`()``.
- If a spec is ambiguous, write the question under "Open questions" and ask Victor rather than guessing on anything the teacher would see.

## Stack

- Kotlin (latest stable), JDK 17, Android Gradle Plugin and Compose BOM pinned in `gradle/libs.versions.toml` (spec 001 picks the versions and records them here).
- Jetpack Compose with Material 3 under the Tutora theme, Navigation Compose with type-safe routes, `ViewModel` + `StateFlow`, Coroutines and Flow.
- Room with exported schemas and the bundled SQLite driver; `kotlinx-datetime`; `kotlinx.serialization`.
- Tests: JUnit and `kotest-property` (domain), Room on the JVM (data), Compose UI tests under Robolectric and Roborazzi screenshots (screens), Maestro end to end.
- Static analysis: Android Lint, detekt, ktlint.
- Check the current Android and Kotlin docs before writing against an API that changed recently (Navigation, Room, foreground services, per-app language).

## Commands

- `./gradlew check` (build, lint, detekt, ktlint, unit tests) must pass before any commit is pushed.
- `./gradlew installDebug` for a dev build on the emulator or phone; `./gradlew assembleRelease` with the `TUTORA_KEYSTORE_*` and `TUTORA_VERSION_CODE` environment variables for a signed APK.
- Maestro flows live in `.maestro/` (run only when Victor asks).

## Git

- One spec per branch (`spec/NNN-name`) and per PR into `main`.
- Commit messages start with the spec number: `001: add settings repo`.
- Docs-only changes go on `docs/<topic>` branches.
- Never commit the release keystore or any `.env`.
