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
- [`docs/design-system/`](docs/design-system/): `README.md` (usage rules, primitives, shared types), `Tailwind.md`, `Theming.md` and `components/<Name>/README.md` (rules and props per component).
- `src/ui/` is the design system itself: `theme/tokens/tokens.json` (source of all colors, type, spacing), the theme code and the components. There is no separate reference implementation; the dev gallery `/dev/components` is the live preview.
- [`docs/mockups/`](docs/mockups/): one HTML file per screen with the exact copy, layout and sample data.

## Rules that are easy to get wrong

- Money is integer cents of reais (v1 has no USD). Never floats. Rounding lives only in `src/domain/money.ts`.
- `src/domain/` imports nothing from React, Expo, React Native or SQLite. Pass `now` in.
- Screens never touch `src/data/`. Every write is a use case in one transaction.
- No hex colors, raw font sizes or raw spacing in `src/app` or `src/ui/components`; use token classes (`bg-primary`, `text-ink-muted`, `p-4`). Mockup hex values map to tokens.
- Every user-facing string goes through i18n with keys in both `src/i18n/pt-BR.json` and `src/i18n/en.json`.
- New UI pieces come from the design system's 30 components. If a screen needs a new component, add it to the design system first: the component in its own folder `src/ui/components/<Name>/` (`<Name>.tsx` + `index.ts`), its guideline with props in `docs/design-system/components/<Name>/README.md`, and an entry in the dev gallery.
- All Markdown documentation lives under `docs/`. Only `README.md`, `CLAUDE.md` and `.claude/commands/` stay at the root.
- Import app code with the `@/` alias (`@/ui/components`, `@/ui/theme/tokens`, `@/domain/money`), never long relative paths. Inside a component folder only its own `index.ts` uses `./`.
- Test names carry criterion ids: `it('AC-001-3: …')`.
- If a spec is ambiguous, write the question under "Open questions" and ask Victor rather than guessing on anything the teacher would see.

## Stack as installed

- Expo SDK 58, React 19.3, React Native 0.88 (New Architecture is always on, so there is no `newArchEnabled` flag), Expo Router 58 with `src/app` as the routes root and typed routes.
- NativeWind 4.2.7 with Tailwind CSS 3.4. NativeWind 5 was still a release candidate when the repo was created. `inlineRem: 16` is set in `metro.config.js`.
- Reanimated 4 with `react-native-worklets`, expo-sqlite + Drizzle ORM (drizzle-kit for migrations), TanStack Query 5, Zustand 5, i18next + react-i18next, culori.
- Jest 30 through `jest-expo`, React Native Testing Library 14 and fast-check. `better-sqlite3` runs the migration SQL in Node.
- Expo SDK 58 changed many APIs. Check the versioned docs (`https://docs.expo.dev/versions/v58.0.0/`, and `https://docs.expo.dev/llms.txt` for the index) before writing code against an Expo or Router API. Two examples: in tests `renderRouter` returns a promise, and tabs show only the screens declared in the layout.

## Tooling gotchas

- Add native or Expo packages with `npx expo install <pkg>` so versions match the SDK. Plain JS libraries go through `npm install`.
- npm runs install scripts only for packages approved in `package.json` under `allowScripts` (esbuild, better-sqlite3, unrs-resolver, fsevents, @parcel/watcher). After adding a package that needs one, run `npm approve-scripts <pkg>` and then `npm rebuild <pkg>`.
- `react-dom` is pinned to `19.3.0` to match `react`. A newer `react-dom` breaks peer resolution. `react-test-renderer` is not installed: RNTL 14 doesn't need it, and its peer range rejects React 19.3.0.
- App config is `app.config.ts`, not `app.json`. `android.versionCode` comes from the `TUTORA_VERSION_CODE` env var (default 1) and must grow on every release build. `version` follows the last spec shipped (`1.1.0` = spec 001).
- `metro.config.js` adds `.sql` to the source extensions so Drizzle migrations are bundled.

## Commands

- `npm run typecheck`, `npm run lint`, `npm test` must all pass before any commit is pushed.
- `npm run tokens` regenerates `src/ui/theme/tokens/tokens.ts` from `src/ui/theme/tokens/tokens.json`.
- `npx expo run:android` for a dev build; Maestro flows live in `.maestro/`.

## Git

- One spec per branch (`spec/NNN-name`) and per PR into `main`.
- Commit messages start with the spec number: `001: add settings repo`.
- Docs-only changes go on `docs/<topic>` branches.
- Never commit the release keystore or any `.env`.
