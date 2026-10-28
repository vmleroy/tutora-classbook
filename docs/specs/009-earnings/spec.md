# Spec 009: Earnings

Status: done
Depends on: 008 Payments and packages (balances, payments, packages, credit and bonus), 006 Lessons (done values, charges), 004 Places and students (place types, commission, school salary)
Mockups: docs/mockups/Earnings.dc.html (Ganhos tab: period, type filter, summary card, "De onde veio"), Main.dc.html (Hoje's earnings card: Ganho hoje, previstos, Semana, month, Alunos devem). The 6-month trend is not in the mockup; its place was decided with Victor (Open questions).

## Goal

The teacher sees what they earned: today and this week on Hoje, and on the Ganhos tab for any month, quarter or year, with what came in and what is still to receive, where it came from, and how the last six months went. A school's monthly salary counts next to private classes, a filter separates the two, and the numbers are what the teacher keeps: after a place's commission and without what a bonus paid for. The numbers are the same whether classes were timed live or marked by hand.

## User stories

- **US-1.** As a teacher, I open Hoje and see what I earned today against what was expected, plus this week and this month.
- **US-2.** As a teacher, I open Ganhos and see that in October I earned R$ 5.020, received R$ 4.200 and still have R$ 820 to receive.
- **US-3.** As a teacher with a school job and private students, I filter Ganhos to private classes only, or to school and formal job only.
- **US-4.** As a teacher, I see which students and places my money came from, biggest first, and open any of them.
- **US-5.** As a teacher, I look at the last six months to see whether I'm earning more or less.

## Acceptance criteria

### What counts

- **AC-009-1.** A class marked done is earned on its day, at its value with travel. A cancelled or missed class with a charge is earned at its charge. Scheduled, live, and uncharged cancelled or missed classes earn nothing.
- **AC-009-2.** At a place where the student pays and the place keeps a commission, what is earned is the value after the commission (30% of R$ 100,00 → R$ 70,00), rounded to the centavo once. What the student owes stays the full value (AC-008-1).
- **AC-009-3.** A class paid for by a package earns its package price per class (AC-008-23) on its day.
- **AC-009-4.** The part of a class that a bonus covered (money or minutes, AC-008-31) is not earned.
- **AC-009-5.** A school's monthly salary is earned as a whole in each month it is set: once per month, three times in a quarter, and once for each month of a year up to the current month. Ganho hoje and the week on Hoje leave it out.
- **AC-009-6.** Recebido is the money of the payments recorded in the period (by their date), package purchases included; a bonus is never received. A school's salary counts as received in its month.
- **AC-009-7.** A receber is what is still owed for the classes given in the period, as on the payers' balances.

### Hoje

- **AC-009-8.** Hoje has an earnings card: "Ganho hoje" with what today's classes earned, "de R$ 380,00 previstos" (the day's Previsto, AC-006-16), then Semana (Monday to today), the month's name with its total, and "Alunos devem" with what everyone owes now. Tapping it opens Ganhos.

### Ganhos

- **AC-009-9.** Ganhos has Mês, Trimestre and Ano at the top, the current one first, with ‹ and › to move to the previous or next period, and the range under them ("Outubro 2026", "Ago – Out 2026", "2026"). › stops at the current period.
- **AC-009-10.** Under the period, Tudo, Particulares and "Escola e emprego" filter everything on the tab: Escola and Cursinho count as school and formal job, every other place as private classes.
- **AC-009-11.** The summary card shows "Ganho no período" with its total, then Recebido and A receber, and a bar filled in the share received. When the period had a commission or a bonus, a line under it says so ("comissão dos locais: R$ 30,00 · bônus dado: R$ 50,00").
- **AC-009-12.** Under the summary, six bars show what was earned in each month ending at the period's last month, the received part filled in, each labelled with its month ("mai" … "out"). They follow the type filter.
- **AC-009-13.** "De onde veio" lists each student, group, paying place and school that earned something in the period, largest first, with its total, a bar in proportion to the largest, and "deve R$ 240" when it owes now. Tapping a row opens its detail.
- **AC-009-14.** A period with nothing earned shows "Nada ganho neste período" instead of the card and the list.

### Added after trying the build (Victor, 28 Oct 2026)

- **AC-009-16.** Ajustes › Geral has "Durações em HH:MM", off at first. On, every duration in the app reads as hours and minutes ("01:30") instead of minutes ("90 min"): classes, Hoje, the live screen, places, and bonus minutes.

### Language

- **AC-009-15.** Every label, month name and amount follows the app language; switching it never changes a number.

## Out of scope

- **010:** the Relatório button on Ganhos and the earnings PDF and Excel; the button doesn't show until then.
- A school's salary as money owed or received by date (a pay day): it counts as received in its month.
- Expenses, taxes and net income.
- Currencies other than reais (constitution rule 9).

## Open questions

Answered by Victor, 28 Oct 2026:

- A school's salary counts as a whole month, in each month; Hoje's day and week leave it out.
- At a place with a commission, earned is what the teacher keeps; the commission shows as its own line.
- The part of a class paid by a bonus is not earned; the bonus shows as its own line.
- The 6-month trend goes on Ganhos, under the summary card: six monthly bars of earned, with the received part filled in, following the type filter.

Assumed, for Victor to confirm in review:

- A class is earned on the day it was given, not when it was marked done or paid.
- Recebido counts by payment date, so a payment in November for October's classes is received in November.
- "Alunos devem" on Hoje is everyone's balance (students, groups and places), as Hoje's pending alert.
- A class at a place that pays the teacher counts as private or as school and formal job by the place's type, like every other class.
- The week runs Monday to Sunday in both languages (architecture doc).
