# Button

Pressable action with a verb label; one filled button per area says what the screen is for.

## Variants
| variant | when |
| --- | --- |
| `primary` | The main action of a screen or sheet: Salvar, Registrar, Gerar PDF. |
| `accent` | Starting money-making: Iniciar aula, the + action. At most one per screen. |
| `outline` | Secondary actions beside a primary: Cancelar, Marcar como dada. |
| `ghost` | Low-weight actions in headers and rows: Editar, Ver tudo. |
| `danger` | Destructive actions: Excluir aula, Restaurar backup. Always confirmed in a sheet. |

Sizes: `sm` 36px (in rows and sheets), `md` 48px (default), `lg` 56px (the one bottom action of a form or the live screen, full width).

## Consumer provides
`children` (the label), `onPress`, and optionally `icon`, `loading`, `disabled`, `fullWidth`.

## Rules
- Label = verb first, sentence case: "Salvar aula", never "OK" or "Submit".
- A form's bottom button can carry the value: "Registrar aula · R$ 180,00".
- Never two `primary` or two `accent` buttons side by side; pair `outline` + `primary` (1fr : 2fr in sheets).
- `loading` keeps the label and disables the button; never hide the label.
- NativeWind: `Pressable` with `tv()` variants, e.g. `bg-primary text-on-primary rounded-lg h-12 px-4`.

## Props

```ts
interface ButtonProps {
  variant?: 'primary' | 'accent' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  icon?: IconName;
  fullWidth?: boolean;
  loading?: boolean;
  disabled?: boolean;
  onPress?: () => void;
  children: React.ReactNode;
}
```
