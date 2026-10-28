# Spec 006: Lessons

Status: in progress
Depends on: 005 Schedule (and 004 for prices and who pays)
Mockups: docs/mockups/AddLesson.dc.html (Registrar mode), LessonDetail.dc.html (status line, Quem paga, Marcar como dada), EditLesson.dc.html (Situação and the fee switch), Fees.dc.html

## Goal

A class on the schedule can now end the way it really did: given, cancelled or missed. The teacher marks a class as done with its planned minutes or the real ones, logs a lesson that already happened without ever having scheduled it, and changes a class's status in its edit form. When a student cancels late or doesn't show up, Tutora suggests the fee from the teacher's rule and the teacher keeps it or waives it. A class that is done keeps its value from then on, so price changes no longer touch it. This is what payments (008) and earnings (009) will count.

## User stories

- **US-1.** As a teacher, I finish Ana's class and mark it done in two taps, adjusting the minutes when it ran long.
- **US-2.** As a teacher, I log yesterday's class with Pedro that I never put on the schedule, with its real duration, and see what it was worth as I type.
- **US-3.** As a teacher, I mark a class as cancelled or missed, and Tutora tells me whether my rule charges a fee and how much, so I can keep it or let it go.
- **US-4.** As a teacher, I set my cancellation rule once, and change it for the few students or places that are different.

## Acceptance criteria

### Marking a class as done

- **AC-006-1.** On the detail of a scheduled class whose start time has passed, Marcar como dada opens a sheet with the real duration, starting at the planned duration and changed with − / +, the value updating as it changes, and Confirmar with that value ("Confirmar · R$ 120,00").
- **AC-006-2.** Confirming makes the class Dada with that duration. Its value is worked out as in spec 005 (per hour: price × minutes ÷ 60; per lesson: the price; a value typed by hand stays) plus its travel fee.
- **AC-006-3.** A class that is done keeps its value: changing the student's or place's price afterwards does not change it.
- **AC-006-4.** Marking a class of a series as done changes only that class, and filling the series later never overwrites it.

### Logging a lesson given

- **AC-006-5.** Nova aula has two modes at the top, Registrar aula dada and Agendar aula. The + still opens Agendar (AC-005-1).
- **AC-006-6.** Registrar asks for the student or group and place (as in AC-005-2), the date (today or earlier, never later), the start time and the real duration ("Duração real"). Travel is asked or added as in AC-005-5. Repetir is not shown.
- **AC-006-7.** The value is shown as it is typed, with how it was worked out ("90 min × R$ 120,00/h"), and can be typed over by hand. The save button reads "Registrar aula · R$ 180,00".
- **AC-006-8.** Saving creates a class that is already Dada, on its date, with that value. It appears on Hoje and Agenda on that day.

### Status

- **AC-006-9.** The edit form has Situação: Agendada, Dada, Cancelada and Faltou, with a line under it that says what the choice means (for example "Entra no ganho e no saldo da aluna").
- **AC-006-10.** A status change always applies to that class only, even when "Esta e as próximas" is chosen for the other changes.
- **AC-006-11.** Dada and Faltou can only be picked for a class whose start time has passed; Cancelada can be picked at any time.
- **AC-006-12.** Changing a done class's duration recomputes its value from the price it had when it was marked done; a value typed by hand stays as typed.
- **AC-006-13.** Moving a class from Dada back to Agendada lets its value follow the current price again (AC-005-16), unless it was typed by hand.
- **AC-006-14.** The lesson detail shows the status at the top ("Agendada · em 25 min", "Dada", "Cancelada", "Faltou"), with the fee when one is kept ("Cancelada · taxa R$ 60,00"), and who pays (the student or the place).

### On Hoje and Agenda

- **AC-006-15.** A done class shows as done (a check and "Dada"). A cancelled or missed class stays on its day, muted, labelled Cancelada or Faltou, with its fee when one is kept.
- **AC-006-16.** The day's "Previsto" counts scheduled and done classes at their value plus kept fees. A cancelled or missed class without a fee counts nothing.
- **AC-006-17.** "Próxima aula" never shows a class that is cancelled, missed or done.

### Cancellation and no-show rule

- **AC-006-18.** Ajustes › Aulas e cobrança has Cancelamentos e faltas, which opens the rule: "Cancelou em cima da hora" with on/off, the notice window (2h, 12h, 24h or 48h) and what to charge (50%, 100% or a fixed amount typed by the teacher); "Faltou sem avisar" with on/off and the same three choices; an example worked out live for a R$ 120 class at 14:00; and Salvar regra.
- **AC-006-19.** For a teacher who has never set it, both parts are off, so no fee is ever suggested. Turning a part on starts at 24h and 50% for late cancellations and at 100% for no-shows.
- **AC-006-20.** The student and place edit forms have "Cancelamentos e faltas" with Padrão (shows what the default rule does), Própria (the same controls as Ajustes, for this student or place only) and Não cobrar.
- **AC-006-21.** The rule for a class is the student's own choice; when the student follows Padrão, the place's choice; when the place also follows Padrão, the default in Ajustes. A group's class uses its place's choice, then the default.

### Fees on a class

- **AC-006-22.** Picking Cancelada shows "Avisou em" with a date and time, starting at now. When the notice came less than the window before the start (or after it), the class is a late cancellation.
- **AC-006-23.** For a late cancellation, or for Faltou, when the rule for the class charges, the form shows a switch "Cobrar cancelamento" or "Cobrar a falta", on, with the fee and why ("R$ 60,00 · 50% · menos de 24h"). Turning it off reads "Dispensado desta vez: não cobra nada".
- **AC-006-24.** A percentage fee is that share of the class's value without travel, rounded to the centavo once. A fixed fee is the amount in the rule.
- **AC-006-25.** A cancellation with enough notice, or a rule that doesn't charge, shows no switch, only a line saying why ("Avisou com antecedência: sem taxa", "Não cobra cancelamentos").
- **AC-006-26.** A class without a price and a percentage rule suggests no fee and says so ("Aula sem preço: sem taxa").
- **AC-006-27.** Saving keeps the fee or the waiver on the class. A kept fee stays the same if the rule or the price changes later.

### Language

- **AC-006-28.** Statuses, fee lines, the example in the rule and the sheet follow the app language; switching it never changes a value or a fee.

## Out of scope

- **007:** Iniciar aula and the live timer; the button stays out of the detail and the form until then.
- **008:** the Pagamento section of Registrar ("Fica a receber" / "Já foi pago" and the method), marking classes paid, the student's balance on the lesson detail, and kept fees reaching the balance.
- **009:** the earnings card and kept fees counting as earned.
- **014:** a reminder about past classes that were never marked.
- Editing the amount of a suggested fee: the teacher keeps it or waives it.

## Open questions

Answered by Victor, 27 Oct 2026:

- The Pagamento section of Registrar waits for 008; in 006 every logged class is owed.
- Fee rules come with the default in Ajustes and a per-student and per-place choice (Padrão, Própria, Não cobrar).
- A cancellation asks when the student gave notice, starting at now.
- Marcar como dada opens a sheet with the duration before confirming.

Assumed, for Victor to confirm in review:

- A percentage fee is taken from the class's value without travel, since the teacher didn't travel.
- A group's class follows its place's rule (AC-006-21). A school class covered by a salary has no value, so a percentage fee is nothing; a fixed fee would still be suggested.
- Dada and Faltou are refused before the class starts; Cancelada is allowed any time (AC-006-11).
- A past class nobody marked stays Agendada and keeps counting in Previsto until marked.
