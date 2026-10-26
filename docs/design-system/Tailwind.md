# Tailwind and NativeWind

Every token here becomes a Tailwind name, so a class in the app and a token in this system are the same word.

## Setup rules

- NativeWind's default `rem` is 14px. Set it to 16px (`inlineRem: 16` in the NativeWind config) so `p-4` is 16px, matching `space-4`.
- Colors are CSS variables, set at the app root by `ThemeProvider` with NativeWind's `vars()` from the resolved palette. Components never contain a hex value; a lint check fails the build if one appears in `src/app` or `src/ui/components`.
- Variants are written with `tailwind-variants` (`tv()`), class merging with `cn()` (`clsx` + `tailwind-merge`).

## tailwind.config.js (core of it)

```js
const v = (name) => `rgb(var(--${name}) / <alpha-value>)`;
module.exports = {
  content: ['./src/**/*.{ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    colors: {
      transparent: 'transparent',
      background: v('background'), surface: { DEFAULT: v('surface'), sunken: v('surface-sunken') },
      line: { DEFAULT: v('line'), soft: v('line-soft'), strong: v('line-strong') },
      ink: { DEFAULT: v('ink'), muted: v('ink-muted') },
      primary: { DEFAULT: v('primary'), soft: v('primary-soft') },
      'on-primary': { DEFAULT: v('on-primary'), muted: v('on-primary-muted') },
      accent: { DEFAULT: v('accent'), soft: v('accent-soft'), ink: v('accent-ink') },
      'on-accent': v('on-accent'),
      earnings: { DEFAULT: v('earnings'), 'on-header': v('earnings-on-header') },
      header: v('header'), 'on-header': { DEFAULT: v('on-header'), muted: v('on-header-muted') },
      success: { DEFAULT: v('success'), soft: v('success-soft'), ink: v('success-ink') },
      danger: { DEFAULT: v('danger'), soft: v('danger-soft'), ink: v('danger-ink') },
      secondary: { DEFAULT: v('secondary'), soft: v('secondary-soft'), ink: v('secondary-ink') },
      // tag-amber … tag-teal follow the same DEFAULT / soft / ink shape
      focus: v('focus'), scrim: 'rgb(var(--scrim) / 0.5)',
    },
    fontFamily: { sans: ['Nunito_400Regular'], bold: ['Nunito_700Bold'], black: ['Nunito_800ExtraBold'] },
    borderRadius: { xs: '6px', sm: '9px', md: '12px', lg: '14px', xl: '16px', '2xl': '20px', '3xl': '28px', full: '999px' },
    extend: {
      spacing: { 0.5: '2px', 1.5: '6px', 2.5: '10px', 3.5: '14px', 4.5: '18px', 7: '28px', 11: '44px', 12: '48px', 14: '56px', 18: '72px' },
      fontSize: {
        'money-display': ['48px', '56px'], 'money-xl': ['34px', '40px'], 'money-lg': ['26px', '32px'],
        'title-xl': ['26px', '32px'], 'title-lg': ['22px', '28px'], 'title-md': ['20px', '26px'],
        section: ['16px', '22px'], body: ['15px', '21px'], row: ['14px', '20px'], caption: ['13px', '18px'], meta: ['12px', '16px'], tab: ['11px', '14px'],
      },
    },
  },
};
```

The tokens are hex values; `ThemeProvider` converts each one to space-separated RGB channels (`--primary: 46 94 78`) when it sets the variables, so Tailwind's opacity modifiers (`bg-primary/10`) work.

## Typical classes

| Token or style | Class |
| --- | --- |
| background / surface / surface-sunken | `bg-background` `bg-surface` `bg-surface-sunken` |
| ink / ink-muted | `text-ink` `text-ink-muted` |
| primary button | `bg-primary text-on-primary rounded-lg h-12 px-4 font-black` |
| accent button | `bg-accent text-on-accent rounded-lg h-12 px-4 font-black` |
| card | `bg-surface rounded-2xl p-4 shadow-card` (shadow via a `Card` component on Android: `elevation`) |
| badge paid | `bg-success-soft text-success-ink rounded-sm px-2 py-1 text-meta font-black` |
| money | `text-money-xl font-black text-earnings` + `style={{ fontVariant: ['tabular-nums'] }}` |
| screen gutter | `px-5` |

Shadows: React Native draws them per platform, so `shadow-*` tokens are applied by the `Card`, `Fab` and segmented-control components (iOS `shadow*` props, Android `elevation`), not by class names.
