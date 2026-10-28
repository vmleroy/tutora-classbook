# Plan 003: Money and language (Kotlin)

Architecture reference: "Money and time", "Theming and i18n". Mockup: `Settings.dc.html` (Geral group).

## Approach

- **Domain (`Money.kt`).**
  - Amounts are `Cents` (a `Long`), with exact `add` and `sum`, and `multiplyRate` with half-away-from-zero rounding.
  - On top of those: `lessonValue`, `afterCommission`, `wholeReais`.
  - `typedCents` handles right-filled money fields, capped at R$ 99.999,99.
- **Formatting.** `formatMoney(cents, language, compact)` in the design system always formats BRL, through `NumberFormat` for `pt-BR` or `en-US`. `MoneyField` shows it and fills from the right.
- **Language.**
  - Ajustes › Geral has "Idioma do app" with a sample under each option, written in that option's own language.
  - The choice is saved in `settings`, and `MainActivity` applies it with `AppCompatDelegate.setApplicationLocales`. The screen is rebuilt in the new language, with no app restart.
  - Language splits are off in bundles, so every language stays in the APK.
- **No currency** anywhere: not in settings, not in formatting.

## Test strategy

- **Domain:** `MoneyRulesTest` (AC-003-1–4, 7–10).
- **Formatting:** `DesignSystemTest` (AC-003-5, 6).
- **Screens:** `FeatureScreensTest` (AC-003-11–14).
