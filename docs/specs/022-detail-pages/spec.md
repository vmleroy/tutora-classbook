# Spec 022: Detail pages, clearer and connected

Status: draft
Depends on: 004 Places and students (details, links, groups), 008 Payments and packages (balances, history, credits, packages), 016 Visual refresh (the look), 017 Landscape (the side panel), 020 (the group split, the student's list)
Mockups: docs/mockups/StudentDetail.dc.html, PlaceDetail.dc.html, A_Aluno.dc.html (today's). New mockups for the three details come with the plan, and Victor picks between two directions before it is ready.

## Goal

The detail of a student, a group and a place has grown one section at a time: three or four figures, the main button and two small ones, the package, credits and bonuses, the places, the groups, the split, the classes and the payments, all stacked in one long scroll. The teacher has to read everything to find the one thing they came for, and the links between them (Ana is in Inglês B, which is at Ventura, which pays per class) are spread over three screens. After this spec each detail answers its main question at the top in one line, puts the rest in a few clear parts, and shows the connections as a small map the teacher can walk through: from a student to their groups and places, from a group to its place and members, from a place to its groups and students, each link with the one figure that matters there.

## User stories

- **US-1.** As a teacher, I open Ana and see at once whether she owes me and when her next class is, without scrolling.
- **US-2.** As a teacher, I see that Ana is in Inglês B at Ventura and what she pays there, and I get to the group with one tap.
- **US-3.** As a teacher, on Ventura I see its groups and students and what each owes, without opening each one.
- **US-4.** As a teacher, the three details look and work the same way, so I always know where things are.

## Acceptance criteria

### One structure for the three

- **AC-022-1.** The student, group and place details share one structure, top to bottom: who it is, the status line, the main action, the parts (AC-022-4), in the same places on each. Editar and Arquivar stay where they are today.
- **AC-022-2.** The status line says the one thing that matters most, in words: "Deve R$ 240,00 · 2 aulas", "Em dia", "Crédito de R$ 30,00", "Te deve R$ 1.200,00 · paga no dia 5" for a place, "Coberto pelo salário" for a school's group. Red while something is owed, as today (AC-016-2).
- **AC-022-3.** Under it, "Próxima aula": day, time and place, tapping into the class; "Nenhuma aula marcada" with Nova aula when there is none.

### The parts

- **AC-022-4.** The rest is in parts with a title each: Resumo, Aulas, Pagamentos and Ligações. Resumo holds the figures of the month (classes, hours, earned or paid) and, only when they apply, the package, credits and bonuses, the split.
- **AC-022-5.** A part that has nothing doesn't show (no package, no credits, a place with no groups); nothing is shown empty "just in case".
- **AC-022-6.** Aulas and Pagamentos show the last 5 with "Ver todas", as today (AC-008-9); Registrar pagamento and Relatório sit in their part, not in a row of buttons at the top.

### Connections

- **AC-022-7.** Ligações shows the links as one list grouped by kind: on a student, their places and groups; on a group, its place and members; on a place, its groups and students. Each row has the name, its kind, and the one figure that matters for this link: the student's price at a place, their share in a split group, what a member owes, how many students a group has.
- **AC-022-8.** Every row in Ligações opens that detail, and the way back returns here. Sideways, it opens in the side panel (AC-017-11).
- **AC-022-9.** Adding and removing a link (Ligar aluno, Adicionar à turma, Trocar local) happens in Ligações, with the confirmations of today (AC-004-33 to 35).
- **AC-022-10.** A student's row for a group says how its classes are paid there: "sua parte R$ 50,00/aula" when split, "a turma paga" or "o local paga".

### Fit

- **AC-022-11.** Every detail fits a 360 dp wide phone at Muito grande without anything cut, and the status line and Próxima aula are in view without scrolling on a 640 dp tall phone at the normal size.

### Language

- **AC-022-12.** Every new word is in Português and English.

## Out of scope

- Changing what is stored or how balances are worked out: this spec only reorganises what the details show.
- The lesson detail (it is already one class) and the Cadastro lists.
- A graph drawing of the connections.

## Open questions

For Victor, before this is marked ready:

1. **Parts as tabs or one scroll.** Resumo, Aulas, Pagamentos and Ligações as tabs at the top of the detail (one at a time, shorter screen), or one scroll with each part titled and collapsible?
2. **What is in Resumo.** Which figures belong there for a student, a group and a place? Today: Deve, Aulas no mês, Preço. Any to add or drop?
3. **Packages, credits and bonuses.** Do they stay in Resumo when in use, or move into Pagamentos?
4. **The main action.** Is Registrar pagamento the main button on every detail, or should it depend on the status (Nova aula when nothing is owed)?
5. **Two directions.** May the plan bring two mockup directions for you to pick from (for example tabs vs. one scroll), before it is marked ready?
