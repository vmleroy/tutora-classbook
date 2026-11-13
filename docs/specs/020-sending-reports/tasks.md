# Tasks 020: Fatura and detailed report

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass. Branch `spec/020-sending-reports` from `spec/015-app-lock`.

Paths: domain tests in `core/domain/src/test/kotlin/app/tutora/domain/`, data tests in `core/data/src/test/kotlin/app/tutora/data/`, app tests in `app/src/test/kotlin/app/tutora/`.

### Data and domain

- [x] 1. `student.guardian` column, `MIGRATION_7_8`, VERSION 8, schema `8.json`; backup upgrade step 7; `Student.guardian` in the domain and mapping. Test: `MigrationsTest.kt`, `BackupRulesTest.kt`. (AC-020-17)
- [x] 2. `ReportClass` gains start, travel, charge, place and notes; `observeClasses` returns notes; `ReportsRepository.subject` fills them. Test: `InvoiceRulesTest.kt`. (AC-020-4, AC-020-5)
- [x] 3. `invoiceOf`: period classes oldest first, total, paid, toPay of the period only, settled, a place grouped by student, a salary school by month. Test: `InvoiceRulesTest.kt`. (AC-020-8, AC-020-12, AC-020-13)
- [x] 4. `detailOf`: totals with travel and extra charges, per-student groups and subtotals. Test: `InvoiceRulesTest.kt`. (AC-020-16)
- [x] 5. `splitShares` (property test), `memberReport`, `canSplit`. Test: `InvoiceRulesTest.kt`. (AC-020-21, AC-020-22)
- [x] 6. `invoicesDue` and `ReportsRepository.due`: rows above zero, largest first, total; split groups as member rows. Test: `InvoiceRulesTest.kt`. (AC-020-23, AC-020-26)
- [x] 7. `pixPayload`: BR Code fields, key normalized by kind, name and city cut and unaccented, CRC16; null without name or city. Test: `PixCodeRulesTest.kt`. (AC-020-10, AC-020-11)

### Documents

- [x] 8. `io.nayuki:qrcodegen` in the version catalog; `qrMatrix`; `PdfPlan` gains groups, row notes, due and pix; `AndroidPdf` draws them, QR with quiet zone. Architecture doc: "Pix QR code" row in Feature libraries. Test: `PdfLayoutTest.kt`. (AC-020-10)
- [x] 9. `invoicePlan`: header with the reports' title, Fatura, name, date, Para (with Responsável), period; the class table with Situação; Total do período, Já pago, Valor a pagar or "Pago, obrigado"; Vencimento; Pix block. Test: `InvoiceDocsTest.kt`. (AC-020-3, AC-020-6, AC-020-7, AC-020-9, AC-020-12)
- [x] 10. `invoiceText`: greeting, the classes still to pay, total, due date, Pix and copia e cola; the settled message. Test: `InvoiceDocsTest.kt`. (AC-020-14)
- [x] 11. `detailPlan` and `detailSheets`: start–end, place, travel, charge, situation, notes under the row when on; per-student groups; the Excel columns. Test: `InvoiceDocsTest.kt`. (AC-020-15, AC-020-19)
- [x] 12. Every new string in `values/` and `values-pt-rBR/`; both documents and the text checked in English and Portuguese. Test: `InvoiceDocsTest.kt`. (AC-020-27)

### Screens

- [x] 13. Relatório's Enviar section: Fatura and Relatório detalhado tiles and their sheets, file names by kind, the Pix hint linking to Seus dados. Test: `ReportScreensTest.kt`. (AC-020-1, AC-020-2, AC-020-11)
- [x] 14. Turma inteira / Dividir por aluno in both sheets, the member list with PDF and Texto. Test: `ReportScreensTest.kt`. (AC-020-20, AC-020-21)
- [x] 15. Faturas do mês from Ganhos: period, total, rows with PDF and Texto and the check, Vencimento for all, Dividir turmas por aluno, empty state. Test: `InvoicesScreenTest.kt`. (AC-020-23, AC-020-24, AC-020-25, AC-020-26)
- [x] 16. Responsável on the student's edit form and detail; Cidade in Seus dados. Test: `CatalogTest.kt`, `InvoicesScreenTest.kt` (the detail line), `SettingsPagesTest.kt`. (AC-020-17, AC-020-18)

- [x] 18. Seus dados in three sections with a line each: Você, Pix, Relatórios. Test: `SettingsPagesTest.kt`. (AC-020-28)

- [x] 19. Relatório only for the one it was opened from: no subject switch or name list. Test: `ReportScreensTest.kt`. (AC-020-29)

- [x] 20. The period as one segmented row over De and Até, no Outro…; SegmentedControl leaves every segment off for a value it doesn't list. Test: `ReportScreensTest.kt`. (AC-020-30)

- [x] 21. `PeriodField` in the design system (guideline, gallery, 47 components), used by Relatório, Evolução and the backup spreadsheet. Test: `DesignSystemTest.kt`, `EarningsScreensTest.kt`. (AC-020-31)
- [x] 22. Short fixed chip rows as segmented rows; `MethodControl` for the payment method. Test: `SourceChecksTest.kt`. (AC-020-32)

- [x] 23. Até never past today: `DateField` and `PeriodField` take `latest`; Evolução shows today in Até for quick periods. Test: `EarningsScreensTest.kt`. (AC-020-33)

### Finish

- [x] 17. Product doc: Reports feature and the "Send a monthly report" flow mention Fatura, Relatório detalhado and Faturas do mês; data model lists `student.guardian`. `./gradlew check` green once. Spec status in progress → done at `/verify`.

## Pending on a device

- Scan the Fatura's QR code with two real bank apps: Pix opens with the right key, name and amount.
- The Fatura and the detailed PDF opened in a PDF viewer and in WhatsApp: the QR code, the amount box and a nine-column table read well; a place with many students spills onto a second page.
- The detailed Excel in Google Sheets: times, amounts and dates sum and sort.
- Faturas do mês and the two sheets with the phone sideways and at Muito grande.
