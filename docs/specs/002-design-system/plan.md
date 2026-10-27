# Plan 002: Design system (Kotlin)

Architecture reference: [`docs/architecture.md`](../../architecture.md), "Theming and i18n". Design reference: [`design/tokens.json`](../../../design/tokens.json) and [`docs/design-system/`](../../design-system/). Built together with 003–005 on the 001 PR, at Victor's request.

## Approach

- **Tokens.**
  - `design/tokens.json` is the only source.
  - `:core:domain:generateColorSets` writes the eight color sets as hex strings, for the palette rules.
  - `:core:designsystem:generateTutoraTokens` writes `TutoraColors` (the 53 tokens plus the part tokens `btn-primary`, `on-btn-primary`, `tab-active` and `tab-active-soft`) with `fromPalette`, and spacing, radius, size, opacity and type.
  - Both run on every build into `build/generated` and are never committed, so they cannot go stale (AC-002-1).
  - Component sizes the Expo version typed by hand (icon 18 and 26, borders, dots, switch, timer ring, agenda hour) are now size tokens.
- **Palette (domain).**
  - `ColorMath.kt`: sRGB ↔ OKLab ↔ OKLCH with chroma clamping, and WCAG contrast. This replaces `culori`.
  - `Palette.kt`: `resolvePalette(theme, scheme, custom)` and `checkCustomColor`, ported one to one.
- **Theme.** `TutoraTheme(themeId, mode, custom, largeText, reduceMotion)` resolves the palette, provides `TutoraColors`, `TutoraTypography` (×1.15 for Large text) and reduce-motion, and fills Material 3's `ColorScheme` from the same palette. `motionSpec()` is a short tween, or a snap when the phone turns animations off.
- **Components.**
  - The 30 components are in `:core:designsystem/components`, grouped by family: Foundations, Actions, Selection, Inputs, Display, Lists, Navigation, Feedback.
  - `TextField` comes in three kinds as separate composables: `TextField`, `MoneyField` and `DurationField`.
  - `BottomSheet` is Material 3's `ModalBottomSheet`, so it covers the screen, rises above the keyboard and closes on back.
  - Every pressable is at least 44 × 44 and labelled.
- **Icons.** The 22 Lucide icons (ISC) are converted to vector drawables.
- **Gallery.** `ComponentGallery` in the design system shows every component with theme, mode and text-size switchers. `DevComponentsActivity` opens it, in the debug source set only (AC-002-15).

## Test strategy

- **Domain** (`PaletteTest`): contrast over all 8 sets, and the custom-color rules as kotest property tests (AC-002-2, 5–9).
- **Design system** (`DesignSystemTest`, Robolectric):
  - tokens, measured spacing and theme switching
  - the gallery in light and dark, with tap variants
  - a semantics walk for 44 dp hit areas and labels
  - money formatting, the gallery switcher, Large text and reduce-motion
  - (AC-002-1, 3, 4, 5, 10–13, 15–17)
- **Source check** (`SourceChecksTest`): no raw dp, sp or color literals in UI code (AC-002-14).

## Risks

- **AC-002-8** asks for the Suas cores screen to show the ratio and confirm. The rule (`checkCustomColor`) is built and tested; the screen itself comes with spec 013.
- **Large text at 360 dp without clipping** (AC-002-16) still needs a device or screenshot check.
