# Spec 004: Places and students

Status: ready (Kotlin rebuild: re-plan)
Depends on: 003 Money and language
Mockups: docs/mockups/Places.dc.html, PlaceDetail.dc.html, PlaceEdit.dc.html, Students.dc.html, StudentDetail.dc.html, StudentEdit.dc.html

## Goal

The teacher registers once where they teach and whom they teach: their places (the built-in Casa do Aluno and Online / Remoto, plus schools, salinhas, cursinhos and private spots), their students and their class groups, and links students to places with a price for each. From then on Tutora knows what any lesson is worth before it exists: who pays, how much, and what the teacher keeps. Lessons (005, 006) build on this.

## User stories

- **US-1.** As a teacher, I register a salinha once (who pays, the rate or the commission, when it settles, the usual duration) so I never type it again.
- **US-2.** As a teacher, I add a student, link them to the places where we meet, and give them their own price at a place when it differs from the place's.
- **US-3.** As a teacher at a school, I record my monthly salary and my class groups there.
- **US-4.** As a teacher, I open a student and see their places, or open a place and see its students, and jump from one to the other.
- **US-5.** As a teacher, I archive a student or place I no longer teach without losing its history, and remove it for good only when I decide to.

## Acceptance criteria

### Places

- **AC-004-1.** Locais lists the two default places, Casa do Aluno and Online / Remoto, under "Padrão", then the teacher's own places under "Seus locais". The default places are places like any other: Locais shows its empty state only when nothing is listed. Each row shows its price, default duration and how many students are linked.
- **AC-004-2.** A built-in place starts with no price and shows "Sem preço" until the teacher sets one. It can be edited (price, charging per hour or per lesson, duration) but never renamed, archived or removed.
- **AC-004-3.** A new place has a name, an optional address and a type: Escola, Salinha, Cursinho or Particular. Saving without a name is refused with a message that says so.
- **AC-004-4.** A place's type decides how it counts in Ganhos: Escola and Cursinho count as "Escola e emprego", the rest as "Aulas particulares". The edit form says which, under the type.
- **AC-004-5.** For a Salinha, Cursinho or Particular place the teacher chooses who pays: the place (a rate per hour or per lesson and a settlement cycle: por aula, semanal or mensal) or the student (a rate and the percentage the place keeps, 0% to 90%).
- **AC-004-6.** An Escola is paid by a monthly salary instead of a rate.
- **AC-004-7.** The edit form previews a 60-minute lesson as the teacher changes it: "Você ganha R$ 60,00, a receber do local no fechamento do mês" when the place pays, "Aluno paga R$ 100,00, você fica com R$ 70,00" when the student pays with 30% to the place.
- **AC-004-8.** Every place has a default duration, 60 minutes unless changed.
- **AC-004-27.** A duration is set with − / + in 5-minute steps, shown as "90 min · 1h30". Tapping it lets the teacher type the exact time as hh:mm, filling from the right (1, 3, 0 → 01:30); minutes over 59 or more than 5 hours are refused with a message.
- **AC-004-9.** Casa do Aluno has a travel fee rule: varies (asked in each lesson), a fixed amount, or none. It starts as "varies" and the list row states the rule.

### Students

- **AC-004-10.** A student has a name (required), subject and grade, phone, how they are charged (Por hora or Por aula), a color tag and notes.
- **AC-004-11.** A student is saved with at least one linked place (saving with none is refused with a message); one of them is starred as the default. The first place linked becomes the default, and removing the default moves the star to the next one.
- **AC-004-12.** At each linked place the student pays by the hour or lesson, either the place's price ("Do local") or their own price ("Personalizado"). Prices are set with − / + in R$ 5 steps, and tapping the amount lets the teacher type the exact value.
- **AC-004-13.** At a place that pays the teacher, the student has no price of their own; the card says "Pago pelo local" with the place's rate.
- **AC-004-14.** Alunos lists active students and groups by name, with search by name and a "Por local" filter. Each row shows the subject and places, or the group's place and size.
- **AC-004-28.** A new student starts with a random color tag, which the teacher can change. Every row that lists a student (Alunos, a place's students, pickers, Arquivados) shows their initials in their color.
- **AC-004-15.** A student's detail shows their contact, rate and linked places; tapping a place opens that place's detail.

### Groups

- **AC-004-16.** A group (turma) belongs to one place and has a name, subject, optional members chosen from the students linked to that place, and a price per lesson. A group at an Escola has no price of its own; it is covered by the salary.
- **AC-004-17.** A place's detail shows its type, address, who pays and its rate, duration and settlement cycle, then the students linked to it with "+ Ligar aluno", then its groups. Tapping a student opens their detail.

### Price of a lesson

- **AC-004-18.** The price for a student at a place is the student's own price there if set, else the place's price. If the place pays, it is the place's rate whatever the student's price.
- **AC-004-19.** The price for a group lesson is the group's price; at an Escola there is no per-lesson price.
- **AC-004-20.** The resolved price says where it came from (the place, the student's own price or the group), so later screens can show "do local Salinha Aprender+".
- **AC-004-21.** When neither the student nor the place has a price, the price is "not set", never zero.
- **AC-004-22.** Changing a place's price changes the resolved price of every linked student who uses "Do local", and of no one with their own price.

### Archive and remove

- **AC-004-23.** Arquivar hides a student, group or own place from lists, filters and pickers, and keeps everything about it. Arquivados opens from an archive icon in the Alunos and Locais headers (not a row at the end of the list) and shows archived items to restore them.
- **AC-004-24.** Only an archived item can be removed permanently, after a confirmation that names it and says it cannot be undone. Removing a place removes its links and groups, never the students.
- **AC-004-25.** Archiving a place keeps its students; their links to it are hidden until it is restored. A student whose only place is archived shows "Sem local" in the list.

### Default places in Ajustes

- **AC-004-29.** Ajustes has "Mostrar locais padrão", on by default. Turned off, Casa do Aluno and Online / Remoto leave Locais and the "Ligar a outro local" picker; students already linked to them keep those links.

### Language

- **AC-004-26.** Every text on these screens is in both languages; built-in place names follow the app language (Casa do Aluno / Student's home, Online / Remoto / Online / Remote) while names the teacher typed never change.

## Out of scope

Balances, payments, "Devendo" filter, Receber da salinha and Registrar pagamento (008); packages and the Pacote option (008); lessons, Próximas / Últimas aulas, Nova aula and the schedule (005, 006); cancellation and no-show rules (006); reports (010); first-run setup of the built-in prices (014). The "Paga em" currency row in the student mockup is gone (reais only, constitution rule 9).

## Answered questions (Victor, 27 Oct 2026)

- Group members are optional and drawn from the students linked to the group's place; a school group can be just a class name.
- A student needs at least one place to be saved; only archiving their only place leaves them "Sem local".
- Prices use − / + steppers in R$ 5 steps; tapping the amount opens exact entry.
- A Particular place asks who pays, like a Salinha.
