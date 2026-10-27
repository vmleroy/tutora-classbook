import { clampChroma, converter, formatHex } from 'culori';

const toOklch = converter('oklch');
const toRgb = converter('rgb');

/** a → b by `t` (0 = a, 1 = b), per sRGB channel, the way the theme tokens were derived. */
export function mix(a: string, b: string, t: number): string {
  const ca = toRgb(a);
  const cb = toRgb(b);
  if (!ca || !cb) throw new RangeError(`Cannot mix ${a} and ${b}`);
  return formatHex({
    mode: 'rgb',
    r: ca.r + (cb.r - ca.r) * t,
    g: ca.g + (cb.g - ca.g) * t,
    b: ca.b + (cb.b - ca.b) * t,
  });
}

export function oklchLightness(hex: string): number {
  return toOklch(hex)?.l ?? 0;
}

/** Moves OKLCH lightness `amount` of the way toward white, keeping hue and chroma. */
export function lighten(hex: string, amount: number): string {
  const c = toOklch(hex);
  if (!c) throw new RangeError(`Not a color: ${hex}`);
  return formatHex(clampChroma({ ...c, l: c.l + (1 - c.l) * amount }, 'oklch'));
}

/** Same hue, with the lightness of `reference` and at most `maxChroma` chroma. */
export function atLightnessOf(hex: string, reference: string, maxChroma: number): string {
  const c = toOklch(hex);
  const r = toOklch(reference);
  if (!c || !r) throw new RangeError(`Not a color: ${hex} or ${reference}`);
  return formatHex(clampChroma({ ...c, l: r.l, c: Math.min(c.c ?? 0, maxChroma) }, 'oklch'));
}

/** Shifts OKLCH lightness by `delta` (negative = darker). */
export function shiftLightness(hex: string, delta: number): string {
  const c = toOklch(hex);
  if (!c) throw new RangeError(`Not a color: ${hex}`);
  return formatHex(clampChroma({ ...c, l: Math.min(1, Math.max(0, c.l + delta)) }, 'oklch'));
}
