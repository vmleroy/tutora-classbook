# Swatch

One color the teacher can pick in Aparência (spec 013), read aloud by its name ("Vinho", "Do tema").

## Kinds
- **COLOR**: a preset, filled with its color.
- **THEME**: "Do tema", half the theme's own color and half the card: back to the theme.
- **ADD**: the + that opens Suas cores on that color.

## Rules
- 36 dp circle (`size-control-sm`) inside a 48 dp press area (`size-touch`).
- Chosen: a `size-border-strong` ring in `ink`; otherwise a hairline in `line`.
- Screens lay them out in a `FlowRow`; the preset colors are data in the domain, never literals in a screen.

## Props

```kotlin
Swatch(color: Color, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, kind: SwatchKind = SwatchKind.COLOR)
```
