import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { cn } from '@/ui/cn';
import type { IconName } from '@/ui/components/Icon';
import { IconButton, type IconButtonProps } from '@/ui/components/IconButton';
import { Text } from '@/ui/components/Text';
import { spacing } from '@/ui/theme/tokens';

export interface ScreenHeaderProps {
  variant?: 'hero' | 'tab' | 'push' | 'modal';
  title: string;
  eyebrow?: string;
  actions?: {
    icon: IconName;
    label: string;
    variant?: IconButtonProps['variant'];
    onPress?: () => void;
  }[];
  action?: ReactNode;
  onBack?: () => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

/** The top of every screen. `hero` sits on the header token: primary in light, a deep shade in dark. */
export function ScreenHeader({
  className,
  variant = 'tab',
  title,
  eyebrow,
  actions = [],
  action,
  onBack,
}: ScreenHeaderProps) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const buttons = actions.map((a) => (
    <IconButton
      key={a.icon}
      icon={a.icon}
      label={a.label}
      variant={a.variant ?? (variant === 'hero' ? 'onPrimary' : 'sunken')}
      onPress={a.onPress}
    />
  ));

  if (variant === 'hero') {
    return (
      <View
        testID="screen-header-hero"
        className={cn(
          'flex-row items-start justify-between rounded-b-3xl bg-header px-5 pb-6',
          className,
        )}
        style={{ paddingTop: insets.top + spacing['7'] }}
      >
        <View className="min-w-0 flex-1 gap-0.5">
          {eyebrow ? (
            <Text variant="caption" weight={800} className="text-on-header-muted">
              {eyebrow}
            </Text>
          ) : null}
          <Text variant="title-xl" accessibilityRole="header" className="text-on-header">
            {title}
          </Text>
        </View>
        <View className="flex-row gap-2">{buttons}</View>
      </View>
    );
  }

  return (
    <View
      testID={`screen-header-${variant}`}
      className={cn('flex-row items-center gap-3 px-5 pb-3', className)}
      style={{ paddingTop: insets.top + spacing[variant === 'tab' ? '6' : '5'] }}
    >
      {onBack ? (
        <IconButton
          icon={variant === 'modal' ? 'close' : 'chevron-left'}
          label={variant === 'modal' ? t('ui.close') : t('ui.back')}
          onPress={onBack}
        />
      ) : null}
      <View className="min-w-0 flex-1 gap-0.5">
        <Text variant={variant === 'tab' ? 'title-xl' : 'title-lg'} accessibilityRole="header">
          {title}
        </Text>
        {eyebrow ? (
          <Text variant="caption" weight={800} className="text-ink-muted">
            {eyebrow}
          </Text>
        ) : null}
      </View>
      {buttons}
      {action ?? null}
    </View>
  );
}
