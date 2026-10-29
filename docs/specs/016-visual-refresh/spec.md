# Spec 016: Visual refresh 2026 and the month calendar

Status: done
Depends on: 002 Design system, 005 Schedule, 010 Reports
Mockups: docs/mockups/DirA.dc.html (Hoje), A_Agenda.dc.html, A_AgendaMes.dc.html, A_AgendaAno.dc.html, A_Aluno.dc.html, A_Ganhos.dc.html, A_Ajustes.dc.html, A_Estilo.dc.html

## Goal

Tutora gets the 2026 look Victor chose on the canvas ("A · Quadro refinado"): the same classroom colors, in current Android style. Hoje keeps its green band with a white earnings card over it and the Agenda opens with a white card holding the week, as Victor preferred after trying the first version; elsewhere titles sit on the page, the main numbers live in one colored card, lists are grouped cards, paired actions are joined buttons and corners are larger. Nothing the teacher can do changes, and every theme, dark mode and Suas cores still repaint the whole app, because the new look uses only theme colors. The Agenda also gets easier to read in time: its title is the month and year, and a new Mês view shows the whole month with the classes of each day, with a quick way to jump to any month and year.

## User stories

- **US-1.** As a teacher, I open Tutora and it looks current and calm, with the money I care about in one clear card on each screen.
- **US-2.** As a teacher who picked another theme or my own colors, I see the new look painted in my colors, with readable text.
- **US-3.** As a teacher, I always see which month and year the Agenda is showing, and can go back to today in one tap.
- **US-4.** As a teacher, I see a whole month at once, which days have classes and of what kind, and jump to any month to plan ahead or look back.

## Acceptance criteria

### The new look, everywhere

- **AC-016-1.** No tab screen has a colored band at the top except Hoje: there the date, the greeting and the + sit on the Topo band, which runs under the top of the white earnings card (Victor, 28 Oct 2026). Every other screen's title sits on the background, with its actions beside it.
- **AC-016-2.** Hoje's and Ganhos' earnings sit on a white card: the amount, its bar, and the figures under it in equal columns (Semana, the month and Alunos devem on Hoje; Recebido and A receber on Ganhos), owed in red (Victor, 28 and 29 Oct 2026). A student, group or place opens as on its mockup (StudentDetail, PlaceDetail): the picture and name, then three tiles (what is owed, this month's classes, the rate), the main action on its own line (Registrar pagamento, Receber da salinha) and Relatório and Nova aula under it, with Editar in the header (Victor, 29 Oct 2026). The Topo color paints Hoje's band and the live lesson.
- **AC-016-3.** What is owed sits in red (a red tile or red text) and what is paid or settled in green, the same in every theme.
- **AC-016-4.** Lists of classes, students, places and settings are grouped cards with thin dividers between rows; a section title sits above each group.
- **AC-016-5.** On Hoje's next class, Marcar como dada (outlined, with a check) sits beside Iniciar aula (in the Botão de destaque color), the same size (Victor, 28 Oct 2026). Registrar pagamento sits on the balance card in the Botão de destaque color; Relatório stays a round button in the header.
- **AC-016-6.** The Nova aula button is in the Botão de destaque color: round on Hoje's band, and on the Agenda a small rounded square (48 dp) floating in the bottom-right corner, its fill a little see-through so the classes behind it show (Victor, 28 and 29 Oct 2026).
- **AC-016-7.** The tab bar sits on the sunken card tone; the active tab shows a pill in the Menu ativo color behind its icon, and its label in bold.
- **AC-016-8.** Back, Editar and Relatório on detail screens are round icon buttons; every tappable thing is at least 48 dp.
- **AC-016-9.** The look uses only theme colors: in each of the 4 themes, light and dark, and with any accepted Suas cores choice, every text on its ground reaches 4.5:1 (3:1 for big money figures).
- **AC-016-10.** Every Suas cores part still paints something visible: Topo Hoje's band and the live lesson, Botões principais the main buttons, chosen chips and segments and switches, Botão de destaque the + and progress bars, Números de ganhos the money on cards, Fundo the page, Cartões the cards, and Menu ativo the tab pill.
- **AC-016-11.** Every screen keeps all its features, copy and order of information; only shapes, spacing and where colors sit change. With Texto grande nothing is clipped at 360 dp wide.
- **AC-016-12.** The component gallery shows the new and changed pieces (summary card, earnings card, grouped list, tab bar, icon buttons, month grid, month picker) in their states.

### Agenda: month and year

- **AC-016-13.** The Agenda opens with a white card at the top: "Agenda" with the month and year of the day being shown under it ("Outubro 2026" / "October 2026"), updated as the teacher moves between weeks or months (Victor, 28 Oct 2026).
- **AC-016-14.** A Hoje button next to the title brings the Agenda back to today in the current view.
- **AC-016-15.** The view switch reads Dia, Semana and Mês. Dia and Semana work as before (spec 005), with the week strip under a "Semana de 26 out a 1 nov" line and ‹ › to move a week; each day of the strip sits right over its column of the grid, the days are filled pills, and in Semana a line separates the columns and the chosen day's is lighter (Victor, 29 Oct 2026).

### Agenda: the month view

- **AC-016-16.** Mês shows the month as a grid of weeks from Monday to Sunday, with the days of the next and previous months greyed out; tapping one of them opens its month on that day, and ‹ › move a month (Victor, 29 Oct 2026). Days of the month are filled squares with room between them, so each reads on its own on any phone (Victor, 29 Oct 2026).
- **AC-016-17.** Each day shows one dot per class that exists for it, up to two, colored by kind (school, done, private, group), with a legend under the grid; a day with more than two classes shows "+N" (Victor, 28 Oct 2026). A cancelled class has no dot (Victor, 28 Oct 2026).
- **AC-016-18.** Today is filled in the Botões principais color; the chosen day is tinted. Tapping a day chooses it and lists its classes under the grid with their count and expected value ("2 aulas · R$ 300"); tapping a class opens it.
- **AC-016-19.** Every day can be read aloud as "28 de outubro, 2 aulas".

### Agenda: jump to a month

- **AC-016-20.** Tapping the month and year opens a sheet with the year and ‹ › to change it, the twelve months with the number of classes in each, the shown month highlighted, months past the 8-week limit disabled, and "Voltar para hoje".
- **AC-016-21.** Choosing a month there opens Mês on it.
- **AC-016-22.** Month names and the week's first day follow the app language; switching it never moves a class.
- **AC-016-23.** The Agenda looks ahead only as far as the schedule is filled, 8 weeks from today (AC-005-8): › stops at the month that holds that day, and later months cannot be chosen. Past months stay reachable. Days in the shown month after that day carry no dots (Victor, 28 Oct 2026).

## Out of scope

- New features on any screen, and changes to the theme colors themselves, the four themes, or how Suas cores derives its colors (002).
- The Aparência screens (013), the app lock (015) and the PDF layout (done in 010).
- A year grid with every day of the year.

## Decisions

- Later months: the Agenda stops at the 8-week horizon; no projected classes (Victor, 28 Oct 2026).
- Cancelled classes: no dot on the month (Victor, 28 Oct 2026).
- Order: 016 is built right after 010 merges, before 011–015 (Victor, 28 Oct 2026).

## Open questions

None.
