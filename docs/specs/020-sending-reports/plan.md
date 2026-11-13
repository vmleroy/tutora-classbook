# Plan 020: Fatura and detailed report

Architecture reference: docs/architecture.md (Architecture layers, Database schema and migrations, Money and time, Feature libraries, Spec-driven development and testing). Mockups: docs/mockups/Report.dc.html and ReportPdf.dc.html for today's report; the Fatura PDF, the detailed PDF and Faturas do mês follow ReportPdf's look and are drawn from the design system's components (no new mockup file is needed to build them; one can be added for review).

## Branch

Implemented on `spec/020-sending-reports`, branched from `spec/015-app-lock` (PR #22 is open): 020 builds on 015's Seus dados page and its Pix key kinds (`pixKind`, `AppLock.kt`). The PR is based on `spec/015-app-lock` and retargeted to `main` once #22 merges.

## Approach

020 adds two documents on top of 010's report, built from the same report data, and leaves today's report, PDF, Excel and text unchanged (AC-020-1).

**Domain (`:core:domain`, plain Kotlin)**

- `Reports.kt`: `ReportClass` gains `start: LocalTime` (the scheduled start, from `start_at`, which the timer never moves), `travel: Cents`, `charge: Cents` (a cancellation's `fee_cents`), `place: String`, `notes: String?`. `minutes` stays `planned_min`, which ending a class (007, 006) overwrites with the real time, so it already is "time spent in class, real when timed, else planned" (AC-020-4). The scheduled end is `start + minutes`.
- `Invoice.kt` (new):
  - `invoiceOf(report: SubjectReport): Invoice`: the period's classes oldest first, `total`, `paid`, `toPay` (sum of the period's classes' `left`, no credit, no other period; AC-020-8), `settled = toPay == 0` (AC-020-12), and, for a place, the classes grouped by `who` with subtotals (AC-020-13). A salary school keeps its months (AC-010-6).
  - `detailOf(report)`: the totals Aulas, Horas, Aulas (valor), Deslocamento, Cobranças extras, Total, Pago, A receber, with zeros dropped by the caller (AC-020-16), and the per-student groups for a place.
  - `splitShares(cents: Cents, members: List<String>): List<Cents>`: equal shares in whole cents; the leftover cents go one each to the first members by name (sorted with the app's accent- and case-insensitive collation), so the shares sum to `cents` (AC-020-21). Lives beside the money rules in `Money.kt`, with `Math.addExact`.
  - `memberReport(group: SubjectReport, members, member): SubjectReport`: each class's `value` and `left` replaced by the member's share; `canSplit(group, place)`: members ≥ 1 and the group's classes are paid by the students, not a school or a place that pays (AC-020-20, 22).
  - `invoicesDue(rows, from, to): List<DueRow>`: per student, group and place, what is still to pay for the period's classes, those above zero, largest first, with the total (AC-020-23); with `splitGroups` a splittable group becomes one row per member (AC-020-26).
- `PixCode.kt` (new): the Pix "copia e cola" payload (EMV BR Code, static): fields 00, 26 (`br.gov.bcb.pix` + key), 52 `0000`, 53 `986`, 54 amount as `240.00`, 58 `BR`, 59 name, 60 city, 62 txid `***`, 63 CRC16-CCITT (poly 0x1021, init 0xFFFF) over the whole string. The key is normalized by `pixKind` (015): CPF and CNPJ digits only, phone as `+55…`, e-mail lower case, random key as is. Name is cut to 25 and city to 15 characters, accents removed, upper case. `pixPayload(key, name, city, cents): String?` returns null when name or city is empty (AC-020-11). Amount 0 is never asked for (settled Faturas have no Pix block).

**Data (`:core:data`)**

- `EarnedRow` / `observeClasses` gain `notes` and `place_name` is already there; `ReportsRepository.subject` fills the new `ReportClass` fields.
- `ReportsRepository.members(groupId): Flow<List<Member>>` (id, name, guardian) for the split, and `ReportsRepository.due(from, to, zone): Flow<List<DueRow>>` built from the same classes and `leftByLesson()` as `subject`, so Faturas do mês agrees with each Fatura.
- Student: `guardian` column (AC-020-17). Settings: `pix_city` key (AC-020-18).

**State and screens (`:app`)**

- `ui/reports/Report.kt`: under "Exportar este relatório", the "Enviar" section (AC-020-1): two `ChoiceField` tiles, Fatura and Relatório detalhado, each opening a `BottomSheet`:
  - Fatura sheet: `SegmentedControl` Turma inteira / Dividir por aluno when `canSplit` (AC-020-20), `DateField` "Vencimento" (optional, cleared by its ×; AC-020-9), then `Button` PDF and `Button` Copiar como texto. With Dividir por aluno, a `ListGroup` of members, each row with PDF and Texto (AC-020-21).
  - Detalhado sheet: the same split control, `Switch` "Incluir observações" (on; AC-020-15), PDF and Excel.
  - `AlertBanner` "Adicione sua chave Pix em Seus dados" with a link to Seus dados when the key is empty (AC-020-11).
- `ui/reports/InvoiceDocs.kt` (new, beside `ReportDocs.kt`): `invoicePlan`, `invoiceText`, `detailPlan`, `detailSheets`, pure functions from the domain models and `ReportWords`, like 010's `subjectPlan`. Words for both languages come from string resources (AC-020-27).
- `ui/reports/Invoices.kt` (new): Faturas do mês, a screen opened from Ganhos' header beside Relatório and Evolução (an `IconButton` with a new "Faturas do mês" label). `ScreenHeader`, the period of Ganhos (passed in), a `DateField` Vencimento for all (AC-020-25), a `Switch` "Dividir turmas por aluno" (AC-020-26), a `SummaryCard` with the total, and a `ListGroup` of `ListRow`s with the amount and two small `Button`s, PDF and Texto, and a check `Icon` once used (kept in `rememberSaveable`, gone on close; AC-020-24).
- `ui/catalog/Students.kt`: "Responsável" `TextField` on the edit form, shown under the name on the detail (AC-020-17). `SettingsScreen.kt`: "Cidade" `TextField` in Seus dados with "Usada no QR code do Pix" (AC-020-18).
- No new design-system component: every piece above exists (46 components).

**Adapters**

- `adapters/export/Exports.kt`: `PdfPlan` gains optional `groups` (a heading and subtotal row between table rows, for places and split groups), `notes` under a row (`PdfRow.note`), `due: Pair<String, String>?` (Valor a pagar or "Pago, obrigado", with the Vencimento), and `pix: PdfPix?` (key, name, payload, QR matrix). `AndroidPdf.draw` draws them; a QR is drawn as filled squares on the Canvas, 4 modules of quiet zone, about 35 mm wide.
- `adapters/export/Qr.kt` (new): `qrMatrix(payload): List<BooleanArray>` with Project Nayuki's QR Code generator (`io.nayuki:qrcodegen`, MIT, pure Java, no transitive dependencies, offline), error correction M. Added to `gradle/libs.versions.toml` and to the architecture doc's Feature libraries table ("Pix QR code").
- File names: `fileName` takes a kind ("fatura", "relatório detalhado" / "invoice", "detailed report") (AC-020-2).

## Data

| Change | Migration | Default / backfill |
| --- | --- | --- |
| `student.guardian TEXT` (nullable) | `MIGRATION_7_8`, `ALTER TABLE student ADD COLUMN guardian TEXT`; `TutoraDatabase.VERSION = 8`, schema `8.json` exported | null for every student |
| Backup: the `student` table may lack `guardian` | `backupUpgrades[7] = { t -> t }` (a missing nullable column reads as null), as 5→6 | null |
| `settings` key `pix_city` | none (key–value table) | empty |

No view or aggregate changes; the documents reuse `observeClasses`, `leftByLesson` and the ledgers of 008–010.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| SaveStudent (exists, 004) | `student.guardian` | calendar (unchanged) | student flows (Room) |
| SaveSettings (exists) | `pix_city` | none | settings flow |
| ShareInvoicePdf / ShareDetailPdf / ShareDetailXlsx | none (read only) | Exports.pdf / share | none |
| CopyInvoiceText | none | Exports.copy | none |

Sharing writes nothing, so it stays in the screen layer through `LocalExports`, as 010 does; no new use case.

## Screens and components

| Screen | Components |
| --- | --- |
| Relatório (010), Enviar section and its two sheets | ChoiceField, BottomSheet, SegmentedControl, DateField, Switch, Button, ListGroup, ListRow, AlertBanner |
| Faturas do mês (new, from Ganhos) | ScreenHeader, DateField, Switch, SummaryCard, ListGroup, ListRow, Button, Icon, EmptyState ("Ninguém deve nada neste período") |
| Student edit and detail | TextField; the detail's existing header line |
| Seus dados | TextField |

Sideways, Faturas do mês and the sheets keep the readable width (AC-017-4) through the existing scaffold.

## Test strategy

Lean, as the project's memory says: one focused test per criterion, JVM only, full `./gradlew check` once at the end.

| Level | File | Criteria |
| --- | --- | --- |
| Domain | `core/domain/.../InvoiceRulesTest.kt` | AC-020-4, 5, 8, 12, 13, 16, 21 (property test: shares always sum to the class value and differ by at most one cent), 22, 23, 26 |
| Domain | `core/domain/.../PixCodeRulesTest.kt` | AC-020-10 (payload for a known key, name, city and amount matches the Banco Central's published example, CRC included), AC-020-11 (null without name or city) |
| Data | `core/data/.../MigrationsTest.kt` | AC-020-17 (7→8 keeps rows, guardian null) |
| Domain | `core/domain/.../BackupRulesTest.kt` | AC-020-17 (a version-7 backup upgrades and restores) |
| App, pure | `app/.../InvoiceDocsTest.kt` | AC-020-3, 6, 7, 9, 14, 15, 19, 27 (plans, text and sheets from fixtures in both languages) |
| App, pure | `app/.../PdfLayoutTest.kt` | AC-020-10 (the drawn QR has the payload's matrix size and quiet zone) |
| Screens | `app/.../ReportScreensTest.kt` | AC-020-1, 2, 11 (hint), 20 |
| Screens | `app/.../InvoicesScreenTest.kt` | AC-020-24, 25 |
| Screens | `app/.../CatalogTest.kt`, `SettingsPagesTest.kt` | AC-020-17 (field and detail line), AC-020-18 |

Device checks for the PR (pending, not run unless Victor asks): scanning the Fatura's QR code with two real bank apps (Pix opens with the right key, name and amount); the PDFs opened in a PDF viewer and WhatsApp; the detailed Excel in Google Sheets.

## Risks

- **Pix payload rejected by a bank.** Banks validate the CRC, field lengths and the key format strictly. Mitigation: test against the Banco Central's published static example, normalize keys by kind, strip accents from name and city; the scan by real bank apps is a pending device check.
- **A new library.** `qrcodegen` is small, stable and pure Java; the alternative is a hand-written QR encoder (Reed–Solomon, masking), several hundred lines for no gain. It stays in `:app` (adapters), so the domain keeps no dependency.
- **Split cents.** Equal split with leftover cents keeps every share exact; a member who joined mid-month still gets an equal share of earlier classes (no membership history). Accepted in the spec's assumptions.
- **Long Faturas.** A place with many students may pass one page; the Fatura then paginates like 010 (the page header repeats), and the Pix block stays on the last page.
- **Faturas do mês cost.** One combined flow over the period's classes and `leftByLesson`, not one query per payer.
- **Stacked on #22.** If 015 changes before it merges, rebase `spec/020-sending-reports` on it.
