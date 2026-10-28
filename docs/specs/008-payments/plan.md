# Plan 008: Payments and packages (Kotlin)

Architecture reference: "State management and data flow" (aggregates in SQL), "Database schema and migrations" (`payment` payer CHECK, `v_balance`), "Money and time". Mockups: StudentDetail, PlaceDetail, Students, Places, Main, LessonDetail, Live, AddLesson, StudentEdit.

## Approach

The idea: **when a class becomes owed, freeze who owes it and how much**, the same way 006 freezes its price. Balances are then plain sums in SQL. Which classes are paid is worked out in the domain, one payer at a time, on read.

- **Domain** (`core/domain/Payments.kt`, new).
  - `PayerRef(kind: PayerKind, id)` where `PayerKind` is `STUDENT`, `GROUP` or `PLACE`. `payerOf(lesson)` picks the place when the place pays the teacher, else the group, else the student (AC-008-1). So a group at a salinha that pays is owed by the salinha.
  - `PayMethod` is `PIX`, `CASH`, `TRANSFER` or `CARD` (AC-008-5).
  - `dueOf(lesson, pkg: PackageState?)` returns `Due(payer, cents, packageId?)`, or null when nothing is owed.
    - A done class owes its worth (value + travel). A done package class with a class left owes only its travel, and takes that package. A class without a price or covered by the salary owes nothing (AC-008-1, 20).
    - A cancelled or missed class owes its charge, never from a package (AC-008-2, 23).
    - Scheduled and live classes owe nothing (AC-008-3).
  - `allocate(dues, payments)` returns `Allocation`: what is still owed on each class, the classes each payment paid, and the credit left. Payments are taken in `paid_at` order. Each first pays its chosen classes, up to what they still owe. Then each payment's leftover, in the same order, pays the oldest owed classes. Whatever remains is credit (AC-008-6, 8, 9). It is pure and run on every read, so editing, deleting or undoing never leaves a stale "paid" flag (AC-008-3, 12).
  - `Balance` is `Owes(cents)`, `Settled` or `Credit(cents)`, from due − paid (AC-008-4).
  - `packageUnit(price, lessons)` is the price ÷ lessons, rounded half away from zero once, through a new `divideRounded` in `Money.kt` (AC-008-23). `PackageState(id, total, used)` gives `left` and `lastOne` (AC-008-21).
  - Pricing: `RateType` gains `PACKAGE`, valid on a student only. `resolvePrice` gives a package student, at a place where the student pays, `Price.Set(packageUnit, LESSON, PriceSource.PACKAGE)`. The student's own price at that place is ignored ("Usa o pacote" in the mockup). `Student` gains `packageLessons` and `packagePriceCents`.
- **Data** (`core/data`).
  - Tables `payment`, `payment_lesson` and `package`. On `lesson`: `payer`, `due_cents` and `package_id`. On `student`: the two package terms. Migration 3 → 4, written by hand (see Data).
  - View `v_balance`: per payer, Σ `due_cents` of its classes minus Σ `amount_cents` of its payments that aren't package purchases.
  - `PaymentRepository`:
    - `balances(): Flow<Map<PayerRef, Cents>>` for the lists and Hoje.
    - `ledger(payer): Flow<Ledger>`: the payer's owed classes, its payments with their chosen classes, and the `Allocation`. The detail screens, lesson detail and the payment sheet read it.
    - `openPackage(studentId)` and `payment(id)`.
- **Freezing who owes** (`app/usecases/Lessons.kt`).
  - `freezeDone` and `cancel` call one new helper, `withDue(ctx, row, lesson)`. It sets `payer`, `due_cents` and `package_id` from `dueOf`, with the student's open package when it has a class left.
  - `undo` and `Live.discard` clear the three columns (AC-008-3, 22).
  - `Schedule` edits of a done class (AC-006-12) go through `withDue` again. `Live.end` already goes through `freezeDone`.
  - Done classes from before 008 get their columns filled once by `Payments.backfill()`, run from `StartApp`. It runs the same `withDue`, so rounding stays in the domain (rule 8). A migration in SQL would have to round on its own.
