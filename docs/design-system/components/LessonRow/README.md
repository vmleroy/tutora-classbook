# LessonRow

One lesson in a day list: time, tag dot, who and where, and its state or value.

## Consumer provides
`time`, `tag`, `title` (student or group), `subtitle` (subject · place · duration), and either `status` (Badge) or `trailing` (expected value), `onPress` → Lesson detail.

## Rules
- Done: success badge with a check and "Dada". Missed: danger "Faltou". Cancelled: neutral "Cancelada". A charge joins the label ("Cancelada · cobra R$ 60,00"). Scheduled: expected value in ink-muted.
- Rows are 56px+ tall with `space-2` between them; swipe actions are not used.

## Props

```ts
interface LessonRowProps {
  time: string;
  tag: Tag;
  title: string;
  subtitle?: string;
  status?: { tone: BadgeProps['tone']; label: string };
  /** An icon inside the status badge (the check on a done class). */
  statusIcon?: IconName;
  trailing?: string;
  onPress?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
