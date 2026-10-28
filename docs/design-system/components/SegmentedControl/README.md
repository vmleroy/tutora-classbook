# SegmentedControl

Two to four mutually exclusive options that switch a view or a mode in place.

## Use
- Modes of one screen (Registrar / Agendar), periods (Mês / Trimestre / Ano), Dia / Semana on Agenda, language in Ajustes.
- `md` (44px segments) in forms, `sm` (36px) in filters and settings rows.

## Consumer provides
`options` ({value, label}), `value`, `onChange`, `label` (accessible group name).

## Rules
- Equal-width segments; keep labels short. A label that doesn't fit wraps to two lines and every segment grows with it, so a label is never clipped (AC-002-16), at 360px and with Large text.
- Five or more options, or options that filter a list → use Chips.
- The selected segment is `surface` with `primary` text and `shadow-thumb`; never a primary fill.

## Props

```ts
interface SegmentedControlProps<T extends string = string> {
  options: { value: T; label: string }[];
  value?: T;
  onChange?: (value: T) => void;
  label: string;
  size?: 'sm' | 'md';
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
