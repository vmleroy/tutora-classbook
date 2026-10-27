# Plan 001: Foundation (Kotlin)

Architecture reference: [`docs/architecture.md`](../../architecture.md), all sections. Mockups: [`NavMap`](../../mockups/NavMap.dc.html) (tabs; Alunos and Locais are now the Cadastro tab), [`Settings`](../../mockups/Settings.dc.html) (the Sobre row). The Expo version of this spec is in the `expo-final` tag; its decisions carry over, its code does not.

## Versions (checked 27 Oct 2026, latest stable)

| Piece | Version |
| --- | --- |
| Gradle | 9.8.2 (AGP 9.4 needs 9.6 or newer) |
| Android Gradle Plugin | 9.4.1 (built-in Kotlin support) |
| Kotlin | 2.4.21; KSP 2.3.12 |
| Compose BOM | 2026.09.00 |
| Navigation Compose | 2.10.2; Lifecycle 2.11.0; Activity Compose 1.13.0; AppCompat 1.8.0; Core SplashScreen 1.2.0 |
| Room | 2.8.5 with `androidx.sqlite:sqlite-bundled` 2.7.1 |
| kotlinx | datetime 0.8.0, serialization 1.12.0, coroutines 1.11.0 |
| Tests | JUnit 4.13.2, kotest-property 6.2.6, Robolectric 4.17, Roborazzi 1.77.0 |
| Lint | Android Lint, ktlint-gradle 14.2.0. Detekt is left out: its Kotlin 2.4 support is still alpha (2.0.0-alpha). |
| SDK | compileSdk 37, targetSdk 37, minSdk 26 |
| JDK | 21 toolchain (Robolectric needs it for SDK 36+), downloaded by the Foojay resolver |
| Espresso | 3.7.0, pinned for UI tests (the Compose rule brings 3.5, which breaks on Android 37) |

## Approach

- **Modules** (`settings.gradle.kts`, versions in `gradle/libs.versions.toml`):
  - `:core:domain`: Kotlin/JVM, plain Kotlin. It holds money (`Cents` = `Long`, `add`, `multiplyRate`, `roundHalfAwayFromZero`), `Language` with `firstRunLanguage(phoneTags)` and `startupLanguage(saved, phoneTags)`, and the settings model with its defaults.
  - `:core:data`: Android library with Room. `TutoraDatabase` v1 has one `settings` table (key, value JSON text, updated_at). The module also holds `SettingsRepository`, `PreMigrationBackup` and `openTutoraDatabase()`, which runs the backup, then Room with `BundledSQLiteDriver`, WAL and `PRAGMA foreign_keys=ON`. Schemas are exported to `core/data/schemas/`.
  - `:core:designsystem`: Android library with Compose.
    - A `generateTutoraTokens` Gradle task turns `design/tokens.json` into `TutoraTokens.kt` under `build/generated`. It runs on every build and is never committed, so it cannot go stale.
    - `TutoraTheme` provides the Quadro-negro light and dark palettes through a `CompositionLocal`; the other themes and custom colors come in 002. Material 3's `ColorScheme` is filled from the same palette.
    - Typography is Nunito, from the variable font in `res/font`, with a `money` style that has tabular figures (`"tnum"`).
  - `:app` (`com.vmleroy.tutora.app`, a new id so it can sit next to the Expo app):
    - `TutoraApplication` with an `AppContainer`, and `MainActivity` (AppCompat, for the per-app language).
    - The splash screen stays up until `StartApp` finishes.
    - `TutoraNavHost` has five tab destinations, each its own nested graph, so each keeps its back stack (`saveState` / `restoreState`).
    - A bottom bar for the tabs (Material 3 `NavigationBar` styled with tokens; the design-system TabBar comes in 002).
    - Each tab shows an empty state; Ajustes shows the Sobre row.
    - `StartupError` is the screen shown when a migration fails.
- **Start-up** (`StartApp` use case): read the phone's locales, open the database (backup first, then migrate), and insert any missing default settings. The language comes from the saved setting, or the phone on a first run, and is applied with `AppCompatDelegate.setApplicationLocales`. On a migration failure the result carries the backup path and the message.
- **Strings:** `values/strings.xml` (English) and `values-pt-rBR/strings.xml`. Android Lint's `MissingTranslation` and `ExtraTranslation` are errors.
- **Dark mode:** `configChanges="uiMode"`, so the app follows the phone's mode by recomposing, without restarting the activity.
- **Release:** `app/build.gradle.kts` signs from `TUTORA_KEYSTORE_PATH`, `TUTORA_KEYSTORE_PASSWORD`, `TUTORA_KEY_ALIAS` and `TUTORA_KEY_PASSWORD`, and takes `versionCode` from `TUTORA_VERSION_CODE`. `versionName` is `1.1.0`. `docs/release.md` is rewritten around `./gradlew assembleRelease`.
- **CI:** `.github/workflows/ci.yml` runs `./gradlew check` on JDK 17.
- **Maestro:** `.maestro/shell.yaml` uses Compose `testTag`s exposed as resource ids (`testTagsAsResourceId`).

## Data

`TutoraDatabase` version 1: `settings(key TEXT PRIMARY KEY, value TEXT NOT NULL, updated_at TEXT NOT NULL)`. The default rows are language, theme_id `quadro`, theme_mode `system`, setup_done `false`, custom_colors `{}`, text_scale `normal` and show_default_places `true`. The schema JSON `1.json` is exported and committed.

## Use cases

| Use case | Writes | Adapters called |
| --- | --- | --- |
| `StartApp` | settings (missing defaults only) | phone locales, file copy |

## Screens and components

The tab shell, five empty states, Ajustes with Sobre, and StartupError. They use Material 3 surfaces themed by `TutoraTheme`; the 30 design-system components arrive in 002.

## Test strategy

Lean: one focused test per criterion, all on the JVM.
- **Domain** (`core/domain/src/test`, JUnit + kotest-property): money (AC-001-17), language (AC-001-5, 6), and a source scan for domain purity (AC-001-16).
- **Data** (`core/data/src/test`, Robolectric + bundled SQLite): defaults, WAL and foreign keys (AC-001-12); the pre-migration copy (AC-001-13); exported schemas and the migration cases (AC-001-15).
- **App** (`app/src/test`, Robolectric + Compose UI test):
  - the tab order and back stacks (AC-001-1, 2, 3), splash gating (AC-001-4) and the error screen (AC-001-14)
  - theme colors and following dark mode (AC-001-8, 9), and fonts (AC-001-11)
  - string resources in both languages (AC-001-7), the raw-color scan (AC-001-10) and the Sobre row (AC-001-22)
- **Manual or device:** AC-001-18 (timing of `./gradlew test`), AC-001-19 (`./gradlew check`), AC-001-20 (install over), AC-001-21 (Maestro), and AC-001-2's 300 ms on a phone.

## Risks

- **New versions together** (AGP 9 built-in Kotlin, Kotlin 2.4, Room 2.8 with KSP 2.3): if a combination fails to build, step back one minor version and record it here.
- **Robolectric and SDK 37** (happened): it needs JDK 21 and two JVM flags; settled by the JDK 21 toolchain.
- **Nunito variable font:** weights 400–800 through `FontVariation`; check the rendering on a device.
