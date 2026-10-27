import type { ReactNode } from 'react';
import { ScrollView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

type Props = { children: ReactNode; testID?: string; safeTop?: boolean };

/** A screen's scrolling body on the app background, with the 20px side gutter. */
export function Screen({ children, testID, safeTop = false }: Props) {
  const insets = useSafeAreaInsets();
  return (
    <ScrollView
      testID={testID}
      className="flex-1 bg-background"
      contentContainerClassName="gap-4 px-5 py-4"
      contentContainerStyle={safeTop ? { paddingTop: insets.top } : undefined}
    >
      {children}
    </ScrollView>
  );
}
