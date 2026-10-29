# Plan 013: Appearance

Architecture reference: docs/architecture.md (theming and i18n, layers, testing pyramid), docs/design-system/Theming.md. Mockups: docs/mockups/Theme.dc.html, docs/mockups/ColorPicker.dc.html, docs/mockups/Settings.dc.html.

## Approach

Spec 002 built everything that paints: `resolvePalette(theme, scheme, custom)`, the seven `CustomPart`s, dark-mode lightening, `checkCustomColor`, Large text, and `TutoraTheme` recomposing from `Settings`. This spec adds the two pages that change those settings, plus themes of the teacher's own.

- **Domain (`:core:domain`).**
  - **`theme/Presets.kt`:**
    - the quick main and highlight presets, each a name key and a hex;
    - `quickMain = listOf(HEADER, PRIMARY_BTN, ACTIVE_TAB)` and `quickHighlight = listOf(HIGHLIGHT_BTN)` (AC-013-8);
    - `applyQuick(custom, parts, hex)`, where a null hex means "Do tema".
  - **`theme/Hsl.kt`:** `Rgb.toHsl()` and `Hsl.toHex()` for Matiz, Intensidade and Brilho (AC-013-12), and `readHex(text)`, which accepts "#7A2E3B", "7a2e3b" or nothing (AC-013-12).
  - **Contrast:**
    - `partContrast(theme, scheme, part, hex)` returns the text the palette puts on the part (white or dark, from its `on-*` token), the ratio, and its level: good from 4.5:1, fair from 3:1, poor below (AC-013-13).
    - `lowContrastParts(theme, scheme, custom)` drives the question on leaving Suas cores (AC-013-15). Both build on `checkCustomColor`.
  - **`Settings` gains:**
    - `savedThemes: List<SavedTheme>`, where `SavedTheme(id, name, themeId, custom)` (AC-013-5);
    - `savedThemeId: String?`, the saved theme the current colors came from.
    - `unsavedColors(settings)` is true when there are custom colors that match no saved theme; it decides the question before switching themes (AC-013-6).
- **Data (`:core:data`).**
  - `SettingsRepository` writes two new keys, `saved_themes` (a JSON list) and `saved_theme_id`. They're ordinary settings, not device keys, so a backup carries them (confirmed by Victor).
  - No table and no migration.
- **Use cases (`:app usecases/Appearance.kt`).** `Appearance(ctx)`. Each write saves `Settings` through `SettingsRepository` in one transaction, so the app repaints at once from the settings Flow (AC-002-5).
  - Mode and theme: `setMode`; `chooseTheme(id, keepColors = false)`, which clears custom colors and `savedThemeId`.
  - Saved themes: `chooseSaved(id)`; `saveAsTheme(name)`, which returns the new id and marks it current; `renameSaved`; `deleteSaved`.
  - Colors: `setQuick(parts, hex)`; `setPart(part, hex)`; `restore()`, which clears all custom colors.
  - Text: `setTextSize`.
- **Screens (`:app ui/appearance/`).**
  - **Aparência** (route `AppearanceHome`):
    - the preview, then Modo as a `SegmentedControl`;
    - Tema as a grid of `ThemeCard`s: four built-in plus saved, with long-press or ⋯ for Renomear and Apagar;
    - "Salvar como tema" with a name sheet, then Personalizar: two `SwatchRow`s, each with "Do tema", the presets and +;
    - "Restaurar tema", Tamanho do texto as a `SegmentedControl`, and the "Cores avançadas" row.
  - **Suas cores** (route `CustomColors(part)`):
    - the preview, then "Escolha o que pintar" as selectable rows with a swatch each;
    - a hex `TextField`, three `ColorSlider`s, and the contrast line with a `TagDot`-style dot;
    - "Restaurar" in the header, and the "Salvar assim?" `BottomSheet` on back when `lowContrastParts` isn't empty.
  - **`AppearancePreview`** is a small Hoje (date, greeting, the earnings card, Iniciar aula, and in Suas cores also Salvar aula and the tab bar). It's built from the real components inside a nested `TutoraTheme` with the candidate settings, so the preview and the app can never disagree.
  - **Ajustes:** the "Aparência" row in Geral, with the summary line.
- **Design system (`:core:designsystem`), three new components**, each with its guideline and gallery entry (36 → 39):
  - **`Swatch`:** a color circle with a selected ring, read by its name. Its variants are "Do tema" (half theme, half outline) and "+" (opens Suas cores).
  - **`ThemeCard`:** the theme's name and its main and highlight dots, outlined when chosen.
  - **`ColorSlider`:** a labelled slider whose track shows the range of colors (hue, intensity or brightness), with a 48 dp thumb area.

  A row of swatches is a `FlowRow` of `Swatch`es in the screen. Theme and preset hex values live in the domain as data the teacher chooses from. Screens turn them into colors at the edge, and never write a color literal (rule 13).

## Data

No table and no migration. Two new settings keys, `saved_themes` and `saved_theme_id`, are inserted with defaults (an empty list and nothing) and go into backups like the other settings.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `Appearance.setMode` / `setTextSize` | `theme_mode` / `text_scale` | none | settings (the theme repaints) |
| `Appearance.chooseTheme` / `chooseSaved` | `theme_id`, `custom_colors`, `saved_theme_id` | none | settings |
| `Appearance.saveAsTheme` / `renameSaved` / `deleteSaved` | `saved_themes`, `saved_theme_id` | none | settings |
| `Appearance.setQuick` / `setPart` / `restore` | `custom_colors`; `saved_theme_id` cleared when the colors no longer match | none | settings |

## Screens and components

See Approach. Copy goes in a new `strings_appearance.xml` in both languages, including the theme and preset names. The contrast ratio is written with the app language's decimal mark.

## Test strategy

Lean, JVM only, one focused test per criterion; `./gradlew check` once at the end.

- **Domain** (`core/domain/.../AppearanceRulesTest.kt`):
  - quick colors paint exactly their parts, and "Do tema" clears them (8);
  - reading hex and the HSL round trip (12), the contrast line's text and levels (13);
  - dark-mode lightening shown (14), the low-contrast parts on leaving (15);
  - red and green untouched (17), unsaved colors (6).
- **Use cases with a real Room database** (`app/src/test/.../AppearanceTest.kt`):
  - Sistema at first and the modes (3);
  - saving, renaming, deleting and choosing a saved theme (5), and a saved theme through a backup and restore (5);
  - switching theme clearing colors (6), Restaurar (9, 16).
- **Screens** (`app/src/test/.../AppearanceScreensTest.kt`, Robolectric + Compose):
  - the Ajustes row and the page (1), and the preview following a choice (2);
  - the theme cards and choosing one (4);
  - the question before switching, with Salvar e trocar (6), and the swatches read by name (7);
  - Grande growing the text (10);
  - Suas cores' parts, hex and sliders (11, 12), and the dark-mode note (14);
  - a live change and "Salvar assim?" on leaving (15), and English (18).
- **Design system:** the gallery lists 39 components (`DesignSystemTest`).
- **Device checks, pending in the PR:** Large text screenshots at 360 dp on every tab (AC-013-10 and AC-002-16), Sistema following the phone's switch live, and the sliders' feel.

## Risks

- **Previews cost time.** A nested `TutoraTheme` per preview is cheap. The theme cards don't render previews, only two dots each.
- **Slider precision.** Hue 0–360, intensity and brightness 0–100 in whole steps; the hex is the source of truth, and the sliders are derived from it, so moving one slider never drifts the others.
- **Saved themes and old backups.** A backup from before 013 has no `saved_themes`; the restore's `ensureDefaults` fills it with an empty list.
