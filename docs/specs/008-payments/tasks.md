# Tasks 008: Payments and packages

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain: `PayerRef`, `PayMethod`, `payerOf`, `dueOf`, `Balance`, `divideRounded` and `packageUnit`; `RateType.PACKAGE`, the student's package terms and `PriceSource.PACKAGE` in `resolvePrice`. Test: `core/domain/src/test/kotlin/app/tutora/domain/PaymentRulesTest.kt`. (AC-008-1, 2, 4, 23)
- [x] 2. Domain: `allocate`: chosen classes first, then oldest first, partial payments, credit. Test: `PaymentRulesTest.kt`. (AC-008-6, 8)
- [x] 3. Data: tables `payment`, `payment_lesson` and `package`; lesson `payer`, `due_cents` and `package_id`; student package terms; the balance query; migration 3 → 4 and exported schema 4; `PaymentRepository` (`balances`, `ledger`, `lastPackage`, `payment`). Test: `core/data/src/test/kotlin/app/tutora/data/MigrationsTest.kt` (3 → 4).
- [x] 4. Use cases: `withDue` in `freezeDone`, `cancel`, `undo`, `Live.end` / `discard` and the `Schedule` edit of a done class; `Payments.backfill` from `StartApp`. Test: `app/src/test/kotlin/app/tutora/PaymentsTest.kt`. (AC-008-3, 22)
- [x] 5. Use cases: `Payments.record`, `edit`, `delete`, `lastMethod`; refuse future dates and classes owed by someone else. Test: `PaymentsTest.kt`. (AC-008-10, 12)
- [x] 6. Use cases: `Payments.buyPackage`; package classes use the package, then owe the price per class once it runs out; deleting a package's payment. Test: `PaymentsTest.kt`. (AC-008-20)
- [x] 7. Use cases: `Lessons.logLesson` with `paidWith`. Test: `PaymentsTest.kt`. (AC-008-17)
- [x] 8. Screens: `PaymentSheet` (amount, method, date, note, preview line, Escolher aulas, save label) and the `PaymentDetail` route with Editar and Excluir. Test: `app/src/test/kotlin/app/tutora/PaymentScreensTest.kt`. (AC-008-5, 6, 7)
- [x] 9. Screens: balance card, Registrar pagamento / Receber da salinha, Pago / A receber badges and Pagamentos on the student, group and place details. Test: `PaymentScreensTest.kt`. (AC-008-9, 11, 14)
- [x] 10. Screens: balances on Alunos, Turmas and Locais rows; the Devendo filter; the Hoje alert opening Alunos › Devendo. Test: `PaymentScreensTest.kt`. (AC-008-13, 14, 15)
- [x] 11. Screens: lesson detail Pago / A receber and Saldo row; Pagamento in Registrar aula dada; balance line and Registrar pagamento on Aula concluída. Test: `PaymentScreensTest.kt`. (AC-008-16, 17, 18)
- [x] 12. Screens: Pacote in the student edit form; the package card, "7 de 10 usadas" on the detail and row, "Resta 1 aula no pacote" on the detail and Aula concluída, Novo pacote. Test: `PaymentScreensTest.kt`. (AC-008-19, 20, 21)
- [x] 13. Copy in `strings_features.xml` in both languages, with plurals; a test switching the language on a student with payments. Version 1.8.0. Test: `PaymentScreensTest.kt`. (AC-008-24)
- [x] 14. Update `docs/architecture.md` (frozen `payer` and `due_cents`, allocation in the domain) and `docs/product.md` (the package's lessons used is derived), run `./gradlew check` once, mark the spec done.
- [x] 15. Screens, after trying the build: Marcar como paga on the lesson detail; the places that pay for a student's classes on the student's detail; "Nenhuma aula a receber" in the sheet; the sheet's body scrolls (design system `BottomSheet`). Test: `PaymentScreensTest.kt`. (AC-008-25, 26)
- [x] 16. A student sees and pays their classes a place owes: `PaymentRepository.elsewhere`, `Payments.record` splits the money to the place, the student's list and sheet show them. Test: `PaymentsTest.kt`, `PaymentScreensTest.kt`. (AC-008-26, 27)
- [x] 17. Credits and bonus: `credit` table and migration 4 → 5, `credits` setting, held credit, credit uses and bonus minutes in `allocate`, `Payments.giveBonus` / `useCredit`, overpaying refused when off; Ajustes switch, credit line, Usar crédito and Dar bônus sheets. Test: `PaymentRulesTest.kt`, `PaymentsTest.kt`, `PaymentScreensTest.kt`, `MigrationsTest.kt`. (AC-008-28 to 31)
- [x] 18. Layout: `SectionHeader` action and tappable aside (design system), small + on connection sections, Aulas and Pagamentos sections with Ver todas page, names on class rows, no Valor card, place note in Locais, search in Nova aula's picker. Test: `PaymentScreensTest.kt`. (AC-008-32 to 34)
- [x] 19. Filters on Turmas (Todos, Devendo, Por local) and search with Todos / Devendo on Locais. Test: `PaymentScreensTest.kt`. (AC-008-35)
- [x] 20. Locais: the places where students pay show what is owed for their classes (`PaymentRepository.owedAtPlaces`). Test: `PaymentScreensTest.kt`. (AC-008-36)
- [x] 21. Créditos e bônus as its own section (edit, remove, give back), a bonus is money or minutes, the warning when turning it off, and using a bonus while marking a class done. Test: `PaymentsTest.kt`, `PaymentScreensTest.kt`. (AC-008-37, 38, 39)

## Pending on a device

- Maestro flow: record a payment from a student's detail and see the class turn Pago.
- Large-text screenshots of the payment sheet with Escolher aulas open, at 360 px.
- Tab switch under 300 ms on Alunos with a year of classes and payments (constitution rule 18).
- Start-up time after upgrading a phone with a year of classes from version 1.7.0 (the one-time fill of who owes each class).
