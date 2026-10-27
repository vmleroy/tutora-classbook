# Spec 002: Design system

Status: in progress
Depends on: 001 Foundation
Source of truth: the code in `src/ui` (tokens in `src/ui/theme/tokens.json`, the components) and [`docs/design-system/`](../../design-system/) (usage rules, component guidelines and props), also published as the [Tutora Design System](https://claude.ai/artifact/VqkNrENr4qcTcuDvqYwPei)

## Goal

Every screen in Tutora is built from one set of tokens and 30 shared components, so the four themes, light and dark mode, custom colors and large text work everywhere without per-screen work.

## User stories

- **US-1.** As a teacher, I pick any of the four themes in light or dark and the whole app changes at once, with readable text everywhere.
- **US-2.** As a teacher, I repaint seven parts of the app and text on them stays readable.
- **US-3.** As a teacher who needs bigger text, I turn on Large text and every screen scales without clipping.
- **US-4.** As the developer, I build a new screen only from shared components and tokens, and I can see every component in isolation.

## Acceptance criteria

### Tokens

- **AC-002-1.** `src/ui/theme/tokens.ts` is generated from `src/ui/theme/tokens.json` by a script (`npm run tokens`), never edited by hand; CI fails if the generated file is out of date.
- **AC-002-2.** For each of the 8 token sets (4 themes × light/dark), every text pair named in the token usage notes has contrast ≥ 4.5:1 (≥ 3:1 for large money on primary). A unit test checks all pairs.
- **AC-002-3.** `tailwind.config.js` exposes every color token as a class name equal to the token name (`bg-surface-sunken`, `text-danger-ink`), plus the spacing, radius and font-size scales from the design system's Tailwind section.
- **AC-002-4.** NativeWind runs with rem = 16, so `p-4` renders 16 px. A component test asserts the computed padding.

### Theme switching and custom colors

- **AC-002-5.** Given any theme and mode, when the teacher changes either one, every visible screen updates within one frame of the store change, with no restart and no flash of the old palette.
- **AC-002-6.** Given a custom color for one of the 7 parts, then only the tokens mapped to that part in the Theming section change; `success`, `danger`, `secondary`, tags, `ink`, `ink-muted` and `focus` never change.
- **AC-002-7.** Given a custom color in dark mode, then it is lightened in OKLCH as the Theming section says, and `on-*` text flips between white and ink by contrast.
- **AC-002-8.** Given a custom color that leaves text below 4.5:1, then the Suas cores screen shows the ratio in `danger-ink` and asks for confirmation before saving.
- **AC-002-8b.** In dark mode, the hero header and the live lesson screen use the `header` token, never `primary`, and a custom header color is deepened the same way.
- **AC-002-9.** "Restaurar cores" removes every custom color and returns to the theme's tokens.

### Components

- **AC-002-10.** The 30 components listed in the design system exist in `src/ui/components/` with the names and props listed in each component's guideline (`docs/design-system/components/<Name>/README.md`, section Props).
- **AC-002-11.** Each component has a render test in both a light and a dark token set, and a test per variant listed in its guidelines.
- **AC-002-12.** Every pressable component has a hit area of at least 44 × 44 and an accessibility label (visible text or `accessibilityLabel`). A test walks the rendered tree of each component and fails otherwise.
- **AC-002-13.** `Money` takes integer cents and formats with `Intl.NumberFormat` for the app language and currency, always with tabular figures. Tests cover pt-BR BRL, en BRL, pt-BR USD and en USD.
- **AC-002-14.** No hex color, raw font size or raw spacing number appears in `src/app` or `src/ui/components` outside the generated tokens (lint rule).
- **AC-002-15.** A hidden developer screen (`/dev/components`, only in development builds) shows every component in every variant, with a theme and mode switcher.

### Text size and motion

- **AC-002-16.** With Large text on, all type styles scale by 1.15 and no component clips or overlaps at 360 px width (checked on the developer screen in a Maestro screenshot flow).
- **AC-002-17.** With the system's reduce-motion setting on, sheets, switches and segment thumbs change state without animation.

## Out of scope

The screens themselves (each screen's spec composes these components). Icons beyond the ones in the design system's Icon list are added when a screen spec needs them.

## Decisions

- Large text scales all type styles by 1.15 (decided 26 Oct 2026; revisit only if the Maestro check finds clipping).
- In dark mode the hero header and the live lesson screen use the `header` token, a deep shade of the theme's primary, with light text on it. Light mode keeps primary as the header (decided 26 Oct 2026).
- In dark mode, custom colors for buttons, highlight, earnings, active tab and header are lightened 40% in OKLCH. A custom **background** or **cards** color is not lightened, since that would turn the dark ground light. It keeps its hue at the lightness of the theme's own dark background or surface, with only a light tint of chroma (0.04 at most). Proposed 26 Oct 2026 during task 4; Victor to confirm.
- On-color text (on primary buttons, accent and header) is whichever of white and ink reads better in light mode, and of light ink and the dark background in dark mode.
