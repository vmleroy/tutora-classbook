# Tasks 001: Foundation (Kotlin)

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [ ] 1. Gradle wrapper, version catalog, the four modules, ktlint, Lint as errors, CI workflow. (AC-001-19)
- [ ] 2. `:core:domain`: money and language rules; a purity scan. Tests: `MoneyTest.kt`, `LanguageTest.kt`, `DomainPurityTest.kt`. (AC-001-5, AC-001-6, AC-001-16, AC-001-17)
- [ ] 3. `:core:data`: Room v1 `settings`, repository, pre-migration backup, open with WAL and FK, exported schema. Tests: `DatabaseTest.kt`, `PreMigrationBackupTest.kt`, `MigrationsTest.kt`. (AC-001-12, AC-001-13, AC-001-15)
- [ ] 4. `:core:designsystem`: token generator from `design/tokens.json`, `TutoraTheme` (Quadro-negro light and dark), Nunito, the money style. Tests: `ThemeTest.kt`, `TypographyTest.kt`, `RawColorScanTest.kt`. (AC-001-8, AC-001-9, AC-001-10, AC-001-11)
- [ ] 5. `:app`: StartApp, splash gating, language apply, the five-tab NavHost with back stacks, empty states, Ajustes › Sobre, StartupError, strings in both languages. Tests: `StartAppTest.kt`, `TabsTest.kt`, `StartupErrorTest.kt`, `StringsTest.kt`, `SettingsScreenTest.kt`. (AC-001-1, AC-001-2, AC-001-3, AC-001-4, AC-001-7, AC-001-14, AC-001-22)
- [ ] 6. Release signing from environment variables, `docs/release.md`, Maestro shell flow on testTags. (AC-001-18, AC-001-20, AC-001-21: manual or device)

## Pending on a device

- AC-001-2: tab switch under 300 ms on the phone.
- AC-001-18: `./gradlew test` under 60 s (measured on the computer, noted in the PR).
- AC-001-20: a release APK installed over a lower `versionCode` keeps the database.
- AC-001-21: `.maestro/shell.yaml` on an emulator.
