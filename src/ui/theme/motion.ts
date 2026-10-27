import { useEffect } from 'react';
import {
  useReducedMotion,
  useSharedValue,
  withTiming,
  type SharedValue,
} from 'react-native-reanimated';

/** Sheets, switches and segment thumbs: short and functional (design system: Motion). */
export const MOTION_MS = 200;

/**
 * A shared value that follows `target`, animated over MOTION_MS, or set at once when the
 * phone asks for reduced motion (AC-002-17).
 */
export function useMotion(target: number): SharedValue<number> {
  const reduced = useReducedMotion();
  const value = useSharedValue(target);
  useEffect(() => {
    value.value = reduced ? target : withTiming(target, { duration: MOTION_MS });
  }, [target, reduced, value]);
  return value;
}
