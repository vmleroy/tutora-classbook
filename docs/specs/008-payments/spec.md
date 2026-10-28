# Spec 008: Payments and packages

Status: in progress
Depends on: 006 Lessons (the done value, cancellation charges), 004 Places and students (who pays, groups), 007 Live timer (Aula concluída)
Mockups: docs/mockups/StudentDetail.dc.html (Deve, Registrar pagamento, Últimas aulas with A receber / Pago, Pagamentos), PlaceDetail.dc.html (Me deve, Receber da salinha, Pagamentos da salinha), Students.dc.html (balance per row, Devendo filter, package row), Places.dc.html (A receber da salinha), Main.dc.html (pending-payments alert), LessonDetail.dc.html (Saldo da aluna), Live.dc.html (balance line on Aula concluída), AddLesson.dc.html (Pagamento in Registrar), StudentEdit.dc.html (Pacote). There is no mockup for the payment sheet itself.

## Goal

The teacher can now see who owes them money and record it when it comes in. Every class that was given, and every cancellation that was charged, adds to the balance of whoever pays for it: the student, the group, or the place when the place pays the teacher. A payment brings that balance down. By default it pays the oldest classes first, and the teacher can also pick the classes it pays. A student can also prepay a package of classes, which each class given uses up. This is the "who owes me" half of Tutora; earnings (009) and reports (010) build on it.

## User stories

- **US-1.** As a teacher, I see on Ana's detail that she owes R$ 240 for two classes, and record her Pix in three taps when she pays.
- **US-2.** As a teacher, at month end I open Salinha Aprender+ and settle everything it owes me in one payment.
- **US-3.** As a teacher, Pedro pays me for one specific class, not his oldest one, and I record exactly that.
- **US-4.** As a teacher, Mariana buys a package of 10 classes up front, and I see how many she has left and get warned when only one is left.
- **US-5.** As a teacher, I open Hoje and see at once how many students still owe me and how much.

## Acceptance criteria

### Who owes

- **AC-008-1.** A class marked done owes its value to whoever pays for it: the student, the group (as one, whatever its members), or the place when the place pays the teacher. A class with no price, and a class at an Escola (covered by the salary), owe nothing.
- **AC-008-2.** A cancelled or missed class with a charge owes that charge to the same payer. Without a charge it owes nothing.
- **AC-008-3.** Scheduled and live classes owe nothing. Undoing a done class or a charge (AC-006-13) takes it off the balance; any money already paid for it stays with the payer and pays the next owed class.
- **AC-008-4.** The balance of a student, group or place is what its classes owe minus what it has paid. It shows as "Deve R$ 240", "Em dia" at zero, or "Crédito R$ 40" when it has paid more than it owes.

### Recording a payment

- **AC-008-5.** Registrar pagamento on a student's or group's detail, and Receber da salinha on a place that pays the teacher, open the payment sheet: the amount, starting at the balance owed; the method (Pix, Dinheiro, Transferência, Cartão), starting at the last method used for that payer, or Pix; the date (today or earlier, never later); an optional note; and Salvar with the amount ("Registrar · R$ 240,00").
- **AC-008-6.** By default the sheet says which classes the payment pays ("Paga 2 aulas: 15/10 e 22/10"), taken oldest first while the amount lasts. Any amount above zero can be saved, less or more than the balance.
- **AC-008-7.** "Escolher aulas" in the sheet lists the payer's owed classes, newest first, with date, minutes and what is still owed on each. Ticking classes sets the amount to their total. The amount can still be typed over afterwards.
- **AC-008-8.** A payment pays the classes ticked for it first. Money left over, from any payment, pays the oldest owed classes. Money left after every owed class is paid is the payer's credit, and pays the next classes as they are given.
- **AC-008-9.** A class paid in full shows "Pago" in green wherever its money is shown. A class still owing all or part shows "A receber", with what is left when it was paid in part ("A receber · falta R$ 40").
- **AC-008-10.** Saving a payment updates the balance, the classes it pays, the lists and Hoje at once.

### Seeing and changing payments

