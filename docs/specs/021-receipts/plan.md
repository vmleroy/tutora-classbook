# Plan 021: Comprovantes

Architecture reference: docs/architecture.md (Architecture layers, State management and data flow, Database schema and migrations, Feature libraries, Spec-driven development and testing). Mockups: three new files, `docs/mockups/PaymentSheet.dc.html` (the Comprovante part of Registrar / Editar pagamento), `PaymentDetail.dc.html` (the thumbnails) and `Receipt.dc.html` (the viewer and Anexar comprovante), made first (task 1) in ReportPdf's look and the design system's components, for Victor to see before the screens are built.

## Branch

Implemented on `spec/021-receipts`, branched from `spec/020-sending-reports` (PR #23 is open): 021 adds to 020's Fatura and Relatório detalhado sheets and documents (AC-021-7, 8). The PR is based on `spec/020-sending-reports` and retargeted to `main` once #23 merges.

## Approach

A comprovante is a file Tutora owns, in its private storage, with one row per file on its payment. The database knows which payment it proves, its kind and size; the file lives at `filesDir/comprovantes/<row id>.<jpg|png|webp|heic|pdf>`. Nothing about it goes through the network (AC-021-11): the app has no INTERNET permission and keeps none.

Victor, 13 Nov 2026: a payment holds **as many comprovantes as the teacher wants**, images and PDFs; "Incluir comprovantes" is on **both** the Fatura and the Relatório detalhado.

**Domain (`:core:domain`, plain Kotlin)**

- `Receipts.kt` (new):
  - `ReceiptKind { IMAGE, PDF }` and `Receipt(id, paymentId, kind, mime, bytes, position)`.
  - `receiptPolicy(mime, bytes, width, height): ReceiptIntake`: `Keep` (an image up to 2000 px on its longer side, or a PDF up to 10 MB), `Shrink(w, h)` (an image larger than 2000 px, scaled so its longer side is 2000, aspect kept, rounded half away from zero), `TooLarge` (a PDF over 10 MB) or `Unsupported` (anything but an image or a PDF). The adapter measures and applies; the rule is here so it is tested in milliseconds (AC-021-2).
  - `receiptHeading(day, method, cents, words)`: "16/10/2026 · Pix · R$ 540,00" for the document's section (AC-021-7); `receiptUsage(count, bytes)`: the numbers for "12 comprovantes · 8,4 MB", one decimal, half away from zero (AC-021-12).
  - `attachable(payments, today, zone)`: the payments of the last 60 days, newest first, for Anexar comprovante (AC-021-3).
- `Payments.kt`: `PaymentRecord` gains `receipts: Int` (how many), so every list can show the mark without another query (AC-021-4).
- `Reports.kt`: `ReportPayment` gains `id` and `receipts: Int`; the documents need both (AC-021-7, 8).
- `Backup.kt`: `backupUpgrades[9] = { t -> t }` (a backup before 021 has no `payment_receipt` table, which restores as empty; AC-021-10); `backupFileName` takes the extension (`.zip` now; `.json` still read).

**Data (`:core:data`)**

- `ReceiptRow` entity, table `payment_receipt` (see Data), and `ReceiptDao`: by payment, insert, delete, `count()` + `SUM(bytes)` as a Flow for Ajustes (AC-021-12), every file name (for the sweep).
- `PaymentDao`'s payment queries join a `COUNT` of receipts, so `PaymentRecord.receipts` and `ReportPayment.receipts` come from the same rows (AC-021-4, 7, 8).
- `BackupStore.dump()` already reads every table by name, so `payment_receipt` goes into the JSON with no change; `replaceAll` likewise.

**Adapters (`:app adapters/receipts/`)**

- `ReceiptFiles` interface (fake in tests, `AndroidReceiptFiles` on the phone):
  - `stage(uri): Staged | Refused(reason)`: copies the picked or shared file into `filesDir/comprovantes/pending/` at once (a content URI's permission does not last), reads its size and, for an image, its bounds; applies `receiptPolicy` (decode with `inSampleSize`, scale, re-encode JPEG at quality 90, EXIF orientation applied first); a PDF is opened once with `PdfRenderer` to count pages and refuse one with a password or a broken file (AC-021-1, 2).
  - `commit(staged, id)`: moves it to `comprovantes/<id>.<ext>`; `delete(file)`; `file(name): File`; `thumbnail(name, px)` (an image sampled down, or a PDF's first page); `pages(name)` and `page(name, i, px)` for the viewer and the document.
- `MainActivity`: `ACTION_SEND` and `ACTION_SEND_MULTIPLE` for `image/*` and `application/pdf` (an intent filter on the activity, AC-021-3). The streams are staged right away, before anything is shown, then a `ShareRequest(staged)` goes to the navigation like a notification's open request; `LockGate` already covers whatever it opens (AC-015-9).
- The camera: `ActivityResultContracts.TakePicture` into a file in `cache/incoming/` through the existing `FileProvider` (a new `cache-path`; nothing under `files/` is ever exposed). No CAMERA permission: the system camera app takes the photo. The gallery: `PickMultipleVisualMedia` (the photo picker, no permission). A file: `OpenMultipleDocuments` with `image/*` and `application/pdf` (AC-021-1).
- `Exports`: `shareFile(file, mime)` for Compartilhar from the viewer and for a backup too large for memory (AC-021-5, 10). `PdfPlan` gains `receipts: List<PdfReceipt>` (heading, kind, file name); `AndroidPdf.draw` adds, after the last page, a "Comprovantes" title on the first and one page per image or per PDF page, headed by its payment, the picture fitted inside the margins (a PDF page rendered by `PdfRenderer` at 150 dpi) (AC-021-7). `pdfPages` counts those pages so the footer's "página x de y" stays right.
- `BackupArchive` (new, `java.util.zip`, in `:app` so the domain stays shareable): writes `tutora.json` and `comprovantes/<file>` into a zip in `cache/backups/`, streamed file by file, and reads one back: the JSON entry first (for the preview), the files only on restore (AC-021-10). A picked file starting with `{` is still read as a `.json` backup.

**Use cases (`:app usecases/`)**

- `PaymentDraft` gains `receipts: List<ReceiptRef>`: an existing receipt's id, or a staged file. `Payments.record` and `edit` write the payment and its receipt rows in the same transaction, then commit the staged files; `edit` deletes the rows the teacher took off and, after the transaction, their files (AC-021-1). A payment `record` splits (a student's payment that also pays classes a place owes, AC-008-27) and `recordAll` (a split group, AC-020-40) give each payment they write its own copy of the comprovantes: they prove the same transfer.
- `Payments.delete` returns a `DeletedPayment` (the payment, its chosen classes, its receipt rows, and a package with the classes that used it) and leaves the files; `Payments.restore(deleted)` writes them back in one transaction (AC-021-6). This is new for payments: today Excluir has no Desfazer.
- `Receipts` (new): `attach(paymentId, staged)` from Anexar comprovante, `replace(id, staged)` and `remove(id)` from the viewer, each in one transaction with the file work after it; `sweep()` at start-up deletes files with no row and anything left in `pending/` (a crash, an Excluir not undone, a sheet closed without saving).
- `Backups`: a backup is the zip (`Tutora backup 2026-11-13 18-45.zip`), by hand and automatic; `BackupFolder.write` takes the archive file instead of bytes. Restore reads a zip or a `.json`; the current `comprovantes/` moves aside to `comprovantes-before-restore/` with the current data (AC-011-16), the zip's files land in `comprovantes/`; Desfazer restauração swaps them back; the next successful backup lets the old folder go, as it lets `before-restore.json` go. A row whose file is missing from the zip is left out, and the restore preview says how many (constitution rule 3). A `.json` backup restores with no comprovantes (AC-021-10).

**State and screens (`:app ui/`)**

- `ui/payments/Payments.kt`:
  - `PaymentSheet` and `GroupPaySheet`: a "Comprovante" part under Observação: a row of `Thumbnail`s with their × and an "Adicionar" tile that opens a `BottomSheet` with Tirar foto, Escolher da galeria, Escolher arquivo. Staged files live in `rememberSaveable` (their names), so turning the phone or the lock keeps them (AC-021-1).
  - `PayerHistory`'s payment rows: a paperclip mark, labelled "Com comprovante" for TalkBack, when `receipts > 0` (AC-021-4). Hoje has no list of payments today, so the payer's Pagamentos (student, group and place detail) is where it shows; 022 reuses the same row.
  - `PaymentDetailScreen`: the thumbnails under the note; tapping one opens the viewer at it (AC-021-4, 5). Excluir, confirmed as now, then an `UndoBar` "Pagamento excluído" with Desfazer on the payer's detail, held in `AppServices` as the last `DeletedPayment` until it times out (AC-021-6).
- `ui/receipts/ReceiptViewer.kt` (new): a full-screen page, the payment's comprovantes one after another ("2 de 5"); an image in `ZoomImage` (pinch and double tap), a PDF as its pages one under the other; the header's Compartilhar, Substituir and Remover, Remover after a confirmation sheet (AC-021-5).
- `ui/receipts/AttachReceipt.kt` (new): "Anexar comprovante": `ListGroup` of the payments `attachable` returns, each payer, date, method and amount, those that already have one with the mark; tapping one attaches and opens its detail. "Registrar novo pagamento" asks for the payer in a `SearchField` of students, groups and places, then opens `PaymentSheet` with the file already in its Comprovante part (AC-021-3).
- `ui/reports/Sending.kt`: `Switch` "Incluir comprovantes" in both the Fatura and the Relatório detalhado sheets, off at first, shown only when a payment in the document has one (AC-021-7). `InvoiceDocs.kt` and `ReportDocs.kt` put the receipts on the plan, and a "Comprovante" column (Sim / Não) on the Pagamentos sheet of the report's Excel (AC-010-10) and the detailed Excel (AC-020-19), and of Backup e planilha's spreadsheet (AC-011-19) for the same reason (AC-021-8). Text never changes (AC-021-9).
- `ui/backup/Backup.kt`: under Backup completo, "12 comprovantes · 8,4 MB" from the receipt Flow, hidden when there are none; the restore preview counts the comprovantes (AC-021-10, 12).

**Design system (`:core:designsystem`, before the screens)**

- `Thumbnail` (new): a rounded tile showing an `ImageBitmap` or, while it loads or for a PDF, the file icon with "PDF"; an optional × (touch size) to take it off; an "Adicionar" variant with the plus. Guideline in `docs/design-system/components/Thumbnail/README.md`, gallery entry.
- `ZoomImage` (new): an image that fills its box and zooms with pinch and double tap, panning inside its bounds, for the viewer. Guideline and gallery entry.
- `ListRow` gains `mark: Pair<IconName, String>?`, a small icon before the chevron with its spoken label. `IconName` gains `PAPERCLIP`, `CAMERA`, `IMAGE` and `SHARE` (line icons drawn like the others).
- 47 → 49 components; CLAUDE.md and the design-system README say so.

## Data

| Change | Migration | Default / backfill |
| --- | --- | --- |
| New table `payment_receipt` (`id` TEXT PK, `payment_id` TEXT NOT NULL → `payment.id` ON DELETE CASCADE, `kind` TEXT NOT NULL, `mime` TEXT NOT NULL, `file` TEXT NOT NULL, `bytes` INTEGER NOT NULL, `position` INTEGER NOT NULL, `created_at`, `updated_at`), index on `payment_id` | `MIGRATION_9_10` (CREATE TABLE and INDEX); `TutoraDatabase.VERSION = 10`, schema `10.json` exported | empty: no payment has a comprovante yet |
| Backup: a version-9 file has no `payment_receipt` | `backupUpgrades[9] = { t -> t }` | no comprovantes |
| Backup file: `.zip` with `tutora.json` and `comprovantes/` | none (file format) | `.json` still restores |

No view changes. The cascade means removing a payment, or its package, removes its rows; the files go in the sweep, so Desfazer can bring both back (AC-021-6).

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| Payments.record / recordAll / edit (exist, 008, 020) | payment, payment_lesson, payment_receipt | ReceiptFiles.commit / delete after the transaction | payment and receipt Flows (Room) |
| Payments.delete (exists) | removes payment (rows cascade); returns `DeletedPayment` | none (files stay until the sweep) | payment Flows |
| Payments.restore (new) | payment, payment_lesson, payment_receipt, package and its classes' `package_id` / `due_cents` | none | payment and lesson Flows |
| Receipts.attach / replace / remove (new) | payment_receipt | ReceiptFiles.commit / delete | receipt Flows |
| Receipts.sweep (new, start-up) | none | ReceiptFiles.delete | none |
| Backups.makeBackup / runAuto / saveToFolder (exist) | settings (backup log) | BackupArchive, BackupFolder, Exports.shareFile | backup Flow |
| Backups.restore / undoRestore (exist) | every table | BackupArchive, file moves | all (Room) |

Sharing a comprovante or a document writes nothing and stays in the screen layer through `LocalExports`, as 010 does.

## Screens and components

| Screen | Components |
| --- | --- |
| Registrar / Editar pagamento, the split group's sheet | Thumbnail (new), BottomSheet, ListRow (Tirar foto, Escolher da galeria, Escolher arquivo), AlertBanner (a refused file) |
| The payer's Pagamentos | ListRow with `mark` (new parameter) |
| Pagamento (detail) | Thumbnail, UndoBar on the payer's detail after Excluir |
| Comprovante (viewer, new) | ScreenScaffold, ZoomImage (new), HeaderAction (Compartilhar, Substituir, Remover), BottomSheet (Remover?) |
| Anexar comprovante (new) | ScreenHeader, ListGroup, ListRow, SearchField, Button, EmptyState ("Nenhum pagamento nos últimos 60 dias") |
| Fatura and Relatório detalhado sheets | Switch |
| Backup e planilha | the existing text line under Backup completo |

Sideways, the viewer uses the whole screen and the other pages keep the readable width (AC-017-4).

## Test strategy

Lean, as the project's memory says: one focused test per criterion, JVM only, full `./gradlew check` once at the end. Bitmaps and `PdfRenderer` stay behind `ReceiptFiles`, whose fake returns plain files and small bitmaps, so no test needs a real decoder.

| Level | File | Criteria |
| --- | --- | --- |
| Domain | `core/domain/.../ReceiptRulesTest.kt` | AC-021-2 (keep, shrink to 2000 px with the aspect kept, PDF over 10 MB refused, other kinds refused), AC-021-3 (60 days, newest first), AC-021-12 (usage numbers) |
| Data | `core/data/.../MigrationsTest.kt` | AC-021-10 (9→10 keeps every row; the table is empty) |
| Domain | `core/domain/.../BackupRulesTest.kt` | AC-021-10 (a version-9 backup upgrades with no comprovantes) |
| App, use cases | `app/.../ReceiptsTest.kt` (new) | AC-021-1 (record with five, edit adds one and takes one off), AC-021-2 (the kept copy survives the original's deletion), AC-021-6 (Excluir drops rows and keeps files; restore brings both back; the sweep removes files of a payment not restored) |
| App, use cases | `app/.../BackupTest.kt` | AC-021-10 (zip round trip brings rows and files back; a `.json` backup restores with none; Desfazer restauração brings the earlier files back) |
| App, pure | `app/.../InvoiceDocsTest.kt` | AC-021-7 (plans carry each receipt with its heading only when asked), AC-021-8 (Comprovante column, Sim / Não), AC-021-9 (text identical with and without), AC-021-13 (headings in both languages) |
| App, pure | `app/.../PdfLayoutTest.kt` | AC-021-7 (one page per image and per PDF page, footer counts them) |
| Screens | `app/.../PaymentScreensTest.kt` | AC-021-1 (Comprovante part and its three choices), AC-021-4 (mark on the row, thumbnails on the detail), AC-021-5 (viewer: Compartilhar, Substituir, Remover asks first), AC-021-6 (Desfazer after Excluir) |
| Screens | `app/.../ReceiptScreensTest.kt` (new) | AC-021-3 (Anexar comprovante list, Registrar novo pagamento opens the sheet with the file) |
| Screens | `app/.../ReportScreensTest.kt` | AC-021-7 (Incluir comprovantes off at first, only when a payment has one, on both sheets) |
| Screens | `app/.../BackupScreensTest.kt` | AC-021-12 (the usage line) |
| Screens | `app/.../AppLockTest.kt` | AC-021-3, AC-021-11 (a share request while locked shows the lock screen; the viewer behind it) |
| Source | `app/.../SourceChecksTest.kt` | AC-021-11 (no INTERNET permission; `file_paths.xml` exposes only cache folders) |
| Design system | `core/designsystem/.../DesignSystemTest.kt` | Thumbnail and ZoomImage in the gallery (AC-021-4, 5) |

Every new string in `values/` and `values-pt-rBR/`, which lint checks (AC-021-13).

Device checks for the PR (pending, not run unless Victor asks): Compartilhar › Tutora from a real bank app (Nubank, Itaú or Inter) and from WhatsApp, locked and unlocked; Tirar foto with the system camera; a 4000 px photo and a 9 MB bank PDF attached; a Relatório detalhado with five comprovantes opened in a PDF viewer and WhatsApp; a backup with comprovantes restored on a clean install; the zip read from Google Drive's folder.

## Risks

- **Backup size.** Photos can make the backup tens of megabytes. Mitigation: images capped at 2000 px; the zip is streamed to a file, never held in memory; Ajustes shows the space (AC-021-12). Recentes keeps its own copy of a shared backup (AC-011-11), so that copy doubles the space once; accepted, it is replaced by the next one.
- **PDF size of the sent document.** `PdfDocument` embeds a bitmap per comprovante page; a rendered PDF page at 150 dpi is about 1240 × 1754. Mitigation: render at 150 dpi, images drawn at their kept size; check a five-comprovante document's size on a device.
- **PDFs Android can't open.** `PdfRenderer` refuses password-protected or broken PDFs. Mitigation: they are refused when picked ("Este PDF não pode ser aberto"), never after.
- **Share target and the lock.** A shared file must not show anything before the unlock. Mitigation: staging only copies bytes; the screen it opens is under `LockGate`, tested.
- **Automatic backup to a folder** now writes a `.zip`; the folder's pruning keeps the newest N by name, `.json` files written before 021 included.
- **Stacked on #23.** If 020 changes before it merges, rebase `spec/021-receipts` on it.
