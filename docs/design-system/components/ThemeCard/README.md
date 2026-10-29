# ThemeCard

A theme in Aparência's Tema list (spec 013): its name and two dots, the theme's main and highlight colors in the current mode. Themes of the teacher's own use it too.

## Rules
- `surface` card, `radius-lg`, padding `spacing-3`.
- Chosen: `size-border-strong` outline in `btn-primary`; otherwise a hairline in `line`.
- Never draws a full preview: two dots keep the list light.

## Props

```kotlin
ThemeCard(name: String, main: Color, highlight: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)
```
