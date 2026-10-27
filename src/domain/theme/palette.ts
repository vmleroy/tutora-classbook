// Turns the teacher's choices into one flat palette (docs/design-system/Theming.md).
import { atLightnessOf, lighten, mix, oklchLightness, shiftLightness } from './color';
import { colorSets, type ColorToken, type ThemeId } from './colorSets';
import { contrastRatio } from './contrast';

export type Scheme = 'light' | 'dark';
export type ThemeMode = Scheme | 'system';

/** The seven parts of Suas cores. */
export const customParts = [
  'header',
  'primaryBtn',
  'highlightBtn',
  'earnings',
  'background',
  'cards',
  'activeTab',
] as const;
export type CustomPart = (typeof customParts)[number];
export type CustomColors = Readonly<Partial<Record<CustomPart, string>>>;

/** Variables that components read for repaintable parts; they default to primary. */
export type PartToken = 'btn-primary' | 'on-btn-primary' | 'tab-active' | 'tab-active-soft';
export type PaletteToken = ColorToken | PartToken;
export type Palette = Readonly<Record<PaletteToken, string>>;

/** Which tokens each part may change; everything else stays the theme's. */
export const partTokens: Readonly<Record<CustomPart, readonly PaletteToken[]>> = {
  header: ['header', 'on-header', 'on-header-muted', 'earnings-on-header'],
  primaryBtn: ['btn-primary', 'on-btn-primary'],
  highlightBtn: ['accent', 'accent-soft', 'on-accent', 'accent-ink', 'earnings-on-header'],
  earnings: ['earnings'],
  background: ['background'],
  cards: ['surface', 'surface-sunken'],
  activeTab: ['tab-active', 'tab-active-soft'],
};

const DARK_LIFT = 0.4; // custom colors are lightened about 40% in OKLCH in dark mode
const HEADER_DEPTH = 0.68; // dark header = primary mixed 68% toward the background
const DARK_GROUND_CHROMA = 0.04; // a custom background or cards color keeps only a tint in dark mode

/** The chosen mode, or the phone's when the mode is "system". */
export function resolveScheme(mode: ThemeMode, systemScheme: Scheme | null | undefined): Scheme {
  return mode === 'system' ? (systemScheme ?? 'light') : mode;
}

function best(ground: string, candidates: readonly string[]): string {
  return candidates.reduce((a, b) => (contrastRatio(b, ground) > contrastRatio(a, ground) ? b : a));
}

export function resolvePalette(
  theme: ThemeId,
  mode: ThemeMode,
  systemScheme: Scheme | null | undefined,
  custom: CustomColors = {},
): Palette {
  const scheme = resolveScheme(mode, systemScheme);
  const base = colorSets[`${theme}-${scheme}`];
  const p: Record<PaletteToken, string> = {
    ...base,
    'btn-primary': base.primary,
    'on-btn-primary': base['on-primary'],
    'tab-active': base.primary,
    'tab-active-soft': base['primary-soft'],
  };
  const dark = scheme === 'dark';
  // White or ink text, whichever reads better on the ground (dark mode: ink is light).
  const textOn = (ground: string) =>
    best(ground, dark ? [base.ink, base.background] : ['#ffffff', base.ink]);
  const lift = (hex: string) => (dark ? lighten(hex, DARK_LIFT) : hex);

  if (custom.background) {
    p.background = dark
      ? atLightnessOf(custom.background, base.background, DARK_GROUND_CHROMA)
      : custom.background;
  }
  if (custom.cards) {
    p.surface = dark ? atLightnessOf(custom.cards, base.surface, DARK_GROUND_CHROMA) : custom.cards;
    // Sunken sits one step darker than surface in light mode and one step lighter in dark.
    const step = oklchStep(base.surface, base['surface-sunken']);
    p['surface-sunken'] = shiftLightness(p.surface, step);
  }
  if (custom.primaryBtn) {
    p['btn-primary'] = lift(custom.primaryBtn);
    p['on-btn-primary'] = textOn(p['btn-primary']);
  }
  if (custom.activeTab) {
    p['tab-active'] = lift(custom.activeTab);
    p['tab-active-soft'] = mix(p['tab-active'], p.surface, dark ? 0.75 : 0.85);
  }
  if (custom.earnings) p.earnings = lift(custom.earnings);
  if (custom.highlightBtn) {
    p.accent = lift(custom.highlightBtn);
    p['on-accent'] = textOn(p.accent);
    p['accent-soft'] = mix(p.accent, base.surface, dark ? 0.78 : 0.72);
    p['accent-ink'] = best(p['accent-soft'], [base['accent-ink'], base.ink]);
  }
  if (custom.header) {
    p.header = dark ? mix(lift(custom.header), base.background, HEADER_DEPTH) : custom.header;
    p['on-header'] = textOn(p.header);
    p['on-header-muted'] = mix(p['on-header'], p.header, 0.2);
  }
  if (custom.header || custom.highlightBtn) {
    // Money on header: accent when it holds 3:1 there (large text only), else on-header.
    p['earnings-on-header'] = contrastRatio(p.accent, p.header) >= 3 ? p.accent : p['on-header'];
  }
  return p;
}

function oklchStep(from: string, to: string): number {
  return oklchLightness(to) - oklchLightness(from);
}

/** Text pairs each part puts at risk, checked when the teacher picks a color (AC-002-8). */
const partPairs: Readonly<Record<CustomPart, readonly [PaletteToken, PaletteToken][]>> = {
  header: [['on-header', 'header']],
  primaryBtn: [['on-btn-primary', 'btn-primary']],
  highlightBtn: [
    ['on-accent', 'accent'],
    ['accent-ink', 'accent-soft'],
  ],
  earnings: [
    ['earnings', 'background'],
    ['earnings', 'surface'],
  ],
  background: [
    ['ink', 'background'],
    ['ink-muted', 'background'],
  ],
  cards: [
    ['ink', 'surface'],
    ['ink-muted', 'surface'],
    ['ink', 'surface-sunken'],
  ],
  activeTab: [['tab-active', 'surface']],
};

export const MIN_TEXT_CONTRAST = 4.5;

/** The lowest text contrast a custom color leaves, and whether it reaches 4.5:1. */
export function checkCustomColor(
  theme: ThemeId,
  scheme: Scheme,
  part: CustomPart,
  hex: string,
): { ok: boolean; ratio: number } {
  const p = resolvePalette(theme, scheme, null, { [part]: hex });
  const ratio = Math.min(
    ...partPairs[part].map(([text, ground]) => contrastRatio(p[text], p[ground])),
  );
  return { ok: ratio >= MIN_TEXT_CONTRAST, ratio };
}
