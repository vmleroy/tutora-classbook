# SegmentedControl

Two to four mutually exclusive options that switch a view or a mode in place.

## Use
- Modes of one screen (Registrar / Agendar), periods (Mês / Trimestre / Ano), Dia / Semana on Agenda, language in Ajustes.
- `md` (44px segments) in forms, `sm` (36px) in filters and settings rows.

## Consumer provides
`options` ({value, label}), `value`, `onChange`, `label` (accessible group name).

## Rules
- Equal-width segments; labels short enough to fit without wrapping at 390px.
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
