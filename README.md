# Tutora

A classroom in a phone: an offline-first app that shows a teacher what they teach today, who owes them, and how much they earned this month.

**Status:** design and architecture settled; implementation starts with [spec 001](docs/specs/001-foundation/spec.md).

## Stack

Expo (React Native, TypeScript strict, Expo Router) · NativeWind (Tailwind) · expo-sqlite + Drizzle · TanStack Query + Zustand · i18next (pt-BR, en) · Jest, React Native Testing Library, fast-check, Maestro. Android first, sideloaded APK.

## What is in this repo

| Path                                           | What                                                                                            |
| ---------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| [`CLAUDE.md`](CLAUDE.md)                       | How Claude Code works in this repo, spec by spec                                                |
| [`docs/specs/`](docs/specs/)                   | Constitution, roadmap and the specs (spec, plan, tasks)                                         |
| [`docs/product.md`](docs/product.md)           | Product documentation v1                                                                        |
| [`docs/architecture.md`](docs/architecture.md) | Architecture and technology v1                                                                  |
| [`docs/mockups/`](docs/mockups/)               | The 27 screen mockups                                                                           |
| [`docs/design-system/`](docs/design-system/)   | Design system usage rules, Tailwind and theming notes, and a guideline with props per component |
| [`src/ui/`](src/ui/)                           | The design system in code: tokens (`theme/tokens/tokens.json`), theme and the shared components        |
| [`.claude/commands/`](.claude/commands/)       | `/spec`, `/plan`, `/implement`, `/verify`                                                       |

## Working with Claude Code

```bash
git clone git@github.com:vmleroy/tutora-classbook.git
cd tutora-classbook
claude
```

Then, for example: `/implement 001`. Claude Code reads `CLAUDE.md` on start and follows the spec cycle.

Published references: [design system](https://claude.ai/artifact/VqkNrENr4qcTcuDvqYwPei) · [mockup canvas](https://claude.ai/artifact/B8YKPsZff8TMJ7qLQd1JJY).
