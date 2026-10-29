# Spec 011: Backup and restore

Status: ready
Depends on: 001 Foundation (migrations and the copy taken before one), 010 Reports (the Excel writer, Seus dados in Ajustes), 016 Visual refresh (the look of Ajustes and its pages)
Mockups: docs/mockups/Backup.dc.html, docs/mockups/Settings.dc.html (the "Backup e planilha" row)

## Goal

Everything Tutora knows lives only on the phone, so a lost, broken or replaced phone today means losing every student, class and payment. After this spec the teacher can save a full copy of their data as one file and share it anywhere (Google Drive, WhatsApp, email), turn on an automatic copy to a folder they choose once, and bring everything back from a copy on the same or a new phone. They can also export a readable spreadsheet of all their data. Nothing needs an account or the network: the copy is a file, and where it goes is the teacher's choice (constitution rules 1 and 2). Restoring replaces data, so it is always confirmed and the current data is copied aside first (rule 3).

## User stories

- **US-1.** As a teacher, I save a backup to my Google Drive before I change phones, and restore it on the new one.
- **US-2.** As a teacher, I turn on a weekly backup to a Drive folder once and stop thinking about it.
- **US-3.** As a teacher, I'm warned if my backups stopped, so I don't find out when it's too late.
- **US-4.** As a teacher, I export all my classes and payments to a spreadsheet to keep my own accounts.
- **US-5.** As a teacher who restored the wrong file, I can get back what I had a moment ago.

## Acceptance criteria

### Where it lives

- **AC-011-1.** Ajustes has "Backup e planilha" in Seus dados, under the teacher's name and Pix key. Its line says the state: "Automático, toda semana · último ontem", "Só manual · último 25/10" or "Nenhum backup ainda". It opens the Backup e planilha page, laid out as its mockup: Backup automático, then Fazer agora (Backup completo, Planilha .xlsx, Restaurar backup), then Recentes.

### A backup by hand

- **AC-011-2.** "Backup completo (.json)" makes one file with every student, place, group, link, schedule, class, timer, payment, package and setting, and opens the phone's share sheet with it. The file is named with the date and time, "Tutora backup 2026-10-29 20-00.json".
- **AC-011-3.** A backup carries the app's name, the version of its data and when it was made, so a later Tutora can read an older backup.
- **AC-011-4.** Making a backup changes nothing in the app's data except the date of the last backup.

### Automatic backup

- **AC-011-5.** "Backup automático" is off at first. Turning it on asks for a folder with the phone's folder picker (which can be a Google Drive, OneDrive or phone folder); cancelling the picker leaves it off. The page then shows "Salvar em <folder>" with "Trocar".
- **AC-011-6.** It runs Todo dia, Toda semana or Todo mês (Toda semana at first), in the evening, at 20:00. The line under the switch reads "Próximo: dom, 1 nov, 20:00 · último ontem", or "Desligado · só backup manual" when off.
- **AC-011-7.** "Guardar os últimos N backups" (5 at first, from 1 to 30, with − and +) keeps only the newest N files Tutora wrote in that folder; older ones are deleted. Files Tutora didn't write there are never touched.
- **AC-011-8.** If a run was missed (the phone was off, or the battery saver held it back), the backup is made the next time Tutora opens, once, not once per missed run.
- **AC-011-9.** If the folder can't be written (deleted, or its access withdrawn), the run fails without losing anything, and the page and the Ajustes row say "Não foi possível salvar na pasta. Escolha a pasta de novo." with "Trocar".

### Staying safe

- **AC-011-10.** When there is data and no backup has been made for 14 days (or ever, 14 days after the first class was added), the Ajustes row shows in red "Último backup há 16 dias", and Hoje shows a short alert, "Sem backup há 16 dias", that opens the Backup e planilha page (Victor, 29 Oct 2026). Both go away with the next backup, manual or automatic.

### Recentes

- **AC-011-11.** Recentes lists the last 5 backups, newest first, with the day and time, how it was made ("Automático · Google Drive" or "Manual · compartilhado") and its size; a failed automatic run shows in red with "Falhou". It is a list only: tapping a row does nothing, and restoring always goes through Restaurar backup (Victor, 29 Oct 2026).

### Restore

- **AC-011-12.** "Restaurar backup" opens the phone's file picker for a backup file. Picking a file that isn't a Tutora backup, or is damaged, says "Este arquivo não é um backup do Tutora" and changes nothing.
- **AC-011-13.** Before restoring, a confirmation says what the backup holds and what it replaces: "Restaurar o backup de 25/10/2026, 20:00? 12 alunos, 340 aulas, 96 pagamentos. Os dados atuais deste celular serão substituídos." with "Restaurar" and "Cancelar". Cancelling changes nothing.
- **AC-011-14.** Restoring replaces all the app's data with the backup's, all at once: if anything fails midway, the data stays exactly as it was before. Afterwards the app reopens on Hoje showing the restored data, in the backup's language and theme.
- **AC-011-15.** A backup made by an older version of Tutora restores, brought up to date on the way. One made by a newer version than the installed app is refused: "Este backup é de uma versão mais nova do Tutora. Atualize o app para restaurá-lo."
- **AC-011-16.** Before restoring, the current data is copied aside on the phone. Right after a restore, the page offers "Desfazer restauração" until the next restore or backup (Victor, 29 Oct 2026), which brings back exactly what was there before.
- **AC-011-17.** Restoring keeps this phone's own backup setup (automatic on or off, the folder, how many to keep): a backup from another phone doesn't point at a folder this phone can't reach.
- **AC-011-18.** After a restore every amount, balance and earning shows exactly as it did when the backup was made (same students, classes, payments, credit and totals for any period).

### Spreadsheet

- **AC-011-19.** "Planilha .xlsx" asks for a period, with quick choices (Este mês, Mês passado, Este ano, Tudo) and Personalizado, which opens De and Até as on reports (Victor, 29 Oct 2026). Generating opens the share sheet with "Tutora planilha outubro 2026.xlsx". It has the sheets Aulas, Pagamentos, Alunos, Locais and Resumo (one line per month: earned, received, to receive). Dates are dates and amounts are numbers in reais, so the spreadsheet can sum them.
- **AC-011-20.** The spreadsheet is for reading only: picking it in Restaurar backup is refused as in AC-011-12.

### Language

- **AC-011-21.** The page, its messages, file names and the spreadsheet's headings follow the app language. A backup made in Portuguese restores the same on a phone set to English, with every amount and date the same.

## Out of scope

- Signing in to Google Drive or any account: Drive is reached only through the share sheet and the folder picker.
- Password-protecting the backup file (spec 015 may revisit; see Open questions).
- Merging a backup into the current data: restore always replaces everything.
- Choosing the backup folder during first-run setup: that is spec 014, which reuses this page's folder choice.
- Calendar events (012): a restore doesn't rewrite the phone's calendar.

## Open questions

Answered by Victor on 29 Oct 2026: the spreadsheet offers quick periods and Personalizado (AC-011-19); the 14-day warning is on the Ajustes row with an alert on Hoje (AC-011-10); Desfazer restauração lasts until the next backup or restore (AC-011-16); Recentes is a list only (AC-011-11).

Also confirmed by Victor on 29 Oct 2026: restoring isn't allowed during a live class ("Encerre a aula ao vivo antes de restaurar"), and the automatic backup runs at 20:00, not at a time the teacher picks.
