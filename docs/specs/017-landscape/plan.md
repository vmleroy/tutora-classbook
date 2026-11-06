# Plan 017: Landscape

> **Revised 5 Nov 2026** after Victor tried the first version on the emulator: fixed two columns looked strange and cramped. Every screen now keeps its upright structure at the readable width, and what is tapped opens in a side panel (option B of the "Tutora sideways" board). The live class takes layout L1, improved. The sections below are updated; `TwoPane`, `size-pane-min` and `MonthGrid`'s height fitting are gone.

Architecture reference: docs/architecture.md (layers, state, the design system). Mockups: none; the layouts are built from the design system's components (the spec's answers, 4 Nov 2026).

This spec builds on spec 014. Its branch, `spec/017-landscape`, starts from `spec/014-reminders-setup` while PR #20 is open.

## Approach

- **Turning keeps everything (AC-017-1):**
  - `MainActivity` declares `configChanges="orientation|screenSize|screenLayout|smallestScreenSize|uiMode"`. Turning the phone then never recreates the activity: Compose redraws with the new size, and every `remember`, scroll position, open sheet and back stack stays as it was.
  - That is stronger than relying on `rememberSaveable` alone, which many screens' small states don't use. `rememberSaveable` still covers process death, as the architecture doc says; its state row gains one line about this.
  - No XML layouts or `-land` resources exist, so nothing needs the recreation.
- **One rule for "sideways":** `TutoraTheme.landscape` is true when the window is wider than tall and shorter than 600 dp. It's read from `LocalConfiguration` in `TutoraTheme`, so tablets stay out (out of scope) and no window-size library is added. Screens branch on it; nothing else measures the screen.
- **Design system first:**
  - **Tokens:** `size-content-max` (600 dp, the readable width) and `size-rail` (88 dp).
  - **`NavRail` (new):** the five `TabItem`s in a column on the left, with the same icons, labels, active pill and count. It pads for the start, top and bottom safe-drawing insets (AC-017-2, 5).
  - **`SidePanel` (new):** a panel over the right side (at most 55% and 600 dp) on a scrim, sliding in; the screen under it doesn't move.
  - **`Modifier.readableWidth()`:** `widthIn(max = Size.contentMax)`, centered. `ScreenScaffold`'s column uses it, so cards and forms never stretch (AC-017-4, 14).
  - **`BottomSheet`:** `sheetMaxWidth = Size.contentMax` and its content already scrolls (AC-017-15).
  - **`MonthGrid` and `WeekStrip`:** sideways their days are `size-month-day` and touch-high rows instead of squares; the strip carries the weekday inside the tile.
  - **`TimerRing`:** `fill` grows it to the height of its place.
  - Components: 45 (`NavRail`, `SidePanel`), each with a guideline and a gallery entry.
- **Screens:**
  - **Shell (`TutoraNav`):** in landscape a `Row(NavRail, NavHost)` and no bottom bar. Details and forms take the whole width, as upright.
  - **`ScreenScaffold`:** readable width; `safeDrawingPadding` already keeps cut-outs and side navigation buttons clear (AC-017-5). Forms keep `imePadding` and bring the focused field into view (AC-017-14).
  - **The panel (AC-017-6, 7, 10, 11):** `app/ui/Panel.kt` holds one `DetailPanel` for the app. Screens open a page with `rememberOpenPage(nav)`: upright it navigates, sideways it fills the panel (student, group, place, a class, a day of Mês). `DetailPanelHost` in the shell draws it over the tab's screen (never the rail), only over the screen it was opened from, closes it on Back first, and opens its page full screen when the phone turns upright. Screens never check the orientation for this.
  - **Hoje, Ganhos, Cadastro:** their upright structure at the readable width; rows open in the panel.
  - **Agenda:** the header is one row sideways and its title row slides away on scroll (the strip stays). Dia full width, Semana and Mês at the readable width; a Mês day opens its classes in the panel (`AgendaDay`, a panel-only page).
  - **Live class (AC-017-12, 13):** its parts (`LiveParts`) laid out by `LiveUpright` or `LiveSideways`, picked in one line. Sideways (L1): the left side on the main color with back, the status and Descartar (an icon) above a ring as tall as fits; the right side with who, the value and its rate, the note, and Pausar and Encerrar at the bottom. `keepScreenOn` while the screen is open.
  - Every other screen gets landscape through `ScreenScaffold`'s readable width; nothing else changes.
- **Language (AC-017-16):** no new strings. The rail reads the tabs' strings.

## Data

None. No table, column or migration.

## Use cases

None. Nothing is written; only layouts change.

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| — | — | — | — |

## Screens and components

- **New design-system components:** `NavRail` and `SidePanel`, plus the `readableWidth` modifier and the two size tokens.
- **Changed:** `BottomSheet` (maximum width), `MonthGrid` (fits the height), `ScreenScaffold` (readable width).
- **Screens with their own landscape layout:** shell, Hoje, Ganhos, Agenda (Dia, Semana, Mês), Cadastro with its details, the live class.

## Test strategy

Lean: one test per criterion, JVM only.

- **Landscape screen tests:** `@Config(qualifiers = "pt-rBR-w800dp-h360dp-land")`.
- **`Harness.landscape`:** a mutable flag that provides a turned `LocalConfiguration`, so a test can turn the "phone" in the middle of a screen. That's how AC-017-1 is proved: type, turn, and find everything kept. A source check also makes sure the manifest keeps `configChanges`.
- **Layout assertions** compare node bounds: the left pane's nodes end before the right pane's begin, and readable content is at most `Size.contentMax` wide.
- **A shared `assertFitsSideways()`** checks that no text node extends past the root's width, at Muito grande text and Ampliada interface, for every main screen (AC-017-3).
- **Test files:**
  - `DesignSystemTest`: AC-017-2 (NavRail), 4 (readable width), 15 (sheet width).
  - `LandscapeTest`: AC-017-1, 2, 3, 6 to 12, 14, 16.
  - `LiveScreensTest`: AC-017-13 (`keepScreenOn` on, then off after leaving).
  - `SourceChecksTest`: AC-017-1 (manifest), 5 (the rail and scaffold use safe-drawing insets).
- **Pending on a device:**
  - turning a real phone both ways on every screen;
  - a phone with a camera cut-out and with side navigation buttons;
  - the keyboard over a field sideways;
  - a 360 dp tall phone at Muito grande;
  - the screen staying on through a long live class.

## Risks

- **Mês on a 360 dp tall phone:** the one-row header and the fitted grid must leave room for six weeks; the test checks the grid's bottom is inside the window.
- **`configChanges`:** the activity no longer restarts on turning, so anything read once from the configuration in `onCreate` would go stale. Nothing does today; the source check guards the list.
- **Two panes of detail screens:** they were written as full screens. The `onBack` parameter and the readable width keep them working in half the width, but long names and wide tiles need the fit check.
- **Back in Cadastro:** Back closes the pane before leaving the tab. `BackHandler` order is tested.
