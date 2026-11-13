# Spec 019: Evolução, any period and tidier filters

Status: done
Depends on: 014 (Evolução, AC-014-31 to 35), 009 Earnings (the money rules), 010 Reports (the De and Até pattern of "Outro…")
Mockups: none. Built from the design system's components (Chip, DateField, SelectField, MonthBars).

## Goal

Evolução, opened from Ganhos, shows only 6 months, 12 months or one calendar year. A teacher who wants to see two years, or one term from a given day to another, can't. Its filters are also three rows of chips that look alike and push the chart down. After this spec Evolução takes any period from one day to five years, chosen with De and Até as in the reports: the period is simply a gap of dates, and the chart shows what falls inside it, month by month. Its type and place-kind filters become dropdowns like "Quem", and Ganhos' type filter too; Quem gains a search box, and Quem and Local take more than one choice. Nothing about the money changes.

## User stories

- **US-1.** As a teacher, I compare the last two years month by month to see if I'm growing.
- **US-2.** As a teacher, I look at a school term, from the first day of class to the last, on its own.
- **US-3.** As a teacher, I set my filters without the chart being pushed off the screen.

## Acceptance criteria

### Any period

- **AC-019-1.** *(13 Nov 2026: Personalizado removed; De and Até always show under the quick periods, see AC-020-31.)* Evolução's period choices are 6 meses (at first), 12 meses, Ano and Personalizado. Personalizado shows De and Até date fields under the choices, as "Outro…" in the reports (AC-010-3); the other choices fill them in.
- **AC-019-2.** De and Até take any day. Até is never before De nor after today, and the period is at most 5 years; a longer one says so under the fields and keeps the last valid period.
- **AC-019-3.** The period is a gap of dates and Evolução shows only what falls inside it, one bar per month as today (Victor, 5 Nov 2026). The first and last month count only their days inside the period; a period inside one month is one bar. A school's salary counts in its month when that month's first day is inside the period (AC-009-5). The bars keep scrolling sideways when they don't fit.
- **AC-019-4.** The list under the chart (AC-014-34) shows one row per bar with the same figures: earned, received, to receive, classes and hours, and the change from the bar before.
- **AC-019-5.** Tapping a bar or a row works as today (AC-014-35's comparison of two); a 2-year period by month compares any two of its 24 months.
- **AC-019-6.** The chosen period, De and Até are kept while Evolução is open, through a turn of the phone; Evolução opens again at 6 meses.

### Filters as dropdowns

- **AC-019-7.** "Tipo" is a dropdown field with Tudo, Particulares and Escola e emprego, the same choices and rules as Ganhos' (AC-014-19, 20). It opens a small sheet with the choices; the field shows the chosen one.
- **AC-019-8.** "Local" is a dropdown field with the place kinds (Online, Casa do Aluno, Escola, Salinha, Cursinho, Particular); one or more can be checked, and none checked is Todos os locais. The field shows the chosen ones ("Online, Escola"), or the first two and how many more ("Online, Escola +1").
- **AC-019-12.** "Quem" opens a list of every student, group and place with a search box: typing filters the list as it goes, ignoring accents and capitals ("joao" finds "João"). Any number of them can be checked, and the chart and list then show what came from all the checked ones together; none checked is Todos. The field shows them as Local does. *(This replaces AC-014-35's one payer; Victor, 5 Nov 2026.)*
- **AC-019-9.** Tipo, Quem and Local sit together under the period, one per row, and combine as before (AC-014-35). With the phone sideways they keep the readable width (AC-017-4).

- **AC-019-11.** Ganhos' type filter (AC-014-19, 20) is the same "Tipo" dropdown, so both screens match (Victor, 5 Nov 2026).

### From the same emulator round (Victor, 5 Nov 2026)

- **AC-019-13.** The number on the Agenda tab says what it counts: while classes wait to be confirmed, the Agenda shows "2 aulas para confirmar ›" at the top, opening the list (AC-014-6), and screen readers read the tab as "Agenda, 2 aulas para confirmar". Sideways the line slides away with the title row (AC-017-8).
- **AC-019-14.** Ganhos' summary shows Recebido and A receber with their cents ("R$ 1.234,56"), like Ganho above them; an amount too long for its column gets smaller instead of being cut. Hoje's card keeps its rounded figures.

- **AC-019-15.** Seus dados has "Título no topo dos relatórios". What is typed there replaces "TUTORA · CONTROLE DE AULAS" at the top of every PDF report, the class reports and Ganhos' earnings report; left empty, the reports keep it (Victor, 5 Nov 2026).

### Language

- **AC-019-10.** Every new word is in Português and English.

## Out of scope

- Comparing two whole periods (this year against last year) side by side.
- Saving a favorite period.
- Exporting Evolução; the earnings report (AC-010-13) keeps doing that.

## Open questions

Answered by Victor on 5 Nov 2026: the period is just a gap of dates with the data inside it, month by month (no bars by day, week or year); 5 years is the longest period; Ganhos' type filter becomes the same dropdown. He asked to build it on the spec 017 branch. Later the same day: the dropdowns work like a combo box, with a text filter, and take more than one person, school or kind of place.
