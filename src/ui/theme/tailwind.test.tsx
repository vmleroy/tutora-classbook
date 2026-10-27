import { render, screen } from 'react-native-css-interop/test';
import { View } from 'react-native';

import { appCss, withAppStyles } from '../../../tests/nativewind';
import { ThemeProvider } from './ThemeProvider';
import { colorSets, radius, spacing, typeStyles, type ColorToken } from './tokens';

withAppStyles();

const ruleFor = (css: string, cls: string) => {
  const escaped = cls.replace(/[.]/g, '\\\\.').replace(/[/]/g, '\\\\/');
  return new RegExp(`\\.${escaped} \\{([^}]*)\\}`).exec(css)?.[1] ?? '';
};

describe('tailwind config', () => {
  it('AC-002-3: every color token is a class with its own name, for fills, text and borders', async () => {
    const css = await appCss([
      ...Object.keys(colorSets['quadro-light']).flatMap((t) => [
        `bg-${t}`,
        `text-${t}`,
        `border-${t}`,
      ]),
    ]);
    const missing = (Object.keys(colorSets['quadro-light']) as ColorToken[]).flatMap((token) =>
      (['bg', 'text', 'border'] as const)
        .filter((kind) => !ruleFor(css, `${kind}-${token}`).includes(`var(--${token})`))
        .map((kind) => `${kind}-${token}`),
    );
    expect(missing).toEqual([]);
  });

  it('AC-002-3: custom-color parts have their own classes that buttons and the tab bar read', async () => {
    const css = await appCss([
      'bg-btn-primary',
      'text-on-btn-primary',
      'bg-tab-active-soft',
      'text-tab-active',
    ]);
    expect(ruleFor(css, 'bg-btn-primary')).toContain('var(--btn-primary)');
    expect(ruleFor(css, 'text-on-btn-primary')).toContain('var(--on-btn-primary)');
    expect(ruleFor(css, 'bg-tab-active-soft')).toContain('var(--tab-active-soft)');
    expect(ruleFor(css, 'text-tab-active')).toContain('var(--tab-active)');
  });

  it('AC-002-3: spacing, radius and font-size scales match the tokens', async () => {
    const spaceClasses = Object.keys(spacing).map((k) => `p-${k}`);
    const radiusClasses = Object.keys(radius).map((k) => `rounded-${k}`);
    const typeClasses = Object.keys(typeStyles).map((k) => `text-${k}`);
    const css = await appCss([...spaceClasses, ...radiusClasses, ...typeClasses]);
    for (const [k, px] of Object.entries(spacing)) {
      expect([k, ruleFor(css, `p-${k}`)]).toEqual([
        k,
        expect.stringMatching(new RegExp(`padding: (${px}px|${px / 16}rem)`)),
      ]);
    }
    for (const [k, px] of Object.entries(radius)) {
      expect([k, ruleFor(css, `rounded-${k}`)]).toEqual([k, expect.stringContaining(`${px}px`)]);
    }
    for (const [k, s] of Object.entries(typeStyles)) {
      const rule = ruleFor(css, `text-${k}`);
      expect([k, rule]).toEqual([k, expect.stringContaining(`font-size: ${s.fontSize}px`)]);
      expect([k, rule]).toEqual([k, expect.stringContaining(`line-height: ${s.lineHeight}px`)]);
    }
  });

  it('AC-002-4: NativeWind runs with rem = 16, so p-4 renders 16 px', async () => {
    await render(
      <ThemeProvider theme="quadro" mode="light">
        <View testID="box" className="m-2 gap-3 p-4" />
      </ThemeProvider>,
    );
    expect(screen.getByTestId('box')).toHaveStyle({ padding: 16, margin: 8, rowGap: 12, columnGap: 12 });
  });
});
