# TimerRing

The live lesson clock: elapsed time in a ring that fills toward the planned duration.

## Rules
- Only on the live lesson screen, on the `header` ground. The ring fills with `accent` and keeps going past 100% (the ring stays full).
- Elapsed is computed from stored timestamps every second; the money figure below it uses Money `display` with `tone="onHeader"`.
- Screen readers get the elapsed time once a minute, not every second.

## Props

```ts
interface TimerRingProps {
  elapsed: string;
  progress: number;
  caption?: string;
  label?: string;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}
```
