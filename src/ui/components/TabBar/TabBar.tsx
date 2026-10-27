import { useTranslation } from 'react-i18next';
import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { cn } from '@/ui/cn';
import { Icon, type IconName } from '@/ui/components/Icon';
import { Text } from '@/ui/components/Text';
import { Touchable } from '@/ui/components/Touchable';
import { size } from '@/ui/theme/tokens';

export type TabName = 'today' | 'schedule' | 'registry' | 'earnings' | 'settings';

export interface TabBarProps {
  active?: TabName;
  /** Called with the tab the teacher pressed; the router does the navigation. */
  onSelect?: (tab: TabName) => void;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

export const tabs: readonly { name: TabName; icon: IconName }[] = [
  { name: 'today', icon: 'home' },
  { name: 'schedule', icon: 'calendar' },
  { name: 'registry', icon: 'users' },
  { name: 'earnings', icon: 'wallet' },
  { name: 'settings', icon: 'gear' },
];

/** The five tabs, always in this order, labels always shown. Active = the active-menu part. */
export function TabBar({ className, active = 'today', onSelect }: TabBarProps) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  return (
    <View
      accessibilityRole="tablist"
      accessibilityLabel={t('ui.mainNav')}
      className={cn('flex-row border-t border-line bg-surface', className)}
      style={{ height: size.tabbar + insets.bottom, paddingBottom: insets.bottom }}
    >
      {tabs.map((tab) => {
        const on = tab.name === active;
        const label = t(`tabs.${tab.name}`);
        return (
          <Touchable
            key={tab.name}
            testID={`tab-${tab.name}`}
            accessibilityRole="tab"
            accessibilityLabel={label}
            accessibilityState={{ selected: on }}
            onPress={() => onSelect?.(tab.name)}
            pressedFeedback={false}
            className="flex-1 items-center justify-center gap-0.5"
            style={{ height: size.tabbar }}
          >
            <View
              testID={`tab-pill-${tab.name}`}
              className={cn('rounded-xl px-4 py-0.5', on && 'bg-tab-active-soft')}
            >
              <Icon name={tab.icon} tone={on ? 'tab-active' : 'ink-muted'} />
            </View>
            <Text
              variant="tab"
              weight={on ? 800 : 700}
              className={on ? 'text-tab-active' : 'text-ink-muted'}
            >
              {label}
            </Text>
          </Touchable>
        );
      })}
    </View>
  );
}
