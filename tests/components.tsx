import type { i18n } from 'i18next';
import type { ReactElement } from 'react';
import { I18nextProvider } from 'react-i18next';
import { render, screen } from 'react-native-css-interop/test';

import type { ColorSetId } from '@/domain/theme/colorSets';
import { createI18n } from '@/i18n';
import { ThemeProvider } from '@/ui/theme/ThemeProvider';

import { withAppStyles } from './nativewind';

export { withAppStyles };
export const testSets = ['quadro-light', 'quadro-dark'] as const satisfies readonly ColorSetId[];

const languages = new Map<string, Promise<i18n>>();
const i18nFor = (language: 'pt-BR' | 'en') => {
  if (!languages.has(language)) languages.set(language, createI18n(language));
  return languages.get(language)!;
};

/** Renders inside the app's providers, in one token set and language. */
export async function renderComponent(
  ui: ReactElement,
  {
    set = 'quadro-light',
    language = 'pt-BR',
    textSize = 'normal',
  }: { set?: ColorSetId; language?: 'pt-BR' | 'en'; textSize?: 'normal' | 'large' } = {},
) {
  const [theme, mode] = set.split('-') as ['quadro', 'light' | 'dark'];
  const instance = await i18nFor(language);
  return render(
    <I18nextProvider i18n={instance}>
      <ThemeProvider theme={theme} mode={mode} textSize={textSize}>
        {ui}
      </ThemeProvider>
    </I18nextProvider>,
  );
}

type Host = {
  type: unknown;
  props: Record<string, unknown>;
  children: (Host | string)[];
};

const flat = (style: unknown): Record<string, unknown> =>
  Array.isArray(style)
    ? Object.assign({}, ...style.map(flat))
    : style && typeof style === 'object'
      ? (style as Record<string, unknown>)
      : {};

function textOf(node: Host | string): string {
  if (typeof node === 'string') return node;
  return node.children.map(textOf).join('');
}

function hosts(node: Host | string): Host[] {
  if (typeof node === 'string') return [];
  const self = typeof node.type === 'string' ? [node] : [];
  return [...self, ...node.children.flatMap(hosts)];
}

/** Every element a finger can press: Pressable hosts and switches. */
function pressables(): Host[] {
  return hosts(screen.root as unknown as Host).filter(
    (n) =>
      typeof n.props.onClick === 'function' ||
      typeof n.props.onResponderRelease === 'function' ||
      (n.props.accessibilityRole === 'adjustable' && n.props.accessible),
  );
}

/**
 * AC-002-12: each pressable reaches 44 × 44 with its hitSlop and has a name (visible text
 * or an accessibility label). Returns the problems found, empty when all is well.
 */
export function pressableProblems(): string[] {
  const problems: string[] = [];
  for (const node of pressables()) {
    const style = flat(node.props.style);
    const slop = (node.props.hitSlop ?? {}) as Record<string, number>;
    const height = Number(style.height ?? style.minHeight ?? NaN);
    const width = Number(style.width ?? style.minWidth ?? NaN);
    const name =
      (node.props.accessibilityLabel as string | undefined) ??
      (node.props['aria-label'] as string | undefined) ??
      textOf(node).trim();
    const id = String(node.props.testID ?? '') || name || 'pressable';
    if (!name) problems.push(`${id}: no accessible name`);
    // A view pinned to all four edges (a scrim) covers the screen.
    const covers =
      style.position === 'absolute' &&
      (style.inset === 0 ||
        [style.top, style.bottom, style.left, style.right].every((v) => v === 0));
    if (covers) continue;
    if (Number.isNaN(height)) problems.push(`${id}: no height`);
    else if (height + (slop.top ?? 0) + (slop.bottom ?? 0) < 44)
      problems.push(`${id}: height ${height} + slop < 44`);
    if (!Number.isNaN(width) && width + (slop.left ?? 0) + (slop.right ?? 0) < 44)
      problems.push(`${id}: width ${width} + slop < 44`);
  }
  return problems;
}

export function pressableCount(): number {
  return pressables().length;
}
