import tokens from './tokens.json';

const px = (name: string) => parseFloat(tokens.size.tokens.find((t) => t.name === name)!.value);
const opacityOf = (name: string) =>
  parseFloat(tokens.opacity.tokens.find((t) => t.name === name)!.value);

/** Fixed sizes from the design system, for props that take numbers (icons, hitSlop). */
export const size = {
  touch: px('size-touch'),
  controlSm: px('size-control-sm'),
  controlMd: px('size-control-md'),
  controlLg: px('size-control-lg'),
  icon: px('size-icon'),
} as const;

export const opacity = {
  disabled: opacityOf('opacity-disabled'),
  pressed: opacityOf('opacity-pressed'),
} as const;

/** hitSlop that grows a control of `height` to the 44px touch target. */
export function touchSlop(height: number) {
  const pad = Math.max(0, (size.touch - height) / 2);
  return { top: pad, bottom: pad, left: pad, right: pad };
}
