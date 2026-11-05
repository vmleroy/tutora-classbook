# UndoBar

A bar at the bottom of the screen after an action that changed many things at once, with "Desfazer" for a few seconds: "6 aulas marcadas como dadas · Desfazer" after "Confirmar todas como dadas" (spec 014, AC-014-9).

## Consumer provides
The `message` already written for the app language (with its plural), the `actionLabel` ("Desfazer"), `onUndo`, and `onTimeout`, which the screen uses to forget the undo.

## Rules
- Only after an action the teacher already confirmed in a sheet, and only when it changed several items; a single change is undone from its own page.
- `ink` fill with `surface` text, so it reads as the app's one dark bar in every theme and mode; the action in `accent`, at least `touch` (48) tall.
- Corners `radius-xl`, 16 from the screen's sides, above the navigation bar.
- It closes by itself after 6 seconds (`UNDO_MS`). Screen readers hear it once, politely.
- One at a time: a new action replaces the bar and its timer.
