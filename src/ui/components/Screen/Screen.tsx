import type { ReactNode } from 'react';
import { ScrollView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { cn } from '@/ui/cn';

type Props = { children: ReactNode; testID?: string; safeTop?: boolean; className?: string };

/** A screen's scrolling body on the app background, with the 20px side gutter. */
export function Screen({ className, children, testID, safeTop = false }: Props) {
  const insets = useSafeAreaInsets();
  return (
    <ScrollView
      testID={testID}
      className={cn('flex-1 bg-background', className)}
      contentContainerClassName="gap-4 px-5 py-4"
      contentContainerStyle={safeTop ? { paddingTop: insets.top } : undefined}
    >
      {children}
    </ScrollView>
  );
}