- **AC-008-11.** The detail of a student, group or place shows its balance at the top ("Deve", or "Me deve" for a place) and, at the end, Pagamentos, newest first: date, method, how many classes it paid ("Pix · 4 aulas") and "+ R$ 540". A payer with none shows "Nenhum pagamento ainda".
- **AC-008-12.** Tapping a payment opens it with Editar and Excluir. Editing changes the amount, method, date, note or chosen classes, and the balance follows. Excluir asks to confirm and names the amount and date ("Excluir pagamento de R$ 540,00 de 16/10?"); the classes it paid go back to owing.

### Where balances show

- **AC-008-13.** Each row on Alunos and Turmas shows its balance (Deve R$ 240, Em dia or Crédito R$ 40). Alunos has a "Devendo" filter with a count ("Devendo · 3") listing only students who owe.
- **AC-008-14.** A place that pays the teacher shows "A receber da salinha: R$ 360" (or of the cursinho, etc., after its type) on Locais. Its detail has Receber da salinha, which pays everything it owes by default.
- **AC-008-15.** Hoje shows "3 alunos com pagamento pendente · R$ 560" when any student, group or place owes. Tapping it opens Alunos with Devendo on. With nothing owed it is not shown.
- **AC-008-16.** A lesson's detail shows "Pago" or "A receber" by its value, and the payer's balance ("Saldo da aluna · 2 aulas a receber · R$ 240,00"), which opens the payer.

### Paying with the class

- **AC-008-17.** Registrar aula dada has a Pagamento section: "Fica a receber" (the default) or "Já foi pago", which shows the method. Saving with "Já foi pago" also records a payment of the class's value that pays this class.
- **AC-008-18.** Aula concluída (after ending a live class) says "Adicionado ao saldo da aluna. Saldo pendente agora: R$ 360,00" (or of the group, or the place) and offers Registrar pagamento, which opens the payment sheet with that class ticked.

### Packages

- **AC-008-19.** A student's edit form offers Pacote as a third way to charge, beside Por hora and Por aula: how many classes (− / +, 10 at first) and the price for all of them. It holds at any of the student's places where the student pays.
- **AC-008-20.** Buying a package on the student's detail ("Novo pacote") records its payment with a method and date. From then on each class of that student marked done, at a place where the student pays, uses one class of the package instead of owing its value.
- **AC-008-21.** The student's detail and their row on Alunos show the package's use ("7 de 10 usadas"). When one class is left it says so ("Resta 1 aula no pacote"), on the detail and on Aula concluída.
- **AC-008-22.** Undoing a done class that used the package gives that class back to the package.
- **AC-008-23.** A class given after the package is used up owes the package's price per class (the price ÷ the number of classes, rounded to the centavo once) until a new package is bought. A cancellation charge is always owed as money and never uses a package class.

### Language

- **AC-008-24.** Balances, payment methods, the sheet and every label follow the app language; switching it never changes an amount, a balance or which classes are paid.

## Out of scope

- **009:** the "Alunos devem" money card and the other money cards on Hoje, received vs. earned, and the place's commission in earnings. A school's monthly salary as money owed or received.
- **010:** balances and payments in reports.
- **014:** the weekly "who still owes you" reminder.
- More than one package open at a time for the same student, and packages for groups.
- Refunds and paying money back out of a credit.

## Open questions

Answered by Victor, 28 Oct 2026:

- Which classes a payment pays mixes three ways: the balance is one total that payments bring down; by default a payment pays the oldest classes first; the teacher can also pick the classes it pays (AC-008-6 to 8).
- A group class is owed by the group as one, with its own balance and payments, whatever its members.
- Packages are part of 008.
- The methods are Pix, Dinheiro, Transferência and Cartão.

Assumed, for Victor to confirm in review:

- At a place where the student pays and the place keeps a commission, the student owes the class's full value. What the teacher keeps after commission is a 009 earnings question.
- A charged cancellation is owed by the same payer as the class would have been.
- Paying more than is owed is allowed and kept as credit; the sheet doesn't block it.
- Ticking classes in "Escolher aulas" fills in the amount but doesn't lock it.
- The pending-payments alert on Hoje counts students, groups and places together ("3 com pagamento pendente" if it reads better than "alunos" when a place is among them).
- A package's class is used when the class is marked done, whatever its minutes; a package bought while classes are still owed doesn't pay those classes.
- A package covers the class, not its travel: at Casa do Aluno a package class still owes its travel fee.
- Archiving a student, group or place keeps its balance and payments; it still counts on Hoje until it is settled.
