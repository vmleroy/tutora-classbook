# Tasks 017: Landscape

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Design system: `TutoraTheme.landscape`; tokens `size-content-max`, `size-rail`; `NavRail`, `SidePanel`, `Modifier.readableWidth()`; `BottomSheet` maximum width; `MonthGrid` and `WeekStrip` rows sideways; `TimerRing` fill; guidelines and gallery (45 components). Test: `core/designsystem/src/test/kotlin/app/tutora/designsystem/DesignSystemTest.kt`. (AC-017-2, AC-017-4, AC-017-15)
- [x] 2. Shell: `configChanges` in the manifest; `NavRail` in landscape in `TutoraNav`; `ScreenScaffold` at the readable width; `Harness.landscape` and `assertFitsSideways()`. Test: `app/src/test/kotlin/app/tutora/LandscapeTest.kt`, `SourceChecksTest.kt`. (AC-017-1, AC-017-2, AC-017-3, AC-017-5)
- [x] 3. Hoje and Ganhos at the readable width, rows opening in the side panel (`Panel.kt`). Test: `LandscapeTest.kt`. (AC-017-6, AC-017-7)
- [x] 4. Agenda: the one-row header, Dia, Semana at the readable width, and Mês at the readable width, a day opening in the panel. Test: `LandscapeTest.kt`. (AC-017-8, AC-017-9, AC-017-10)
- [x] 5. Cadastro's rows opening in the panel, `onBack` on the detail screens, Back closing the panel first, upright showing the page full screen. Test: `LandscapeTest.kt`. (AC-017-11)
- [x] 6. The live class sideways in layout L1 (`LiveParts`, `LiveUpright`, `LiveSideways`); the screen kept on while it is open. Test: `LandscapeTest.kt`, `LiveScreensTest.kt`. (AC-017-12, AC-017-13)
- [x] 7. Forms and sheets at the readable width, the focused field in view; the same layouts in English. Test: `LandscapeTest.kt`. (AC-017-14, AC-017-16)
- [x] 8. `./gradlew check`, roadmap status, device checks pending in the PR.

## Notes from implementing

- Revised on 5 Nov 2026 after Victor's emulator look: the first version split Hoje, Ganhos, Mês, Cadastro and the live class into fixed columns, which looked cramped. Now every screen keeps its upright structure and taps open a side panel (option B); the live class is L1, improved. `TwoPane`, `size-pane-min` and `MonthGrid`'s height fitting were removed.
- Sideways the Agenda's week strip carries the weekday inside each tile and its ‹ › sit beside it; the title row slides away as the hours or days scroll up and comes back on scrolling down.
- `ScreenScaffold`'s title lines up with its body at the readable width.

## Pending on a device

- Turning a real phone both ways on every screen, with a sheet open and a form half typed.
- A phone with a camera cut-out, and one with the navigation buttons on the side.
- The keyboard over a field sideways.
- A 360 dp tall phone at Muito grande text and Ampliada interface.
- The screen staying on through a long live class, and the phone's own timeout back after it.
