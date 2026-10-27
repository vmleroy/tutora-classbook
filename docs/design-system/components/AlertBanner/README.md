# AlertBanner

Tinted full-width row that calls attention to something that needs action, and opens it when tapped.

## Tones
`danger` money owed or overdue, `accent` something ending soon, `secondary` maintenance (backup, rate offline), `success` a confirmation that stays.

## Rules
- One banner per screen, below the main card. The message names the count and the thing; the value sits on the right.

## Props

```ts
interface AlertBannerProps {
  tone?: 'danger' | 'accent' | 'success' | 'secondary';
  icon?: IconName;
  message: string;
  value?: string;
  onPress?: () => void;
}
```
