# Spec 005: Schedule

Status: ready (Kotlin rebuild: re-plan)
Depends on: 004 Places and students
Mockups: docs/mockups/Main.dc.html, Schedule.dc.html, AddLesson.dc.html (Agendar mode), EditLesson.dc.html (date, time, duration, value and "Aplicar mudanças em")

## Goal

The teacher puts their week into Tutora once: recurring classes (weekly or every two weeks) and one-off ones, for a student or a group, at one of their places. Tutora keeps the next 8 weeks filled, shows each day and week on the Agenda, and opens on Hoje with the next class and the day's list. Moving or cancelling a recurring class asks whether it is only this one or this and the following. Marking classes as done, money earned and payments come in later specs.

## User stories

- **US-1.** As a teacher, I schedule Ana every Monday at 14:00 once, and the next weeks fill in by themselves.
- **US-2.** As a teacher, I open the app and see my next class and today's list without searching.
- **US-3.** As a teacher, I move one Monday class to Tuesday without touching the rest of the series, or move the whole series from a date on.
- **US-4.** As a teacher, I see my week at a glance, with how much the day is expected to earn.

## Acceptance criteria

### Scheduling a class

- **AC-005-1.** The + in the Agenda and Hoje headers opens Nova aula in Agendar mode.
- **AC-005-2.** The teacher picks a student or a group. Only the student's linked places are offered, with the default one pre-selected. A group's place is fixed.
- **AC-005-3.** Date and start time are set. Duration starts at the place's default duration and can be changed.
- **AC-005-4.** The value of the class is shown as it is set up: the resolved price (spec 004) for the duration, plus the travel fee. Without a price it reads "Sem preço" and the class can still be saved. The teacher can type a value by hand for this class.
- **AC-005-5.** At Casa do Aluno with travel "varies", the class asks for its travel fee and suggests the last one used for that student. Travel "fixed" adds the fixed fee; "none" adds nothing.
- **AC-005-6.** Repetir offers Não repete, Semanal and Quinzenal.
- **AC-005-7.** Saving a repeating class fills the schedule from its date up to 8 weeks ahead: weekly on the same weekday, or every other week.

### Keeping 8 weeks ahead

- **AC-005-8.** Whenever the app opens on a new day, every repeating series is filled up to 8 weeks from today. Opening it again the same day, or twice, never creates a duplicate.
- **AC-005-9.** A recurring class stays at the same local clock time on every date, whatever the time zone rules of the day.

### Editing and removing

- **AC-005-10.** Tapping a class opens its edit form with date, start time, duration, value and notes. The form shows the series ("Repete toda segunda") for a recurring class.
- **AC-005-11.** For a recurring class, "Aplicar mudanças em" offers "Só esta aula" and "Esta e as próximas".
- **AC-005-12.** "Só esta aula" changes only that class. The class then stands on its own: filling the series later never overwrites it.
- **AC-005-13.** "Esta e as próximas" applies the change to that class and every later class of the series, and the series continues from there with the new day, time, duration or value. Earlier classes do not change, and a later class that was changed on its own keeps its changes.
- **AC-005-14.** Removing a class asks the same question. "Esta e as próximas" ends the series at that date, so it stops filling in.
- **AC-005-15.** Any class can be edited or removed on its own, including one that already happened. "Esta e as próximas" is offered only from a class still ahead, so a series change never reaches classes that already happened.

### Prices on the schedule

- **AC-005-16.** A scheduled class follows the current price: when the student's or place's price changes, its value changes too. A class with a value typed by hand keeps it.

### Agenda

- **AC-005-17.** Agenda shows the month and a week strip from Monday to Sunday, in both languages. Today is marked, each day with classes has a dot, and the teacher can move to the previous or next week.
- **AC-005-18.** Dia shows the chosen day as a 24-hour grid, opening at the first class (or the current time on a day without classes), with a colored block per class: name, subject or place, and time. The next class is marked "Próxima".
- **AC-005-19.** Above the grid the day reads "Hoje · 5 aulas" (or the date) and "Previsto R$ 380". Expected counts the classes with a price; when a class is covered by a salary, it adds "+ escola".
- **AC-005-20.** Semana shows the seven days side by side with the same blocks.
- **AC-005-21.** Two classes at overlapping times are both visible, side by side.

### Hoje

- **AC-005-22.** Hoje opens with the date ("Segunda, 26 de outubro") and a greeting for the time of day (Bom dia, Boa tarde, Boa noite), with no name until first-run setup asks for it.
- **AC-005-23.** "Próxima aula" shows the next class that has not started yet ("em 25 min", or the day and time if it is not today), with the student or group, subject, place, rate, start time and duration.
- **AC-005-24.** "Aulas de hoje" lists today's classes in time order with their count, time, name, place and duration, and the expected value.
- **AC-005-25.** With no classes, Hoje and Agenda keep their empty states.

### Navigation

- **AC-005-27.** The tab bar holds Hoje, Agenda, Cadastro, Ganhos and Ajustes. Cadastro has Alunos and Locais as sub-pages, switched at its top; Ajustes is reachable from every screen through its tab (Victor, 27 Oct 2026).

### Language

- **AC-005-26.** Weekday and month names, dates and times follow the app language; switching it never moves a class.

## Out of scope

- **006:** Registrar (logging a past lesson), Marcar como dada, the Situação statuses, cancellation and no-show fees, and lesson detail.
- **007:** Iniciar aula and the live timer.
- **008:** the owed total and the pending-payments alert.
- **009:** the earnings card (Ganho hoje, Semana, Outubro).
- **012:** calendar sync and busy time.
- **014:** reminders, and the teacher's name in the greeting.
- **Not planned:** dragging blocks to reschedule. Classes move through the edit form.

## Answered questions (Victor, 27 Oct 2026)

- 005 has the Agendar mode of Nova aula and editing or removing scheduled classes. Registrar and statuses are in 006.
- Hoje shows the next class and the day's list. Money cards come with 006, 008 and 009.
- No drag to reschedule.
- Scheduled classes follow price changes. Only a class whose value was typed by hand keeps its value, until it is done (006).
- The week starts on Monday in both languages.
- "Esta e as próximas" leaves a later class that was changed on its own as it is.
- The Dia grid covers the full 24 hours.
- The greeting has no name until 014.
