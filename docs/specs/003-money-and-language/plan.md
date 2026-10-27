# Plan 003: Money and language

Architecture reference: [`docs/architecture.md`](../../architecture.md), sections "Money and time" and "Theming and i18n". Mockups: [`Settings.dc.html`](../../mockups/Settings.dc.html) (the Geral group; its Moeda and Cotação rows are out of v1).

## Approach

- **Domain (`src/domain/money.ts`).** Amounts become plain `Cents` (safe integers of centavos); the `Money` object and its `currency` go away. One place rounds: `roundHalfAwayFromZero` and `multiplyRate`. On top of them: `lessonValue(price, 'hour' | 'lesson', minutes)`, `afterCommission(amount, bp)`, `sum(amounts)` (exact, never rounds), `wholeReais` for compact amounts, and `typedCents(text, previous, max)` for money fields. Spec 004 adds price resolution on top of these.
- **Formatting (`src/ui/format.ts`).** `formatMoney(cents, language, compact)` always formats BRL; the language only picks `pt-BR` or `en-US`. `formatDayMonth` writes the language samples.
- **Money field.** `TextField` gets `kind="money"` with `cents`, `onChangeCents` and `maxCents` (default R$ 99.999,99). The input shows `formatMoney(cents)`; each text change goes through `typedCents`, which reads the digits of the text as cents, so typing appends on the right and deleting drops the last digit. The caret is pinned to the end. No new component: the design system's TextField guideline already promised money fields.
- **Language switch.** Store action `setLanguage` saves through the `updateSettings` use case (one transaction), then calls `i18n.changeLanguage`; react-i18next repaints every mounted screen. Start-up already prefers the saved language over the phone's (`ensureDefaultSettings` never overwrites).
- **Ajustes.** A `ListGroup` "Geral" with the language row: title, hint "Só textos e datas", a small `SegmentedControl` (Português / English) and, under each option, its sample in its own language ("Datas 26/10 · R$ 1.234,56" / "Dates 10/26 · R$1,234.56"). The Sobre card stays below.
- **USD removed.** `currency` leaves `Settings`, the settings repo, `Money` props and `formatMoney`. Product, architecture and design-system docs drop USD, FX and PTAX.

## Data

No migration. Installs from 001/002 keep a stale `currency` row in `settings`; `loadSettings` reads only known keys, so it is ignored. Removing it would need a data migration for no visible gain.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `updateSettings` (existing), used by `setLanguage` | `settings.language` | none | none (settings live in the Zustand store) |

## Screens and components

- `src/app/settings/index.tsx`: Geral group with the language row (ListGroup, SegmentedControl, Text).
- `TextField`: `kind="money"`; guideline and dev gallery updated.
- `Money`: `currency` prop removed.

## Test strategy

Jest only, one focused test per criterion (see `tasks.md`). Domain rules in `src/domain/money.test.ts`; formatting in `display.test.tsx`; the money field through the real input in `selection.test.tsx`; Ajustes and the restart in `tests/app/settings.test.tsx`. No Maestro flow: the language switch is a plain re-render that the screen test proves.

## Risks

- **Android caret.** A controlled input reformatting on every keystroke can move the caret; pinning `selection` to the end handles it, but it is only proven on a device. Pending check on the emulator or Victor's phone.
- **Intl on Hermes.** Hermes ships `Intl.NumberFormat`; the Node tests use ICU. Spaces in "R$ 1.234,56" may be non-breaking on both; tests normalise them.
