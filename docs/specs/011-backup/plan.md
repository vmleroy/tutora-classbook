# Plan 011: Backup and restore

Architecture reference: docs/architecture.md (layers, database schema and migrations › Backup format, feature libraries › Backup and restore, testing pyramid). Mockups: docs/mockups/Backup.dc.html, docs/mockups/Settings.dc.html.

## Approach

The backup is a dump of every table as JSON, so it never drifts from the schema: the data layer reads and writes tables generically, the domain owns the file's shape, its checks and the upgrade chain, and one use case class (`Backups`) does every write.

- **Domain (`:core:domain`, `Backup.kt`).** `BackupFile(app, schemaVersion, exportedAt, tables)`, where `tables` maps a table name to its rows and a row maps a column to a JSON value. `encodeBackup` / `readBackup(text)` returns `Ok`, `NotTutora` (not JSON, not `app: "tutora"`, missing `tables`: this also catches the .xlsx, which is a zip) or `Newer(version)`. `upgradeBackup(file, to)` runs a chain of pure steps, one per schema version (1→2 … 4→5); each step so far only adds columns that may be null or tables that start empty, so the steps are small and named after the migration they mirror. The schedule rules: `nextAutoBackup(last, every, now, zone)` (20:00 daily, Sunday 20:00 weekly, the 1st at 20:00 monthly), `autoBackupDue`, `filesToPrune(written, keep)`, `backupWarningDays(lastBackupAt, firstLessonAt, now, zone)` (14 days) and `backupSummary(file)` (students, classes, payments). `:core:domain` gains `kotlinx-serialization-json` (plain Kotlin, multiplatform; the purity test still holds).
- **Data (`:core:data`, `BackupData.kt`).** `BackupStore(db)`:
  - `dump()` reads every user table (from `sqlite_master`, leaving out `room_*`, `sqlite_*` and `android_metadata`) on one reader connection, so the copy is consistent, and turns each value into a JSON string, number or null.
  - `replaceAll(tables, keep)` runs in one writer transaction with `PRAGMA defer_foreign_keys = ON`. It deletes every table, inserts each row with only the columns the table has (an older backup's missing columns take their defaults), and restores the settings keys in `keep`. A failure anywhere rolls everything back (AC-011-14).
  - Backup state lives in its own settings keys, read and written only through `BackupStore`, so a settings save from Ajustes can never overwrite what the worker just logged: `backup_auto`, `backup_every`, `backup_keep`, `backup_folder_uri`, `backup_folder_name`, `backup_written` (names Tutora wrote in the folder), `backup_log` (the last 5 entries), `backup_failed`, `last_backup_at`, `restore_undo`. These are the **device keys**: never written into a backup, and kept through a restore (AC-011-17).
  - `firstLessonAt()` reads `MIN(created_at)` from `lesson`, and `liveCount()` counts `lesson_timer` rows.
- **Use cases (`:app usecases/Backups.kt`).** `Backups(ctx, folder, scheduler, undoFile)`:
  - `makeBackup()` builds the file for a manual backup. `runAuto()` writes it to the folder, prunes, logs, and plans the next run. `runIfDue()` is called once from `StartApp` to catch up on missed runs.
  - `setAuto(on, every, keep, folder)` saves the setup.
  - `inspect(bytes)` checks a picked file and summarises it for the confirmation.
  - `restore(file)` copies the current data aside as a backup JSON in `filesDir/backups/before-restore.json`, then replaces everything. `undoRestore()` does the same from that copy.
  - After a restore, `Payments.backfill()`, `Schedule.fillSchedule()` and `Live.autoStart()` run as at start-up.
- **Adapters (`:app adapters/backup/`).**
  - `BackupFolder` writes, lists and deletes in a folder the teacher picked: Storage Access Framework, `DocumentsContract`, with a persisted URI permission.
  - `BackupScheduler` plans the next automatic run: WorkManager, as the architecture doc says, a unique one-time work set for the next 20:00 slot and re-planned after each run.
  - `BackupFiles` reads a picked file's bytes.
  - Each has an in-memory fake for tests. Sharing reuses `Exports.share` from spec 010.
- **State and screens.** `AppServices` exposes `backups` and a `backupState` Flow. The screens are:
  - The new **Backup e planilha** page (`ui/backup/Backup.kt`, route `BackupHome` in the Ajustes graph).
  - The **"Backup e planilha" row** in Ajustes › Seus dados.
  - A **`BackupAlert`** on Hoje next to `PendingAlert`.
  - The system folder and file pickers through `rememberLauncherForActivityResult` (`OpenDocumentTree`, `OpenDocument`).

## Data

No new table and no migration: the schema stays at version 5. The new state is settings rows (key → JSON value), inserted with defaults by `ensureDefaults`: `backup_auto` false, `backup_every` "week", `backup_keep` 5, the rest empty. The backup's `schemaVersion` is `TutoraDatabase.VERSION`, so every later migration must add its step to `upgradeBackup`; `MigrationsTest` gets a check that the chain reaches the current version.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `Backups.makeBackup` | `last_backup_at`, `backup_log`, clears `restore_undo` | `Exports.share` (from the screen) | backup state |
| `Backups.runAuto` / `runIfDue` | `last_backup_at`, `backup_log`, `backup_written`, `backup_failed`, clears `restore_undo` | `BackupFolder.write/delete`, `BackupScheduler.plan` | backup state |
| `Backups.setAuto` | `backup_auto`, `backup_every`, `backup_keep`, `backup_folder_*` | `BackupScheduler.plan` (or cancel) | backup state |
| `Backups.restore` / `undoRestore` | every table in one transaction; `restore_undo` | undo file in app storage | every Flow (Room re-emits) |
| `Backups.spreadsheet(range)` | nothing | none (the screen shares) | — |

## Screens and components

- **Backup e planilha** follows Backup.dc.html in the 016 look:
  - `ListGroup` "Backup automático": a switch `ListRow` whose line is "Próximo: … · último …" or "Desligado · só backup manual". When it's on, the group adds a `SegmentedControl` (Todo dia / Toda semana / Todo mês), a `ListRow` "Salvar em <folder>" with "Trocar", and a `Stepper` (1–30). The hint text sits under the group.
  - "Fazer agora": `Button` Backup completo (.json), then outlined Planilha .xlsx and Restaurar backup, plus "Desfazer restauração" when it's offered.
  - "Recentes": a `ListGroup` of up to 5 `ListRow`s with a `TagDot`, and a danger `Badge` "Falhou".
  - Sheets (`BottomSheet`): the restore confirmation; the spreadsheet period (`Chip`s for Este mês, Mês passado, Este ano, Tudo and Personalizado, with `DateField` De / Até as on reports). Errors use `AlertBanner`.
- **Ajustes:** a `ListRow` in Seus dados. In the warning state its icon tile is `TileTone.DANGER` and its line reads "Último backup há N dias".
- **Hoje:** `AlertBanner` "Sem backup há N dias", which opens the page.
- No new design-system component is needed; every piece exists.
- Copy goes in a new `strings_backup.xml` in both languages, with plurals for days (`one`, `many`, `other` in Portuguese).
- The spreadsheet reuses the spec 010 `Xlsx` writer and `Sheet` / `Cell`. Aulas, Pagamentos, Alunos and Locais come from the dump's rows plus names. Resumo has one line per month from `EarningsRepository`, through a new `months(from, to)` that applies the same domain earnings rules per month.

## Test strategy

Lean, JVM only, one focused test per criterion; `./gradlew check` once at the end.

- **Domain** (`core/domain/.../BackupRulesTest.kt`): the file's shape and round trip (AC-011-3), the 20:00 slots (6), pruning only Tutora's own files (7), one catch-up for many missed runs (8), the 14-day warning (10), rejecting a file that isn't a backup (12) or is newer, and upgrading an older one (15).
- **Use cases with a real Room database and fakes** (`app/src/test/.../BackupTest.kt`):
  - Every table is in the file and the name is right (2). A backup changes only the last-backup date (4).
  - Prune (7), catch-up (8) and folder failure (9).
  - The confirmation summary (13). The round trip, and a failing restore leaving data unchanged (14).
  - Undo (16) and the device keys being kept (17). The same balances and earnings after a round trip (18).
  - The spreadsheet's sheets and name (19), and the spreadsheet refused as a backup (20).
- **Screens** (`app/src/test/.../BackupScreensTest.kt`, Robolectric + Compose, `Harness.settle()`):
  - The Ajustes row and the page's layout (1).
  - The switch asking for a folder, and a cancelled picker leaving it off (5).
  - The "Próximo" line (6), the warning on the Ajustes row and on Hoje (10), and Recentes (11).
  - The confirmation sheet and Hoje after a restore (13, 14), and the page in English (21).
- **Device checks, pending in the PR:** the Google Drive folder in the picker, a real WorkManager run at 20:00, and restoring on a second phone.

## Risks

- **Google Drive in the folder picker** varies by phone (architecture risk). The page always says which folder it saves to, and a failed write shows at once (AC-011-9).
- **WorkManager is a new dependency.** Its version must be checked against the catalog when it's added, and Robolectric tests use the fake scheduler, never WorkManager.
- **A manual backup counts when the file is made**, not when the share finishes: Android doesn't reliably say whether the share sheet completed. "Último" can therefore read "hoje" for a share the teacher cancelled.
- **Big data sets:** a year of classes is a few hundred KB. The dump and the restore run off the main thread, the restore in one transaction.
- **Future migrations** must add their backup step. The chain check in `MigrationsTest` fails if one is missing.
