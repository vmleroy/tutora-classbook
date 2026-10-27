import { opacity as opacityTokens, size as sizeTokens } from '@/ui/theme/tokens';

/** Fixed sizes from the design system, for props that take numbers (icons, hitSlop). */
export const size = {
  touch: sizeTokens.touch,
  controlSm: sizeTokens['control-sm'],
  controlMd: sizeTokens['control-md'],
  controlLg: sizeTokens['control-lg'],
  icon: sizeTokens.icon,
} as const;

export const opacity = {
  disabled: opacityTokens.disabled,
  pressed: opacityTokens.pressed,
} as const;

/** hitSlop that grows a control of `height` to the 44px touch target. */
export function touchSlop(height: number) {
  const pad = Math.max(0, (size.touch - height) / 2);
  return { top: pad, bottom: pad, left: pad, right: pad };
}
