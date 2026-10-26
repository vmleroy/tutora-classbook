# Money

Formatted money from integer cents, always in tabular figures so totals and the live counter never jitter.

## Consumer provides
`cents` (integer), `currency` (BRL or USD; the app currency by default), `size` (display, xl, lg, md, sm), `tone` (default, earnings, owed, paid, onHeader), `compact` (no cents, for stat trios), `showSign`.

## Rules
- Format with `Intl.NumberFormat` for the app language: pt-BR "R$ 1.234,56", en "R$1,234.56" or "$30.00".
- One `xl` figure per card; `display` only on the live lesson screen.
- Owed money uses `danger-ink`; never color a positive total red.
- With "Mostrar as duas moedas" on, the converted value follows in `caption` ink-muted: "US$ 30,00 ≈ R$ 162,00".
