# Tasks 013: Appearance

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain `theme/Presets.kt` and `theme/Hsl.kt`: presets, `quickMain` / `quickHighlight`, `applyQuick`, `readHex`, HSL both ways, `partContrast` and its levels, `lowContrastParts`. Test: `core/domain/src/test/kotlin/app/tutora/domain/AppearanceRulesTest.kt`. (AC-013-8, AC-013-12, AC-013-13, AC-013-14, AC-013-15, AC-013-17)
- [x] 2. Domain `SavedTheme`, `Settings.savedThemes` / `savedThemeId`, `unsavedColors`; `SettingsRepository` keys `saved_themes` and `saved_theme_id` with defaults. Test: `AppearanceRulesTest.kt`. (AC-013-6)
- [x] 3. Use cases `Appearance`: `setMode`, `chooseTheme`, `chooseSaved`, `saveAsTheme`, `renameSaved`, `deleteSaved`, `setQuick`, `setPart`, `restore`, `setTextSize`; wired into `AppServices`. Test: `app/src/test/kotlin/app/tutora/AppearanceTest.kt`. (AC-013-3, AC-013-5, AC-013-6, AC-013-9, AC-013-16)
- [x] 4. Design system: `Swatch`, `ThemeCard`, `ColorSlider`, each with its README and gallery entry (39 components). Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt`. (AC-013-4, AC-013-7, AC-013-11)
- [x] 5. Strings in `strings_appearance.xml` (English and Portuguese); `AppearancePreview`; the Aparência page and the Ajustes row in Geral. Test: `app/src/test/kotlin/app/tutora/AppearanceScreensTest.kt`. (AC-013-1, AC-013-2, AC-013-4, AC-013-6, AC-013-7, AC-013-10, AC-013-18)
- [x] 6. Suas cores: parts, hex field, sliders, contrast line, dark-mode note, Restaurar, "Salvar assim?" on leaving. Test: `AppearanceScreensTest.kt`. (AC-013-11, AC-013-12, AC-013-14, AC-013-15)
- [x] 7. More themes and Cor de fundo (Victor, 4 Nov 2026): `extraThemes` as a base theme plus fixed colors, resolved by `resolvePalette`; `backgroundPresets`; the Tema cards and the Cor de fundo row. Test: `AppearanceRulesTest.kt`, `AppearanceScreensTest.kt`. (AC-013-19, AC-013-20)
- [x] 8. Live clock on one line with Large text (found in review). Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt`. (AC-002-16)
- [x] 10. Sizes after review (Victor, 4 Nov 2026): buttons and segments drawn at 32 / 40 / 48 dp inside a 48 dp press area, lighter centered labels; Mês always six weeks of square tiles with small corners; swiping changes month (with screen-reader actions); the week strip less round. Test: `AgendaMonthTest.kt`, `MonthRulesTest.kt`, `DesignSystemTest.kt`. (AC-016-29, AC-016-30)
- [x] 11. Tamanho do texto with Muito grande, and Tamanho da interface (Compacta, Normal, Ampliada). Test: `AppearanceScreensTest.kt`. (AC-013-10, AC-013-21)
- [x] 12. Semana as a list of days, like Teams' agenda in Tutora's look: sticky DayHeading, AgendaCard per class, busy time in order, the strip following the scroll and the scroll following the strip, "Próxima semana". Test: `AgendaMonthTest.kt`, `FeatureScreensTest.kt`. (AC-016-31, AC-005-20)
- [x] 9. `./gradlew check`, update the roadmap status, and list the device checks as pending in the PR.

## Pending on a device

- Muito grande text with Ampliada interface at 360 dp on every tab: nothing cut off (AC-013-10, AC-013-21).
- The month swipe with a finger (checked on the emulator on 4 Nov 2026), and with TalkBack's actions (AC-016-29).

- Grande at 360 dp on every tab and on both pages: nothing cut off (AC-013-10, AC-002-16).
- Sistema following the phone's dark mode switch while the app is open (AC-013-3).
- The sliders' feel, and the preview keeping up while dragging (AC-013-12, AC-013-15).
- Themes of one's own after a backup is restored on another phone (AC-013-5).
