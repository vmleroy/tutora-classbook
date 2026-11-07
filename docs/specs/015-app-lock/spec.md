# Spec 015: App lock

Status: ready
Depends on: 010 Reports and 019 (Seus dados: name, Pix key, report title), 007 Live timer (the ongoing notification), 011 Backup (restore), 014 (Ajustes in pages, the reminders' notifications, the setup's "Seus dados, seguros"), 017 Landscape (the side panel, turning the phone, the screen kept on during a live class)
Mockups: none yet. The lock screen and the new rows follow the design system (ListRow with a Switch, SelectField, an EmptyState-like centred screen); a board can be added before `/plan`.

## Goal

Tutora holds things the teacher may not want anyone who picks up the phone to see: students' names and phones, what each one owes, earnings, and the teacher's own Pix key. The teacher can turn on a lock so that Tutora asks for the phone's own unlock (fingerprint, face or the phone's PIN, pattern or password) when it is opened, and again once it has been closed or left in the background for a while. Once unlocked, Tutora is used as today, with no more asking, except before the few things that take everything off the phone or replace it. Tutora is hidden in the recent-apps screen; screenshots and notifications stay as they are. Nothing leaves the phone: the lock uses what the phone already has (constitution rules 1 and 2).

## User stories

- **US-1.** As a teacher, I turn on "Bloquear o Tutora" so a student who borrows my phone can't open my earnings or my Pix key.
- **US-2.** As a teacher, I unlock Tutora with my fingerprint, as I unlock my phone, and then use it without being asked again.
- **US-3.** As a teacher, I switch to WhatsApp mid-class and come back without unlocking again, but if I leave it for longer, it asks again.
- **US-4.** As a teacher, I start and stop a class from its notification, without opening Tutora.

## Acceptance criteria

### Turning it on

- **AC-015-1.** Ajustes › Seus dados has a "Privacidade" group under the name, Pix key and report title, with "Bloquear o Tutora", off at first. Turning it on asks for the phone's unlock once, to prove it works, before it takes effect. Ajustes' line for Seus dados then also says "Bloqueio ligado".
- **AC-015-2.** On a phone with no screen lock set, the switch can't be turned on. The row says "Defina um bloqueio de tela no celular para usar" and offers to open the phone's security settings.
- **AC-015-3.** Turning it off asks for the phone's unlock first; then Tutora never locks until it is turned on again.
- **AC-015-4.** With the lock on, "Bloquear após" shows under it: Imediatamente, 1 minuto or 5 minutos (at first) (Victor, 7 Nov 2026).
- **AC-015-5.** It is also offered in the setup's step 4 "Seus dados, seguros" as a third switch (AC-014-22), and once when the Pix key is first saved: "Sua chave Pix fica no app. Quer proteger o Tutora com o bloqueio do celular?", with "Ativar" and "Agora não" (Victor, 7 Nov 2026).

### Locking and unlocking

- **AC-015-6.** With the lock on, opening Tutora shows a lock screen with the app's name and "Desbloquear" before any of its content. It asks for the fingerprint or face, and falls back to the phone's PIN, pattern or password. Tutora has no PIN of its own (Victor, 7 Nov 2026).
- **AC-015-7.** Once unlocked, nothing in the app asks again while it is in use (Victor, 7 Nov 2026), except AC-015-3 and AC-015-15.
- **AC-015-8.** After Tutora has been closed or in the background longer than "Bloquear após", it locks again. Coming back sooner doesn't ask. Turning the phone upright or sideways is not leaving: it never locks or asks again (AC-017-1). A live class locks like any other time; its timer keeps counting (AC-007-6).
- **AC-015-9.** Cancelling the unlock leaves the lock screen in place, with "Desbloquear" to try again. Nothing behind it can be seen or reached: not a screen, an open sheet, the side panel (AC-017-6 to 11), nor what a notification or a shared file would open.
- **AC-015-10.** Once unlocked, Tutora is exactly where it was: the tab, the screen, an open panel or sheet, and anything typed (as AC-017-1). Opened from a notification, it goes on to what the notification opens (a class, Aulas para confirmar, Cadastro on who owes, the live class, the end sheet).
- **AC-015-11.** The lock screen works upright and sideways, with every text size, in every theme, light and dark.

### Notifications and the live class

- **AC-015-12.** Tutora's notifications stay as they are, with or without the lock: who, the time and the amounts (Victor, 7 Nov 2026). Their buttons that work without opening Tutora (Pausar, Retomar, Marcar todas como dadas) keep working without the unlock.
- **AC-015-13.** The reminder before a class (AC-014-12) has "Iniciar aula" when "Aula ao vivo" is on (AC-007-3) and no other class is live. Tapping it starts the class as AC-007-1 does, without opening Tutora; the live notification takes its place, with Pausar and Encerrar (Victor, 7 Nov 2026). Encerrar opens Tutora on the end sheet, behind the lock when it is locked.

### Nothing private in sight

- **AC-015-14.** With the lock on, Tutora's screen is hidden in the recent-apps screen where the phone allows it without blocking screenshots. Screenshots are never blocked (Victor, 7 Nov 2026).
- **AC-015-15.** With the lock on, Backup completo, Planilha .xlsx and Restaurar backup (AC-011) ask for the phone's unlock before they start; cancelling it does nothing (Victor, 7 Nov 2026).
- **AC-015-16.** With the lock on or off, the Pix key never shows in full until asked: Ajustes' line for Seus dados shows it masked ("•••• 9876"), and on Seus dados the field is masked with an eye button that shows it, to read or edit, while the page is open (Victor, 7 Nov 2026).

### Language

- **AC-015-17.** The lock screen, the Ajustes and setup rows, the Pix key offer, "Iniciar aula" on the notification and their messages follow the app language.

## Out of scope

- A PIN or password of Tutora's own, separate from the phone's.
- Hiding names or amounts in notifications on the phone's lock screen.
- Encrypting the database on the phone: the phone's own storage encryption covers a locked phone.
- A password on backup files and on the automatic backup folder (011); it would change 011's file format.
- The calendar's events (012): once sent to the phone's calendar, they show wherever the calendar app shows them.
- Hiding the Pix key on reports: when it is set, reports show it, by design (AC-010-16).
- Remote wipe or several profiles on one phone.

## Open questions

Answered by Victor on 7 Nov 2026: the phone's unlock only; Bloquear após Imediatamente, 1 or 5 minutos, 5 at first, with the switch turning it off; off at first, offered in the setup and when the Pix key is first saved; screenshots stay allowed; notifications stay as they are; the Pix key hidden behind an eye button; backups and the spreadsheet ask for the unlock. The lock is a guard at the door: it asks when Tutora is opened and after it was closed or idle, never again while in use. A live class can be started and paused from notifications, so its timer runs whatever the lock.
