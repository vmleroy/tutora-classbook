# Tutora

A classroom in a phone: an offline-first app that shows a teacher what they teach today, who owes them, and how much they earned this month.

**Status:** being rebuilt in native Kotlin, starting again from [spec 001](docs/specs/001-foundation/spec.md). The first version (Expo + React Native, specs 001–005) is archived at the `expo-final` tag.

## Stack

Kotlin · Jetpack Compose (Material 3 under the Tutora theme) · Room · Coroutines and Flow · kotlinx-datetime · string resources (pt-BR, en) · JUnit, kotest-property, Robolectric, Roborazzi, Maestro. Android first, sideloaded APK; iOS much later through Kotlin Multiplatform.

## What is in this repo

| Path                                           | What                                                                                            |
| ---------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| [`CLAUDE.md`](CLAUDE.md)                       | How Claude Code works in this repo, spec by spec                                                |
| [`docs/specs/`](docs/specs/)                   | Constitution, roadmap and the specs (spec, plan, tasks)                                         |
| [`docs/product.md`](docs/product.md)           | Product documentation v1                                                                        |
| [`docs/architecture.md`](docs/architecture.md) | Architecture and technology v1                                                                  |
| [`docs/mockups/`](docs/mockups/)               | The 27 screen mockups                                                                           |
| [`docs/design-system/`](docs/design-system/)   | Design system usage rules, theming notes, and a guideline per component |
| [`design/tokens.json`](design/tokens.json)     | The design tokens: colors for 4 themes × light/dark, type, spacing, radius |
| [`.claude/commands/`](.claude/commands/)       | `/spec`, `/plan`, `/implement`, `/verify`                                                       |

## Working with Claude Code

```bash
git clone git@github.com:vmleroy/tutora-classbook.git
cd tutora-classbook
claude
```

Then, for example: `/implement 001`. Claude Code reads `CLAUDE.md` on start and follows the spec cycle.

Published references: [design system](https://claude.ai/artifact/VqkNrENr4qcTcuDvqYwPei) · [mockup canvas](https://claude.ai/artifact/B8YKPsZff8TMJ7qLQd1JJY).
