/** '#2e5e4e' or '#2e5e4e80' → '46 94 78', the form Tailwind's `rgb(var(--x) / <alpha>)` needs. */
export function hexToChannels(hex: string): string {
  const m = /^#([0-9a-f]{6})([0-9a-f]{2})?$/i.exec(hex);
  if (!m?.[1]) throw new RangeError(`Not a #rrggbb color: ${hex}`);
  const n = parseInt(m[1], 16);
  return `${(n >> 16) & 255} ${(n >> 8) & 255} ${n & 255}`;
}
