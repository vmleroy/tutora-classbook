# EarningsSummary

The money card at the top of Hoje: earned today, progress to expected, and week / month / owed.

## Consumer provides
`title`, `earnedCents`, `expectedCents` and its label, and up to three `stats` ({label, cents, tone}).

## Rules
- Three stats at most, compact money (no cents).
- Owed uses `tone: 'owed'`; tapping it opens the pending-payments list.
- Ganhos uses the same card with "Recebido / A receber" and the success-on-danger bar.

## Props

```ts
interface EarningsSummaryProps {
  title: string;
  earnedCents: number;
  expectedCents?: number;
  expectedLabel?: string;
  stats?: { label: string; cents: number; tone?: MoneyProps['tone'] }[];
}
```
