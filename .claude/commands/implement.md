---
description: Implement a ready spec in one pass, with lean tests
argument-hint: 'NNN [task number]'
---

Implement spec $ARGUMENTS.

1. Check the spec is `ready` (or `in progress`) and that `plan.md` and `tasks.md` exist. If not, stop and suggest `/plan`. Product questions still open in the spec go to Victor first, in one batch.
2. Work on a branch named `spec/NNN-short-name` (from `main`, or from the previous spec's branch if it hasn't merged). Set Status: in progress.
3. Implement all the tasks in one pass, following the constitution: domain stays pure, writes are use cases, money is integer cents, no hex colors in components, every string through i18n in both languages.
4. Test only what proves a criterion: one focused Jest test per `AC-NNN-n`, its id in the name, in the test file the task names. No red-first runs, no extra cases, no property tests unless the criterion is about "never" or "always". Update existing tests the change breaks; don't add tests for unchanged code.
5. Run the affected test files while working, then `npm run typecheck && npm run lint && npm test` once at the end. Fix until green.
6. No emulator or Maestro runs unless Victor asks; list device checks under "Pending on a device" in `tasks.md`.
7. Check the boxes in `tasks.md` and commit in a few logical commits: `NNN: <summary>`. Then run `/verify NNN`.
