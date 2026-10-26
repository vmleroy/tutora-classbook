# Tasks 001: Foundation

Each task lists the criteria it proves and where the test lives.

- [x] 1. Create the Expo app with TypeScript strict, Expo Router and a dev client; app config with name, package and versioning. (AC-001-19)
- [ ] 2. ESLint, Prettier, type check scripts; import-boundary rule for `src/domain/`. Test: `tests/boundaries.test.ts`. (AC-001-16, AC-001-19)
- [ ] 3. Jest with `jest-expo`, RNTL and fast-check; `npm test` runs everything in Node. (AC-001-18)
- [ ] 4. `domain/money.ts` with property tests. Test: `src/domain/money.test.ts`. (AC-001-17)
- [ ] 5. Theme tokens and `resolvePalette` for Quadro-negro light and dark. Test: `src/domain/theme/palette.test.ts`. (AC-001-8)
- [ ] 6. NativeWind setup, `tailwind.config.js` with variable colors, `ThemeProvider` with `vars()`, Nunito fonts, base components; hex-color lint check; decide the Large text approach. Tests: `src/ui/theme/ThemeProvider.test.tsx`, `tests/no-hex.test.ts`. (AC-001-9, AC-001-10, AC-001-11)
- [ ] 7. i18n with typed keys, pt-BR and en files, phone-language detection on first run. Test: `src/i18n/i18n.test.ts`. (AC-001-5, AC-001-6, AC-001-7)
- [ ] 8. Database open, pragmas, Drizzle schema for `settings`, first migration, pre-migration copy, error screen. Tests: `src/data/db.test.ts`, `src/data/migrations.test.ts`. (AC-001-12, AC-001-13, AC-001-14, AC-001-15)
- [ ] 9. Settings repo and Zustand store loaded at start-up; splash held until ready. Test: `src/app/_layout.test.tsx`. (AC-001-4)
- [ ] 10. Tabs layout with five empty states, gear to Ajustes, About row. Tests: `src/app/(tabs)/tabs.test.tsx`, `src/app/settings/settings.test.tsx`. (AC-001-1, AC-001-3, AC-001-22)
- [ ] 11. Maestro flow through tabs and settings. Test: `.maestro/shell.yaml`. (AC-001-2, AC-001-21)
- [ ] 12. Release keystore, `docs/release.md`, build and install-over-update check on a real phone. Manual check recorded in the PR. (AC-001-20)
