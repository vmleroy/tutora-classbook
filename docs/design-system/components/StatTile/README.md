# StatTile

One of the three figures at the top of a student, group or place: "Deve R$ 240,00", "Outubro · 6 aulas", "Valor R$ 120/h" (spec 016, after the StudentDetail and PlaceDetail mockups).

## Consumer provides
A `label`, the `value` already written for the app language, and a `tone`.

## Rules
- Three in a row, in a `Row` with `IntrinsicSize.Min` height so they share one height; each takes an equal share of the width.
- `PLAIN` is a `surface` tile. `OWED` (what is owed, while something is) is `dangerSoft` with `dangerInk`; `PAID` ("Em dia") is `successSoft` with `successInk`, the same in every theme.
- Keep values short: a rate is "R$ 100/h", never the sentence that explains who pays; the label says that ("Paga").
- Corners `radius-lg` (14); the tile is one element for screen readers.
