# Spec 015: App lock

Status: draft
Depends on: 010 Reports and 019 (Seus dados: name, Pix key, report title), 007 Live timer (the ongoing notification), 011 Backup (restore), 014 (Ajustes in pages, the reminders' notifications, the setup's "Seus dados, seguros"), 017 Landscape (the side panel, turning the phone, the screen kept on during a live class)
Mockups: none yet. The lock screen and the new rows follow the design system (ListRow with a Switch, SelectField, an EmptyState-like centred screen); a board can be added before `/plan`.

## Goal

Tutora holds things the teacher may not want anyone who picks up the phone to see: students' names and phones, what each one owes, earnings, and the teacher's own Pix key. The teacher can turn on a lock so that Tutora asks for the phone's own unlock (fingerprint, face or the phone's PIN, pattern or password) before showing anything. Once the app has been in the background for a while, it asks again. With the lock on, nothing private shows in the recent-apps screen or in Tutora's notifications on the phone's lock screen. Nothing leaves the phone: the lock uses what the phone already has (constitution rules 1 and 2).

## User stories

- **US-1.** As a teacher, I turn on "Bloquear o Tutora" so a student who borrows my phone can't open my earnings or my Pix key.
- **US-2.** As a teacher, I unlock Tutora with my fingerprint, as I unlock my phone.
- **US-3.** As a teacher, I switch to WhatsApp mid-class and come back without unlocking again, but if I leave it for longer, it asks again.
- **US-4.** As a teacher, I don't want my students' names and amounts showing in the recent-apps screen or on the phone's lock screen.

## Acceptance criteria

### Turning it on

- **AC-015-1.** Ajustes › Seus dados has a "Privacidade" group under the name, Pix key and report title, with "Bloquear o Tutora", off at first. Turning it on asks for the phone's unlock once, to prove it works, before it takes effect. Ajustes' line for Seus dados then also says "Bloqueio ligado".
- **AC-015-2.** On a phone with no screen lock set, the switch can't be turned on. The row says "Defina um bloqueio de tela no celular para usar" and offers to open the phone's security settings.
- **AC-015-3.** Turning it off also asks for the phone's unlock first.
- **AC-015-4.** With the lock on, "Bloquear após" shows under it: Imediatamente, 1 minuto (at first) or 5 minutos.

### Locking and unlocking

- **AC-015-5.** With the lock on, opening Tutora shows a lock screen with the app's name and "Desbloquear" before any of its content. It asks for the fingerprint or face, and falls back to the phone's PIN, pattern or password.
- **AC-015-6.** After Tutora has been in the background longer than "Bloquear após", it locks again. Coming back sooner doesn't ask. Turning the phone upright or sideways is not leaving: it never locks or asks again (AC-017-1).
- **AC-015-7.** Cancelling the unlock leaves the lock screen in place, with "Desbloquear" to try again. Nothing behind it can be seen or reached: not a screen, an open sheet, the side panel (AC-017-6 to 11), nor what a notification or a shared file would open.
- **AC-015-8.** Once unlocked, Tutora is exactly where it was: the tab, the screen, an open panel or sheet, and anything typed (as AC-017-1). Opened from a notification, it goes on to what the notification opens (a class, Aulas para confirmar, Cadastro on who owes, the live class).
- **AC-015-9.** The lock screen works upright and sideways, with the phone's text size, in every theme, light and dark.

### Nothing private in sight

- **AC-015-10.** With the lock on, Tutora's screen is hidden in the recent-apps screen.
- **AC-015-11.** With the lock on, Tutora's notifications on the phone's lock screen show no names and no amounts: the live class shows "Aula ao vivo" and the clock; the reminder before a class "Aula em 15 min"; Quem te deve "Lembrete do Tutora"; Aulas para confirmar its count. Unlocking the phone shows them in full, as today.
- **AC-015-12.** With the lock on, a notification's buttons that change something (Pausar, Retomar, Encerrar, Marcar todas como dadas) ask for Tutora's unlock first, then do it.
- **AC-015-13.** Ajustes' line for Seus dados no longer shows the Pix key in full; on Seus dados the Pix key shows masked ("•••• •••• 9876") with "Mostrar" to reveal and edit it while the page is open. This holds with the lock off too.
- **AC-015-14.** With the lock on, Restaurar backup (AC-011) asks for the phone's unlock before it replaces anything.

### Language

- **AC-015-15.** The lock screen, the Ajustes rows, the notifications' hidden versions and their messages follow the app language.

## Out of scope

- A PIN or password of Tutora's own, separate from the phone's.
- Encrypting the database on the phone: the phone's own storage encryption covers a locked phone.
- A password on backup files and on the automatic backup folder (011); it would change 011's file format.
- The calendar's events (012): once sent to the phone's calendar, they show wherever the calendar app shows them.
- Hiding the Pix key on reports: when it is set, reports show it, by design (AC-010-16).
- Remote wipe or several profiles on one phone.

## Open questions

For Victor before `/plan` (assumptions in each; written 28 Oct, updated 7 Nov 2026 after 016, 017 and 019):

1. **Phone's unlock only, or also a PIN of Tutora's own?** Assumed: the phone's unlock only.
2. **Bloquear após:** Imediatamente, 1 minuto (at first) and 5 minutos?
3. **On by default, and where is it offered?** Assumed off. Offered in the setup's step 4 "Seus dados, seguros" as a third switch, and once when the Pix key is first saved ("Sua chave Pix fica no app. Quer proteger o Tutora com o bloqueio do celular?").
4. **Screenshots:** assumed allowed, with Tutora hidden only in recent apps. Some older phones can't hide it from recent apps without also blocking screenshots; on those, should the lock block screenshots, or leave recent apps showing Tutora?
5. **During a live class:** the screen stays on (AC-017-13), but if the phone locks itself or the teacher presses the power button, assumed Tutora asks again after "Bloquear após" like any other time. Or should a live class never re-lock?
6. **Notifications on the phone's lock screen (AC-015-11, 12):** generic texts as written, and buttons that ask the unlock first? Or hide Tutora's notifications on the lock screen altogether?
7. **Pix key masked with the lock off too (AC-015-13)?** Assumed yes, since Ajustes' list shows it in full today.
8. **Backups (AC-015-14):** assumed only Restaurar asks for the unlock. Should Backup completo and Planilha .xlsx (which carry everything off the phone) ask too?
