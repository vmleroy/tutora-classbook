# Spec 012: Calendar sync and .ics

Status: done
Depends on: 005 Schedule (classes 8 weeks ahead, the Agenda), 006 Lessons (done, cancelled and missed classes), 011 Backup (a restore replaces every class), 016 Visual refresh (the look of Ajustes and the Agenda)
Mockups: docs/mockups/Calendar.dc.html, docs/mockups/Settings.dc.html (the "Sincronizar calendário" row)

## Goal

The teacher already lives in Google Calendar or Outlook for the rest of their life, and today Tutora's classes don't show up there. After this spec Tutora copies the classes of the next 8 weeks into a calendar: one of the teacher's own calendars in the phone's Google or Microsoft account, which the account carries to the cloud, or a "Tutora – Aulas" calendar of its own kept only on the phone, so Tutora itself still never touches the network (constitution rules 1 and 2). Any change in Tutora updates or removes the event. The other way round, the teacher can see their other appointments as grey busy time on Tutora's Agenda, so they don't book a class over a dentist appointment. For anyone who doesn't want sync, the classes can be exported once as an .ics file.

## User stories

- **US-1.** As a teacher, I see my Tutora classes in Google Calendar on my laptop, next to everything else.
- **US-2.** As a teacher, when I move or cancel a class in Tutora, my calendar follows without me touching it.
- **US-3.** As a teacher, I see when I'm busy with other things on Tutora's Agenda before I book a class.
- **US-4.** As a teacher, I don't want my students' prices showing on a calendar my family can see.
- **US-5.** As a teacher who won't connect a calendar, I send my classes to someone as one file.

## Acceptance criteria

### Where it lives

- **AC-012-1.** Ajustes › Geral has "Calendário", whose line says the state: the chosen calendar and the last update ("Tutora – Aulas · victor@gmail.com · atualizado agora"), or "Desligado". It opens the Calendário page as on its mockup: "Enviar aulas para o calendário" with its switch and status line, "Em qual calendário?", the two options, the note, and "Exportar agenda como .ics".

### Turning it on

- **AC-012-2.** "Enviar aulas para o calendário" is off at first. Turning it on asks for the phone's calendar permission once; refusing it leaves the switch off and says "Sem acesso ao calendário. Permita nas configurações do celular." with a way to open them.
- **AC-012-3.** "Em qual calendário?" offers both (Victor, 29 Oct 2026): "Só neste aparelho" and every calendar of the phone's accounts that can be written to, by its name and account ("Tutora – Aulas · victor@gmail.com", "Pessoal · victor@outlook.com"). The note under them says that to see the classes in Google Agenda or Outlook, the teacher picks one of their calendars, and that creating one called "Tutora – Aulas" once on calendar.google.com or outlook.com keeps the classes apart. Only the calendar in use says "Em uso", and only while sync is on: with it off, none does and nothing is written anywhere. Tapping a calendar turns sync on into it; the switch alone uses "Só neste aparelho" (Victor, 29 Oct 2026).
- **AC-012-4.** With "Só neste aparelho", Tutora makes a calendar of its own on the phone, "Tutora – Aulas", in the Botões principais color. With an account's calendar, Tutora adds its events to it, next to the events already there, and the account takes them to the cloud. Either way Tutora writes into that one calendar only. (Android lets an app make a calendar only on the phone; one in a Google or Microsoft account has to be created there.)
- **AC-012-5.** Every event Tutora makes carries Tutora's mark: a hidden tag the calendar apps don't show, and a last line "Criado pelo Tutora" in its description. Tutora only ever changes or deletes events with its mark (Victor, 29 Oct 2026).

### What goes in

