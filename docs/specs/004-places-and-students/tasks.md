# Tasks 004: Places and students

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [ ] 1. `uuidv7` in `src/domain/id.ts`, `src/adapters/random.ts` on `expo-crypto`. No test of its own; ids are exercised by the use-case tests.
- [ ] 2. Schema and migration 0001 (`place`, `student`, `student_place`, `group_class`, `group_member`), `seed-v1.sql` fixture. Test: `src/data/migrations.test.ts` (existing AC-001-15 cases cover 0001).
- [ ] 3. Place rules and pricing: `src/domain/places.ts`, `src/domain/pricing.ts` (`resolvePrice`, `previewLesson`). Tests: `src/domain/places.test.ts`, `src/domain/pricing.test.ts`. (AC-004-4, AC-004-5, AC-004-6, AC-004-7, AC-004-8, AC-004-11, AC-004-18, AC-004-19, AC-004-20, AC-004-21, AC-004-22)
- [ ] 4. Repos and place use cases, built-ins at start-up (`ensureBuiltinPlaces`). Test: `src/usecases/places.test.ts`. (AC-004-2, AC-004-3, AC-004-9)
- [ ] 5. Student, link and group use cases. Test: `src/usecases/students.test.ts`. (AC-004-10, AC-004-12, AC-004-13, AC-004-16)
- [ ] 6. Archive, restore, remove permanently. Test: `src/usecases/places.test.ts`. (AC-004-23, AC-004-24, AC-004-25)
- [ ] 7. Design system: Icon `star`, `search`, `archive`; Stepper `onPressValue`; READMEs and gallery. Existing component tests stay green.
- [ ] 8. Query hooks and invalidation map (`src/state/places.ts`, `students.ts`, `queryKeys.ts`); i18n keys in both files.
- [ ] 9. Places screens: list, detail, edit/new with preview, archived. Test: `tests/app/places.test.tsx`. (AC-004-1, AC-004-17, AC-004-26)
- [ ] 10. Students screens: list with search and Por local, detail, edit/new with place cards and color, archived; group edit. Test: `tests/app/students.test.tsx`. (AC-004-14, AC-004-15)
- [ ] 11. Docs: product doc data model (place kinds, `archived_at`, group members), architecture schema table; full typecheck, lint, test.

## Pending on a device

- Cross-tab navigation from a student to a place and back feels right on Android.
- Steppers and the exact-amount sheet on a small screen with Large text.
