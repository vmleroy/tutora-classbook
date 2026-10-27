# Tutora constitution

These rules apply to every spec, plan and pull request. A change that breaks one needs an amendment to this file first, agreed with Victor.

## Product

1. **Offline first.** Every feature works with no network. v1 makes no network calls at all.
2. **The teacher's data stays on the phone.** No accounts, no backend, no analytics. Data leaves the phone only when the teacher shares or backs it up.
3. **Nothing is lost silently.** Destructive actions (delete, restore, migration) are confirmed or preceded by an automatic copy of the database.
4. **Language never changes an amount.** Switching Português/English changes only texts and how numbers and dates are written; every amount stays the same.

## Code

5. **Kotlin with warnings as errors.** No `!!` and no unchecked cast without a comment explaining why.
6. **Layers point down:** screens → state (ViewModels) → use cases → domain / repositories / adapters. Screens never touch the data layer or run SQL.
7. **The domain core is pure Kotlin.** The domain module depends on nothing from Android, Compose or the database, so it can be shared with iOS later. Time comes in as a `now` argument.
8. **Money is integer cents of reais (BRL).** No floating-point money, ever. Rounding is half away from zero, once per result, in the domain's money rules only.
9. **One currency.** Every amount in v1 is in reais. USD and exchange rates are out of v1; bringing them back needs an amendment and a forward migration.
10. **Instants are UTC ISO 8601 text** with milliseconds and `Z`. Local days, weeks and months are computed in the device time zone and converted to a UTC `[from, to)` range.
11. **Every write is a use case** that runs in one SQLite transaction, then calls adapters and invalidates queries.
12. **Migrations only go forward**, start from the exported schema of the previous version, and are tested against a database seeded at that version.
13. **Styling is Jetpack Compose with the design-system theme.** Colors, type and spacing come only from the semantic theme tokens (`primary`, `earnings`), never raw color or size values in screens and components.
14. **All user-facing text is a string resource** present in both Portuguese (pt-BR) and English. Money and dates are formatted for the app language at the edge.

## Process

15. **Spec first.** Each feature has `spec.md` (what and why, no technology), `plan.md` (how) and `tasks.md` (ordered steps) before code.
16. **Every acceptance criterion has a test** whose name contains its id (for example `AC-001-3`).
17. **One spec per branch and pull request.** CI (build, static analysis, tests) must pass before merge.
18. **Performance budget**, checked on a mid-range Android phone before each release: cold start to Today under 1.5 s, tab switch under 300 ms with a year of data, lists at 60 fps.
19. **The release signing key is never lost.** It lives outside the repo, in a password manager plus an offline copy.

## Amendments

- 27 Oct 2026 (Victor): v1 is reais only. Rules 1, 4, 8 and 9 no longer mention USD, exchange rates or the rate fetch.
- 27 Oct 2026 (Victor): Tutora is rebuilt as a native Android app in Kotlin with Jetpack Compose; iOS comes much later. Rules 5, 6, 7, 8, 12, 13, 14 and 17 no longer name Expo, React Native, TypeScript, NativeWind, Drizzle or `Intl`. The Expo version is archived at the `expo-final` tag.
