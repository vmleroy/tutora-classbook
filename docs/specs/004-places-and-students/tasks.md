# Tasks 004: Places and students

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. `uuidv7` in `src/domain/id.ts`, `src/adapters/random.ts` on `expo-crypto`. Test: `src/domain/id.test.ts`.
- [x] 2. Schema and migration 0001 (`place`, `student`, `student_place`, `group_class`, `group_member`), `seed-v1.sql` fixture. Test: `src/data/migrations.test.ts` (existing AC-001-15 cases cover 0001).
- [x] 3. Place rules and pricing: `src/domain/places.ts`, `src/domain/pricing.ts` (`resolvePrice`, `previewLesson`). Tests: `src/domain/places.test.ts`, `src/domain/pricing.test.ts`. (AC-004-4, AC-004-5, AC-004-6, AC-004-7, AC-004-8, AC-004-11, AC-004-18, AC-004-19, AC-004-20, AC-004-21, AC-004-22)
- [x] 4. Repos and place use cases, built-ins at start-up (`ensureBuiltinPlaces`). Test: `src/usecases/catalog.test.ts`, `tests/app/places.test.tsx`. (AC-004-2, AC-004-3, AC-004-9)
- [x] 5. Student, link and group use cases. Test: `src/usecases/catalog.test.ts`. (AC-004-10, AC-004-12, AC-004-13, AC-004-16)
- [x] 6. Archive, restore, remove permanently. Test: `src/usecases/catalog.test.ts`. (AC-004-23, AC-004-24, AC-004-25)
- [x] 7. Design system: Icon `star`, `search`, `archive`; Stepper `onPressValue`; READMEs and gallery. Existing component tests stay green.
- [x] 8. Query hooks and actions (`src/state/catalog.ts`, shared `queryClient.ts`); i18n keys in both files.
- [x] 9. Places screens: list, detail, edit/new with preview, archived. Test: `tests/app/places.test.tsx`. (AC-004-1, AC-004-17, AC-004-26); the + Ligar aluno link also in `src/usecases/catalog.test.ts`
- [x] 10. Students screens: list with search and Por local, detail, edit/new with place cards and color; group edit; shared Arquivados screen. Test: `tests/app/students.test.tsx`. (AC-004-14, AC-004-15)
- [x] 11. Docs: product doc data model (place kinds, `archived_at`, group members), architecture schema table; full typecheck, lint, test.

- [x] 12. Exact duration entry: `typedDuration` in `src/domain/duration.ts`, `TextField kind="duration"`, `DurationStepper`; bottom sheets rise above the keyboard. Tests: `src/domain/duration.test.ts`, `src/ui/components/__tests__/selection.test.tsx`. (AC-004-27)

## Pending on a device

- Cross-tab navigation from a student to a place and back feels right on Android.
- Steppers and the exact-amount and exact-duration sheets on a small screen with Large text, with the keyboard open (the sheet must sit above it).
- Removing an item for good returns to its tab (`router.dismissTo`).
- `.maestro/shell.yaml` now expects "Nenhum local seu ainda" on Locais (not run).
