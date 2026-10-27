import fs from 'node:fs';
import path from 'node:path';

import { colorSetIds, colorSets } from '../src/ui/theme/tokens';
import { checkTokens, renderTokens, TOKENS_JSON, TOKENS_TS } from './tokens';

const root = path.resolve(__dirname, '..');
const json = () => JSON.parse(fs.readFileSync(path.join(root, TOKENS_JSON), 'utf8'));

describe('token pipeline', () => {
  it('AC-002-1: the committed tokens.ts is exactly what the script generates', async () => {
    const committed = fs.readFileSync(path.join(root, TOKENS_TS), 'utf8');
    expect(committed).toBe(await renderTokens(json()));
  });

  it('AC-002-1: the generated file says it is generated and must not be edited', async () => {
    const out = await renderTokens(json());
    expect(out.split('\n')[0]).toMatch(/GENERATED .* do not edit/i);
  });

  it('AC-002-1: --check fails when tokens.json and tokens.ts drift apart', async () => {
    const changed = json();
    changed.color.tokens[0].value['quadro-light'] = '#000000';
    const committed = fs.readFileSync(path.join(root, TOKENS_TS), 'utf8');
    expect(await checkTokens(changed, committed)).toBe(false);
    expect(await checkTokens(json(), committed)).toBe(true);
  });

  it('AC-002-1: every color token has a value for all 8 sets', () => {
    expect(colorSetIds).toHaveLength(8);
    const names = json().color.tokens.map((t: { name: string }) => t.name);
    for (const id of colorSetIds)
      expect(Object.keys(colorSets[id]).sort()).toEqual([...names].sort());
  });
});
