# Tasks 011: Backup and restore

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain `Backup.kt`: `BackupFile`, `encodeBackup` / `readBackup` (Ok, NotTutora, Newer), `upgradeBackup` chain 1→5, `backupSummary`; add `kotlinx-serialization-json` to `:core:domain`. Test: `core/domain/src/test/kotlin/app/tutora/domain/BackupRulesTest.kt`. (AC-011-3, AC-011-12, AC-011-15)
- [x] 2. Domain schedule rules: `nextAutoBackup`, `autoBackupDue`, `filesToPrune`, `backupWarningDays`. Test: `BackupRulesTest.kt`. (AC-011-6, AC-011-7, AC-011-8, AC-011-10)
- [x] 3. Data `BackupStore`: `dump()`, `replaceAll(tables, keep)` in one transaction with deferred foreign keys, the device keys and their defaults, `firstLessonAt()`, `liveCount()`; a check that every database version has its backup step. Test: `app/src/test/kotlin/app/tutora/BackupTest.kt`. (AC-011-2, AC-011-14, AC-011-15)
- [x] 4. Adapters `BackupFolder` (write, delete, adopt a folder, read a picked file) and `BackupScheduler` (WorkManager), with fakes; wire into `AppContainer`, `AppServices`, `StartApp` (defaults) and `AppViewModel` (`runIfDue`). Test: `BackupTest.kt`. (AC-011-8, AC-011-9)
- [x] 5. Use cases `Backups`: `makeBackup` (file name, only the last-backup date changes), `runAuto` (write, prune, log, plan next), `setAuto`. Test: `BackupTest.kt`. (AC-011-2, AC-011-4, AC-011-7, AC-011-9)
- [x] 6. Use cases `inspect`, `restore`, `undoRestore`: refused during a live class, summary, copy aside, keep device keys, backfill and fill after. Test: `BackupTest.kt`. (AC-011-13, AC-011-14, AC-011-16, AC-011-17, AC-011-18, AC-011-20)
- [x] 7. Spreadsheet: `Backups.sheetData` (Resumo month by month from `EarningsRepository.view`), the five sheets, the file name, the periods with Personalizado. Test: `BackupTest.kt`. (AC-011-19)
- [x] 8. Strings in `strings_backup.xml` (English and Portuguese) and the Backup e planilha page: Backup automático group, Fazer agora, Recentes, confirmation and period sheets, folder and file pickers. Test: `app/src/test/kotlin/app/tutora/BackupScreensTest.kt`. (AC-011-1, AC-011-5, AC-011-6, AC-011-11, AC-011-13, AC-011-21)
- [x] 9. The Ajustes row in Seus dados with its state and warning, and `BackupAlert` on Hoje; after a restore the app shows Hoje. Test: `BackupScreensTest.kt`. (AC-011-1, AC-011-10, AC-011-14)
- [x] 10. `./gradlew check`, update the roadmap status, and list the device checks as pending in the PR.

## Pending on a device

- Picking a Google Drive folder in the folder picker, and an automatic backup landing there (AC-011-5, 6).
- A real WorkManager run at 20:00 with the app closed, and the catch-up after the phone was off (AC-011-6, 8).
- Revoking the folder's access and seeing "Não foi possível salvar na pasta" (AC-011-9).
- Restoring on a second phone, and Desfazer restauração (AC-011-14, 16).
- The .xlsx opening in Google Sheets and LibreOffice (AC-011-19).
- Large text at 360 dp on Backup e planilha.
