# Tasks 021: Comprovantes

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass. Branch `spec/021-receipts` from `spec/020-sending-reports`.

Paths: domain tests in `core/domain/src/test/kotlin/app/tutora/domain/`, data tests in `core/data/src/test/kotlin/app/tutora/data/`, design-system tests in `core/designsystem/src/test/kotlin/app/tutora/designsystem/`, app tests in `app/src/test/kotlin/app/tutora/`.

### Mockups

- [ ] 1. `docs/mockups/PaymentSheet.dc.html` (Comprovante part), `PaymentDetail.dc.html` (thumbnails), `Receipt.dc.html` (viewer and Anexar comprovante), in both languages' copy; Victor looks before task 9. Test: none (docs). (AC-021-1, AC-021-3, AC-021-4, AC-021-5)

### Data and domain

- [ ] 2. `payment_receipt` table, `ReceiptRow`, `ReceiptDao`, `MIGRATION_9_10`, VERSION 10, schema `10.json`; backup upgrade step 9; `PaymentRecord.receipts` and `ReportPayment.id` / `receipts` from a count. Test: `MigrationsTest.kt`, `BackupRulesTest.kt`. (AC-021-10)
- [ ] 3. `Receipts.kt`: `receiptPolicy`, `receiptHeading`, `receiptUsage`, `attachable`. Test: `ReceiptRulesTest.kt`. (AC-021-2, AC-021-3, AC-021-12)

### Files and use cases

- [ ] 4. `ReceiptFiles` interface, its fake, `AndroidReceiptFiles` (stage, shrink, count PDF pages, commit, delete, thumbnail, pages); `cache-path` for the camera; `Ctx.receiptFiles`. Test: `ReceiptsTest.kt`. (AC-021-2)
- [ ] 5. `PaymentDraft.receipts`; `record`, `recordAll` and `edit` write receipt rows and commit or delete files; `Receipts.attach`, `replace`, `remove`, `sweep` (at start-up). Test: `ReceiptsTest.kt`. (AC-021-1, AC-021-2)
- [ ] 6. `Payments.delete` returns `DeletedPayment`; `Payments.restore`. Test: `ReceiptsTest.kt`. (AC-021-6)
- [ ] 7. `BackupArchive` (zip with `tutora.json` and `comprovantes/`, streamed); `Backups` writes and restores it, `.json` still read, the comprovantes folder aside for Desfazer restauração, missing files counted; `BackupFolder.write` takes a file; `Exports.shareFile`. Test: `BackupTest.kt`. (AC-021-10)

### Design system

- [ ] 8. `Thumbnail` and `ZoomImage` with guidelines and gallery entries; `ListRow.mark`; icons PAPERCLIP, CAMERA, IMAGE, SHARE; 49 components in CLAUDE.md and the design-system README. Test: `DesignSystemTest.kt`. (AC-021-4, AC-021-5)

### Screens

- [ ] 9. Comprovante part in `PaymentSheet` and `GroupPaySheet`: thumbnails with ×, Adicionar with Tirar foto, Escolher da galeria, Escolher arquivo; a refused file explained. Test: `PaymentScreensTest.kt`. (AC-021-1, AC-021-2)
- [ ] 10. The mark on the payer's payment rows; thumbnails on the payment detail. Test: `PaymentScreensTest.kt`. (AC-021-4)
- [ ] 11. `ReceiptViewer`: one comprovante after another, zoomable image, PDF pages; Compartilhar, Substituir, Remover with its confirmation. Test: `PaymentScreensTest.kt`. (AC-021-5)
- [ ] 12. Excluir pagamento then Desfazer on the payer's detail. Test: `PaymentScreensTest.kt`. (AC-021-6)
- [ ] 13. Share target: intent filters, staging in `MainActivity`, `ShareRequest` routed under `LockGate`; `AttachReceipt` with the 60-day list and Registrar novo pagamento. Test: `ReceiptScreensTest.kt`, `AppLockTest.kt`. (AC-021-3, AC-021-11)

### Documents

- [ ] 14. `PdfPlan.receipts`, `pdfPages` counts their pages, `AndroidPdf` draws the Comprovantes section. Test: `PdfLayoutTest.kt`. (AC-021-7)
- [ ] 15. Incluir comprovantes in the Fatura and Relatório detalhado sheets; plans carry the receipts when on; Comprovante column on the Excel files; text unchanged. Test: `ReportScreensTest.kt`, `InvoiceDocsTest.kt`. (AC-021-7, AC-021-8, AC-021-9)

### Settings, privacy, language

- [ ] 16. "12 comprovantes · 8,4 MB" in Backup e planilha; the restore preview's count. Test: `BackupScreensTest.kt`. (AC-021-12)
- [ ] 17. No INTERNET permission and only cache folders in `file_paths.xml`; the viewer behind the lock. Test: `SourceChecksTest.kt`, `AppLockTest.kt`. (AC-021-11)
- [ ] 18. Every new string in `values/` and `values-pt-rBR/`; headings checked in both languages; full `./gradlew check`. Test: `InvoiceDocsTest.kt`, lint. (AC-021-13)
