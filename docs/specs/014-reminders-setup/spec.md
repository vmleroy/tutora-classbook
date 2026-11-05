# Spec 014: Reminders, confirming classes and first-run setup

Status: in progress
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

- **AC-014-12.** Ajustes › Aulas › "Lembretes" has "Aviso antes da aula": Desligado, 5, 15 (at first), 30 or 60 minutos antes (Victor, 4 Nov 2026). A notification then comes before each class with who, where and the time ("Ana Souza · 14:00 · Online / Remoto"); tapping it opens the class. Cancelled classes and forecast classes get none.
- **AC-014-13.** "Quem te deve" sends once a week, on the day and hour chosen (Segunda, 08:00 at first; on from the start; Victor, 4 Nov 2026), a notification with the total owed and how many ("3 alunos te devem R$ 480,00"); tapping it opens Cadastro on those who owe. With no one owing, nothing is sent. It can be turned off.
- **AC-014-14.** "Aulas para confirmar" sends one notification a day at the hour chosen (20:00 at first) while any class is to confirm ("5 aulas para confirmar"), with "Marcar todas como dadas" and "Ver" as its actions (Victor, 4 Nov 2026); Ver opens the list (AC-014-6), and Marcar todas asks nothing more, since its notification said how many. It can be turned off; with Aulas passadas on "Marcar como dadas sozinhas" it never comes.
- **AC-014-15.** Reminders keep working with Tutora closed and after the phone restarts, and follow every change: a class moved, cancelled or added changes its reminder. Turning a reminder off removes what was planned.
- **AC-014-16.** If the phone hasn't allowed Tutora's notifications, turning a reminder on asks once; refused, the row says "Notificações desligadas no celular" with a way to open the phone's settings.

### First-run setup

- **AC-014-17.** On its very first run, with no data yet, Tutora opens on "Configuração inicial" with four steps and a bar showing which; each step has "Pular" (to the end) and Voltar/Continuar. A teacher who already has data never sees it; Ajustes keeps every choice.
- **AC-014-18.** Step 1, Idioma: Português or English, starting from the phone's; choosing changes the setup's own words at once.
- **AC-014-19.** Step 2, "Como você dá aula?": Aulas particulares, Escola ou emprego fixo, Salinha ou cursinho, any of them. What is chosen decides what the end of the setup suggests next (AC-014-23) and which of Ganhos' filters show, and hides what doesn't apply (Victor, 4 Nov 2026):
  - Without "Escola ou emprego fixo": the Escola e emprego filter in Ganhos, the salary on places and the "Escola" kind when adding a place are hidden.
  - Without "Salinha ou cursinho": the Salinha and Cursinho kinds and "Quem paga: o local" are hidden when adding a place.
  - Without "Aulas particulares": the Particulares filter in Ganhos is hidden.
- **AC-014-20.** Ajustes › Geral has "Como você dá aula", to change these at any time; and anything hidden shows again as soon as the teacher already has a place or class of that kind (a backup restored, a place added before), so nothing they have is ever out of sight.
- **AC-014-21.** Step 3, "Seus preços padrão": Online / Remoto and Casa do Aluno per hour (R$ 100 and R$ 120 at first, with − and +), and Deslocamento (Varia por aula, Fixo or Não cobro, each with its line of explanation). These become the two built-in places' prices.
- **AC-014-22.** Step 4, "Seus dados, seguros": Backup automático with "Escolher pasta" (spec 011) and "Aviso antes da aula" (AC-014-12), each on or off.
- **AC-014-23.** The end shows a summary of the choices and two ways out: "Cadastrar meu primeiro aluno" (opening a new student) and "Ir para Hoje". Skipping keeps the defaults of every step not seen.

### After the device checks (Victor, 5 Nov 2026)

- **AC-014-25.** In the setup's Deslocamento, "Fixo" shows an amount the teacher types, R$ 20,00 at first; it becomes Casa do Aluno's fixed travel (AC-014-21).
- **AC-014-26.** In Lembretes, Quem te deve's day is one row of short day names (Seg Ter Qua Qui Sex Sáb Dom, Mon … Sun in English), and each hour can be typed (AC-014-29).
- **AC-014-27.** The reminder before class comes on time:
  - A flag in the build chooses how. With the flag on (at first), Tutora uses the exact-alarm permission given at install (`USE_EXACT_ALARM`), and the teacher does nothing.
  - With the flag off, Tutora asks for exact alarms (`SCHEDULE_EXACT_ALARM`). Until the teacher allows them, Lembretes shows "Lembrete na hora certa" with "Ativar", which opens the phone's page for it in one tap. Meanwhile reminders still come, up to an hour late.
  - "Quem te deve" and "Aulas para confirmar" stay inexact; a few minutes late doesn't matter for them.
