import { useState } from 'react';
import { Pressable, type PressableProps, type StyleProp, type ViewStyle } from 'react-native';

import { opacity } from '../theme/metrics';

type Props = Omit<PressableProps, 'style'> & {
  className?: string;
  style?: StyleProp<ViewStyle>;
  /** Dim while pressed (opacity-pressed). On by default. */
  pressedFeedback?: boolean;
};

/**
 * Pressable with the design system's pressed state. NativeWind drops a function `style`
 * when `className` is set, so the pressed opacity is tracked here and passed as an object.
 */
export function Touchable({
  style,
  pressedFeedback = true,
  onPressIn,
  onPressOut,
  ...rest
}: Props) {
  const [pressed, setPressed] = useState(false);
  return (
    <Pressable
      {...rest}
      onPressIn={(e) => {
        setPressed(true);
        onPressIn?.(e);
      }}
      onPressOut={(e) => {
        setPressed(false);
        onPressOut?.(e);
      }}
      style={[style, pressedFeedback && pressed && { opacity: opacity.pressed }]}
    />
  );
}
