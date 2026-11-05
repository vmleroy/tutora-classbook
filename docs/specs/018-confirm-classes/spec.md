# Spec 018: Confirm classes

Status: draft
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

- **AC-018-1.** A class is *to confirm* once its end time has passed while it is still scheduled. Done, cancelled, missed and live classes never are, and neither are forecast classes (AC-016-35).

### Seeing them

- **AC-018-2.** In the Agenda (Dia, Semana and Mês' day list) and in Hoje's list, a class to confirm is dimmed and marked "A confirmar", so it reads apart from classes ahead and from classes already confirmed.
- **AC-018-3.** Hoje shows an alert while any class is to confirm: "3 aulas para confirmar". Tapping it opens the list of them (AC-018-5). With none, there is no alert.
- **AC-018-4.** On Mês, a day holding a class to confirm shows it on its tile (a small ring around the day), so a forgotten week stands out.

### Confirming

- **AC-018-5.** The list "Aulas para confirmar" shows every class to confirm, oldest first, each with its day, time, who, where and value, and three actions: Dada, Cancelada and Faltou.
- **AC-018-6.** Dada confirms the class with its planned duration and value, exactly as Marcar como dada does (AC-006-2); its row leaves the list at once. Tapping the class instead opens it, to change the duration first.
- **AC-018-7.** Cancelada and Faltou open the same sheet as on the class (AC-006-9 to 11), with who cancelled, a note and the charge; confirming it removes the row.
- **AC-018-8.** "Confirmar todas como dadas" confirms every class left in the list as Dada with its planned duration, after asking: "Marcar 6 aulas como dadas? R$ 720,00 entram nos ganhos." It can be undone with "Desfazer" for a few seconds.
- **AC-018-9.** Every confirmation counts at once in Hoje's earnings, Ganhos, balances and reports, the same as a class marked done or cancelled by hand, on the class's own day.
- **AC-018-10.** A class can also be confirmed from its own page as today (AC-006-1, 9); it then leaves the list and loses its "A confirmar" mark.

### Language

- **AC-018-11.** The alert, the list, the marks and the messages follow the app language, with plurals ("1 aula para confirmar", "3 aulas para confirmar").

## Out of scope

- Confirming classes from the notification bar or a reminder (spec 014's reminders could link here later).
- Marking classes done without the teacher (see Open questions).
- Changing a confirmed class back; that's Desfazer on the class (spec 006).

## Open questions

For Victor. The assumptions above are marked here so they are easy to change.

- **"Add an option of confirm classes":** assumed this means the confirm list and its actions. Should there also be a setting in Ajustes › Aulas, "Aulas passadas: confirmar uma a uma / marcar como dadas sozinhas", so a teacher who never misses classes isn't asked?
- **When a class becomes "to confirm":** assumed when it ends. Or a little later (one hour after), so a class just finished doesn't nag?
- **Confirmar todas:** assumed it confirms all as Dada after asking, with a short Desfazer. Keep it, or only one by one?
- **Where the alert lives:** assumed only on Hoje. Also a number on the Agenda tab?
