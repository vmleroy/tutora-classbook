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

**Color.** Use semantic tokens only. `background` is notebook paper; `surface` holds cards, list groups, sheets and the tab bar; `surface-sunken` is the quiet fill of tracks and icon buttons. `ink` for text, `ink-muted` for labels and captions. `primary` (chalkboard) for primary buttons, selected options and the active tab. `header` is the ground of the hero header and the live lesson screen: primary in light mode, a deep shade of primary in dark mode so large colored areas stay calm at night. `accent` (pencil) for starting a lesson, the + action, progress fills and the live ring; never as text on light surfaces. Money figures use `earnings`. Status colors are fixed across themes so paid and owed always read the same: `success` for paid and done, `danger` for owed and overdue, `secondary` for school and formal job. Each status and tag hue has a `-soft` fill and an `-ink` text color that pass 4.5:1 on it in every theme.

**Themes.** Four themes (Quadro-negro, Caderno azul, Biblioteca, Giz pastel) × light and dark = eight token sets. Themes change color only; layout, type and spacing never change. The teacher can repaint seven parts (header, main buttons, highlight button, earnings numbers, background, cards, active menu); see the Theming section for how.

**Student tags.** Seven tag hues (`tag-amber`, `tag-green`, `tag-blue`, `tag-violet`, `tag-orange`, `tag-red`, `tag-teal`) identify students and places on avatars, dots, agenda blocks and report rows. A tag is never the only signal: the name is always beside it.

**Type.** Nunito for everything, weights 400, 700 and 800. Money always uses tabular figures. Scale: `money-display` 48 (live counter only), `money-xl` 34, `money-lg` 26, `title-xl` 26, `title-lg` 22, `title-md` 20, `section` 16, `body`/`body-strong` 15, `row-title` 14, `caption` 13, `meta` 12, `badge` 12, `tab` 11. Nothing is smaller than 11px, and only tab labels are 11. The Large text setting scales all of it by 1.15.

**Spacing.** A 2px-based scale named like Tailwind's (`space-1` = 4px … `space-7` = 28px). Screens have a 20px side gutter (`space-5`). Cards pad 16px. Rows in a list sit 8px apart; sections 16px apart.

**Shape.** Rounded and friendly: buttons and fields `radius-lg` (14), compact rows `radius-xl` (16), cards `radius-2xl` (20), sheets and the hero header `radius-3xl` (28) on their open side, pills and avatars `radius-full`.

**Depth.** Flat by default. `shadow-card` on cards, `shadow-raised` only on the summary card that overlaps the hero header, `shadow-fab` only on the floating + button. Dark themes drop shadows and separate layers with `surface` steps.

**Touch.** Nothing pressable is smaller than 44×44 (`size-touch`). Default controls are 48px, the main bottom action 56px. Pressed state: `opacity-pressed`; disabled: `opacity-disabled`.

**Motion.** Short and functional: 150–250 ms ease-out for sheets, segment thumbs and switches; the live counter changes digits without animation. Respect the system's reduce-motion setting by removing all non-essential motion.

**Empty states.** The ruled-paper pattern (lines every 28px in `line-soft`) appears only on empty states.

## Accessibility

- Text pairs hold 4.5:1 in all eight themes (checked when the tokens are generated); large money on primary holds 3:1. `on-primary`, `on-accent` and `earnings-on-primary` are chosen by contrast, never by hand.
- Custom colors go through the same check: the Suas cores screen shows the ratio and flips text between white and ink automatically.
- Every icon-only control has an accessible label. Status is always a word, not only a color.
- Focus rings: 2px `focus`, offset 2px.

## Iconography

Line icons on a 24-unit grid with 2px round strokes, as in the mockups; in the app use `lucide-react-native`. Sizes: 22 default, 18 in buttons and rows, 26 on the FAB. Only `play` is filled. No emoji.

## Components

Thirty components cover every mockup screen. They are grouped as Foundations (Icon), Actions (Button, IconButton, Fab), Selection (SegmentedControl, Chip, Switch, Checkbox, Stepper), Inputs (TextField, SelectField), Display (Badge, Avatar, TagDot, Money, ProgressBar, Card, EarningsSummary), Lists (SectionHeader, LessonRow, ListGroup, ListRow, AlertBanner), Navigation (ScreenHeader, TabBar, WeekStrip, AgendaBlock) and Feedback (BottomSheet, EmptyState, TimerRing). Each has its rules in its own guidelines.

The design system is the code in `src/ui`; there is no separate reference implementation.

- **Tokens:** `src/ui/theme/tokens.json` is the source of every color, type style, spacing, radius, size and opacity. `tailwind.config.js` and the theme code read it; nothing copies values by hand.
- **Components:** `src/ui/components/<Name>.tsx`, one per component, with the props listed in its guideline (`docs/design-system/components/<Name>/README.md`, section Props), styled with NativeWind classes that map to the tokens (see the Tailwind section).
- **Preview:** the developer screen `/dev/components` (development builds only) shows every component in every variant, with a theme, mode and text-size switcher. The published [Tutora Design System](https://claude.ai/artifact/VqkNrENr4qcTcuDvqYwPei) is the original visual reference. The web prototype it was built from (`design-system/components/bundle.js`, `bundle.css` and the `preview.html` files) is kept only in git history, at commit `7c3c831`, for exact measurements while porting.

### Primitives

Two building blocks sit under the 30 components and are not counted among them:

- **Text:** all app text. `variant` is a type style name (`body`, `title-xl`, `money-xl`…), sized from the tokens and multiplied by the Large text scale; money styles and `tabular` get tabular figures.
- **Screen:** a screen's scrolling body on `background` with the 20px gutter.

### Shared types

```ts
type IconName = 'plus' | 'minus' | 'check' | 'close' | 'chevron-left' | 'chevron-right' | 'home' | 'calendar' | 'users' | 'pin' | 'wallet' | 'clock' | 'alert' | 'file' | 'cloud' | 'gear' | 'calendar-check' | 'no-show' | 'play';
type Tag = 'amber' | 'green' | 'blue' | 'violet' | 'orange' | 'red' | 'teal';
type Currency = 'BRL' | 'USD';
type TokenName = /* every color token name in tokens.json */;
function formatMoney(cents: number, currency?: Currency, locale?: string, compact?: boolean): string;
```
