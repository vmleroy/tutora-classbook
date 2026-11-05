# Tasks 017: Landscape

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Design system: `TutoraTheme.landscape`; tokens `size-content-max`, `size-pane-min`, `size-rail`; `NavRail`, `TwoPane`, `Modifier.readableWidth()`; `BottomSheet` maximum width; `MonthGrid` fitting its height; guidelines and gallery (45 components). Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt`. (AC-017-2, AC-017-4, AC-017-15)
- [x] 2. Shell: `configChanges` in the manifest; `NavRail` in landscape in `TutoraNav`; `ScreenScaffold` at the readable width; `Harness.landscape` and `assertFitsSideways()`. Test: `app/src/test/kotlin/app/tutora/LandscapeTest.kt`, `SourceChecksTest.kt`. (AC-017-1, AC-017-2, AC-017-3, AC-017-5)
- [x] 3. Hoje and Ganhos in two panes. Test: `LandscapeTest.kt`. (AC-017-6, AC-017-7)
- [x] 4. Agenda: the one-row header, Dia, Semana at the readable width, and Mês in two panes with the fitted grid. Test: `LandscapeTest.kt`. (AC-017-8, AC-017-9, AC-017-10)
- [x] 5. Cadastro with the list and the page side by side, `onBack` on the detail screens, and Back closing the pane first. Test: `LandscapeTest.kt`. (AC-017-11)
- [x] 6. The live class in two panes; the screen kept on while it is open. Test: `LandscapeTest.kt`, `LiveScreensTest.kt`. (AC-017-12, AC-017-13)
- [x] 7. Forms and sheets at the readable width, the focused field in view; the same layouts in English. Test: `LandscapeTest.kt`. (AC-017-14, AC-017-16)
- [x] 8. `./gradlew check`, roadmap status, device checks pending in the PR.

## Notes from implementing

- `size-pane-min` is 260 dp, not the plan's 300: at 300 Cadastro's 0.4 pane would not fit beside the rail on an 800 dp phone. Below it, two scrolling panes stack into one column.
- Sideways the Agenda's week strip has touch-high tiles instead of squares, and its ‹ › sit beside it (the week's dates are already in the title), so Dia keeps room for the hours.
- `TimerRing` shrinks (staying round) when its parent has less room, so the live class fits 360 dp without scrolling.
- After the first emulator look (5 Nov 2026) the Agenda header was still over half the height sideways: the week strip now carries the weekday inside each tile, and the title row slides away as the hours or days scroll up and comes back on scrolling down, the strip staying pinned.
- Cadastro's list is movable content: it keeps its search and chips when it moves into the left pane on turning.

## Pending on a device

- Turning a real phone both ways on every screen, with a sheet open and a form half typed.
- A phone with a camera cut-out, and one with the navigation buttons on the side.
- The keyboard over a field sideways.
- A 360 dp tall phone at Muito grande text and Ampliada interface.
- The screen staying on through a long live class, and the phone's own timeout back after it.
