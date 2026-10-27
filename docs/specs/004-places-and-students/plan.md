# Plan 004: Places and students (Kotlin)

Architecture reference: "Architecture layers", "State management", "Database schema". Mockups: Places, PlaceDetail, PlaceEdit, Students, StudentDetail, StudentEdit.

## Approach

- **Domain.**
  - `Places.kt`: types, pay modes, built-ins, checks, the default-place star, random tags, initials.
  - `Pricing.kt`: `resolvePrice` and `previewLesson`.
  - `Ids.kt`: `uuidv7`, with random bytes supplied by the app.
- **Data.**
  - Room entities `place`, `student`, `student_place`, `group_class` and `group_member`.
  - DAOs whose observed queries name every table they depend on, and `CatalogRepository`, which combines Flows into domain objects.
  - Database version 1 holds every table from 001 to 005. No Kotlin install exists yet, so no migration is needed. The machinery and its test are ready for the first one.
- **Use cases** (`app/usecases/Catalog.kt`): save place, student or group; link a student to a place; archive, restore, remove for good. Each runs in one transaction, ported from the first version.
- **State.** `app/state/AppServices` exposes the repositories' Flows and the use cases; screens reach it through `LocalServices` and never import `:core:data` (a source check enforces it). Forms keep their drafts in Compose state. This stands in for a ViewModel per screen.
- **Screens.**
  - Cadastro (Alunos | Locais), place detail and edit, student detail and edit, group edit, Arquivados.
  - Price and duration steppers whose value opens an exact-entry sheet.
  - Archive actions with a confirmation before removing.
- **Copy:** the first version's strings, converted to resources in both languages (`strings_features.xml`).

## Test strategy

- **Domain:** `PlacesTest` (AC-004-4–8, 11, 18–22, 28).
- **Use cases:** `CatalogTest` on a real database (AC-004-2, 3, 9, 10, 12, 13, 16, 17, 23–25).
- **Domain:** `MoneyRulesTest` (AC-004-27).
- **Screens:** `FeatureScreensTest` (AC-004-1, 2, 14, 15, 17, 23, 26, 28, 29).
