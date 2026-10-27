# Plan 002: Design system

Architecture reference: [`docs/architecture.md`](../../architecture.md), sections "Theming and i18n" and "Architecture layers".
Design reference: `src/ui/theme/tokens/tokens.json` holds every token; [`docs/design-system/`](../../design-system/) holds `README.md` (usage rules, primitives, shared types), `Tailwind.md`, `Theming.md` and `components/<Name>/README.md` (rules and props). The published [Tutora Design System](https://claude.ai/artifact/VqkNrENr4qcTcuDvqYwPei) is the visual reference.

## Approach

`src/ui` is the design system: the tokens and components live there, and the app never copies values by hand. A script turns `tokens.json` into TypeScript; a pure `resolvePalette` turns the teacher's choices into one flat palette; `ThemeProvider` writes that palette as CSS variables with NativeWind's `vars()`; Tailwind classes read the variables. Components are React Native components with the names and props in their guidelines, matched to the published design system's look; the dev gallery is their live preview.

## Files created or changed

```text
scripts/tokens.ts                 src/ui/theme/tokens/tokens.json → src/ui/theme/tokens/tokens.ts (+ --check mode for CI)
src/ui/theme/tokens/tokens.ts            GENERATED: 8 color sets, type styles, spacing, radius, size, opacity, shadow
src/domain/theme/palette.ts       resolvePalette(theme, mode, systemScheme, customColors) → Record<token, hex>
src/domain/theme/contrast.ts      WCAG ratio, onColor(bg) flip, OKLCH lighten/deepen (culori)
src/ui/theme/ThemeProvider.tsx    hex → "r g b" channels, vars(), wraps the root view
src/ui/theme/useTheme.ts          current palette + text scale for components that need raw values (SVG, shadows)
tailwind.config.js                colors, radius, spacing, font sizes from Tailwind.md; inlineRem 16
src/ui/cn.ts                      clsx + tailwind-merge
src/ui/components/<Name>.tsx      30 components, variants with tailwind-variants
src/ui/components/index.ts        barrel export
src/app/dev/components.tsx        dev-only gallery with theme/mode/text-size switcher
eslint rule (local plugin)        no hex, raw fontSize or raw spacing numbers in src/app and src/ui/components
.maestro/ds-large-text.yaml       screenshot flow at 360 px with Large text
```

## Key decisions

- **Token pipeline:** generated file is committed so the app builds without running the script; CI runs `npm run tokens -- --check` and fails on drift (AC-002-1).
- **Custom colors:** parts map to tokens exactly as `docs/design-system/Theming.md` says. Components read `--btn-primary` and `--tab-active`, which default to `primary`; the header part writes `header` directly.
- **Dark-mode header:** `header = mix(primary, background, 0.68)` with `on-header` chosen by contrast; a custom header color goes through the same deepening.
- **Large text:** a 1.15 multiplier applied in the type styles (through a `Text` component that reads the scale), not by changing rem, so spacing stays fixed and only type grows. Confirm in the Maestro screenshot flow (AC-002-16).
- **Shadows:** applied in `Card`, `Fab` and `SegmentedControl` through platform props (`elevation` on Android), not class names.
- **Icons:** `lucide-react-native`, mapped from the design system's `IconName` list in `Icon.tsx`.
- **Motion:** Reanimated with `useReducedMotion()`; when on, set values without animation (AC-002-17).

## Test strategy

- Pure: contrast over all 8 sets and all named pairs, palette resolution with custom colors, dark-mode deepening (Jest + fast-check in Node).
- Components: RNTL render per variant in `quadro-light` and `quadro-dark`, a11y walker for hit area and labels, Money locale matrix.
- Device: Maestro screenshot flow on the dev gallery with Large text at 360 px.

## Risks

- NativeWind `vars()` and class-based colors on Android must update without a flash when the theme changes; measure on a real phone in task 4.
- Hit area on small controls (Chip 36 px, Stepper 32 px buttons) needs `hitSlop` to reach 44 × 44.
