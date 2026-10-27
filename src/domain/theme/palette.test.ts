import * as fc from 'fast-check';

import { mix, oklchLightness } from './color';
import { colorSetIds, colorSets, themeIds } from './colorSets';
import { contrastRatio } from './contrast';
import {
  checkCustomColor,
  customParts,
  partTokens,
  resolvePalette,
  type CustomColors,
  type CustomPart,
} from './palette';

// Quadro-negro colors from docs/product.md, "Visual identity" (light / dark).
const productDoc = {
  primary: ['#2E5E4E', '#6FB39A'],
  accent: ['#F2B33D', '#F5C462'],
  secondary: ['#3D6FB4', '#7FA6DE'],
  danger: ['#D64545', '#EF7A7A'],
  success: ['#3E9D6B', '#6CCB97'],
  background: ['#FAF7F0', '#141C19'],
  surface: ['#FFFFFF', '#1E2925'],
  ink: ['#1F2A2E', '#ECEFEA'],
  'ink-muted': ['#5C6A70', '#9AA8A2'],
} as const;

function expectMatches(palette: Record<string, string>, index: 0 | 1) {
  for (const [token, values] of Object.entries(productDoc)) {
    expect([token, palette[token]?.toLowerCase()]).toEqual([token, values[index].toLowerCase()]);
  }
}

const hex = fc
  .tuple(
    fc.integer({ min: 0, max: 255 }),
    fc.integer({ min: 0, max: 255 }),
    fc.integer({ min: 0, max: 255 }),
  )
  .map(([r, g, b]) => `#${[r, g, b].map((n) => n.toString(16).padStart(2, '0')).join('')}`);
const theme = fc.constantFrom(...themeIds);
const scheme = fc.constantFrom('light' as const, 'dark' as const);
const part = fc.constantFrom(...customParts);

const NEVER_CUSTOM = [
  'success',
  'success-soft',
  'success-ink',
  'danger',
  'danger-soft',
  'danger-ink',
  'secondary',
  'secondary-soft',
  'secondary-ink',
  'ink',
  'ink-muted',
  'focus',
];

