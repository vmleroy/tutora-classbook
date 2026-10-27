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

let compiled: string | undefined;

/** The app's real global.css, compiled with the app's tailwind.config.js. */
export async function appCss(): Promise<string> {
  if (compiled) return compiled;
  const input = fs.readFileSync(path.join(repoRoot, 'global.css'), 'utf8');
  const result = await postcss([
    tailwind({ ...config, content: [path.join(repoRoot, 'src/**/*.{ts,tsx}')] }),
  ]).process(input, { from: undefined });
  compiled = result.css;
  return compiled;
}

/** Call at the top of a test file that asserts on NativeWind styles. */
export function withAppStyles() {
  beforeAll(() => appCss(), 30_000);
  beforeEach(async () => {
    setupAllComponents();
    registerCSS(await appCss(), { inlineRem: 16, ignorePropertyWarningRegex: ['^--tw-'] });
  });
}
