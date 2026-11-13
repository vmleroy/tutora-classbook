# ChoiceField

A dropdown that works like a combo box (spec 019): a `SelectField` showing what is chosen, opening a `BottomSheet` with the choices.

## Consumer provides
`label`, the `choices` (`Choice` with a stable `key`, the `label` shown and searched, an optional `sub`), the `selected` keys and `onChange`. Optional: `multiple`, `allLabel`, `searchLabel`, `icon`.

## Rules

- A long or growing list (the places when adding a class, a group or a student's link) is a `ChoiceField` with `searchLabel`, never a row of chips; `required`, `isError` and `errorKey` work as on any field (spec 020).
- **One choice** (Tipo): the sheet lists the choices as rows; tapping one picks it and closes the sheet.
- **Several** (`multiple`, as Quem and Local): each choice is a `Checkbox`; checking applies at once and the sheet stays open until Pronto. With `allLabel`, a first checkbox reads it ("Todos", "Todos os locais"): it is checked while nothing else is, and checking it clears the rest.
- **Search** (`searchLabel`, for long lists such as Quem): a `TextField` on top filters the choices as they are typed, ignoring accents and capitals ("joao" finds "João").
- The field reads `allLabel` when nothing is chosen, the names when one or two are, and the first two with "+N" past that ("Ana Souza, João Lima +1").
- Used on Ganhos and Evolução (AC-019-7, 8, 11, 12).