- **Use cases** (`app/usecases/Payments.kt`, class `Payments`). Each runs in one transaction, and Room re-emits the balances and ledgers (AC-008-10).
  - `record(draft)`: payer, amount > 0, method, date (today or earlier, `FUTURE_DATE` otherwise), note and chosen lesson ids, which must be owed by that payer. `paid_at` is now for today, and noon local time for an earlier day (AC-008-5 to 8).
  - `edit(id, draft)` and `delete(id)`. Deleting a package's payment also removes the package. The classes that used it go back to owing the package price per class (AC-008-12).
  - `buyPackage(studentId, method, date)` writes the package with the student's current terms, and its payment with `package_id` (AC-008-20).
  - `lastMethod(payer)` is the method the sheet starts at (AC-008-5).
  - `Lessons.logLesson` takes `paidWith: PayMethod?`. With one, the same transaction also records a payment of the class's due that chooses this class (AC-008-17).
- **Screens** (new package `app/ui/payments/`).
  - `PaymentSheet` (`BottomSheet`):
    - `Money` field starting at the balance, method `Chip`s and a date field (the one from `LessonForm`), plus a note `TextField`.
    - A preview line from `allocate` with the draft added ("Paga 2 aulas: 15/10 e 22/10").
    - "Escolher aulas" opens a `Checkbox` list, newest first, with what is left on each. Ticking sums into the amount, which stays editable.
    - The save `Button` reads "Registrar · R$ 240,00" (AC-008-5 to 7).
    - It is reused for editing, and opens pre-ticked from Aula concluída (AC-008-18).
  - `PaymentDetail` route: amount, method, date, note and the classes it paid, with Editar (the sheet) and Excluir with a confirmation (AC-008-12).
  - **Student, group and place details:**
    - A balance card at the top: Deve / Me deve / Em dia / Crédito, using the `Money` component with the `danger` and `success` tokens.
    - Registrar pagamento, or Receber da salinha for a place that pays (its label follows the place type, AC-008-14).
    - Últimas aulas with `Badge` Pago / A receber · falta (AC-008-9).
    - Pagamentos at the end, with the "Nenhum pagamento ainda" `EmptyState` (AC-008-11).
    - On a package student's detail, a package card with a `ProgressBar` ("7 de 10 usadas"), "Resta 1 aula no pacote", and Novo pacote (AC-008-20, 21).
  - **Lists** (`Registry.kt`, `Places.kt`): the balance as a `Badge` on Alunos and Turmas rows, and the package use on a package student's row. Alunos gets a "Devendo · n" filter chip beside Todos and Por local. Locais shows "A receber da salinha: R$ 360" (AC-008-13, 14).
  - **Hoje:** an `AlertBanner` with the count of payers who owe and their sum. It opens Alunos with Devendo on, through a route argument (AC-008-15).
  - **Lesson detail:** a Pago / A receber `Badge` on the value, and a "Saldo" `ListRow` with the payer's balance and owed count, opening the payer (AC-008-16).
  - **Registrar aula dada:** a Pagamento `SegmentedControl` (Fica a receber / Já foi pago), with method chips under Já foi pago (AC-008-17).
  - **Aula concluída:** the balance line and Registrar pagamento (AC-008-18), plus "Resta 1 aula no pacote" when it applies (AC-008-21).
  - **Student edit:** the charging `SegmentedControl` gets Pacote. Under it, a `Stepper` for the number of classes (10 at first) and a money field for the price (AC-008-19).
  - Every design-system component needed already exists, so the design system doesn't change.
- **Copy** goes in `strings_features.xml`, in both languages, with plurals for aulas and alunos. Money and dates are formatted at the edge (AC-008-24).
- Version 1.8.0.

## Data

