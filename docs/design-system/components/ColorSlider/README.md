# ColorSlider

Suas cores' Matiz, Intensidade and Brilho (spec 013): a labelled slider whose track shows the range of colors it moves through.

## Rules
- Label and value above the track in `meta`; track `size-progress` tall, `radius-full`, painted with the given colors.
- The thumb is the system slider's, on `surface`, with the full `size-touch` height to drag.
- The hex code is the source of truth: the screen derives each slider from it, so moving one never drifts the others.

## Props

```kotlin
ColorSlider(label: String, value: Int, onChange: (Int) -> Unit, track: List<Color>, modifier: Modifier = Modifier, range: IntRange = 0..100)
```
