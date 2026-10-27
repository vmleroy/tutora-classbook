import { contrastRatio } from '@/domain/theme/contrast';

import { colorSetIds, colorSets, type ColorToken } from './tokens';

// Text pairs named in the token usage notes (src/ui/theme/tokens.json): [text, ground, minimum].
const AA = 4.5;
const LARGE = 3;
const tags = ['amber', 'green', 'blue', 'violet', 'orange', 'red', 'teal'] as const;
const pairs: [ColorToken, ColorToken, number][] = [
  ...(['background', 'surface', 'surface-sunken'] as const).flatMap(
    (ground): [ColorToken, ColorToken, number][] => [
      ['ink', ground, AA],
      ['ink-muted', ground, AA],
    ],
  ),
  ['on-primary', 'primary', AA],
  ['on-primary-muted', 'primary', AA],
  ['on-header', 'header', AA],
  ['on-header-muted', 'header', AA],
  ['on-accent', 'accent', AA],
  ['accent-ink', 'accent-soft', AA],
  ['earnings', 'background', AA],
  ['earnings', 'surface', AA],
  ['earnings-on-header', 'header', LARGE],
  ['success-ink', 'success-soft', AA],
  ['success-ink', 'surface', AA],
  ['danger-ink', 'danger-soft', AA],
  ['danger-ink', 'surface', AA],
  ['secondary-ink', 'secondary-soft', AA],
  ['secondary-ink', 'surface', AA],
  ...tags.map((t): [ColorToken, ColorToken, number] => [`tag-${t}-ink`, `tag-${t}-soft`, AA]),
];

describe('token contrast', () => {
  it('AC-002-2: contrast ratio follows WCAG 2', () => {
    expect(contrastRatio('#000000', '#ffffff')).toBeCloseTo(21, 5);
    expect(contrastRatio('#ffffff', '#ffffff')).toBeCloseTo(1, 5);
    expect(contrastRatio('#777777', '#ffffff')).toBeCloseTo(4.48, 2);
    expect(contrastRatio('#ffffff', '#777777')).toBeCloseTo(
      contrastRatio('#777777', '#ffffff'),
      10,
    );
  });

  it.each(colorSetIds)('AC-002-2: every named text pair in %s meets its minimum', (id) => {
    const set = colorSets[id];
    const failures = pairs
      .map(([text, ground, min]) => ({
        text,
        ground,
        min,
        ratio: contrastRatio(set[text], set[ground]),
      }))
      .filter((p) => p.ratio < p.min)
      .map((p) => `${p.text} on ${p.ground}: ${p.ratio.toFixed(2)} < ${p.min}`);
    expect(failures).toEqual([]);
  });
});
