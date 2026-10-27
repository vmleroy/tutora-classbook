# EmptyState

What a list shows when it has nothing yet: what will appear here and the one action that adds the first item.

## Rules
- Title says what is missing; body says what the user gets by adding one; one primary button.
- The ruled-paper background (lines every 28px in `line-soft`) is reserved for empty states.

## Props

```ts
interface EmptyStateProps {
  icon?: IconName;
  title: string;
  body?: string;
  action?: React.ReactNode;
}
```
