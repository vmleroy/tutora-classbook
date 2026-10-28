# Tasks 001: Foundation (Kotlin)

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Gradle wrapper, version catalog, the four modules, ktlint, Lint as errors, CI workflow. (AC-001-19)
- [x] 2. `:core:domain`: money and language rules; a purity scan. Tests: `MoneyTest.kt`, `LanguageTest.kt`, `DomainPurityTest.kt`. (AC-001-5, AC-001-6, AC-001-16, AC-001-17)
- [x] 3. `:core:data`: Room v1 `settings`, repository, pre-migration backup, open with WAL and FK, exported schema. Tests: `DatabaseTest.kt`, `PreMigrationBackupTest.kt`, `MigrationsTest.kt`. (AC-001-12, AC-001-13, AC-001-15)
- [x] 4. `:core:designsystem`: token generator from `design/tokens.json`, `TutoraTheme` (Quadro-negro light and dark), Nunito, the money style. Tests: `ThemeTest.kt`, `TypographyTest.kt`, `RawColorScanTest.kt`. (AC-001-8, AC-001-9, AC-001-10, AC-001-11)
- [x] 5. `:app`: StartApp, splash gating, language apply, the five-tab NavHost with back stacks, empty states, Ajustes › Sobre, StartupError, strings in both languages. Tests: `StartAppTest.kt`, `TabsTest.kt`, `StartupErrorTest.kt`, `StringsTest.kt`, `SettingsScreenTest.kt`. (AC-001-1, AC-001-2, AC-001-3, AC-001-4, AC-001-7, AC-001-14, AC-001-22)
- [x] 6. Release signing from environment variables, `docs/release.md`, Maestro shell flow on testTags. (AC-001-18, AC-001-20, AC-001-21: manual or device)

## Pending on a device

- AC-001-2: tab switch under 300 ms on the phone.
- AC-001-18: measured 27 Oct 2026: `./gradlew test --rerun-tasks` (recompiling everything) takes 14 s for all 23 tests; CI runs the same `./gradlew check`.
- AC-001-19: `./gradlew check` (Lint with warnings as errors, ktlint, all tests) passes locally and in CI.
- AC-001-20: `TUTORA_VERSION_CODE=1 ./gradlew assembleRelease` builds a signed 6.7 MB APK (checked with the throwaway key); installing a higher `versionCode` over it on the phone is still to check.
- AC-001-21: `.maestro/shell.yaml` on an emulator.
