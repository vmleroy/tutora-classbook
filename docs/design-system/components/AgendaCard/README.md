# AgendaCard

A class in the Agenda's Semana list (Victor, 4 Nov 2026, after Teams' agenda in Tutora's look): start and end times on the left, a bar in the student's or group's tag color, who and what, and the value or the class's state.

## States
Uses `BlockState`: `default`; `next` and `live` outlined in `accent` (`size-border-strong`) with "Próxima" or "Ao vivo" above the title; `done` shows its status badge; `muted` (cancelled, missed) puts the bar in `line-strong` and strikes the title through.

## Rules
- `surface` card, `radius-xl`; times in tabular figures, the start in `bodyStrong`, the end in `caption`.
- The bar is `spacing-1` wide, full height, `radius-full`.
- A status badge replaces the value when the class has one.
- The whole card is one button and reads as one sentence.

## Props

```kotlin
AgendaCard(start: String, end: String, tag: Tag, title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: String? = null, status: Pair<BadgeTone, String>? = null, state: BlockState = BlockState.DEFAULT, onClick: (() -> Unit)? = null)
```
