# Tasks 002: Design system

- [x] 1. `scripts/tokens.ts`: read `src/ui/theme/tokens/tokens.json`, write `src/ui/theme/tokens/tokens.ts`; CI check for drift. (AC-002-1)
- [x] 2. Contrast test over all 8 token sets. Test: `src/ui/theme/contrast.test.ts`. (AC-002-2)
- [x] 3. `tailwind.config.js` colors, spacing, radius, font sizes; NativeWind rem 16. Test: `src/ui/theme/tailwind.test.tsx`. (AC-002-3, AC-002-4)
- [x] 4. `resolvePalette` with custom parts, dark-mode lightening, on-color flipping; `ThemeProvider` with `vars()`. Tests: `src/domain/theme/palette.test.ts`, `src/ui/theme/ThemeProvider.test.tsx`. (AC-002-5 to AC-002-9)
- [x] 5. Foundations and actions: Icon, Button, IconButton, Fab. (AC-002-10 to AC-002-12)
- [x] 6. Selection and inputs: SegmentedControl, Chip, Switch, Checkbox, Stepper, TextField, SelectField.
- [x] 7. Display: Badge, Avatar, TagDot, Money, ProgressBar, Card, EarningsSummary. Money locale tests. (AC-002-13)
- [x] 8. Lists and navigation: SectionHeader, LessonRow, ListGroup, ListRow, AlertBanner, ScreenHeader, TabBar, WeekStrip, AgendaBlock.
- [x] 9. Feedback: BottomSheet, EmptyState, TimerRing.
- [x] 10. Lint rule against raw colors, font sizes and spacing. (AC-002-14)
- [x] 11. Developer component screen with theme and mode switcher; Maestro screenshot flow with Large text. (AC-002-15, AC-002-16)
- [x] 12. Reduce-motion handling in animated components. (AC-002-17)

## Criteria proven on a device

- **AC-002-16:** `.maestro/ds-large-text.yaml` screenshots all 30 components with Large text at 360 dp, in light and dark (60 screenshots). It ran on an API 36 emulator against a release build made with `EXPO_PUBLIC_DEV_GALLERY=1` on 26 Oct 2026. Nothing clips or overlaps. The run first caught a sheet's actions stacking and then "Cancelar" wrapping mid-word; both were fixed and checked again.
- **AC-002-5 and AC-002-8b on screen:** the same run shows dark mode switching in place and the hero header on the deep `header` shade.
