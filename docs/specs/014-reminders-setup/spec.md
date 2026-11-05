# Spec 014: Reminders, confirming classes and first-run setup

Status: draft
Depends on: 005 Schedule (series filled ahead), 006 Lessons (mark done, cancel, missed), 007 Live timer (notifications), 008 Payments (who owes), 011 Backup (the folder), 016 Visual refresh (Hoje, the Agenda, AgendaCard)
Mockups: docs/mockups/Setup.dc.html (first-run setup), docs/mockups/Settings.dc.html. The confirm list and the reminders follow the design system.
Takes in: the draft spec 018 (Confirm classes), whose criteria and answers are here (Victor, 4 Nov 2026).

## Goal

Three things that keep Tutora in step with the teacher's real week. **Reminders**: a notification before each class, a weekly "who still owes you", and one when classes are waiting to be confirmed, all made on the phone with no server. **Confirming classes**: repeating classes are created ahead, so a class that happened stays "scheduled" until the teacher marks it; after this spec those are clearly *to confirm*, counted on Hoje and the Agenda tab, and cleared in a few taps as Dada, Cancelada or Faltou, one by one or all at once (or marked done on their own, if the teacher prefers). **First-run setup**: a teacher opening Tutora for the first time sets language, how they teach, their default prices, backup and reminders in four skippable steps, instead of finding each in Ajustes. Nothing about money changes until a class is confirmed.

## User stories

- **US-1.** As a teacher, I get a notification 15 minutes before each class, with who and where.
- **US-2.** As a teacher, on Monday morning I'm told who still owes me, and tap to see them.
- **US-3.** As a teacher who didn't open Tutora for two weeks, I see "6 aulas para confirmar" and clear them in one go.
- **US-4.** As a teacher whose student missed one of those classes, I mark that one Faltou and the rest Dada.
- **US-5.** As a new teacher, I set Tutora up in a minute, then add my first student.

## Acceptance criteria

### Confirming classes: which ones

- **AC-014-1.** A class is *to confirm* once it has ended while still scheduled. When that starts is the teacher's choice in Ajustes › Aulas › "Pedir confirmação": "Quando a aula termina" (at first), "Uma hora depois" or "No dia seguinte" (Victor, 4 Nov 2026). Done, cancelled, missed and live classes never are, and neither are forecast classes (AC-016-35).
- **AC-014-2.** Ajustes › Aulas › "Aulas passadas" is "Confirmar uma a uma" (at first) or "Marcar como dadas sozinhas". With the second, a class becomes Dada with its planned duration at the moment it would be to confirm, and shows "Dada sozinha" on its page and in the Agenda, so it can still be changed or cancelled (Victor, 4 Nov 2026).

### Confirming classes: seeing them

