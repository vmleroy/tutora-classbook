Tutora is a classroom in a phone: chalkboard green for structure, pencil yellow for money, notebook paper for the ground, red pen only for what is owed. Build every screen from these tokens and components; screens assemble components and never style raw values.

## Content fundamentals

- **Portuguese first, English equal.** Every string exists in `pt-BR` and `en`. Write pt-BR naturally and translate meaning, not words.
- **Talk to the teacher as "você", warmly and briefly.** "Boa tarde, Victor", "3 alunos com pagamento pendente", "Adicionado ao saldo da aluna".
- **Sentence case everywhere**, including buttons and titles: "Registrar aula", "Backup e planilha". Uppercase only in the `overline` style ("AO VIVO").
- **Buttons are verbs** that say exactly what happens: "Salvar aula", "Iniciar aula", "Gerar PDF", "Registrar pagamento". Never "OK", "Enviar" or "Confirmar" alone.
- **Settings rows state the current value**, not what the feature is: "Automática · US$ 1 = R$ 5,42 · hoje", "Cobra 50% se cancelar com menos de 24h".
- **Money and time are concrete.** "R$ 1.234,56" (pt-BR) or "R$1,234.56" (en), 24-hour times "14:00", short dates "Seg, 26 out", durations "90 min · 1h30". Examples in placeholders start with "Ex.:".
- **Errors say what is wrong and the fix**: "Número incompleto: faltam 8 dígitos". No apologies, no exclamation marks.
- **No emoji** in the interface.

## Visual foundations

**Color.** Use semantic tokens only. `background` is notebook paper; `surface` holds cards, list groups, sheets and the tab bar; `surface-sunken` is the quiet fill of tracks and icon buttons. `ink` for text, `ink-muted` for labels and captions. `primary` (chalkboard) for primary buttons, selected options and the active tab. `header` (Topo) is the ground of Hoje's band, of each other screen's one summary card and of the live lesson screen: primary in light mode, a deep shade of primary in dark mode so large colored areas stay calm at night. Since spec 016 only Hoje has a colored band, with its white earnings card over its lower edge; other titles sit on `background`, and the Agenda's on a white card with the week. `accent` (pencil) for starting a lesson, the + action, progress fills and the live ring; never as text on light surfaces. Money figures use `earnings`. Status colors are fixed across themes so paid and owed always read the same: `success` for paid and done, `danger` for owed and overdue, `secondary` for school and formal job. Each status and tag hue has a `-soft` fill and an `-ink` text color that pass 4.5:1 on it in every theme.

**Themes.** Four themes (Quadro-negro, Caderno azul, Biblioteca, Giz pastel) × light and dark = eight token sets. Themes change color only; layout, type and spacing never change. The teacher can repaint seven parts (header, main buttons, highlight button, earnings numbers, background, cards, active menu); see the Theming section for how.

**Student tags.** Seven tag hues (`tag-amber`, `tag-green`, `tag-blue`, `tag-violet`, `tag-orange`, `tag-red`, `tag-teal`) identify students and places on avatars, dots, agenda blocks and report rows. A tag is never the only signal: the name is always beside it.

**Type.** Nunito for everything, weights 400 to 900; headings and money are 900. Money always uses tabular figures. Scale: `money-display` 48 (live counter only), `money-xl` 40, `money-lg` 26, `title-xl` 28, `title-lg` 22, `title-md` 20, `section` 18, `body`/`body-strong` 15, `row-title` 14, `caption` 13, `meta` 12, `badge` 12, `tab` 11. Nothing is smaller than 11px, and only tab labels are 11. The Large text setting scales all of it by 1.15.

**Spacing.** A 2px-based scale named like Tailwind's (`space-1` = 4px … `space-7` = 28px). Screens have a 20px side gutter (`space-5`). Cards pad 16px. Rows in a list sit 8px apart; sections 16px apart.

