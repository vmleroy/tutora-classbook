import { resolvePalette } from './palette';

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
});
