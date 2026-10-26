import { Tabs } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { Icon, type IconName } from '@/ui/components/Icon';
import { useTheme } from '@/ui/theme/ThemeProvider';
import { typeStyles } from '@/ui/theme/type';

const tabs: {
  name: string;
  title: 'today' | 'schedule' | 'students' | 'places' | 'earnings';
  icon: IconName;
}[] = [
  { name: 'today', title: 'today', icon: 'home' },
  { name: 'schedule', title: 'schedule', icon: 'calendar' },
  { name: 'students', title: 'students', icon: 'users' },
  { name: 'places', title: 'places', icon: 'pin' },
  { name: 'earnings', title: 'earnings', icon: 'wallet' },
];

export default function TabsLayout() {
  const { t } = useTranslation();
  const { palette } = useTheme();
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: palette.primary,
        tabBarInactiveTintColor: palette['ink-muted'],
        tabBarStyle: { backgroundColor: palette.surface, borderTopColor: palette['line-soft'] },
        tabBarLabelStyle: { fontFamily: 'Nunito_700Bold', fontSize: typeStyles.tab.fontSize },
      }}
    >
      {tabs.map((tab) => (
        <Tabs.Screen
          key={tab.name}
          name={tab.name}
          options={{
            title: t(`tabs.${tab.title}`),
            tabBarAccessibilityLabel: t(`tabs.${tab.title}`),
            tabBarButtonTestID: `tab-${tab.name}`,
            tabBarIcon: ({ focused }) => (
              <Icon name={tab.icon} tone={focused ? 'primary' : 'ink-muted'} />
            ),
          }}
        />
      ))}
    </Tabs>
  );
}