| Table | Column | Default / backfill |
| --- | --- | --- |
| payment (new) | `id` TEXT PK | UUIDv7 |
| | `student_id` / `group_id` / `place_id` TEXT, FK | CHECK exactly one is set (architecture doc) |
| | `amount_cents` INTEGER | CHECK > 0 |
| | `method` TEXT | `pix` / `cash` / `transfer` / `card` |
| | `paid_at` TEXT | UTC instant |
| | `note` TEXT | null |
| | `package_id` TEXT FK → package, cascade | null; set only on a package purchase |
| | `created_at`, `updated_at` | now |
| payment_lesson (new) | `payment_id` FK cascade, `lesson_id` FK cascade, PK both | the classes chosen in "Escolher aulas" or the logged class |
| package (new) | `id`, `student_id` FK, `lessons_total` INTEGER, `price_cents` INTEGER, `bought_at` TEXT, `created_at`, `updated_at` | lessons used = classes whose `package_id` is it, never stored |
| lesson | `payer` TEXT | null; `student` / `group` / `place` once owed |
| | `due_cents` INTEGER | null; filled by `Payments.backfill()` at start-up for existing done and charged classes |
| | `package_id` TEXT FK → package, set null | null |
| student | `package_lessons` INTEGER, `package_price_cents` INTEGER | null; `rate_type` may now be `package` |

Indexes: `payment(student_id)`, `payment(group_id)`, `payment(place_id)`, `lesson(payer)`, `lesson(package_id)`. View `v_balance(payer, payer_id, due_cents, paid_cents)`.

The migration 3 → 4 is written by hand: SQLite can't add a CHECK to existing columns through Room's auto-migration, and the view is created there.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `Payments.record` | payment, payment_lesson | none | balances, ledger |
| `Payments.edit` | payment, payment_lesson | none | balances, ledger |
| `Payments.delete` | payment (+ package, lesson.package_id/due) | none | balances, ledger, lessons |
| `Payments.buyPackage` | package, payment | none | balances, ledger, student |
| `Payments.backfill` | lesson (payer, due, package) | none | balances |
| `Lessons.markDone` / `cancel` / `undo`, `Live.end` / `discard`, `Schedule` edit of a done class | lesson (+ payer, due, package) | as before | lessons, balances, ledger |
| `Lessons.logLesson` (paid) | lesson, payment, payment_lesson | none | lessons, balances, ledger |
| `Catalog.saveStudent` | student (package terms) | none | students |

## Test strategy

- **Domain:** `PaymentRulesTest`: `payerOf`, `dueOf` for done, charged, scheduled, no price, salary and package, `allocate` (oldest first, chosen first, partial, credit), `Balance`, `packageUnit` rounding (AC-008-1, 2, 4, 8, 23).
- **Use cases:** `PaymentsTest` on a real database with a frozen clock (AC-008-3, 10, 12, 17, 20, 22).
- **Data:** `MigrationsTest` covers the 3 → 4 step from a database seeded at version 3, including the payer CHECK (rule 12).
- **Screens:** `PaymentScreensTest` under Robolectric (AC-008-5, 6, 7, 9, 11, 13, 14, 15, 16, 18, 19, 21, 24).
- The source check that screens never touch `:core:data` already covers the new package.
- Pending on a device: a Maestro "record a payment" flow and the tab-switch budget with a year of payments.

## Risks

- **A write path that forgets `withDue`.** Every lesson write that changes the status or the minutes goes through `freezeDone`, `cancel`, `undo` or `withDue`. `PaymentsTest` checks each path's `due_cents`.
- **Allocation cost.** It runs for one payer at a time: a year of a weekly student is about 50 classes. The lists read only `v_balance`.
- **The payer frozen at done.** Changing a place's payer later doesn't move classes already given, which matches their frozen price. Classes still scheduled follow the new payer.
- **Package and travel.** A package class still owes its travel fee at Casa do Aluno, assumed in the spec for Victor to confirm.
