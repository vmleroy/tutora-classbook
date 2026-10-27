import { useTranslation } from 'react-i18next';
import { View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';

import { cn } from '@/ui/cn';
import { Text } from '@/ui/components/Text';
import { useTheme } from '@/ui/theme/ThemeProvider';

export interface TimerRingProps {
  elapsed: string;
  progress: number;
  caption?: string;
  label?: string;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const SIZE = 260;
const STROKE = 14;
const R = (SIZE - 2 * 10) / 2; // 120
const C = 2 * Math.PI * R;
const TRACK_ALPHA = 0.14;

/** The live lesson clock on the header ground; the ring stays full past the planned time. */
export function TimerRing({ className, elapsed, progress, caption, label }: TimerRingProps) {
  const { t } = useTranslation();
  const { palette } = useTheme();
  const name = label ?? t('ui.lessonTime');
  const filled = Math.max(0, Math.min(1, progress));
  return (
    <View
      accessible
      accessibilityRole="timer"
      accessibilityLabel={`${name}: ${elapsed}`}
      className={cn('items-center justify-center', className)}
      style={{ width: SIZE, height: SIZE }}
    >
      <Svg
        width={SIZE}
        height={SIZE}
        viewBox={`0 0 ${SIZE} ${SIZE}`}
        style={{ position: 'absolute' }}
      >
        <Circle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={R}
          fill="none"
          stroke={palette['on-header']}
          strokeOpacity={TRACK_ALPHA}
          strokeWidth={STROKE}
        />
        <Circle
          testID="timer-fill"
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={R}
          fill="none"
          stroke={palette.accent}
          strokeWidth={STROKE}
          strokeLinecap="round"
          strokeDasharray={C}
          strokeDashoffset={C * (1 - filled)}
          transform={`rotate(-90 ${SIZE / 2} ${SIZE / 2})`}
        />
      </Svg>
      <Text variant="field-label" className="text-on-header-muted">
        {name}
      </Text>
      <Text variant="money-display" className="text-on-header">
        {elapsed}
      </Text>
      {caption ? (
        <Text variant="caption" className="text-on-header-muted">
          {caption}
        </Text>
      ) : null}
    </View>
  );
}
