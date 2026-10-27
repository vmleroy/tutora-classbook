import fs from 'node:fs';
import path from 'node:path';

import * as components from '@/ui/components';

import { repoRoot } from './sourceFiles';

const docsDir = path.join(repoRoot, 'docs/design-system/components');
const names = fs
  .readdirSync(docsDir)
  .filter((n) => fs.existsSync(path.join(docsDir, n, 'README.md')));

/** Prop names, with `?` when optional, from the first `interface <Name>Props { … }` in `source`. */
function props(source: string, name: string): string[] {
  const start = source.search(new RegExp(`interface ${name}Props(<[^>]*>)? \\{`));
  if (start < 0) return [];
  let depth = 0;
  let body = '';
  for (let i = source.indexOf('{', start); i < source.length; i++) {
    const ch = source[i]!;
    if (ch === '{') depth++;
    if (ch === '}') depth--;
    body += ch;
    if (depth === 0) break;
  }
  const lines = body.slice(1, -1).split('\n');
  const out: string[] = [];
  let nested = 0;
  for (const line of lines) {
    const trimmed = line.trim();
    if (nested === 0) {
      const m = /^([a-zA-Z]+)(\??):/.exec(trimmed);
      if (m) out.push(`${m[1]}${m[2]}`);
    }
    nested += (line.match(/[{(]/g) ?? []).length - (line.match(/[})]/g) ?? []).length;
  }
  return out.sort();
}

describe('design-system contract', () => {
  it('AC-002-10: there are 30 components documented', () => {
    expect(names).toHaveLength(30);
  });

  it.each(names)('AC-002-10: %s exists in src/ui/components with the documented props', (name) => {
    expect(typeof (components as Record<string, unknown>)[name]).toBe('function');
    const doc = fs.readFileSync(path.join(docsDir, name, 'README.md'), 'utf8');
    const code = fs.readFileSync(path.join(repoRoot, 'src/ui/components', `${name}.tsx`), 'utf8');
    const documented = props(doc, name);
    expect(documented.length).toBeGreaterThan(0);
    expect(props(code, name)).toEqual(documented);
  });
});
