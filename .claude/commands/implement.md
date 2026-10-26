---
description: Implement a ready spec task by task, tests first
argument-hint: "NNN [task number]"
---
Implement spec $ARGUMENTS.

1. Check the spec is `ready` (or `in progress`) and that `plan.md` and `tasks.md` exist. If not, stop and suggest `/plan`.
2. Work on a branch named `spec/NNN-short-name` (create it from `main` if missing). Set Status: in progress.
3. Take the first unchecked task (or the one given). Write the tests first; each test name contains its criterion id, e.g. `it('AC-005-3: moves only this class', …)`. Run them and see them fail.
4. Write the smallest code that makes them pass, following the constitution: domain stays pure, writes are use cases, money is integer cents, no hex colors in components, every string through i18n in both languages.
5. Run `npm run typecheck && npm run lint && npm test`. Fix until green.
6. Check the task's box in `tasks.md` and commit: `NNN: <task summary>`.
7. Continue with the next task unless told to do only one. When all are done, run `/verify NNN`.
