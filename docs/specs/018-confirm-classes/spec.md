# Spec 018: Confirm classes

Status: ready
Depends on: 005 Schedule (series filled ahead), 006 Lessons (mark done, cancel, missed), 008 Payments (what a done class owes), 016 Visual refresh (Hoje, the Agenda's views, AgendaCard)
Mockups: none yet. The pieces come from the design system: AlertBanner on Hoje, AgendaCard and AgendaBlock with a new "a confirmar" state, a BottomSheet with the list.

## Goal

Repeating classes are created ahead of time, so a class that already happened stays "scheduled" until the teacher marks it. When the app isn't opened for a while (a busy week, a trip), these pile up silently: they count for nothing in earnings or balances, and nothing tells the teacher they're there. After this spec a past class that is still scheduled is clearly a class *to confirm*: dimmed in the Agenda and Hoje, counted in an alert on Hoje, and confirmed in a few taps, one by one or all at once, as Dada, Cancelada or Faltou. Nothing is decided for the teacher without them seeing it, and nothing about money changes until a class is confirmed.

## User stories

- **US-1.** As a teacher who didn't open Tutora for two weeks, I see "6 aulas para confirmar" and clear them in one go.
- **US-2.** As a teacher, I tell at a glance which of yesterday's classes I still haven't marked.
- **US-3.** As a teacher whose student missed one of those classes, I mark that one Faltou and the rest Dada.

## Acceptance criteria

### Which classes

- **AC-018-1.** A class is *to confirm* once it has ended while still scheduled. When that starts is the teacher's choice in Ajustes › Aulas › "Pedir confirmação": "Quando a aula termina" (at first), "Uma hora depois" or "No dia seguinte" (Victor, 4 Nov 2026). Done, cancelled, missed and live classes never are, and neither are forecast classes (AC-016-35).
- **AC-018-2.** Ajustes › Aulas › "Aulas passadas" is "Confirmar uma a uma" (at first) or "Marcar como dadas sozinhas". With the second, a class becomes Dada with its planned duration at the moment it would be to confirm, and shows "Dada sozinha" on its page and in the Agenda, so it can still be changed or cancelled (Victor, 4 Nov 2026).

### Seeing them

- **AC-018-3.** In the Agenda (Dia, Semana and Mês' day list) and in Hoje's list, a class to confirm is dimmed and marked "A confirmar", so it reads apart from classes ahead and from classes already confirmed.
- **AC-018-4.** Hoje shows an alert while any class is to confirm: "3 aulas para confirmar". Tapping it opens the list of them. With none, there is no alert. The Agenda tab shows the same number on its icon (Victor, 4 Nov 2026).
- **AC-018-5.** On Mês, a day holding a class to confirm shows it on its tile (a small ring around the day), so a forgotten week stands out.

### Confirming

- **AC-018-6.** The list "Aulas para confirmar" shows every class to confirm, oldest first, each with its day, time, who, where and value, and three actions: Dada, Cancelada and Faltou.
- **AC-018-7.** Dada confirms the class with its planned duration and value, exactly as Marcar como dada does (AC-006-2); its row leaves the list at once. Tapping the class instead opens it, to change the duration first.
- **AC-018-8.** Cancelada and Faltou open the same sheet as on the class (AC-006-9 to 11), with who cancelled, a note and the charge; confirming it removes the row.
- **AC-018-9.** "Confirmar todas como dadas" confirms every class left in the list as Dada with its planned duration, after asking: "Marcar 6 aulas como dadas? R$ 720,00 entram nos ganhos." It can be undone with "Desfazer" for a few seconds (Victor, 4 Nov 2026).
- **AC-018-10.** Every confirmation counts at once in Hoje's earnings, Ganhos, balances and reports, the same as a class marked done or cancelled by hand, on the class's own day.
- **AC-018-11.** A class can also be confirmed from its own page as today (AC-006-1, 9); it then leaves the list and loses its "A confirmar" mark.

### Language

- **AC-018-12.** The alert, the list, the marks and the messages follow the app language, with plurals ("1 aula para confirmar", "3 aulas para confirmar").

## Out of scope

- Confirming classes from the notification bar or a reminder (spec 014's reminders could link here later).
- Changing a confirmed class back; that's Desfazer on the class (spec 006).

## Open questions

Answered by Victor on 4 Nov 2026: Ajustes has "Aulas passadas" (confirm one by one, or mark done on their own); when a class asks for confirmation is a choice of three; "Confirmar todas como dadas" with a question and Desfazer; the count shows on Hoje and on the Agenda tab.
