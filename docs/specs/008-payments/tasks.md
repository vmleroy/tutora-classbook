# Tasks 008: Payments and packages

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [ ] 1. Domain: `PayerRef`, `PayMethod`, `payerOf`, `dueOf`, `Balance`, `divideRounded` and `packageUnit`; `RateType.PACKAGE`, the student's package terms and `PriceSource.PACKAGE` in `resolvePrice`. Test: `core/domain/src/test/kotlin/app/tutora/domain/PaymentRulesTest.kt`. (AC-008-1, 2, 4, 23)
- [ ] 2. Domain: `allocate`: chosen classes first, then oldest first, partial payments, credit. Test: `PaymentRulesTest.kt`. (AC-008-6, 8)
- [ ] 3. Data: tables `payment`, `payment_lesson` and `package`; lesson `payer`, `due_cents` and `package_id`; student package terms; view `v_balance`; migration 3 → 4 and exported schema 4; `PaymentRepository` (`balances`, `ledger`, `openPackage`, `payment`). Test: `core/data/src/test/kotlin/app/tutora/data/MigrationsTest.kt` (3 → 4).
- [ ] 4. Use cases: `withDue` in `freezeDone`, `cancel`, `undo`, `Live.end` / `discard` and the `Schedule` edit of a done class; `Payments.backfill` from `StartApp`. Test: `app/src/test/kotlin/app/tutora/PaymentsTest.kt`. (AC-008-3, 22)
- [ ] 5. Use cases: `Payments.record`, `edit`, `delete`, `lastMethod`; refuse future dates and classes owed by someone else. Test: `PaymentsTest.kt`. (AC-008-10, 12)
- [ ] 6. Use cases: `Payments.buyPackage`; package classes use the package, then owe the price per class once it runs out; deleting a package's payment. Test: `PaymentsTest.kt`. (AC-008-20)
- [ ] 7. Use cases: `Lessons.logLesson` with `paidWith`. Test: `PaymentsTest.kt`. (AC-008-17)
- [ ] 8. Screens: `PaymentSheet` (amount, method, date, note, preview line, Escolher aulas, save label) and the `PaymentDetail` route with Editar and Excluir. Test: `app/src/test/kotlin/app/tutora/PaymentScreensTest.kt`. (AC-008-5, 6, 7)
- [ ] 9. Screens: balance card, Registrar pagamento / Receber da salinha, Pago / A receber badges and Pagamentos on the student, group and place details. Test: `PaymentScreensTest.kt`. (AC-008-9, 11, 14)
- [ ] 10. Screens: balances on Alunos, Turmas and Locais rows; the Devendo filter; the Hoje alert opening Alunos › Devendo. Test: `PaymentScreensTest.kt`. (AC-008-13, 14, 15)
- [ ] 11. Screens: lesson detail Pago / A receber and Saldo row; Pagamento in Registrar aula dada; balance line and Registrar pagamento on Aula concluída. Test: `PaymentScreensTest.kt`. (AC-008-16, 17, 18)
- [ ] 12. Screens: Pacote in the student edit form; the package card, "7 de 10 usadas" on the detail and row, "Resta 1 aula no pacote" on the detail and Aula concluída, Novo pacote. Test: `PaymentScreensTest.kt`. (AC-008-19, 20, 21)
- [ ] 13. Copy in `strings_features.xml` in both languages, with plurals; a test switching the language on a student with payments. Version 1.8.0. Test: `PaymentScreensTest.kt`. (AC-008-24)
- [ ] 14. Update `docs/architecture.md` (frozen `payer` and `due_cents`, allocation in the domain) and `docs/product.md` (the package's lessons used is derived), run `./gradlew check` once, mark the spec done.