- **AC-012-6.** Every class from today to 8 weeks ahead becomes one event at its time and duration, whatever its state: scheduled, live, done, cancelled or missed (Victor, 29 Oct 2026). Its title is who it is with and the subject ("Ana Souza · Matemática", or the group's name), its place is the class's place (with the address when it has one), and its notes are the class's notes (Victor, 29 Oct 2026). A cancelled class's title starts with "Cancelada · " and a missed one's with "Faltou · ", so the history shows them.
- **AC-012-7.** "Incluir valor na descrição" is off at first. Off, the event shows only who, the subject and the place; on, it adds the class's value ("Valor: R$ 120,00").
- **AC-012-8.** Creating, moving, editing, cancelling, marking missed or deleting a class in Tutora updates the calendar within moments: a moved class moves its event, a cancelled or missed one is retitled (AC-012-6), a deleted one removes it, and "Só esta aula" and "Esta e as próximas" touch exactly the events they change.
- **AC-012-9.** As days pass, classes that come into the 8 weeks get their events; past events stay as they were.
- **AC-012-10.** The status line reads "Atualizado agora · 24 aulas nas próximas 8 semanas", with the time of the last update.
- **AC-012-11.** Editing or deleting an event in the calendar app never changes Tutora; the next update puts the class's event back as Tutora has it.

### Changing or stopping

- **AC-012-12.** Choosing another calendar moves the events: Tutora's leave the old calendar and appear in the new one; nothing else in either changes.
- **AC-012-13.** Turning it off removes every event with Tutora's mark, and Tutora's own phone calendar if it made one, and nothing else: an account's calendar stays, with every event the teacher had in it (Victor, 29 Oct 2026).
- **AC-012-14.** After a backup is restored (spec 011), the calendar shows the restored classes and none of the replaced ones.

### Cleaning up (Victor, 29 Oct 2026)

- **AC-012-15.** The Calendário page has "Limpar eventos do Tutora", with sync on or off. It finds every event with Tutora's mark in any of the phone's calendars, including ones left from an earlier account or an earlier install, and asks first: "Apagar 24 eventos criados pelo Tutora? Seus outros eventos não são tocados." with "Apagar" and "Cancelar". With none, it says "Nenhum evento do Tutora no celular."
- **AC-012-16.** Apagar deletes exactly those events, and Tutora's own phone calendar once it is empty, and nothing else: an event without the mark stays, even if it has the same title, time or the words "Criado pelo Tutora" typed in by hand. With sync on, cleaning also turns it off, so the events don't come back at the next update.

### Busy time on the Agenda

- **AC-012-17.** "Mostrar outros compromissos" is off at first. On, events from the phone's other calendars appear on the Agenda's Dia and Semana as grey blocks with their time and title, behind the classes; they can't be opened or edited from Tutora. Mês shows no busy time (Victor, 29 Oct 2026).
- **AC-012-18.** An all-day event shows as a thin grey band at the top of its day, not as a block across the day.
- **AC-012-19.** Tutora's own events never show as busy time (the classes are already there), even in an account's calendar that holds other events too: there, only the events without Tutora's mark do.

### .ics export

- **AC-012-20.** "Exportar agenda como .ics" opens the share sheet with "Tutora aulas.ics" holding the classes of the next 8 weeks as in AC-012-6 and 7, which Google Calendar, Outlook and Apple Calendar can import. It works with sync off and needs no permission.
- **AC-012-21.** Importing the same file twice, or a newer one, updates the same events instead of doubling them.

### Language

- **AC-012-22.** The page, its messages, the name of Tutora's own phone calendar ("Tutora – Aulas" / "Tutora – Classes") and the event texts follow the app language; times stay the class's times whatever the language.

## Out of scope

- Reading events back into Tutora as classes, or two-way sync: edits in the calendar app are not pulled back (AC-012-11).
- Reminders on the events: spec 014 handles reminders.
- Signing in to Google or Microsoft: Tutora only uses the accounts the phone already has.
- Busy time on the Mês view and on Hoje.
- Choosing which other calendars count as busy (all visible ones do).

## Open questions

Answered by Victor on 29 Oct 2026: cancelled and missed classes stay in the calendar, marked (AC-012-6); turning sync off removes Tutora's calendar (AC-012-13); the title is who and the subject (AC-012-6); busy time shows on Dia and Semana from all the phone's other calendars (AC-012-17).

Also confirmed by Victor on 29 Oct 2026: only classes from today onwards are sent, never past ones (AC-012-6, 9), and Calendário sits in Ajustes › Geral (AC-012-1).
