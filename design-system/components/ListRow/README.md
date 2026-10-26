# ListRow

Settings and navigation row: optional icon tile, title, subtitle with the current value, and a chevron, switch or badge.

## Consumer provides
`title`, `subtitle` (the current value, so the user knows the state without opening it), optional `icon` + `iconTone`, `trailing` (chevron | switch | none), `badge`, `onPress`.

## Rules
- Subtitles state the current setting ("Automática · US$ 1 = R$ 5,42 · hoje"), not a description of the feature.
- Switch rows are not pressable as a whole except through the switch.
