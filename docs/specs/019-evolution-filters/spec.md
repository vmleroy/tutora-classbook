# Spec 019: Evolução, any period and tidier filters

Status: draft
Depends on: 014 (Evolução, AC-014-31 to 35), 009 Earnings (the money rules), 010 Reports (the De and Até pattern of "Outro…")
Mockups: none. Built from the design system's components (Chip, DateField, SelectField, MonthBars).

## Goal

Evolução, opened from Ganhos, shows only 6 months, 12 months or one calendar year. A teacher who wants to see two years, or one term from a given day to another, can't. Its filters are also three rows of chips that look alike and push the chart down. After this spec Evolução takes any period from one day to five years, chosen with De and Até as in the reports, and its type and place-kind filters become two dropdowns like "Quem". Nothing about the money changes.

## User stories

- **US-1.** As a teacher, I compare the last two years month by month to see if I'm growing.
- **US-2.** As a teacher, I look at a school term, from the first day of class to the last, on its own.
- **US-3.** As a teacher, I set my filters without the chart being pushed off the screen.

## Acceptance criteria

### Any period

- **AC-019-1.** Evolução's period choices are 6 meses (at first), 12 meses, Ano and Personalizado. Personalizado shows De and Até date fields under the choices, as "Outro…" in the reports (AC-010-3); the other choices fill them in.
- **AC-019-2.** De and Até take any day. Até is never before De nor after today, and the period is at most 5 years; a longer one says so under the fields and keeps the last valid period.
- **AC-019-3.** The chart's bars follow the length of the period: one bar per day up to 31 days, one per week up to 6 months, one per month up to 3 years, one per year beyond. Each bar's label says which (day, week "5–11 out", month, year). The bars keep scrolling sideways when they don't fit.
- **AC-019-4.** The list under the chart (AC-014-34) shows one row per bar with the same figures: earned, received, to receive, classes and hours, and the change from the bar before. A day or week cut by the period counts only its days inside it.
- **AC-019-5.** Tapping a bar or a row works as today (AC-014-35's comparison of two); a 2-year period by month compares any two of its 24 months.
- **AC-019-6.** The chosen period, De and Até are kept while Evolução is open, through a turn of the phone; Evolução opens again at 6 meses.

### Filters as dropdowns

- **AC-019-7.** "Tipo" is a dropdown field with Tudo, Particulares and Escola e emprego, the same choices and rules as Ganhos' (AC-014-19, 20). It opens a small sheet with the choices; the field shows the chosen one.
- **AC-019-8.** "Local" is a dropdown field with Todos os locais and the place kinds (Online, Casa do Aluno, Escola, Salinha, Cursinho, Particular). The field shows the chosen one.
- **AC-019-9.** Tipo, Quem and Local sit together under the period, one per row, and combine as before (AC-014-35). With the phone sideways they keep the readable width (AC-017-4).

### Language

- **AC-019-10.** Every new word is in Português and English.

## Out of scope

- Comparing two whole periods (this year against last year) side by side.
- Saving a favorite period.
- Exporting Evolução; the earnings report (AC-010-13) keeps doing that.

## Open questions

1. The bar sizes in AC-019-3 (day up to 31 days, week up to 6 months, month up to 3 years, year beyond): do they match how you'd read it?
2. Is 5 years the right longest period?
3. Should Ganhos' own type chips (Tudo, Particulares, Escola e emprego) also become a dropdown, so both screens match?
