---
description: Write the next spec (spec.md only) from the roadmap
argument-hint: '[NNN or feature name]'
---

Write `spec.md` for: $ARGUMENTS (if empty, the first "not written" row in `docs/specs/README.md`).

1. Read `docs/specs/constitution.md`, `docs/specs/README.md`, `docs/product.md` (the feature and flows it covers), the mockups listed for it in `docs/mockups/`, and the specs it depends on.
2. Copy `docs/specs/_templates/spec.md` to `docs/specs/NNN-short-name/spec.md` and fill it in. Criteria are numbered `AC-NNN-n`, each provable by one test, in plain product language with no technology names.
3. List anything the product doc leaves undecided under "Open questions" instead of guessing. Ask Victor about any question that changes what the teacher sees.
4. Set Status: draft and update the roadmap table in `docs/specs/README.md`.
   Stop after the spec. Do not write the plan or code.
