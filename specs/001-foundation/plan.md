# Plan 001: Foundation

Architecture reference: [`docs/architecture.md`](../../docs/architecture.md). Mockups: [`docs/mockups/`](../../docs/mockups/).

## Stack pinned in this spec

| Area | Package | Note |
| --- | --- | --- |
| App | `expo` (latest stable SDK at repo creation), `expo-router`, `expo-dev-client` | New Architecture, Hermes |
| Styling | `nativewind`, `tailwindcss`, `react-native-reanimated`, `react-native-safe-area-context`, `clsx`, `tailwind-merge`, `tailwind-variants` | Version pair per the open question in the spec |
| Fonts | `expo-font`, `@expo-google-fonts/nunito`, `expo-splash-screen` | Weights 400, 600, 700, 800 |
| i18n | `i18next`, `react-i18next`, `expo-localization` | Typed keys through `i18next` resource typing |
| Data | `expo-sqlite`, `drizzle-orm`, `drizzle-kit` | Bundled SQL migrations |
| State | `@tanstack/react-query`, `zustand` | |
| Colors | `culori` | OKLCH and contrast, used fully in spec 013 |
| Tests | `jest-expo`, `@testing-library/react-native`, `fast-check`, `better-sqlite3` (dev), Maestro CLI | |
| Quality | ESLint (Expo config), Prettier, `eslint-plugin-boundaries` or a small custom import check | Enforces AC-001-16 |

## Files created

```text
app.config.ts                 name Tutora, package com.vmleroy.tutora, versionCode from env
tailwind.config.js            semantic colors as CSS variables, Nunito as font-sans
global.css                    @tailwind base/components/utilities
babel.config.js, metro.config.js   NativeWind setup
drizzle.config.ts
src/app/_layout.tsx           providers: Query, Theme, i18n; splash until ready
src/app/(tabs)/_layout.tsx    5 tabs, icons, labels from i18n
src/app/(tabs)/today.tsx ... earnings.tsx   empty states
src/app/settings/index.tsx    About row
src/domain/money.ts           minimal Money + rounding
src/domain/theme/themes.ts    Quadro-negro light and dark tokens
src/domain/theme/palette.ts   resolvePalette(theme, mode, systemScheme)
src/data/db.ts                open db, pragmas, backup copy, run migrations
src/data/schema.ts            settings table
src/data/migrations/          0000_init.sql + meta
src/data/repos/settingsRepo.ts
src/state/settingsStore.ts    zustand, loaded at start-up
src/ui/theme/ThemeProvider.tsx   vars() from the resolved palette
src/ui/components/            Screen, Text, Button, Card, EmptyState
src/i18n/pt-BR.json, en.json, index.ts
tests/ and *.test.ts beside sources
.maestro/shell.yaml
docs/release.md               keystore and APK steps
```

## Start-up sequence

1. Keep splash. 2. Load fonts. 3. Open database, set pragmas. 4. If migrations are pending: copy the db file, then migrate (error screen on failure). 5. Read settings into the store (first run: insert defaults using the phone language). 6. Resolve the palette, set the CSS variables. 7. Hide splash, show Hoje.

## Risks

- NativeWind and the Expo SDK version must match; check NativeWind's install guide for the chosen SDK first.
- `better-sqlite3` and `expo-sqlite` must accept the same SQL; migrations avoid anything outside plain SQLite.
