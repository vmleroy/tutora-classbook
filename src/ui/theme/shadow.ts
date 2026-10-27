import { Platform, type ViewStyle } from 'react-native';

import type { ColorSetId } from '@/domain/theme/colorSets';
import { shadows } from '@/ui/theme/tokens';

/**
 * A shadow token as platform props: iOS shadow*, Android elevation. Dark sets have none.
 * Shadows are applied here, never through class names (docs/design-system/Tailwind.md).
 */
export function shadowStyle(name: keyof typeof shadows, set: ColorSetId): ViewStyle | undefined {
  const s = shadows[name][set];
  if (!s) return undefined;
  const [r, g, b] = s.rgb.split(' ');
  const color = `rgb(${r}, ${g}, ${b})`;
  return Platform.select<ViewStyle>({
    android: { elevation: Math.round(s.blur / 2), shadowColor: color },
    default: {
      shadowColor: color,
      shadowOffset: { width: s.offsetX, height: s.offsetY },
      shadowOpacity: s.opacity,
      shadowRadius: s.blur / 2,
    },
  });
}
