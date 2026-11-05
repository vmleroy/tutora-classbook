# DayHeading

A day's heading in the Agenda's Semana list; it stays on top while that day's classes scroll under it.

## Rules
- The day's number on a `size-tile` square with `radius-sm` corners, like the month's tiles: `btn-primary` for today, `surface-sunken` otherwise.
- The title is the weekday ("Quinta"), or "Hoje"; the aside is the count and expected value, or "Sem aulas".
- On the page's `background`, opaque, so classes scrolling under it don't show through; announced as a heading.

## Props

```kotlin
DayHeading(day: Int, title: String, modifier: Modifier = Modifier, aside: String? = null, today: Boolean = false)
```
