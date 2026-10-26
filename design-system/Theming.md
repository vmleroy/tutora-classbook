# Theming and custom colors

The palette on screen is always computed, never stored: `resolvePalette(theme, mode, systemScheme, customColors)` turns the teacher's choices into the 50 color variables, and `ThemeProvider` sets them at the root.

## Steps

1. **Pick the token set**: one of the eight sets here (theme × light/dark). Mode "system" follows the phone.
2. **Apply custom colors**: the teacher can repaint seven parts. Each maps to tokens:

| Part (Suas cores) | Tokens it replaces |
| --- | --- |
| Cabeçalho (header) | `header` (hero header and live lesson screen); in dark mode the chosen color is deepened like the theme's own header |
| Botões principais | `primary` on buttons, chips, segments, switches |
| Botão de destaque | `accent`, `accent-soft` (derived) |
| Números de ganhos | `earnings` |
| Fundo | `background` |
| Cartões | `surface`, `surface-sunken` (derived) |
| Menu ativo | `primary` and `primary-soft` in the tab bar |

Because a part can differ from the theme's primary, components read part-specific variables (`--btn-primary`, `--tab-active`) that default to `primary`; the header part writes `header` directly.

3. **Derive and protect**: in dark mode a custom color is lightened about 40% in OKLCH. `-soft` tints are mixed from the color toward `surface`. `on-*` text flips between white and ink by WCAG contrast. If a custom color still gives text under 4.5:1, the Suas cores screen shows the ratio in `danger-ink` and keeps the teacher's color only after a confirmation.
4. **Never customizable**: `success`, `danger`, `secondary`, tag hues, `ink`, `ink-muted`, `focus`. Paid and owed must always read the same.

## Adding a theme later

A new theme is a new pair of token sets. Only `primary`, `accent` and `background` are chosen by hand for each mode; every other token is derived by the same script that checks contrast, so a theme cannot ship with a failing text pair.
