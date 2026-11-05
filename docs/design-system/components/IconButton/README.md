# IconButton

Round 44px button with one icon, for back, close, settings and period arrows.

## Variants
`sunken` (default, on background), `plain` (period arrows), `onPrimary` (on the hero header and the live screen), `accent` (the + in the hero header).

## Consumer provides
`icon`, `label` (required: it is the accessible name, since there is no visible text), `onPress`.

## Rules
- The hit area is always 44×44 even when the drawn circle is smaller.
- One `accent` icon button per screen at most.

## Props

```ts
interface IconButtonProps {
  icon: IconName;
  label: string;
  variant?: 'sunken' | 'plain' | 'onPrimary' | 'accent';
  disabled?: boolean;
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```

## 2026 look (spec 016)
Round, at least 48 dp. `ACCENT_SQUARE` is an add button in a tab header (Cadastro): a 56 dp rounded square (`radius-2xl`) in the highlight color. On Hoje's Topo band the + is the round `ACCENT`. `ON_PRIMARY` sits on the Topo color and uses `onHeaderFill()`.

## Dot (spec 014)
`dot` puts a small `danger` circle at the top end: the bell on Hoje and the Agenda shows it while notifications are off on the phone (AC-014-32).

## Small (Victor, 5 Nov 2026)
`size = SM` draws a 36 dp (`size-control-sm`) circle with a 20 dp icon, still answering on 48 dp. Every header button uses it: back, the header actions, the bell on Hoje and the Agenda, and Ganhos' Evolução and Relatório.

## Count (spec 014)
`count` puts a number at the top end on a `danger` pill with `surface` text ("99+" past 99), read with the label ("Notificações, 3"). The bell shows its unseen notifications this way and falls back to `dot` when there are none and the phone's notifications are off (AC-014-40).
