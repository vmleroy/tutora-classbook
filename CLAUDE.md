# Tutora

Offline-first Android app (iOS later) for teachers: today's lessons, who owes money, and what was earned this month. Built with Expo + React Native + TypeScript, styled with Tailwind through NativeWind, data in SQLite on the phone. No backend, no accounts.

The owner is Victor. Interface copy is Portuguese first (`pt-BR`), with English equal.

## How work happens here: spec-driven development

Nothing is built without a spec. The cycle for every feature is:

1. `/spec NNN`: write `docs/specs/NNN-name/spec.md` (what and why, acceptance criteria `AC-NNN-n`). Victor reviews it.
2. `/plan NNN`: write `plan.md` (how) and `tasks.md` (ordered tasks, each naming its tests).
3. `/implement NNN`: on branch `spec/NNN-name`, one task at a time, tests first, commit per task.
4. `/verify NNN`: every criterion has a passing test whose name contains its id; push the branch and open one PR per spec.

How a session usually runs:

- Victor asks for work in plan mode. Read what's needed, ask about anything only he can decide, write the plan and get it approved before touching code.
- Specs build on each other. When spec N starts before spec N−1 has merged, branch `spec/N-…` from `spec/(N−1)-…` and base its PR on that branch. Retarget the PR to `main` once the earlier one merges.
- Device criteria (Maestro flows, timing on Android, Large-text screenshots) run on a local Android emulator with Maestro. The emulator stands in for the reference phone. Performance budgets and the real signing key still get checked on Victor's phone, and the PR lists them as pending.
- Release builds in development use a throwaway keystore kept outside the repo. The real release key belongs to Victor (constitution rule 19).

Start each session by reading, in this order:

1. [`docs/specs/constitution.md`](docs/specs/constitution.md): the 19 non-negotiable rules. If a change needs to break one, stop and ask.
2. [`docs/specs/README.md`](docs/specs/README.md): the roadmap and the status of each spec.
3. The spec you are working on: its `spec.md`, `plan.md` and `tasks.md`.

Read these when the spec points to them, not all up front:

- [`docs/product.md`](docs/product.md): features, screens, flows, data model, visual identity.
- [`docs/architecture.md`](docs/architecture.md): stack, layers, state, schema, money/FX/time rules, libraries, testing pyramid, build.
- [`docs/design-system/`](docs/design-system/): `README.md` (usage rules), `Tailwind.md`, `Theming.md` and `components/<Name>/README.md` (guidelines per component).
- [`design-system/`](design-system/): the non-Markdown sources. `tokens.json` (source of all colors, type, spacing), `components/index.d.ts` (props contract for the 30 components), and `components/bundle.js` with the `preview.html` files, a web reference implementation to port, not app code.
- [`docs/mockups/`](docs/mockups/): one HTML file per screen with the exact copy, layout and sample data.

## Rules that are easy to get wrong

- Money is integer cents with a currency. Never floats. Rounding lives only in `src/domain/money.ts`.
- `src/domain/` imports nothing from React, Expo, React Native or SQLite. Pass `now` in.
- Screens never touch `src/data/`. Every write is a use case in one transaction.
- No hex colors, raw font sizes or raw spacing in `src/app` or `src/ui/components`; use token classes (`bg-primary`, `text-ink-muted`, `p-4`). Mockup hex values map to tokens.
- Every user-facing string goes through i18n with keys in both `src/i18n/pt-BR.json` and `src/i18n/en.json`.
- New UI pieces come from the design system's 30 components. If a screen needs a new component, add it to the design system first: tokens and preview in `design-system/`, guideline README in `docs/design-system/components/<Name>/`.
- All Markdown documentation lives under `docs/`. Only `README.md`, `CLAUDE.md` and `.claude/commands/` stay at the root.
- Test names carry criterion ids: `it('AC-001-3: …')`.
- If a spec is ambiguous, write the question under "Open questions" and ask Victor rather than guessing on anything the teacher would see.

## Commands

The app does not exist yet; spec 001 creates it. Once it does:

- `npm run typecheck`, `npm run lint`, `npm test` must all pass before any commit is pushed.
- `npm run tokens` regenerates `src/ui/theme/tokens.ts` from `design-system/tokens.json`.
- `npx expo run:android` for a dev build; Maestro flows live in `.maestro/`.

## Git

- One spec per branch (`spec/NNN-name`) and per PR into `main`.
- Commit messages start with the spec number: `001: add settings repo`.
- Docs-only changes go on `docs/<topic>` branches.
- Never commit the release keystore or any `.env`.
