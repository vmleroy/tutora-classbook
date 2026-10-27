# Chip

Pill-shaped choice for filters and for picking one place, repeat rule or payment method from a short list.

## Use
- Filters on Alunos and Ganhos, place picker in Nova aula, payment method in Registrar pagamento.
- Selected = `primary` fill with `on-primary` text; unselected = `surface` with a `line` border.

## Consumer provides
`label`, `selected`, `onPress`. Group chips in a horizontally scrolling row with `space-1.5` gaps.

## Rules
- Keep labels to one or two words. A chip never wraps.
- Single choice rows expose `radiogroup` semantics; filter rows expose toggle buttons.

## Props

```ts
interface ChipProps {
  label: string;
  selected?: boolean;
  onPress?: (selected: boolean) => void;
}
```
