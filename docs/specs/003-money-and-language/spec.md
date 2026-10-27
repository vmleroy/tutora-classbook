# Spec 003: Money and language

Status: ready (Kotlin rebuild: re-plan)
Depends on: 001 Foundation, 002 Design system
Mockups: docs/mockups/Settings.dc.html

## Goal

Every amount in Tutora follows one set of rules in reais, so lesson values, commissions and totals are exact and never drift. Teachers type amounts quickly and safely, and can switch the app between Portuguese and English from Ajustes without any amount changing.

## User stories

- **US-1.** As a teacher, I trust that a 50-minute lesson at R$ 100/h is worth exactly R$ 83,33, and that my month's total is the sum of my lessons.
- **US-2.** As a teacher, I type a price as digits and see it formatted as reais while I type.
- **US-3.** As a teacher, I switch the app to English (or back) in Ajustes and everything changes at once, except my amounts.

## Acceptance criteria

### Money rules

- **AC-003-1.** An hourly lesson is worth price × minutes ÷ 60, rounded once to the centavo, half away from zero (R$ 100/h for 50 min = R$ 83,33).
- **AC-003-2.** A per-lesson price is the same whatever the duration.
- **AC-003-3.** With a commission, the teacher keeps price × (100% − commission), rounded once (R$ 100 with 30% = R$ 70,00).
- **AC-003-4.** A total is the exact sum of the stored lesson values; adding many lessons never drifts by rounding.
- **AC-003-5.** Negative amounts (owed) keep their sign wherever they are shown.

### Showing amounts

- **AC-003-6.** In Portuguese amounts read R$ 1.234,56; in English R$1,234.56. Switching language never changes an amount.
- **AC-003-7.** Compact amounts in summaries show whole reais, rounded half away from zero.

### Typing amounts

- **AC-003-8.** A money field fills from the right as digits are typed (1, 2, 0, 0, 0 → R$ 120,00), formatted in the app language, and keeps the exact centavos.
- **AC-003-9.** A money field accepts only digits; deleting removes the last digit.
- **AC-003-10.** A money field refuses digits beyond R$ 99.999,99.

### Language in Ajustes

- **AC-003-11.** Ajustes opens with a "Geral" group whose first row is "Idioma do app", offering Português and English, each with a sample of how it writes dates and amounts ("Datas 26/10 · R$ 1.234,56" / "Dates 10/26 · R$1,234.56").
- **AC-003-12.** Switching language repaints every text, number and date at once, with no restart; the choice is saved and survives a restart.
- **AC-003-13.** The phone's language never overrides a saved choice.
- **AC-003-14.** No currency choice is offered anywhere, and every amount is in R$ (USD is gone from the app).

## Out of scope

Price resolution per student and place (004), cancellation fees (006), payments, packages and balances (008). USD and exchange rates are not in v1 (constitution rule 9).

## Answered questions

- Upper limit of a money field: R$ 99.999,99 (Victor, 27 Oct 2026).
- The language row shows a sample format under each option (Victor, 27 Oct 2026).
