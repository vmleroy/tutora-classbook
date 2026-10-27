import fs from 'node:fs';
import path from 'node:path';

import postcss from 'postcss';
import { registerCSS, setupAllComponents } from 'react-native-css-interop/test';
import tailwind from 'tailwindcss';

import config from '../tailwind.config.js';
import { repoRoot } from './sourceFiles';

// Metro compiles with the native NativeWind preset; without this, tests get the web preset
// and miss native-only overrides (the preset's `font-sans` is the system font on Android).
process.env.NATIVEWIND_OS = 'android';

const compiled = new Map<string, Promise<string>>();

/**
 * The app's real global.css, compiled with the app's tailwind.config.js. `safelist` adds
 * classes that no source file uses yet, for tests that inspect the generated rules.
 */
export function appCss(safelist: string[] = []): Promise<string> {
  const key = safelist.join(' ');
  let css = compiled.get(key);
  if (!css) {
    const input = fs.readFileSync(path.join(repoRoot, 'global.css'), 'utf8');
    css = postcss([
      tailwind({ ...config, safelist, content: [path.join(repoRoot, 'src/**/*.{ts,tsx}')] }),
    ])
      .process(input, { from: undefined })
      .then((r) => r.css);
    compiled.set(key, css);
  }
  return css;
}

/** Call at the top of a test file that asserts on NativeWind styles. */
export function withAppStyles() {
  beforeAll(() => appCss(), 30_000);
  beforeEach(async () => {
    setupAllComponents();
    registerCSS(await appCss(), { inlineRem: 16, ignorePropertyWarningRegex: ['^--tw-'] });
  });
}
