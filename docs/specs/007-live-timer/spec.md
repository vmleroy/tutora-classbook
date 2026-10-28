# Spec 007: Live timer

Status: done
Depends on: 006 Lessons (Marcar como dada, the done value, Hoje and the lesson detail)
Mockups: docs/mockups/Live.dc.html (live screen, Aula concluída). The balance line and Registrar pagamento on Aula concluída wait for 008.

## Goal

The teacher starts a class when it begins, and Tutora counts it: elapsed time and, for a class paid by the hour, the money earned, going up second by second. They can pause and resume, jot a quick note, and end it, confirming the real duration before it is saved as Dada. The count survives closing the app or restarting the phone, and an ongoing notification keeps showing it while the teacher uses other apps. Teachers who only manage the schedule can turn the timer off in Ajustes.

## User stories

- **US-1.** As a teacher, I tap Iniciar aula when Ana arrives and see the time and what I've earned while the class runs.
- **US-2.** As a teacher, I pause when the class stops for a break and resume after, without losing what was counted.
- **US-3.** As a teacher, I end the class and fix the minutes if I forgot to stop on time, before it is saved.
- **US-4.** As a teacher, I started the wrong class by mistake and discard it without leaving a trace.
- **US-5.** As a teacher, I switch to WhatsApp mid-class and still see the timer in the notification.

## Acceptance criteria

### Starting

- **AC-007-1.** Iniciar aula is offered on the detail of any scheduled class, whatever its day. Tapping it makes the class Ao vivo and opens the live screen. Hoje's "Próxima aula" card offers it too when that class is today (Victor, 28 Oct 2026).
- **AC-007-2.** Only one class can be live at a time. Starting another while one is live says so and offers to open the live one.
- **AC-007-3.** With "Aula ao vivo" off in Ajustes, Iniciar aula is not offered anywhere; a class already live still opens and ends as usual.

### The live screen

- **AC-007-4.** The live screen shows Ao vivo (or Pausada), the student or group with subject, place and planned time, "Tempo de aula" as hh:mm:ss, a ring filling toward the planned duration ("de 60 min planejados"), and Pausar, Encerrar aula and Descartar.
- **AC-007-5.** For a class paid by the hour, "Ganho até agora" goes up every second as price × time counted, with the rate under it ("R$ 120,00 por hora · R$ 2,00 por minuto"). For a class with a fixed price it shows "Valor da aula" with that price, not ticking; covered by a salary, it says so; with no price, "Sem preço".
- **AC-007-6.** The time counted is the time since the start minus the time spent paused. It is worked out from when the class started and was paused, so closing the app, swiping it away or restarting the phone never loses or adds time.
- **AC-007-7.** Pausar stops the count and shows Pausada; Retomar continues from where it stopped.
- **AC-007-8.** "Anotação rápida" takes a note during the class; it is added to the lesson's notes when the class ends.

### Ending

- **AC-007-9.** Encerrar aula opens a sheet with the real duration, the time counted rounded to the nearest minute (at least 1), changed with − / +, the value updating as it changes, Continuar to go back to the class, and "Confirmar · R$ 124,00".
- **AC-007-10.** Confirming saves the class as Dada with that duration and its value worked out as in spec 006 (frozen price, travel), and shows "Aula concluída": who, the time of class and the value, with Desfazer and Voltar para Hoje.
- **AC-007-11.** Desfazer on "Aula concluída" puts the class back to Ao vivo, paused at the time it was ended, with its note.
- **AC-007-12.** Descartar asks first ("Descartar esta aula ao vivo? O tempo contado não é salvo.") and then puts the class back to Agendada, as if it had never started.

### Everywhere else

- **AC-007-13.** While a class is live, Hoje shows it at the top ("Ao vivo · Ana Souza · 00:23:41"), and its detail says Ao vivo with a button to open the live screen; both open the live screen.
- **AC-007-14.** A live class is not "Próxima aula", counts in Previsto at its value as scheduled, and shows as Ao vivo on Hoje and Agenda.

### The notification

- **AC-007-15.** While a class is live, an ongoing notification shows who, the time counted going up, and Pausar (or Retomar) and Encerrar. Tapping it opens the live screen; Encerrar opens the app on the end sheet.
- **AC-007-16.** The notification goes away when the class is ended, discarded or removed. If the phone doesn't allow notifications, the timer still works in the app.

### Ajustes

- **AC-007-17.** Ajustes has an "Aulas" group with "Aula ao vivo" (on by default) and a line on what it does.

### Language

- **AC-007-18.** Every text on the live screen, the sheet, the notification and Aula concluída follows the app language; times and amounts are written for it, and switching never changes the time counted or the value.

## Out of scope

- **008:** the balance line on Aula concluída ("Adicionado ao saldo…") and Registrar pagamento.
- **014:** reminders before a class starts.
- Starting a class that is not on the schedule: log it afterwards with Registrar aula dada (006).
- A home-screen widget or lock-screen controls beyond the notification.

## Open questions

Answered by Victor, 28 Oct 2026:

- Iniciar aula works any time, for any scheduled class; a class started by mistake is discarded with Descartar.
- Encerrar aula opens a confirm sheet with the real duration, like Marcar como dada.
- The timer can be turned off in Ajustes, on by default.
- A class with a fixed price or a salary runs the clock but its value does not tick.

Assumed, for Victor to confirm in review:

- Ending keeps the class's planned start time; the real duration replaces the planned one (as Marcar como dada does).
- Desfazer after ending returns to Ao vivo paused, so the minutes between ending and undoing are not counted.
- The notification shows the time going up, not the money: money in the notification would need the app to wake every second.
- The phone asks for permission to show notifications the first time a class is started.
