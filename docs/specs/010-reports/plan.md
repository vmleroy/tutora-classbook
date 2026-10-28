# Plan 010: Reports (Kotlin)

Architecture reference: "Feature libraries" (PDF reports with `PdfDocument` and Canvas; XLSX: the spike's fallback, a minimal writer), "Architecture layers" (adapters behind interfaces), "Money and time". Mockups: Report, ReportPdf, Earnings (sheet), EarningsPdf.

## Approach

A report is a plain model built in the domain from the same rows as 008 and 009. Three thin adapters turn it into a PDF, an XLSX or a text, and a fourth hands the file to the share sheet. The model, the text, the page breaks and the XLSX content are tested on the JVM; only the drawing touches Android.

- **Domain** (`core/domain/Reports.kt`).
  - `ReportRange` is `THIS_MONTH`, `LAST_MONTH`, `THREE_MONTHS` or `CUSTOM(from, to)`, with `dates(today)`. A custom range keeps De ≤ Até ≤ today (AC-010-3).
  - `SubjectReport(subject, chargeLine, classes: List<ReportClass>, payments: List<ReportPayment>, totals)`.
    - A `ReportClass` has its day, minutes, value (what it costs, before commission), who it was, a charged-cancellation flag, the bonus part, and paid / left from the ledger allocation (AC-010-4, 5, 7).
    - A school's report has `days: List<TaughtDay(day, classes)>`. Its total is the salary × months, counted as received (AC-010-6).
  - `EarningsReport(view: EarningsView, rows by source with classes and hours, classes, payments, parts)`, built from 009's `EarningsView` so the numbers match Ganhos (AC-010-14).
  - The PDF plans, Excel sheets and the WhatsApp text are built in `app/ui/reports/ReportDocs.kt` from a `ReportWords` bundle the screen fills from resources; they are plain functions, tested on the JVM (AC-010-9, 10, 12, 14, 15).
  - `paginate(rowCount, firstPageRows, otherPageRows)` gives the page breaks, with the table header repeated (AC-010-9).
- **Data** (`core/data/ReportsData.kt`).
  - `ReportsRepository.subject(kind, id, from, to, zone)`: the subject's done and charged classes in the range (`EarnedRow` from 009, filtered by payer or by place) and its ledger allocation for paid / left. Payments come by date, with the classes each paid.
  - `earnings(period, filter, parts)` reuses `EarningsRepository`.
- **Adapters** (`app/adapters/export/`), each an interface with an Android implementation and a fake for tests:
  - `PdfWriter`: `AndroidPdfWriter` draws A4 portrait pages with `PdfDocument` and Canvas. Colors come from the active theme's resolved palette (the same `TutoraTokens` values the app uses, never raw hex). Text is Nunito, with the header in `primary`, Pago in `success` and A receber in `danger` (AC-010-9, 14).
  - `XlsxWriter`: a minimal OOXML writer in plain Kotlin over `java.util.zip`.
    - It writes `[Content_Types].xml`, the rels, the workbook, one sheet per table and a styles part with a date format and a money format.
    - Dates are serial numbers, amounts are numbers in reais (cents ÷ 100 written exactly as a decimal string), text is inline strings.
    - About 150 lines, no dependency (AC-010-10, 15).
  - `Sharer`: writes the file to `cacheDir/reports/` and fires `ACTION_SEND` through a `FileProvider` declared in the manifest with `res/xml/file_paths.xml`. The file name is "Ana Souza - outubro 2026.pdf" (AC-010-11).
  - `Clipboard`: `ClipboardManager` (AC-010-12).
  - Wiring: the adapters are one `Exports` interface reached through `LocalExports`, provided by the app (`AndroidExports`) and by the test harness (`FakeExports`, which keeps the bytes and the text). `AppServices` gets `reportsRepo`.
- **Use cases:** none write data. Exporting is a read plus the adapters, called from the report screen and the Ganhos sheet.
- **Screens.**
  - `ReportScreen` (route `Report(kind, id)`):
    - `SegmentedControl` Aluno / Turma / Local, then `Chip`s for the subjects.
    - The range `Chip`s and the De / Até `DateField`s.
    - A summary `Card` with the name line, "6 aulas · 6h30", and Total / Pago / A receber via `Money`.
    - "Aulas no período" or "Dias de aula…" as a `ListGroup`.
    - The export row: PDF and Excel `Button`s side by side, and "Copiar como texto" (GHOST) (AC-010-1 to 8).
  - **Relatório on the details:** an `IconButton` (FILE) in the student, group and place headers, beside the pencil.
  - **Ganhos:** a Relatório header action opens a `BottomSheet`. It has the subtitle line, PDF / Excel choice cards (`Chip`s with subtitles), three `Checkbox`es, and Cancelar / "Gerar PDF" (AC-010-13).
  - **Ajustes:** a "Relatórios" group with "Seu nome" and "Chave Pix" `TextField`s, saved on change (AC-010-16).
  - No new design-system component is needed.
- **Settings:** `teacherName` and `pixKey` (keys `teacher_name`, `pix_key`), empty by default.
- **Copy** in both languages. Dates and month names come from the app language, and so do the PDF's and the file names (AC-010-17). Version 1.10.0.

## Data

No new tables or columns; two settings keys. Database version 5 stays.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `Exports.subjectPdf` / `subjectXlsx` | none | PdfWriter or XlsxWriter, Sharer | none |
| `Exports.subjectText` | none | Clipboard | none |
| `Exports.earningsPdf` / `earningsXlsx` | none | PdfWriter or XlsxWriter, Sharer | none |
| save name / Pix key | settings | none | settings |

## Test strategy

- **Domain:** `ReportRulesTest`: ranges, subject totals (before commission, salary, place's classes with whose), the class list with charges and bonus, the text, pagination (AC-010-3 to 7, 12).
- **Adapters:** `XlsxWriterTest` unzips the bytes on the JVM and checks the sheets, numbers and dates (AC-010-10, 15). `PdfLayoutTest` checks the page plan and the lines the writer is asked to draw: header, Pix, footer, repeated table header (AC-010-9, 14).
- **Screens:** `ReportScreensTest` under Robolectric with the fake sharer and clipboard: opening from a detail, switching subject, periods, the export row, the Ganhos sheet, the Ajustes fields, and English (AC-010-1, 2, 8, 11, 13, 16, 17).
- Pending on a device: open a shared PDF in a viewer and an XLSX in Google Sheets; a 12-month report's page count and look.

## Risks

- **XLSX compatibility.** A hand-written file can be refused by a strict reader. Keeping to the smallest valid package (inline strings, one styles part) and checking it in Google Sheets and LibreOffice is listed as a device check.
- **PDF text fitting.** Canvas doesn't wrap, so long names are ellipsized to their column width by measuring with the same `Paint`. The layout gets a test with a long name.
- **Fonts in the PDF.** Nunito is loaded from `res/font` into the `Paint`; if loading fails, the system sans keeps the PDF readable.
