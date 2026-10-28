# Tasks 010: Reports

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain: `ReportRange`, `SubjectReport` (totals before commission, charged cancellations, bonus, salary days, a place's classes with whose), `paginate`. Test: `core/domain/src/test/kotlin/app/tutora/domain/ReportRulesTest.kt`. (AC-010-3, 4, 5, 6, 7, 9)
- [x] 2. Data: `ReportsRepository.subject` and `earnings`; settings `teacherName`, `pixKey`. Test: covered by the domain and screen tests (no new rule here).
- [x] 3. Adapters: `XlsxWriter` (zip, sheets, numbers, dates). Test: `app/src/test/kotlin/app/tutora/XlsxWriterTest.kt`. (AC-010-10, 15)
- [x] 4. Adapters: `PdfWriter` with its page plan (`AndroidPdfWriter` draws it), `Sharer` with a `FileProvider`, `Clipboard`; fakes for tests. Test: `app/src/test/kotlin/app/tutora/PdfLayoutTest.kt`. (AC-010-9, 14)
- [x] 5. Report builders (`subjectPlan`, `subjectSheets`, `subjectText`, `earningsPlan`, `earningsSheets`) over `ReportWords`, and the export row sharing or copying through `LocalExports`. Test: `ReportScreensTest.kt`. (AC-010-11, 12)
- [x] 6. Screens: `ReportScreen` with subject, period, summary, list and export row; Relatório on student, group and place details. Test: `app/src/test/kotlin/app/tutora/ReportScreensTest.kt`. (AC-010-1, 2, 8)
- [x] 7. Screens: Ganhos' Relatório sheet. Test: `ReportScreensTest.kt`. (AC-010-13)
- [x] 8. Screens: Ajustes › Relatórios (Seu nome, Chave Pix). Test: `ReportScreensTest.kt`. (AC-010-16)
- [x] 9. Copy in both languages, English test, version 1.10.0. Test: `ReportScreensTest.kt`. (AC-010-17)
- [x] 10. Update `docs/architecture.md` (the XLSX spike is settled: own writer), run `./gradlew check` once, mark the spec done.

## Pending on a device

- Open a shared PDF in a viewer and an XLSX in Google Sheets and LibreOffice.
- A 12-month report: page count, repeated table header, nothing clipped.
- Large-text screenshots of the report screen at 360 px.
