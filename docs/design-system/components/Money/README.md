# Money

Formatted money from integer cents, always in tabular figures so totals and the live counter never jitter.

## Consumer provides
`cents` (integer), `size` (display, xl, lg, md, sm), `tone` (default, earnings, owed, paid, onHeader), `compact` (no cents, for stat trios), `showSign`.

## Rules
- Format with `Intl.NumberFormat` for the app language: pt-BR "R$ 1.234,56", en "R$1,234.56". Every amount is in reais.
- One `xl` figure per card; `display` only on the live lesson screen.
- Owed money uses `danger-ink`; never color a positive total red.
- Negative amounts keep their sign ("-R$ 240,00"); compact amounts round to whole reais, half away from zero.

## Props

```ts
interface MoneyProps {
  cents: number;
  locale?: string;
  size?: 'display' | 'xl' | 'lg' | 'md' | 'sm';
  tone?: 'default' | 'earnings' | 'owed' | 'paid' | 'onHeader';
  compact?: boolean;
  showSign?: boolean;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
