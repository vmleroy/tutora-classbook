# Tasks 003: Money and language

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Money rules in reais: `Cents`, `lessonValue`, `afterCommission`, `sum`, `wholeReais`; drop `Money`/`Currency`. Test: `src/domain/money.test.ts`. (AC-003-1, AC-003-2, AC-003-3, AC-003-4, AC-003-7)
- [x] 2. `formatMoney(cents, language, compact)` always in reais; `Money` without `currency`. Test: `src/ui/components/__tests__/display.test.tsx`. (AC-003-5, AC-003-6, AC-003-7)
- [x] 3. `typedCents` and `TextField kind="money"`, guideline and gallery entry. Tests: `src/domain/money.test.ts`, `src/ui/components/__tests__/selection.test.tsx`. (AC-003-8, AC-003-9, AC-003-10)
- [x] 4. `setLanguage` store action and the Geral group in Ajustes with samples. Test: `tests/app/settings.test.tsx`. (AC-003-11, AC-003-12)
- [x] 5. Saved language survives a restart over the phone language. Test: `tests/app/settings.test.tsx`. (AC-003-13)
- [x] 6. Remove `currency` from settings and USD/FX from the docs. Test: `tests/app/settings.test.tsx`. (AC-003-14)

## Pending on a device

- Typing in a money field on Android keeps the caret at the end (emulator or Victor's phone).
