# Spec 010: Reports

Status: done
Depends on: 009 Earnings (earned, received, to receive, the 6-month trend, Ganhos' period and type), 008 Payments and packages (balances, payments, which classes they paid), 004 Places and students (school salary)
Mockups: docs/mockups/Report.dc.html (report for one student, group or place), ReportPdf.dc.html (its PDF), Earnings.dc.html (the Relatório sheet on Ganhos), EarningsPdf.dc.html (the earnings PDF).

## Goal

The teacher can send someone what they owe and what they took: pick a student, a group or a place, a period, check the totals, and share it as a PDF, an Excel sheet or a short text for WhatsApp. From Ganhos they can export their whole earnings for the period on screen. The PDF carries the teacher's name and Pix key, so the student or the salinha can pay right away. Everything is made on the phone, with no network.

## User stories

- **US-1.** As a teacher, at month end I open Ana, tap Relatório, and send her a PDF with her six classes, what she paid and the R$ 240 still owed, with my Pix key.
- **US-2.** As a teacher, I send the salinha a report of the classes I gave there this month, so they pay me.
- **US-3.** As a teacher, I paste a short summary into WhatsApp instead of a PDF.
- **US-4.** As a teacher, I export October's earnings to Excel to keep my own accounts.

## Acceptance criteria

### Opening a report

- **AC-010-1.** The detail of a student, a group and a place has Relatório. It opens the report for that one, for this month.
- **AC-010-2.** The report has Aluno, Turma and Local at the top, then a list to pick which one; switching keeps the period.
- **AC-010-3.** The period is Este mês, Mês passado, 3 meses or Outro…, with De and Até dates shown. Outro… lets both dates be picked, Até never before De and never after today.

### What it shows

- **AC-010-4.** The report shows the name and how they are charged ("Ana Souza · Matemática · R$ 120,00/h"), then "6 aulas · 6h30", and Total, Pago and A receber for the classes in the period. Total is what the classes cost the student, group or place, not what the teacher keeps after a commission. Hours follow Durações em HH:MM (AC-009-16).
- **AC-010-5.** Under it, every class in the period: date, duration, value and Pago or A receber, newest first. A cancelled or missed class with a charge shows as "Cancelada · cobrada", and one without a charge isn't listed.
- **AC-010-6.** For a school paid by salary, the list is the days taught with how many classes each ("23/10 · 4 aulas"), titled "Dias de aula no período (salário mensal)". Total is the salary for each month in the period (AC-009-5), counted as received.
- **AC-010-7.** A place that doesn't pay the teacher lists the classes given there with whose each one was ("22/10 · Ana Souza"), so it works as a record of the work done there.

### Sharing

- **AC-010-8.** "Exportar este relatório" has PDF and Excel side by side, and "Copiar como texto".
- **AC-010-9.** The PDF has a header in the theme's main color with "Relatório de aulas", the teacher's name and the period. Then the student, group or place, the totals, and the table of classes with a total line. Payments received in the period follow, with method and the classes they paid, then the amount still pending, the Pix key, and at the foot "Gerado pela Tutora em 26/10/2026, 18:45 · página 1 de 2". A long period continues on more pages with the table header repeated.
- **AC-010-10.** The Excel file has a sheet "Aulas" (date, duration in minutes, place, value, situation) and a sheet "Pagamentos" (date, method, amount). Dates are dates and amounts are numbers in reais, so the spreadsheet can sum them.
- **AC-010-11.** PDF and Excel open the phone's share sheet with the file, named after the subject and period ("Ana Souza - outubro 2026.pdf").
- **AC-010-12.** "Copiar como texto" copies a short message: the name, the period, "6 aulas · R$ 780,00 · pago R$ 540,00 · a receber R$ 240,00", the dates still owed, and the Pix key when set. "Copiado" confirms it.

### From Ganhos

- **AC-010-13.** Ganhos has Relatório. It opens a sheet titled "Relatório de ganhos" with the period and type in use ("Outubro 2026 · Tudo · usa os filtros da tela"), PDF or Excel, and what to include: "Ganhos por aluno e por local" (on at first), "Lista de aulas" (on), "Pagamentos recebidos" (off). "Gerar PDF" or "Gerar Excel" shares the file.
- **AC-010-14.** The earnings PDF has the totals (classes, hours, Ganho, Recebido, A receber), the table by student, group, place and school (type, classes, hours, earned, to receive) with a total line, the six-month trend, and then the parts chosen. The numbers are those of Ganhos for the same period and type.
- **AC-010-15.** The earnings Excel has one sheet for each part chosen, plus "Resumo" with the totals and the six months.

### Ajustes

- **AC-010-16.** Ajustes starts with "Seus dados", which opens its own page with "Seu nome" and "Chave Pix", both optional. The PDF and the copied text show them when set and leave those lines out when not. *(28 Oct 2026: on its own page at the top of Ajustes, at Victor's request.)*

### Language

- **AC-010-17.** Reports, files and the copied text follow the app language, including dates, month names and how amounts are written; switching it never changes a number.

## Out of scope

- **011:** the full backup spreadsheet (every table, restore).
- Sending by e-mail or WhatsApp from inside the app: the share sheet does it.
- A logo or a photo on the PDF, and choosing its colors apart from the theme.
- Reports across several students at once, other than Ganhos' earnings report.

## Open questions

Answered by Victor, 28 Oct 2026:

- Excel is in 010, written by a small writer of the app's own, with no library.
- Ajustes gets "Seu nome" and "Chave Pix", both optional; reports show them when set.

Assumed, for Victor to confirm in review:

- A student's report counts the classes the student owes; their classes at a place that pays the teacher are on that place's report, not theirs.
- Total on a student's, group's or place's report is what was charged (before any commission); the earnings report uses what the teacher keeps (AC-009-2).
- Pago and A receber on a report are for the period's classes, worked out as on the detail (AC-008-8), so a payment made later for an earlier class counts as paying it.
- Bonus and credit put on a class show as paid on the report, with "bônus" next to it.
- The PDF is A4 portrait.
