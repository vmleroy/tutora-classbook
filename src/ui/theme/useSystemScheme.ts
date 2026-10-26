import { useColorScheme } from 'react-native';

import type { Scheme } from '@/domain/theme/themes';

/** The phone's light or dark setting; null when it reports neither. */
export function useSystemScheme(): Scheme | null {
  const scheme = useColorScheme();
  return scheme === 'light' || scheme === 'dark' ? scheme : null;
}