**Shape.** Rounded and friendly (spec 016): buttons are pills (`radius-full`); fields `radius-lg` (14); avatars and row tiles rounded squares (`radius-lg`); tiles and agenda blocks `radius-xl` (16); the Nova aula button `radius-2xl` (20); cards, list groups, the summary card and sheets `radius-3xl` (28). Segmented controls are joined segments: round outside, small corners inside.

**Depth.** Flat: surfaces are told apart by tone, not shadow. `background` under `surface` cards; the tab bar, icon buttons and tracks on `surface-sunken`; the active tab on a `tab-active-soft` pill.

**Touch.** Nothing pressable is smaller than 48×48 (`size-touch`, since spec 016). Default controls are 48px, the main bottom action 56px. Pressed state: `opacity-pressed`; disabled: `opacity-disabled`.

**Motion.** Short and functional: 150–250 ms ease-out for sheets, segment thumbs and switches; the live counter changes digits without animation. Respect the system's reduce-motion setting by removing all non-essential motion.

**Empty states.** The ruled-paper pattern (lines every 28px in `line-soft`) appears only on empty states.

## Accessibility

- Text pairs hold 4.5:1 in all eight themes (checked when the tokens are generated); large money on primary holds 3:1. Spec 016 checks the 2026 look's pairs (text on the summary card and its tiles, the joined buttons, the tab bar) in every theme and mode and with accepted Suas cores choices. `on-primary`, `on-accent` and `earnings-on-primary` are chosen by contrast, never by hand.
- Custom colors go through the same check: the Suas cores screen shows the ratio and flips text between white and ink automatically.
- Every icon-only control has an accessible label. Status is always a word, not only a color.
- Focus rings: 2px `focus`, offset 2px.

## Iconography

Line icons on a 24-unit grid with 2px round strokes, as in the mockups; in the app use the Lucide icon set as vector drawables. Sizes: 22 default, 18 in buttons and rows, 26 on the FAB. Only `play` is filled. No emoji.

## Components

Thirty-five components cover every mockup screen (MonthBars joined in spec 009; SummaryCard, StatTile, MonthGrid and MonthPicker in spec 016). They are grouped as Foundations (Icon), Actions (Button, IconButton, Fab), Selection (SegmentedControl, Chip, Switch, Checkbox, Stepper), Inputs (TextField, SelectField), Display (Badge, Avatar, TagDot, Money, ProgressBar, Card, EarningsSummary, MonthBars, SummaryCard, StatTile), Lists (SectionHeader, LessonRow, ListGroup, ListRow, AlertBanner), Navigation (ScreenHeader, TabBar, WeekStrip, AgendaBlock), Calendar (MonthGrid, MonthPicker) and Feedback (BottomSheet, EmptyState, TimerRing). Each has its rules in its own guidelines.

## In code

The design system is the `:core:designsystem` module; there is no separate reference implementation.

- **Tokens:** generated on every build from [`design/tokens.json`](../../design/tokens.json) into `TutoraTokens` (spacing, radius, size, opacity, type) and `TutoraColors`. The palette is resolved by the domain (`resolvePalette`), so custom colors and dark mode follow the Theming rules.
- **Theme:** `TutoraTheme(themeId, mode, custom, largeText)`. Read colors as `TutoraTheme.colors.<token>` and type as `TutoraTheme.type.<style>`; never a raw color, dp or sp in screens or components (a source check fails the build).
- **Components:** `components/` holds the 35 composables, grouped by family. Their Kotlin signatures are the reference for parameters. The **Props** blocks in each guideline still show the first (Expo) version's TypeScript; the options are the same, and the names map one to one (`onPress` → `onClick`, `children` → content or text). `TextField`'s kinds are `TextField`, `MoneyField` (every amount of money is typed, filling from the right) and `DurationField`; `SelectField`'s are `DateField` (calendar) and `TimeField` (clock).
- **Gallery:** `ComponentGallery`, opened in debug builds with `adb shell am start -n app.tutora.app/app.tutora.DevComponentsActivity`.
