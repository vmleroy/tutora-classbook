import { View } from 'react-native';

import { cn } from '@/ui/cn';

export interface ProgressBarProps {
  value: number;
  tone?: 'accent' | 'success' | 'primary' | 'secondary';
  track?: 'accent' | 'danger' | 'sunken';
  thin?: boolean;
  label: string;
}

const trackClass = {
  accent: 'bg-accent-soft',
  danger: 'bg-danger-soft',
  sunken: 'bg-surface-sunken',
};
const fillClass = {
  accent: 'bg-accent',
  success: 'bg-success',
  primary: 'bg-primary',
  secondary: 'bg-secondary',
};

/** A bar always shown beside the numbers it stands for. */
export function ProgressBar({
  value,
  tone = 'accent',
  track = 'accent',
  thin,
  label,
}: ProgressBarProps) {
  const pct = Math.round(Math.max(0, Math.min(1, value)) * 100);
  return (
    <View
      accessible
      accessibilityRole="progressbar"
      accessibilityLabel={label}
      accessibilityValue={{ min: 0, max: 100, now: pct }}
      className={cn(
        'flex-row overflow-hidden rounded-full',
        thin ? 'h-1' : 'h-2',
        trackClass[track],
      )}
    >
      <View className={cn('h-full rounded-full', fillClass[tone])} style={{ width: `${pct}%` }} />
    </View>
  );
}
