---
description: Check that every acceptance criterion of a spec has a passing test
argument-hint: 'NNN'
---

Verify spec $ARGUMENTS.

1. List every `AC-NNN-n` in the spec. For each, grep the tests for that id and report covered or missing; criteria marked "manual" or "pending on a device" in tasks.md need a note in the PR instead.
2. Run `npm run typecheck && npm run lint && npm test` once (skip if it already ran green on this exact tree) and report the result.
3. Check the diff against the constitution (layers, money, i18n keys in both files, no hex in components, migrations forward-only).
4. If everything passes, set Status: done in the spec and `docs/specs/README.md`, push the branch and open the PR: what changed, the criteria table, device checks pending.
