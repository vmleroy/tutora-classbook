# PeriodField

A period chosen as a filter: its label, the quick periods in one segmented row, and De and Até under them (spec 020). It replaces rows of period chips, which read like a heading rather than something to change.

## Use
- Relatório (Este mês, Mês passado, 3 meses), Evolução (6 meses, 12 meses, Ano) and the backup spreadsheet (Este mês, Mês passado, Este ano, Tudo).
- Wherever a screen filters by dates. Stepping through periods with ‹ › (Ganhos) stays a `SegmentedControl` with its arrows.

## Consumer provides
`label` ("Período"), `presets` (two to four {value, label}), `selected` (null when the dates were picked by hand), `onPreset`, the two dates with their text in the app language and `onFrom` / `onTo`, the De and Até labels, and a `tag` for tests (the fields are `tag-from` and `tag-to`).

## Rules
- The label sits above, like every field's, so the period reads as one filter among the others (Tipo, Quem, Local).
- The quick periods are a small segmented row; the dates always show what the period is, so there is no "Outro…" or "Personalizado": picking a date is enough, and then no quick period is lit.
- A quick period with no first day (Tudo) shows "—" in De.
- Pass `latest` (today) so no later day can be picked or typed; a quick period that runs to its month's end shows `latest` in Até.
- The caller keeps the rules for the dates (Até never before De, never after today, a longest period) and shows their message under the field.
