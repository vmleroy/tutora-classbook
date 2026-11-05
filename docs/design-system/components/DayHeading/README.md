# DayHeading

A day's heading in the Agenda's Semana list; it stays on top while that day's classes scroll under it.

## Rules
- The day's number on a square with `radius-sm` corners, like the month's tiles: filled in `btn-primary` for the day being read, outlined for today, `surface-sunken` otherwise. With `month`, the short month sits over the day like a calendar page ("Dez" over 1) and the square grows to `size-touch`.
- The title is the weekday ("Quinta"), or "Hoje"; the aside is the count and expected value, or "Sem aulas".
- On the page's `background`, opaque, so classes scrolling under it don't show through; announced as a heading.

## Props

```kotlin
DayHeading(day: Int, title: String, modifier: Modifier = Modifier, aside: String? = null, today: Boolean = false, current: Boolean = false, month: String? = null)
```
