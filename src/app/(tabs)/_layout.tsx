import { Tabs } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { TabBar, tabs, type TabName } from '@/ui/components/TabBar';

export default function TabsLayout() {
  const { t } = useTranslation();
  return (
    <Tabs
      screenOptions={{ headerShown: false }}
      tabBar={({ state, navigateToTab }) => (
        <TabBar
          active={state.routes[state.index]?.name as TabName}
          onSelect={(tab) => {
            const route = state.routes.find((r) => r.name === tab);
            if (route) navigateToTab(route.key);
          }}
        />
      )}
    >
      {tabs.map((tab) => (
        <Tabs.Screen key={tab.name} name={tab.name} options={{ title: t(`tabs.${tab.name}`) }} />
      ))}
    </Tabs>
  );
}
