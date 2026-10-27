import './mockPhone';

import { fireEvent, renderRouter, screen } from 'expo-router/testing-library';
import fs from 'node:fs';
import path from 'node:path';

import { repoRoot } from '../sourceFiles';

const documented = fs
  .readdirSync(path.join(repoRoot, 'docs/design-system/components'))
  .filter((n) =>
    fs.existsSync(path.join(repoRoot, 'docs/design-system/components', n, 'README.md')),
  );

const dev = globalThis as { __DEV__?: boolean };
const devBefore = dev.__DEV__;
afterEach(() => {
  dev.__DEV__ = devBefore;
  delete process.env.EXPO_PUBLIC_DEV_GALLERY;
});

describe('developer component screen', () => {
  it('AC-002-15: shows every component in a development build', async () => {
    dev.__DEV__ = true;
    await renderRouter('./src/app', { initialUrl: '/dev/components' });
    expect(screen).toHavePathname('/dev/components');
    for (const name of documented)
      expect([name, screen.queryByTestId(`gallery-${name}`)]).toEqual([name, expect.anything()]);
  });

  it('AC-002-15: switches theme, mode and text size for everything shown', async () => {
    dev.__DEV__ = true;
    await renderRouter('./src/app', { initialUrl: '/dev/components' });
    const ground = () => screen.getByTestId('gallery-ground');
    expect(ground()).toHaveStyle({ backgroundColor: 'rgba(250, 247, 240, 1)' });
    await fireEvent.press(screen.getByRole('radio', { name: 'Biblioteca' }));
    await fireEvent.press(screen.getByRole('radio', { name: 'Escuro' }));
    expect(ground()).toHaveStyle({ backgroundColor: 'rgba(28, 20, 22, 1)' });
    await fireEvent.press(screen.getByRole('radio', { name: 'Grande' }));
    expect(screen.getByTestId('gallery-sample-body')).toHaveStyle({ fontSize: 15 * 1.15 });
  });

  it('AC-002-15: is not reachable in a release build', async () => {
    dev.__DEV__ = false;
    await renderRouter('./src/app', { initialUrl: '/dev/components' });
    expect(screen).toHavePathname('/today');
  });

  it('AC-002-15: a test build can switch it on with EXPO_PUBLIC_DEV_GALLERY=1', async () => {
    dev.__DEV__ = false;
    process.env.EXPO_PUBLIC_DEV_GALLERY = '1';
    await renderRouter('./src/app', { initialUrl: '/dev/components' });
    expect(screen).toHavePathname('/dev/components');
  });
});
