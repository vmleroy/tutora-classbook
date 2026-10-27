// A hex color is a quoted '#' followed by 3, 4, 6 or 8 hex digits and the closing quote,
// or one used as a value after ':' or '='.
const hex = /(?<=['"`]|[:=]\s*)#(?:[0-9a-f]{8}|[0-9a-f]{6}|[0-9a-f]{3,4})(?=['"`;\s,)}])/gi;

export function hexColorsIn(source: string): string[] {
  return source.match(hex) ?? [];
}
