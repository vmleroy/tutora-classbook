# Tasks 021: Comprovantes

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass. Branch `spec/021-receipts` from `main`.

Paths: domain tests in `core/domain/src/test/kotlin/app/tutora/domain/`, data tests in `core/data/src/test/kotlin/app/tutora/data/`, design-system tests in `core/designsystem/src/test/kotlin/app/tutora/designsystem/`, app tests in `app/src/test/kotlin/app/tutora/`.

### Mockups

- [x] 1. `docs/mockups/PaymentSheet.dc.html` (Comprovante part), `PaymentDetail.dc.html` (thumbnails), `Receipt.dc.html` (viewer and Anexar comprovante), in both languages' copy; Victor looks before task 9. Test: none (docs). (AC-021-1, AC-021-3, AC-021-4, AC-021-5)

### Data and domain

- [x] 2. `payment_receipt` table, `ReceiptRow`, `ReceiptDao`, `MIGRATION_9_10`, VERSION 10, schema `10.json`; backup upgrade step 9; `PaymentRecord.receipts` and `ReportPayment.id` / `receipts` from a count. Test: `MigrationsTest.kt`, `BackupRulesTest.kt`. (AC-021-10)
- [x] 3. `Receipts.kt`: `receiptPolicy`, `receiptUsage`, `attachable`. Test: `ReceiptRulesTest.kt`. (AC-021-2, AC-021-3, AC-021-12)

### Files and use cases

- [x] 4. `ReceiptFiles` interface, its fake, `AndroidReceiptFiles` (stage, shrink, turn upright, refuse a PDF that won't open); `ReceiptStore`; `cache-path` for the camera; `Ctx.receiptFiles`. Test: `ReceiptsTest.kt`. (AC-021-2)
- [x] 5. `PaymentDraft.receipts`; `record`, `recordAll` and `edit` write receipt rows and commit or delete files; `Receipts.attach`, `replace`, `remove`, `sweep` (at start-up). Test: `ReceiptsTest.kt`. (AC-021-1, AC-021-2)
- [x] 6. `Payments.delete` returns `DeletedPayment`; `Payments.restore`. Test: `ReceiptsTest.kt`. (AC-021-6)
- [x] 7. `BackupArchive` (zip with `tutora.json` and `comprovantes/`); `Backups` writes and restores it, `.json` still read, the comprovantes folder aside for Desfazer restauração, missing files counted (`receiptsIn`); "Backup completo (.zip)". Test: `BackupTest.kt`. (AC-021-10)

### Design system

- [x] 8. `Thumbnail` and `ZoomImage` with guidelines and gallery entries; `ListRow.mark`; icons PAPERCLIP, CAMERA, IMAGE, SHARE; 49 components in CLAUDE.md and the design-system README. Test: `DesignSystemTest.kt`. (AC-021-4, AC-021-5)

### Screens

- [x] 9. Comprovante part in `PaymentSheet` and `GroupPaySheet`: thumbnails with ×, Adicionar with Tirar foto, Escolher da galeria, Escolher arquivo; a refused file explained. Test: `PaymentScreensTest.kt`. (AC-021-1, AC-021-2)
- [x] 10. The mark on the payer's payment rows; thumbnails on the payment detail. Test: `PaymentScreensTest.kt`. (AC-021-4)
- [x] 11. `ReceiptViewer`: one comprovante after another, zoomable image, PDF pages (thumbnails and page rendering in `AndroidReceiptFiles`); Compartilhar, Substituir, Remover with its confirmation. Test: `PaymentScreensTest.kt`. (AC-021-5)
- [x] 12. Excluir pagamento then Desfazer on the payer's detail. Test: `PaymentScreensTest.kt`. (AC-021-6)
- [x] 13. Share target: intent filters, staging in `MainActivity`, `ShareRequest` routed under `LockGate`; `AttachReceipt` with the 60-day list and Registrar novo pagamento. Test: `ReceiptScreensTest.kt`, `AppLockTest.kt`. (AC-021-3, AC-021-11)

### Documents

- [x] 14. `PdfPlan.receipts`, `pdfPages` counts their pages, `AndroidPdf` draws the Comprovantes section. Test: `PdfLayoutTest.kt`. (AC-021-7)
- [x] 15. Incluir comprovantes in the Fatura and Relatório detalhado sheets; plans carry the receipts when on; Comprovante column on the Excel files; text unchanged. Test: `ReportScreensTest.kt`, `InvoiceDocsTest.kt`. (AC-021-7, AC-021-8, AC-021-9)

### Settings, privacy, language

- [x] 16. "12 comprovantes · 8,4 MB" in Backup e planilha; the restore preview's count. Test: `BackupScreensTest.kt`. (AC-021-12)
- [x] 17. No INTERNET permission and only cache folders in `file_paths.xml`; the viewer behind the lock. Test: `SourceChecksTest.kt`, `AppLockTest.kt`. (AC-021-11)
- [x] 18. Every new string in `values/` and `values-pt-rBR/`; headings checked in both languages; full `./gradlew check`. Test: `InvoiceDocsTest.kt`, lint. (AC-021-13)

### Pending on a device

Not run (no emulator unless Victor asks); for the PR:

- Compartilhar › Tutora from a real bank app (Nubank, Itaú or Inter) and from WhatsApp, with the lock on and off.
- Tirar foto with the system camera; Escolher da galeria with the photo picker; a bank PDF from Escolher arquivo.
- A 4000 px photo comes in at 2000 px and upright; a 9 MB bank PDF is kept; a PDF with a password is refused.
- The viewer: pinch and double tap on an image, a two-page PDF page by page.
- A Relatório detalhado and a Fatura with five comprovantes opened in a PDF viewer and in WhatsApp (pages, headings, file size).
- A backup with comprovantes restored on a clean install; the `.zip` read back from the Google Drive folder; a backup of tens of MB on a mid-range phone.
- Large text, sideways, and the four themes light and dark on the sheet, the detail and the viewer.
