# Spec 017: Landscape

Status: done
Depends on: 016 Visual refresh (the look, the Agenda's Dia, Semana and Mês), 007 Live timer, 013 Appearance (text and interface sizes), 014 (Ajustes in pages, Ganhos' bars, Notificações)
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

### The panel, Hoje and Ganhos

- **AC-017-6.** Hoje sideways keeps its upright order in one column at a readable width. Tapping a class opens its page in a panel over the right side of the screen, Hoje staying under it (Victor, 5 Nov 2026).
- **AC-017-7.** Ganhos sideways keeps its upright order in one column at a readable width. Tapping someone in "De onde veio" opens their page in the panel.

### Agenda

- **AC-017-8.** Dia sideways shows the day's hours with the week strip above, using the width for the blocks; the first class (or the current hour) is still where it opens.
- **AC-017-9.** Semana sideways shows the list of days (AC-016-31) at a readable width, the strip above it following the scroll.
- **AC-017-10.** Mês sideways shows the month grid at a readable width; tapping a day opens its classes, with their count and value, in the panel, and a class there opens its page in the same panel.

### Cadastro and details

- **AC-017-11.** Cadastro sideways shows its list in one column at a readable width. Tapping a student, group or place opens its page in a panel over the right side, on a dimmed list that stays where it was. Back, ✕ or a tap on the dimmed side closes the panel first; turning the phone upright with a panel open shows that page full screen (Victor, 5 Nov 2026).

### The live class

- **AC-017-12.** The live class sideways gives the clock the whole left side on the main color: the ring as tall as the phone allows, the time inside it, with the status and the way back above. The right side holds who, what was earned so far with its rate, the note, and Pausar and Encerrar at the bottom, all in view without scrolling (Victor, 5 Nov 2026).
- **AC-017-13.** While the live class screen is open, upright or sideways, the phone doesn't dim or lock by itself; leaving the screen gives the phone its own timeout back (Victor, 4 Nov 2026).

### Forms and sheets

- **AC-017-14.** Forms (Nova aula, a student, a place, a payment) keep one column at a readable width, and the field being typed in stays in view above the keyboard.
- **AC-017-15.** Bottom sheets sideways open at a readable width, centered, and scroll when their content is taller than the phone.

### Language

- **AC-017-16.** Every landscape layout reads the same in Português and English; nothing new is written.

## Out of scope

- Tablets and foldables (bigger layouts with more columns).
- A choice in Ajustes to lock the phone upright; the phone's own rotation lock already does it.
- New features on the wider screens.

## Open questions

Answered by Victor on 4 Nov 2026: the screen stays on during a live class; no landscape boards, the plan goes straight from this spec.

Changed by Victor on 5 Nov 2026 after trying it on the emulator: two fixed columns on Hoje, Ganhos, Mês and Cadastro looked strange and cramped. Every screen keeps its upright structure, and the extra width is used only when something is tapped, as a panel over the right side (option B of the "Tutora sideways" board). The live class takes the "big ring on the left" layout (L1), improved.
