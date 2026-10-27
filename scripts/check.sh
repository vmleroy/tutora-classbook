#!/usr/bin/env bash
# The gate before every commit: tokens, types, lint, tests. Exits non-zero on the first failure.
set -euo pipefail
npm run --silent tokens -- --check
npm run --silent typecheck
npm run --silent lint
npm test --silent 2>&1 | tail -5