- **AC-014-28.** Ajustes lists only its pages, each with a line of what is set: Seus dados, Backup e planilha, Geral, Aulas, Lembretes, Pagamentos and Sobre.
  - **Geral:** Idioma, Aparência, Calendário, Como você dá aula, Locais padrão and Durações em HH:MM.
  - **Aulas:** Aula ao vivo, Início automático, Aulas passadas, Pedir confirmação and Marcar como dada antes do horário.
  - **Pagamentos:** Créditos e bônus.
  - **Sobre:** the version and the database's.
  - Every setting keeps working as before; only where it lives changes.
- **AC-014-29.** *(Replaced by AC-014-37: fields per kind instead of bare steppers.)* The value of every − / + stepper in the app can be typed: tapping it turns it into a field with the number keyboard. Money is typed like any amount (AC-003), minutes and hours as numbers. A value out of range becomes the nearest allowed one when the field is left, and − and + still work.
- **AC-014-30.** Ganhos' bars follow the period chosen, and never show another period's earnings (this replaces AC-009-12):
  - Dia has no bars. Mês has one bar per week of the month, Trimestre its three months, and Ano its twelve months.
  - The bars scroll sideways when they don't fit. They follow the type filter, with the received part filled in as before.
  - A period with nothing earned shows its empty state, with no bars.
- **AC-014-31.** "Evolução", from Ganhos, shows earned and received month by month for a range: 6 meses (at first), 12 meses, or a chosen year. It follows the type filter, and the bars scroll sideways.
  - Tapping two months compares them: each one's earned and received, and the difference in reais and percent ("Outubro R$ 3.200 · Setembro R$ 2.800 · +R$ 400 (+14%)").

### Third round (Victor, 5 Nov 2026)

- **AC-014-32.** A bell at the top right of Hoje and of the Agenda opens Lembretes. It shows a dot while the phone has Tutora's notifications off (AC-014-16).
- **AC-014-33.** Ganhos' header has two icon buttons, Evolução and Relatório, each with its name for screen readers. They replace the Relatório button and the "Ver evolução" button.
- **AC-014-34.** Evolução lists each month of the range under the chart, newest first:
  - earned, received and to receive;
  - how many classes and hours;
  - the change from the month before, in reais and percent ("+R$ 400 · +14%"; no percent after a month of R$ 0).
- **AC-014-35.** Evolução filters by type (Tudo, Particulares, Escola e emprego), by who (one student, group or place, chosen from a list), and by place kind (Online, Casa do Aluno, Escola, Salinha, Cursinho, Particular). The filters combine. The chart shows earned and received as two bars side by side for each month, with a legend.
- **AC-014-36.** "Aviso antes da aula" has "Personalizado" beside Desligado, 5, 15, 30 and 60. It opens an hh:mm picker for how long before: from 00:05 to 24:00. The chip and the row then read the chosen time ("2h antes", "1h30 antes").
- **AC-014-37.** Every value is entered with the design system's field for its kind, and forms no longer use bare steppers:
  - text and phone: `TextField`;
  - money: `MoneyField`;
  - a whole number or a percent: `NumberField`, typed, with small − and + inside;
  - a length of time: `DurationField`, typed hh:mm, with − and +;
  - a clock time: `TimeField`, with the clock picker;
  - a date: `DateField`, with the calendar.
  - So Lembretes' hours become TimeFields, the setup's prices MoneyFields, and package classes, backups kept, bonus minutes and the commission NumberFields. Class and place durations become DurationFields. This takes the place of AC-014-29.

### Language

- **AC-014-24.** The setup, the confirm list, the marks, the reminders and their notifications follow the app language, with plurals ("1 aula para confirmar", "3 aulas para confirmar").

## Out of scope

- Reminders sent to students (WhatsApp messages and the like).
- A server or push: every reminder is made on the phone.
- Currency in the setup (the mockup's "Moeda"): Tutora is reais only (constitution rule 9).
- Changing a confirmed class back; that's Desfazer on the class (spec 006).

## Open questions

Answered by Victor on 4 Nov 2026: the reminder before class is 5, 15, 30 or 60 minutes (15 at first); "Quem te deve" is weekly, Monday 08:00, on at first; the "to confirm" notification has Ver and Marcar todas; "Como você dá aula" shapes suggestions and filters and also hides what doesn't apply, with a row in Ajustes to change it. The draft spec 018's questions were answered the same day and are in AC-014-1 to 11.
