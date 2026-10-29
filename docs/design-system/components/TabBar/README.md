# TabBar

Bottom navigation with the five tabs: Hoje, Agenda, Cadastro (Alunos and Locais as sub-pages), Ganhos, Ajustes.

## Rules
- Always five, always in this order; labels always shown.
- Active tab: primary icon on a `primary-soft` pill and a bold primary label. Customizable part: active menu.
- 72px plus the bottom safe-area inset; hidden on the live lesson screen and on modal forms.

## Props

```ts
interface TabBarProps {
  active?: 'today' | 'schedule' | 'registry' | 'earnings' | 'settings';
  /** Called with the pressed tab; the router navigates. */
  onSelect?: (tab: 'today' | 'schedule' | 'registry' | 'earnings' | 'settings') => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```

## 2026 look (spec 016)
The bar sits on `surface-sunken`, 80 dp tall; the active tab has its icon on a `tab-active-soft` pill and its label in ink, weight 900.
