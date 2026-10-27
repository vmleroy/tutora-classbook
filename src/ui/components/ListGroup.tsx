import { Children, type ReactNode } from 'react';
import { View } from 'react-native';

import { Text } from './Text';

export interface ListGroupProps {
  title?: string;
  children: ReactNode;
}

/** A titled white block of rows separated by hairlines, as in Ajustes. */
export function ListGroup({ title, children }: ListGroupProps) {
  const rows = Children.toArray(children);
  return (
    <View accessibilityLabel={title} className="gap-1.5">
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
