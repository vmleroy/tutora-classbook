# EarningsSummary

The money card at the top of Hoje: earned today, progress to expected, and week / month / owed.

## Consumer provides
`title`, `earnedCents`, `expectedCents` and its label, and up to three `stats` ({label, cents, tone}).

## Rules
- Three stats at most, compact money (no cents).
- Owed uses `tone: 'owed'`; tapping it opens the pending-payments list.
- Ganhos uses the same card with "Recebido / A receber" as stats, `receivedShare` for the success-on-danger bar, and `footnote` for the commission and bonus line.

## Props

```ts
interface EarningsSummaryProps {
  title: string;
  earnedCents: number;
  expectedCents?: number;
  expectedLabel?: string;
  stats?: { label: string; cents: number; tone?: MoneyProps['tone'] }[];
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```

## 2026 look (spec 016)
On Ganhos it is drawn on a `SummaryCard`: the headline in earnings-on-header, the expected bar on `onHeaderFill()`, and the stats as `SummaryTile`s of one height (owed in the red tile only when something is owed, paid in the green). With `onCard` it is a white `Card` (Hoje, over the Topo band, and Ganhos): the amount in `earnings`, a hairline, and the stats as equal columns so every label and amount starts at the same place.
