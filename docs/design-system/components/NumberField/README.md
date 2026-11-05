# NumberField

A whole number typed with the number keyboard, with small − and + inside the field: the classes in a package, the backups kept, bonus minutes, a commission in percent (spec 014, AC-014-37).

## Consumer provides
A `label`, the `value`, `onValueChange`, and `min`, `max` and `step`; a `suffix` such as "%" when the number is a percent.

## Rules
- It looks like every other field: the label above, the 48 dp box, `radius-lg` corners, `line` border; the number centered in `bodyStrong` with tabular figures.
- − and + are `button-sm` circles on `surface-sunken`, each answering on 48 dp, read as "Diminuir Aulas" / "Aumentar Aulas". At `min` or `max` they dim to `opacity-disabled`.
- Typing past `max` keeps the previous number; a number under `min` shows "No mínimo 1" under the field.
- Use one field per kind of value: `TextField` for text and phone, `MoneyField` for money, `NumberField` for counts and percent, `DurationField` for lengths of time, `TimeField` for a clock time, `DateField` for a date. Forms never use a bare `Stepper`.