describe('resolvePalette', () => {
  it('AC-001-8: mode "system" with the phone in dark mode uses Quadro-negro dark', () => {
    expectMatches(resolvePalette('quadro', 'system', 'dark'), 1);
  });

  it('AC-001-8: mode "system" with the phone in light mode uses Quadro-negro light', () => {
    expectMatches(resolvePalette('quadro', 'system', 'light'), 0);
  });

  it('AC-001-8: "system" falls back to light when the phone reports no scheme', () => {
    expectMatches(resolvePalette('quadro', 'system', null), 0);
  });

  it('AC-001-8: an explicit light or dark mode ignores the phone', () => {
    expectMatches(resolvePalette('quadro', 'light', 'dark'), 0);
    expectMatches(resolvePalette('quadro', 'dark', 'light'), 1);
  });

  it('AC-002-5: every theme and mode resolves to its own token set, parts defaulting to primary', () => {
    for (const id of colorSetIds) {
      const [t, s] = id.split('-') as [(typeof themeIds)[number], 'light' | 'dark'];
      const palette = resolvePalette(t, s, null);
      expect(palette).toMatchObject(colorSets[id]);
      expect(palette['btn-primary']).toBe(colorSets[id].primary);
      expect(palette['on-btn-primary']).toBe(colorSets[id]['on-primary']);
      expect(palette['tab-active']).toBe(colorSets[id].primary);
      expect(palette['tab-active-soft']).toBe(colorSets[id]['primary-soft']);
    }
  });

  it('AC-002-6: a custom color changes only the tokens mapped to its part', () => {
    fc.assert(
      fc.property(theme, scheme, part, hex, (t, s, p, color) => {
        const base = resolvePalette(t, s, null);
        const custom = resolvePalette(t, s, null, { [p]: color });
        const changed = Object.keys(base).filter(
          (k) => base[k as keyof typeof base] !== custom[k as keyof typeof custom],
        );
        for (const token of changed)
          expect([p, token, partTokens[p].includes(token as never)]).toEqual([p, token, true]);
        for (const token of NEVER_CUSTOM)
          expect(custom[token as keyof typeof custom]).toBe(base[token as keyof typeof base]);
        expect(
          Object.keys(custom)
            .filter((k) => k.startsWith('tag-'))
            .every((k) => custom[k as keyof typeof custom] === base[k as keyof typeof base]),
        ).toBe(true);
      }),
    );
  });

  it('AC-002-6: in light mode the custom color is used as chosen', () => {
    const p = resolvePalette('quadro', 'light', null, {
      primaryBtn: '#7a2e3b',
      highlightBtn: '#d9a441',
      earnings: '#24497e',
      background: '#fbf6ef',
      cards: '#fffdf8',
      activeTab: '#7a2e3b',
      header: '#2f5c9e',
    });
    expect(p['btn-primary']).toBe('#7a2e3b');
    expect(p.accent).toBe('#d9a441');
    expect(p.earnings).toBe('#24497e');
    expect(p.background).toBe('#fbf6ef');
    expect(p.surface).toBe('#fffdf8');
    expect(p['tab-active']).toBe('#7a2e3b');
    expect(p.header).toBe('#2f5c9e');
  });

  it('AC-002-7: in dark mode a custom accent-like color is lightened in OKLCH', () => {
    fc.assert(
      fc.property(theme, hex, (t, color) => {
        const p = resolvePalette(t, 'dark', null, {
          primaryBtn: color,
          highlightBtn: color,
          earnings: color,
          activeTab: color,
        });
        const before = oklchLightness(color);
        for (const token of ['btn-primary', 'accent', 'earnings', 'tab-active'] as const) {
          const after = oklchLightness(p[token]);
          // About 40% of the way to white: L' = L + (1 − L) × 0.4, within rounding to hex.
          expect(after).toBeCloseTo(before + (1 - before) * 0.4, 1);
        }
      }),
    );
  });

  it('AC-002-7: on-* text flips between light and dark text, whichever reads better', () => {
    fc.assert(
      fc.property(theme, scheme, hex, (t, s, color) => {
        const p = resolvePalette(t, s, null, {
          primaryBtn: color,
          highlightBtn: color,
          header: color,
        });
        const base = colorSets[`${t}-${s}`];
        const candidates = s === 'light' ? ['#ffffff', base.ink] : [base.ink, base.background];
        for (const [text, ground] of [
          ['on-btn-primary', 'btn-primary'],
          ['on-accent', 'accent'],
          ['on-header', 'header'],
        ] as const) {
          const best = Math.max(...candidates.map((c) => contrastRatio(c, p[ground])));
          expect(contrastRatio(p[text], p[ground])).toBeCloseTo(best, 5);
        }
      }),
    );
  });

  it('AC-002-8: a custom color that leaves text under 4.5:1 is flagged with its ratio', () => {
    const lowCards = checkCustomColor('quadro', 'light', 'cards', '#5c6a70');
    expect(lowCards.ok).toBe(false);
    expect(lowCards.ratio).toBeLessThan(4.5);
    const fine = checkCustomColor('quadro', 'light', 'primaryBtn', '#7a2e3b');
    expect(fine).toEqual({ ok: true, ratio: expect.any(Number) });
    expect(fine.ratio).toBeGreaterThanOrEqual(4.5);
  });

  it('AC-002-8b: in dark mode the header is a deep shade of primary, never primary itself', () => {
    for (const t of themeIds) {
      const p = resolvePalette(t, 'dark', null);
      expect(p.header).not.toBe(p.primary);
      expect(p.header).toBe(colorSets[`${t}-dark`].header);
      expect(oklchLightness(p.header)).toBeLessThan(oklchLightness(p.primary));
    }
  });

  it('AC-002-8b: a custom header color is deepened the same way in dark mode', () => {
    fc.assert(
      fc.property(theme, hex, (t, color) => {
        const p = resolvePalette(t, 'dark', null, { header: color });
        const lifted = resolvePalette(t, 'dark', null, { primaryBtn: color })['btn-primary'];
        expect(p.header).toBe(mix(lifted, colorSets[`${t}-dark`].background, 0.68));
        expect(contrastRatio(p['on-header'], p.header)).toBeGreaterThanOrEqual(
          Math.min(4.5, contrastRatio(colorSets[`${t}-dark`].ink, p.header)),
        );
      }),
    );
  });

  it("AC-002-8b: the theme's own dark header follows the same rule as a custom one", () => {
    for (const t of themeIds) {
      const set = colorSets[`${t}-dark`];
      const derived = mix(set.primary, set.background, 0.68);
      // Within one step per channel of the hand-tuned token.
      expect(contrastRatio(derived, set.header)).toBeLessThan(1.05);
    }
  });

  it('AC-002-9: no custom colors gives back exactly the theme tokens', () => {
    fc.assert(
      fc.property(theme, scheme, (t, s) => {
        const empty: CustomColors = {};
        expect(resolvePalette(t, s, null, empty)).toEqual(resolvePalette(t, s, null));
        expect(resolvePalette(t, s, null)).toMatchObject(colorSets[`${t}-${s}`]);
      }),
    );
  });
});

describe('custom parts', () => {
  it('AC-002-6: the seven parts map to the tokens in the Theming section', () => {
    const expected: Record<CustomPart, string[]> = {
      header: ['header', 'on-header', 'on-header-muted', 'earnings-on-header'],
      primaryBtn: ['btn-primary', 'on-btn-primary'],
      highlightBtn: ['accent', 'accent-soft', 'on-accent', 'accent-ink', 'earnings-on-header'],
      earnings: ['earnings'],
      background: ['background'],
      cards: ['surface', 'surface-sunken'],
      activeTab: ['tab-active', 'tab-active-soft'],
    };
    expect(partTokens).toEqual(expected);
  });
});