- **AC-014-3.** In the Agenda (Dia, Semana and Mês' day list) and in Hoje's list, a class to confirm is dimmed and marked "A confirmar", so it reads apart from classes ahead and from classes already confirmed.
- **AC-014-4.** Hoje shows an alert while any class is to confirm: "3 aulas para confirmar". Tapping it opens the list of them. With none, there is no alert. The Agenda tab shows the same number on its icon (Victor, 4 Nov 2026).
- **AC-014-5.** On Mês, a day holding a class to confirm shows it on its tile (a small ring around the day), so a forgotten week stands out.

### Confirming classes: confirming

- **AC-014-6.** The list "Aulas para confirmar" shows every class to confirm, oldest first, each with its day, time, who, where and value, and three actions: Dada, Cancelada and Faltou.
- **AC-014-7.** Dada confirms the class with its planned duration and value, exactly as Marcar como dada does (AC-006-2); its row leaves the list at once. Tapping the class instead opens it, to change the duration first.
- **AC-014-8.** Cancelada and Faltou open the same sheet as on the class (AC-006-9 to 11), with who cancelled, a note and the charge; confirming it removes the row.
- **AC-014-9.** "Confirmar todas como dadas" confirms every class left in the list as Dada with its planned duration, after asking: "Marcar 6 aulas como dadas? R$ 720,00 entram nos ganhos." It can be undone with "Desfazer" for a few seconds (Victor, 4 Nov 2026).
- **AC-014-10.** Every confirmation counts at once in Hoje's earnings, Ganhos, balances and reports, the same as a class marked done or cancelled by hand, on the class's own day.
- **AC-014-11.** A class can also be confirmed from its own page as today (AC-006-1, 9); it then leaves the list and loses its "A confirmar" mark.

### Reminders

- **AC-014-12.** Ajustes › Aulas › "Lembretes" has "Aviso antes da aula": Desligado, 5, 15 (at first), 30 or 60 minutos antes. A notification then comes before each class with who, where and the time ("Ana Souza · 14:00 · Online / Remoto"); tapping it opens the class. Cancelled classes and forecast classes get none.
- **AC-014-13.** "Quem te deve" sends once a week, on the day and hour chosen (Segunda, 08:00 at first), a notification with the total owed and how many ("3 alunos te devem R$ 480,00"); tapping it opens Cadastro on those who owe. With no one owing, nothing is sent. It can be turned off.
- **AC-014-14.** "Aulas para confirmar" sends one notification a day at the hour chosen (20:00 at first) while any class is to confirm ("5 aulas para confirmar"), with "Marcar todas como dadas" and "Ver" as its actions; Ver opens the list (AC-014-6), and Marcar todas asks nothing more, since its notification said how many. It can be turned off; with Aulas passadas on "Marcar como dadas sozinhas" it never comes.
- **AC-014-15.** Reminders keep working with Tutora closed and after the phone restarts, and follow every change: a class moved, cancelled or added changes its reminder. Turning a reminder off removes what was planned.
- **AC-014-16.** If the phone hasn't allowed Tutora's notifications, turning a reminder on asks once; refused, the row says "Notificações desligadas no celular" with a way to open the phone's settings.

### First-run setup

- **AC-014-17.** On its very first run, with no data yet, Tutora opens on "Configuração inicial" with four steps and a bar showing which; each step has "Pular" (to the end) and Voltar/Continuar. A teacher who already has data never sees it; Ajustes keeps every choice.
- **AC-014-18.** Step 1, Idioma: Português or English, starting from the phone's; choosing changes the setup's own words at once.
- **AC-014-19.** Step 2, "Como você dá aula?": Aulas particulares, Escola ou emprego fixo, Salinha ou cursinho, any of them. What is chosen decides what the end of the setup suggests next (AC-014-22) and which of Ganhos' filters show at first.
- **AC-014-20.** Step 3, "Seus preços padrão": Online / Remoto and Casa do Aluno per hour (R$ 100 and R$ 120 at first, with − and +), and Deslocamento (Varia por aula, Fixo or Não cobro, each with its line of explanation). These become the two built-in places' prices.
- **AC-014-21.** Step 4, "Seus dados, seguros": Backup automático with "Escolher pasta" (spec 011) and "Aviso antes da aula" (AC-014-12), each on or off.
- **AC-014-22.** The end shows a summary of the choices and two ways out: "Cadastrar meu primeiro aluno" (opening a new student) and "Ir para Hoje". Skipping keeps the defaults of every step not seen.

### Language

- **AC-014-23.** The setup, the confirm list, the marks, the reminders and their notifications follow the app language, with plurals ("1 aula para confirmar", "3 aulas para confirmar").

## Out of scope

- Reminders sent to students (WhatsApp messages and the like).
- A server or push: every reminder is made on the phone.
- Currency in the setup (the mockup's "Moeda"): Tutora is reais only (constitution rule 9).
- Changing a confirmed class back; that's Desfazer on the class (spec 006).

## Open questions

For Victor. The assumptions above are marked here so they are easy to change.

- **Aviso antes da aula:** assumed choices of 5, 15, 30 and 60 minutes, 15 at first, and on at first only if the setup says so. Right?
- **Quem te deve:** assumed weekly, Segunda 08:00, on at first. Or monthly, or off at first?
- **Aulas para confirmar notification:** assumed daily at 20:00 while any are waiting, with "Marcar todas como dadas" right from the notification. Keep that action in the notification, or only "Ver"?
- **Como você dá aula:** assumed it only shapes suggestions and Ganhos' filters at first. Should it also hide parts of the app (for example, no school features for a teacher who only gives private classes)?
