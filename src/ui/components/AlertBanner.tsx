import type { PaletteToken } from '@/domain/theme/palette';

import { Icon, type IconName } from './Icon';
import { Text } from './Text';
import { Touchable } from './Touchable';

type Tone = 'danger' | 'accent' | 'success' | 'secondary';

export interface AlertBannerProps {
  tone?: Tone;
  icon?: IconName;
  message: string;
  value?: string;
  onPress?: () => void;
}

const ground: Record<Tone, string> = {
  danger: 'bg-danger-soft',
  accent: 'bg-accent-soft',
  success: 'bg-success-soft',
  secondary: 'bg-secondary-soft',
};
const ink: Record<Tone, PaletteToken> = {
  danger: 'danger-ink',
  accent: 'accent-ink',
  success: 'success-ink',
  secondary: 'secondary-ink',
};
const text: Record<Tone, string> = {
  danger: 'text-danger-ink',
  accent: 'text-accent-ink',
  success: 'text-success-ink',
  secondary: 'text-secondary-ink',
};

/** A tinted row that names what needs attention and opens it. One per screen. */
export function AlertBanner({
  tone = 'danger',
  icon = 'alert',
  message,
  value,
  onPress,
}: AlertBannerProps) {
  return (
    <Touchable
      accessibilityRole="button"
      accessibilityLabel={value ? `${message}, ${value}` : message}
      onPress={onPress}
      className={`min-h-touch flex-row items-center gap-2.5 rounded-xl px-3.5 py-3 ${ground[tone]}`}
    >
      <Icon name={icon} size={20} tone={ink[tone]} />
      <Text variant="row-title" className={`flex-1 ${text[tone]}`}>
        {message}
      </Text>
      {value ? (
        <Text variant="row-title" weight={800} tabular className={text[tone]}>
          {value}
        </Text>
      ) : null}
    </Touchable>
  );
}
