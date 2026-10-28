# Spec 006: Lessons

Status: done
Depends on: 005 Schedule (and 004 for prices and who pays)
Mockups: docs/mockups/AddLesson.dc.html (Registrar mode), LessonDetail.dc.html (status line, Quem paga, Marcar como dada). EditLesson's Situação and the Fees rule screen were dropped on 27 Oct 2026 (see Open questions).

## Goal

A class on the schedule can now end the way it really did: given, cancelled or missed. The teacher marks a class as done with its planned minutes or the real ones, logs a lesson that already happened without ever having scheduled it, and cancels a class from its detail, saying what happened (the student cancelled, didn't show up, or the teacher cancelled), why, and whether it is charged. Cancelling is not removing: the class stays on its day. A class that is done keeps its value from then on, so price changes no longer touch it. This is what payments (008) and earnings (009) will count.

## User stories

- **US-1.** As a teacher, I finish Ana's class and mark it done in two taps, adjusting the minutes when it ran long.
- **US-2.** As a teacher, I log yesterday's class with Pedro that I never put on the schedule, with its real duration, and see what it was worth as I type.
- **US-3.** As a teacher, I cancel Ana's class when she tells me she can't come, write down why, and decide on the spot whether to charge half, all or some other amount.

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

- **AC-006-9.** The detail of a scheduled class has Cancelar aula at any time, and Marcar como dada once it has started. The edit form has no status: a class is marked done, cancelled or missed only from its detail. *(Changed 27 Oct 2026 at Victor's request: cancelling is not removing, and it asks why.)*
- **AC-006-10.** Marking done or cancelling changes only that class; filling its series later never brings it back as scheduled.
- **AC-006-11.** "Aluno faltou" and Marcar como dada are offered only once the class has started; "Aluno cancelou" and "Eu cancelei" at any time.
- **AC-006-12.** Changing a done class's duration recomputes its value from the price it had when it was marked done; a value typed by hand stays as typed.
- **AC-006-13.** A done, cancelled or missed class shows Desfazer, which puts it back to Agendada: the reason and the charge are cleared, and its value follows the current price again (AC-005-16) unless it was typed by hand.
- **AC-006-14.** The lesson detail shows the status at the top ("Agendada · em 25 min", "Dada", "Cancelada pelo aluno", "Cancelada por você", "Faltou"), with the charge when there is one ("Cancelada pelo aluno · cobra R$ 60,00"), the reason when one was written, and who pays (the student or the place).

### On Hoje and Agenda

- **AC-006-15.** A done class shows as done (a check and "Dada"). A cancelled or missed class stays on its day, muted, labelled Cancelada or Faltou, with its charge when there is one.
- **AC-006-16.** The day's "Previsto" counts scheduled and done classes at their value plus charges for cancelled and missed classes. A cancelled or missed class without a charge counts nothing.
- **AC-006-17.** "Próxima aula" never shows a class that is cancelled, missed or done.

### Cancelling a class

- **AC-006-18.** Cancelar aula opens a sheet: "O que aconteceu?" with Aluno cancelou, Aluno faltou and Eu cancelei; "Motivo" (optional text); the "Cobrar esta aula?" toggle, off at first; and Voltar and Cancelar aula. Nothing changes until it is confirmed.
- **AC-006-19.** With "Cobrar esta aula?" on, the charge is 50%, 100% or Outro valor. A percentage is that share of the class's value without travel, rounded to the centavo once, and is shown ("R$ 60,00"); Outro valor shows a money field. The confirm button reads "Cancelar aula · cobrar R$ 60,00".
- **AC-006-20.** "Eu cancelei" hides "Cobrar esta aula?": a class the teacher cancels is never charged.
- **AC-006-21.** For a class without a price, 50% and 100% are not offered; only Outro valor.
- **AC-006-22.** Confirming saves the status (Cancelada, or Faltou for "Aluno faltou"), the reason and the charge. A charge stays the same if the price changes later.
- **AC-006-23.** There is no cancellation rule to set: Ajustes has no Cancelamentos e faltas, and student and place forms have no fee choice. Each charge is decided in the sheet.

### Added 28 Oct 2026 (Victor)

- **AC-006-25.** Ajustes › Aulas has "Marcar como dada antes do horário", off at first. On, Marcar como dada is offered on any scheduled class, even one still ahead, and confirming it makes the class Dada as in AC-006-2. Off, AC-006-9 and 11 hold: it waits for the class to start.

### Language

- **AC-006-24.** Statuses, reasons, charges and the sheets follow the app language; switching it never changes a value or a charge.

## Out of scope

- **007:** Iniciar aula and the live timer; the button stays out of the detail and the form until then.
- **008:** the Pagamento section of Registrar ("Fica a receber" / "Já foi pago" and the method), marking classes paid, the student's balance on the lesson detail, and charges reaching the balance.
- **009:** the earnings card and charges counting as earned.
- **014:** a reminder about past classes that were never marked.
- A default cancellation rule (notice window, automatic suggestion), in Ajustes or per student: dropped by Victor.

## Open questions

Answered by Victor, 27 Oct 2026:

- The Pagamento section of Registrar waits for 008; in 006 every logged class is owed.
- Marcar como dada opens a sheet with the duration before confirming.

Changed by Victor later on 27 Oct 2026, after trying the first build:

- Cancelling is a button on the lesson detail that opens a sheet with what happened, the reason and "Cobrar esta aula?" as a toggle. It replaces Situação in the edit form.
- The charge is 50%, 100% or another amount, chosen in the sheet each time. There is no rule in Ajustes and no fee choice on students or places, so nothing asks when notice was given.

Assumed, for Victor to confirm in review:

- A percentage fee is taken from the class's value without travel, since the teacher didn't travel.
- The reason is chosen from three chips with an optional note (Victor left this open).
- Marcar como dada and "Aluno faltou" wait for the start; cancelling is allowed any time (AC-006-11).
- A past class nobody marked stays Agendada and keeps counting in Previsto until marked.
