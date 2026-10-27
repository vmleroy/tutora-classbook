# TabBar

Bottom navigation with the five tabs: Hoje, Agenda, Alunos, Locais, Ganhos.

## Rules
- Always five, always in this order; labels always shown.
- Active tab: primary icon on a `primary-soft` pill and a bold primary label. Customizable part: active menu.
- 72px plus the bottom safe-area inset; hidden on the live lesson screen and on modal forms.

## Props

```ts
interface TabBarProps {
  active?: 'today' | 'schedule' | 'students' | 'places' | 'earnings';
}
```
