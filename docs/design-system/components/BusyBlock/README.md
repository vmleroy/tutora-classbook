# BusyBlock

Another appointment from the phone's calendars, shown on the Agenda's Dia and Semana as busy time (spec 012). Tutora never opens or edits it.

## Variants
- **Block**: a timed appointment, placed by the screen like an `AgendaBlock`: top from its start, height from its duration. Title, then its time.
- **Band** (`band = true`): an all-day appointment, a thin bar at the top of its day with the title only.

## Rules
- Fill `surface-sunken`, text `ink-muted` in `caption`, so it reads as background, never as a class.
- Drawn behind the classes; a class over busy time stays fully readable.
- Not pressable and not focusable as a button: screen readers read the title and time once.
- Corners `radius-xl` for blocks, `radius-sm` for bands.
- Never shown for Tutora's own events (they are the classes) or on Mês.

## Props

```kotlin
BusyBlock(title: String, modifier: Modifier = Modifier, time: String? = null, band: Boolean = false, height: Dp = Size.agendaMin)
```
