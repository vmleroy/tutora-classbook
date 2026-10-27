---
description: Write plan.md and tasks.md for a spec
argument-hint: 'NNN'
---

Write `plan.md` and `tasks.md` for spec $ARGUMENTS.

1. Read the spec, `docs/specs/constitution.md`, `docs/architecture.md`, the mockups it lists, and the code that already exists.
2. Fill `docs/specs/_templates/plan.md`: layers touched, tables and migration, use cases, screens and design-system components, test strategy, risks. Follow the architecture doc; if the plan needs to depart from it or from the constitution, stop and say why.
3. Fill `docs/specs/_templates/tasks.md`: small ordered tasks, each naming the criteria it proves and the test file. Every `AC-NNN-n` in the spec appears in at least one task.
4. Resolve the spec's open questions that are technical; leave product questions for Victor.
5. Set the spec's Status to ready and update `docs/specs/README.md`.
