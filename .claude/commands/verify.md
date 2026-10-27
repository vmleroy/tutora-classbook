---
description: Check that every acceptance criterion of a spec has a passing test
argument-hint: "NNN"
---
Verify spec $ARGUMENTS.

1. List every `AC-NNN-n` in the spec. For each, grep the tests for that id and report covered or missing; criteria marked "manual" in tasks.md need a note in the PR instead.
2. Run `npm run typecheck && npm run lint && npm test` and report the result.
3. Check the diff against the constitution (layers, money, i18n keys in both files, no hex in components, migrations forward-only).
4. If everything passes, set Status: done in the spec and `docs/specs/README.md`, and draft the PR description: what changed, the criteria table, manual checks, screenshots to take.
