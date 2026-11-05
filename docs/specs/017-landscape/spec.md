# Spec 017: Landscape

Status: draft
Depends on: 016 Visual refresh (the look, the Agenda's Dia, Semana and Mês), 007 Live timer, 013 Appearance (text and interface sizes)
Mockups: none yet. The landscape layouts follow the design system's components; a board per main screen can be added to the canvas before `/plan`.

## Goal

Tutora is built for a phone held upright. When the teacher turns it sideways (on a stand during a class, on the table next to the notebook, or with a keyboard), the screens today just stretch: the tab bar eats a row, cards grow very wide, and the Agenda shows only a few hours. After this spec every screen keeps working sideways and uses the width: tabs move to the side, main screens show two columns where that helps, forms and sheets keep a comfortable width, and nothing typed, chosen or running is lost when the phone turns. Nothing about the data changes.

## User stories

- **US-1.** As a teacher, I prop my phone sideways during a class and see the live timer and its value without turning the phone.
- **US-2.** As a teacher, I turn the phone sideways to see a week or a month next to the day's classes.
- **US-3.** As a teacher, I turn the phone while filling a form and lose nothing I typed.
- **US-4.** As a teacher who uses Large text, I can still read and reach everything sideways.

## Acceptance criteria

### Everywhere

- **AC-017-1.** Every screen of the app opens and works with the phone sideways, in both directions, and turns back without losing its place: the tab, the screen open, the scroll position, an open sheet and everything typed or chosen stay as they were.
- **AC-017-2.** Sideways, the five tabs move to a column on the left side with the same icons, labels and active pill; the content uses the rest of the width.
- **AC-017-3.** Sideways, nothing is cut off or overlaps on a phone 360 dp tall, with Muito grande text and Ampliada interface too (AC-013-10, AC-013-21); every screen scrolls where its content is taller than the phone.
- **AC-017-4.** Text, cards and forms never stretch wider than is comfortable to read: content that would be wider sits centered at a readable width, with the background on both sides.
- **AC-017-5.** The parts of the phone's screen that are not for apps (a camera cut-out, the navigation buttons on the side) never cover anything.

### Hoje and Ganhos

- **AC-017-6.** Hoje sideways shows two columns: the earnings card and the next class on the left, today's classes and the alerts on the right.
- **AC-017-7.** Ganhos sideways shows the period's card on the left, and the six months and "De onde veio" on the right.

### Agenda

- **AC-017-8.** Dia sideways shows the day's hours with the week strip above, using the width for the blocks; the first class (or the current hour) is still where it opens.
- **AC-017-9.** Semana sideways shows the list of days (AC-016-31) at a readable width, the strip above it following the scroll.
- **AC-017-10.** Mês sideways shows the month grid on the left and the chosen day's classes on the right, both in view without scrolling the page.

### Cadastro and details

- **AC-017-11.** Cadastro sideways shows the list on the left and, after tapping a student, group or place, its page on the right; the list stays where it was. Back closes the right side first.

### The live class

- **AC-017-12.** The live class sideways shows the clock and the value on the left and the note and the Pausar and Encerrar buttons on the right, all in view without scrolling.

### Forms and sheets

- **AC-017-13.** Forms (Nova aula, a student, a place, a payment) keep one column at a readable width, and the field being typed in stays in view above the keyboard.
- **AC-017-14.** Bottom sheets sideways open at a readable width, centered, and scroll when their content is taller than the phone.

### Language

- **AC-017-15.** Every landscape layout reads the same in Português and English; nothing new is written.

## Out of scope

- Tablets and foldables (bigger layouts with more columns).
- A choice in Ajustes to lock the phone upright; the phone's own rotation lock already does it.
- New features on the wider screens.

## Open questions

For Victor.

- **Two panes in Cadastro (AC-017-11):** list and detail side by side, as assumed, or just the list wider, opening details full screen as today?
- **Hoje's two columns (AC-017-6):** is the split right (money and next class left, today's classes right)?
- **The live class sideways (AC-017-12):** a common use on a stand. Should the screen also stay on while a class is live?
- **Mockups:** do you want landscape boards on the canvas before `/plan`, or go straight from this spec?
