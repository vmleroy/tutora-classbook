# Tasks 001: Foundation

Each task lists the criteria it proves and where the test lives.

- [x] 1. Create the Expo app with TypeScript strict, Expo Router and a dev client; app config with name, package and versioning. (AC-001-19)
- [x] 2. ESLint, Prettier, type check scripts; import-boundary rule for `src/domain/`. Test: `tests/boundaries.test.ts`. (AC-001-16, AC-001-19)
- [x] 3. Jest with `jest-expo`, RNTL and fast-check; `npm test` runs everything in Node. (AC-001-18)
- [x] 4. `domain/money.ts` with property tests. Test: `src/domain/money.test.ts`. (AC-001-17)
- [x] 5. Theme tokens and `resolvePalette` for Quadro-negro light and dark. Test: `src/domain/theme/palette.test.ts`. (AC-001-8)
- [x] 6. NativeWind setup, `tailwind.config.js` with variable colors, `ThemeProvider` with `vars()`, Nunito fonts, base components; hex-color lint check; decide the Large text approach. Tests: `src/ui/theme/ThemeProvider.test.tsx`, `tests/no-hex.test.ts`. (AC-001-9, AC-001-10, AC-001-11)
- [x] 7. i18n with typed keys, pt-BR and en files, phone-language detection on first run. Test: `src/i18n/i18n.test.ts`. (AC-001-5, AC-001-6, AC-001-7)
- [x] 8. Database open, pragmas, Drizzle schema for `settings`, first migration, pre-migration copy, error screen. Tests: `src/data/db.test.ts`, `src/data/migrations.test.ts`. (AC-001-12, AC-001-13, AC-001-14, AC-001-15)
- [x] 9. Settings repo and Zustand store loaded at start-up; splash held until ready. Test: `tests/app/layout.test.tsx`. (AC-001-4)
- [x] 10. Tabs layout with five empty states, gear to Ajustes, About row. Tests: `tests/app/tabs.test.tsx`, `tests/app/settings.test.tsx`. (AC-001-1, AC-001-3, AC-001-22)
- [x] 11. Maestro flow through tabs and settings. Test: `.maestro/shell.yaml`. (AC-001-2, AC-001-21)
- [x] 12. Release keystore, `docs/release.md`, build and install-over-update check on a real phone. Manual check recorded in the PR. (AC-001-20)

Screen tests live in `tests/app/`, not beside the routes: Expo Router turns every file under `src/app` into a route.

## Criteria proven outside Jest

- **AC-001-2, AC-001-21 (device):** `.maestro/shell.yaml` taps through the five tabs and the gear on an Android emulator, and requires each tab's screen within 300 ms. It passed on a 360 dp emulator (API 36) with a release build on 26 Oct 2026. Timing on the reference phone is still to check before release (constitution rule 18).
- **AC-001-19 (CI):** `npm run typecheck` and `npm run lint` run in `.github/workflows/ci.yml` on every PR.
- **AC-001-20 (manual):** a release APK built with `npm run release:android` installed over the previous build (`adb install -r`, versionCode 1 → 2, same key) and kept the database. Checked on the emulator with a throwaway key on 26 Oct 2026; repeat with the real key on the phone (`docs/release.md`).
