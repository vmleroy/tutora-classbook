import { execFileSync } from 'node:child_process';
import path from 'node:path';

import { Linter, type ESLint } from 'eslint';

import tutoraPlugin from '../eslint/tutora-plugin';
import { repoRoot } from './sourceFiles';

// The plugin is plain JS (ESLint loads it directly), so it is typed here.
const plugin = tutoraPlugin as unknown as ESLint.Plugin;
const linter = new Linter({ configType: 'flat' });
const lint = (code: string) =>
  linter
    .verify(
      code,
      [
        {
          files: ['**/*.tsx'],
          languageOptions: { parserOptions: { ecmaFeatures: { jsx: true } } },
          plugins: { tutora: plugin },
          rules: { 'tutora/no-raw-design-values': 'error' },
        },
      ],
      'sample.tsx',
    )
    .map((m) => m.message);

describe('no raw design values', () => {
  it('AC-002-14: hex colors are rejected anywhere in a component', () => {
    expect(lint(`const a = '#2e5e4e'; const b = <View style={{ color: '#fff' }} />;`)).toEqual([
      expect.stringMatching(/hex color #2e5e4e/),
      expect.stringMatching(/hex color #fff/),
    ]);
  });

  it('AC-002-14: raw font sizes are rejected; type styles are fine', () => {
    expect(lint(`const s = { fontSize: 13, lineHeight: 18 };`)).toEqual([
      expect.stringMatching(/fontSize/),
      expect.stringMatching(/lineHeight/),
    ]);
    expect(lint(`const s = { fontSize: typeStyles.body.fontSize * scale };`)).toEqual([]);
  });

  it('AC-002-14: raw spacing numbers are rejected; 0 and tokens are fine', () => {
    expect(lint(`const s = { padding: 12, marginTop: 4, gap: 8, paddingHorizontal: 0 };`)).toEqual([
      expect.stringMatching(/padding/),
      expect.stringMatching(/marginTop/),
      expect.stringMatching(/gap/),
    ]);
    expect(lint(`const s = { paddingTop: insets.top + spacing['7'] };`)).toEqual([]);
  });

  it('AC-002-14: arbitrary Tailwind values are rejected in class names', () => {
    expect(lint(`const v = <View className="p-[13px] text-[15px] bg-[#fff] gap-2" />;`)).toEqual([
      expect.stringMatching(/hex color #fff/),
      expect.stringMatching(/p-\[13px\]/),
      expect.stringMatching(/text-\[15px\]/),
      expect.stringMatching(/bg-\[#fff\]/),
    ]);
    expect(lint(`const v = <View className="flex-[2] gap-2 p-4" />;`)).toEqual([]);
  });

  it('AC-002-14: src/app and src/ui/components pass the rule, and it is switched on there', () => {
    // ESLint's Node API loads config through a dynamic import Jest cannot run; use the CLI.
    const eslint = (...args: string[]) =>
      execFileSync(path.join(repoRoot, 'node_modules/.bin/eslint'), args, {
        cwd: repoRoot,
        encoding: 'utf8',
      });
    const results = JSON.parse(
      eslint(
        '--format',
        'json',
        '--rule',
        '{"prettier/prettier": "off"}',
        'src/app',
        'src/ui/components',
      ),
    ) as { filePath: string; messages: { ruleId: string; line: number; message: string }[] }[];
    const raw = results.flatMap((r) =>
      r.messages
        .filter((m) => m.ruleId === 'tutora/no-raw-design-values')
        .map((m) => `${r.filePath.replace(repoRoot, '')}:${m.line} ${m.message}`),
    );
    expect(raw).toEqual([]);
    const config = JSON.parse(eslint('--print-config', 'src/ui/components/Button.tsx')) as {
      rules: Record<string, unknown>;
    };
    expect(config.rules['tutora/no-raw-design-values']).toEqual([2]);
  }, 60_000);
});
