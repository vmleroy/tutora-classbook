import { Children, type ReactNode } from 'react';
import { View } from 'react-native';

import { cn } from '@/ui/cn';
import { Text } from '@/ui/components/Text';

export interface ListGroupProps {
  title?: string;
  children: ReactNode;
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

/** A titled white block of rows separated by hairlines, as in Ajustes. */
export function ListGroup({ className, title, children }: ListGroupProps) {
  const rows = Children.toArray(children);
  return (
    <View accessibilityLabel={title} className={cn('gap-1.5', className)}>
      {title ? (
        <Text variant="badge" className="px-1 text-ink-muted">
          {title}
        </Text>
      ) : null}
      <View className="overflow-hidden rounded-xl bg-surface">
        {rows.map((row, i) => (
          <View key={i}>
            {i > 0 ? <View testID="list-divider" className="h-px bg-line-soft" /> : null}
            {row}
          </View>
        ))}
      </View>
    </View>
  );
}
