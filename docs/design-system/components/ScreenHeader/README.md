# ScreenHeader

Top of every screen: `hero` on Hoje, `tab` on the other tabs, `push` for detail screens, `modal` for forms.

## Variants
- `hero`: `header` fill (primary in light mode, a deep shade of it in dark), rounded bottom (`radius-3xl`), date eyebrow, greeting, settings and + buttons. The summary card overlaps its bottom by 60px.
- `tab`: large title (`title-xl`) on background, optional action on the right.
- `push`: back button + `title-lg`; Editar as a ghost button on the right.
- `modal`: close button + `title-lg`.

## Rules
- Titles name the place, not the action ("Ajustes", "Registrar aula").
- Respect the status-bar inset (`useSafeAreaInsets`).
