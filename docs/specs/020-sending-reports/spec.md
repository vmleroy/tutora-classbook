# Spec 020: Fatura and detailed report

Status: done
Depends on: 010 Reports (subjects, periods, PDF, Excel, copy as text, Seus dados), 019 (the reports' header title, AC-019-15), 015 App lock (the Pix key masked in the app, shown in full on what is sent), 008 Payments and packages (balances, which classes a payment paid), 006 Lessons (travel fee, cancellation charge), 007 Live timer (the real time in class), 004 Places and students (groups and their members)
Mockups: docs/mockups/Report.dc.html, ReportPdf.dc.html (today's report and its PDF). New mockups for the Fatura PDF, the detailed PDF and Faturas do mês come with the plan.

## Goal

Today Relatório makes one PDF that serves the teacher's own control. After this spec the teacher can also send two documents made for the people they work with, students (or their parents) and places (salinha, school, cursinho):

- a **Fatura**: direct, to get paid. Each class of the period, what is still to pay, an optional due date, and the Pix key with a QR code, ready to pay. When nothing is owed it says "Pago, obrigado";
- a **Relatório detalhado**: descriptive, to check the work done. Each class with its day, the scheduled time, the time spent in class, travel, any charge and the class notes.

On both, each class shows the name, the day, the scheduled time, the time spent in class and its value. For a place, both show each student's classes and the totals. A private group can be sent as one, or split into each member's share. At month end, "Faturas do mês" lists everyone who owes, one tap each to send. Everything is still made on the phone, with no network.

## User stories

- **US-1.** As a teacher, at month end I send Ana's mother a Fatura: "Para: Maria Souza (responsável por Ana Souza)", Ana's two unpaid classes, "R$ 240,00 a pagar" by 05/11, and a Pix QR code she scans to pay in a minute.
- **US-2.** As a teacher, I send the salinha a Relatório detalhado with each student's classes, the times and the totals, so they can check it against their own records and pay me.
- **US-3.** As a teacher, I paste the Fatura as a short WhatsApp message instead of a PDF.
- **US-4.** As a teacher, a parent asks why the month cost more; I send the detailed report showing the travel and the late cancellation.
- **US-5.** As a teacher with a group of three siblings' friends, I send each family a Fatura for their child's share of the group's classes.
- **US-6.** As a teacher, on the 1st I open Faturas do mês and send everyone's Fatura in a few minutes.

## Acceptance criteria

### Choosing what to send

- **AC-020-1.** Relatório (AC-010-1, 2, 3) keeps its subject, period, on-screen totals and "Exportar este relatório" (PDF, Excel, copy as text), unchanged. Under them, "Enviar" offers two kinds side by side: "Fatura" ("Para receber: valor e Pix") and "Relatório detalhado" ("Cada aula com dia, horário e deslocamento").
- **AC-020-2.** Fatura is shared as PDF or copied as text; Relatório detalhado as PDF or Excel. File names say which ("Ana Souza - fatura - outubro 2026.pdf", "Ana Souza - relatório detalhado - outubro 2026.pdf").

### What every class row shows

- **AC-020-3.** On the Fatura and the Relatório detalhado, each class is one row, oldest first, with: the name (the student, or the group), the day ("seg 05/10"), the scheduled time ("14:00"), the time spent in class, and the value.
- **AC-020-4.** Time spent in class is the real duration when the class was timed (007), otherwise the planned one, written as Durações em HH:MM says (AC-009-16).
- **AC-020-5.** A cancelled or missed class with a charge shows "Cancelada · cobrada", no time in class, and the charge as its value; one without a charge isn't listed (as AC-010-5).

### Fatura

- **AC-020-6.** The Fatura PDF is A4, one page when it fits. At the top: the reports' title (AC-019-15), "Fatura", the teacher's name, the date it was made, "Para:" and the period. "Para:" is the student's name, or "Maria Souza (responsável por Ana Souza)" when the student has a Responsável (AC-020-17).
- **AC-020-7.** Then the table of the period's classes (AC-020-3) with a Situação column (Pago / A pagar), and under it "Total do período", "Já pago" when something was paid, and "Valor a pagar" in large type.
- **AC-020-8.** Valor a pagar is what is still owed for the chosen period's classes only, worked out as on the report (AC-010-4, AC-008-8). Classes from other periods and credit are not counted.
- **AC-020-9.** Before sharing, the Fatura offers an optional "Vencimento" date, empty at first. When set, the PDF shows "Vencimento: 05/11/2026" next to Valor a pagar and the copied text says "até 05/11"; when empty, neither does.
- **AC-020-10.** The Pix block shows "Pague com Pix", a QR code, the key in full (never masked, unlike the app) and the teacher's name, and the "Pix copia e cola" text. Scanning the QR code in a bank app opens a Pix payment to that key for exactly Valor a pagar.
- **AC-020-11.** The QR code and "Pix copia e cola" need the Pix key, Seu nome and Cidade in Seus dados (AC-020-18); with the key but not the others, the key is shown without the QR code. Without a key, the Pix block is left out and Relatório says "Adicione sua chave Pix em Seus dados", linking there.
- **AC-020-12.** When nothing is owed for the period, the Fatura says "Pago, obrigado" where Valor a pagar would be, lists the classes and "Já pago", and has no Vencimento and no Pix block. The copied text says so too.
- **AC-020-13.** For a place, the Fatura's table is grouped by student, with each student's subtotal, then the total. A school paid by salary shows the salary for each month in the period instead of classes (AC-010-6).
- **AC-020-14.** "Copiar como texto" copies a short message: a greeting, "Fatura de outubro", one line per class still to pay ("19/10 · 14:00 · 1:00 · R$ 120,00"), "Total a pagar: R$ 240,00", the due date when set, then "Pix: <key> (Victor Le Roy)" and the "Pix copia e cola" text when available. "Copiado" confirms it.

### Relatório detalhado

- **AC-020-15.** The detailed PDF has the same header as AC-010-9 with "Relatório detalhado". Its table adds to each row (AC-020-3) the scheduled end ("14:00–15:30"), place, travel, charge and situation, and under the row the class's notes, when it has any and "Incluir observações" is on (on at first).
- **AC-020-16.** Above the table, the totals: Aulas, Horas, Aulas (valor), Deslocamento, Cobranças extras, Total, Pago and A receber, each shown only when not zero. For a place the table is grouped by student with a subtotal line each (classes, hours, value); the grand total closes the table. Payments received follow, as AC-010-9.
- **AC-020-19.** The detailed Excel has the sheet "Aulas" with name, date, scheduled start, scheduled end, minutes in class, place, travel, charge, value, situation and notes (when Incluir observações is on), and "Pagamentos" as AC-010-10. Dates are dates and amounts numbers, so the sheet can sum them.

### Who it is for

- **AC-020-17.** A student's edit form has an optional "Responsável" (name). Student detail shows it under the name when set. It is kept in the backup (011) and restored with it.
- **AC-020-18.** Seus dados has an optional "Cidade", described as "Usada no QR code do Pix". Seu nome, Chave Pix and Cidade are all needed for the QR code (AC-020-11).

### Groups

- **AC-020-20.** For a group whose classes the students pay (not a group at a school or a place that pays the teacher), Fatura and Relatório detalhado offer "Turma inteira" (at first) or "Dividir por aluno". Turma inteira sends one document for the group, with the group's name on each row.
- **AC-020-21.** Dividir por aluno shows one row per member, each sending its own Fatura or Relatório detalhado for that member's share: each class's value divided equally among the group's members, the cents left over going to the first members by name so the shares always add up to the class value. "Para:" is the member (or their Responsável), and the table names the group ("Turma Inglês B · 1/3").
- **AC-020-22.** A group with no members, or at a school or a place that pays the teacher, has no Dividir por aluno.

### Faturas do mês

- **AC-020-23.** Ganhos has "Faturas do mês". It lists, for the period on Ganhos' screen, every student, group and place with something still to pay, the amount on each row, largest first, and the total at the top.
- **AC-020-24.** Each row has "PDF" and "Texto": PDF shares that one's Fatura, Texto copies its message (AC-020-14). A row shared or copied shows a check until the list is closed.
- **AC-020-25.** A Vencimento set at the top of Faturas do mês applies to every Fatura sent from it.
- **AC-020-26.** A private group (AC-020-20) is one row; with "Dividir turmas por aluno" on, its members appear as their own rows instead.

### Language

- **AC-020-27.** Every new word, both PDFs, the Excel and the copied text follow the app language, including dates, weekdays and amounts; switching it never changes a number.

## Out of scope

- Sending from inside the app by e-mail or WhatsApp; the share sheet does it (as 010).
- Tracking whether a Fatura was sent, seen or paid beyond Faturas do mês's check while it is open; a payment is still recorded by hand (008).
- Splitting a group's payments between members: the group pays as one in the app (008); only the documents split.
- Pix charges with an expiry or a bank-issued (dynamic) QR code, which need a network.
- Invoices with tax value (nota fiscal, recibo for income tax).
- A logo or photo on the PDFs.
- The earnings report from Ganhos (AC-010-13 to 15): unchanged.

## Open questions

Answered by Victor, 13 Nov 2026:

- The invoice is called **Fatura**.
- Both are **added**; today's report stays. The Fatura is direct, the Relatório detalhado descriptive; both show, per class, the name, the day, the scheduled time, the time spent in class and the value.
- The Fatura counts **the chosen period only**.
- With nothing owed, the Fatura says **"Pago, obrigado"**.
- The Fatura has a **Pix QR code**, and an **optional Vencimento**.
- Students get an optional **Responsável**.
- A private group can be sent **whole or split per student**; not a group tied to a school or a place.
- The detailed report **always shows amounts**, also to a place.
- **Class notes go on the detailed report only**, never on the Fatura.
- **Faturas do mês** is in scope.

Assumed, for Victor to confirm in review:

- The Pix QR code needs a city, so Seus dados gains "Cidade" (AC-020-18).
- A group's split share is the class value divided equally among its current members, leftover cents to the first by name (AC-020-21).
- Class notes on the detailed report sit behind "Incluir observações", on at first, since some notes are only for the teacher (AC-020-15).
- Faturas do mês lives on Ganhos and uses Ganhos' period (AC-020-23).
