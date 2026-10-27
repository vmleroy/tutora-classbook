import { screen } from 'react-native-css-interop/test';

import { colorSets } from '@/domain/theme/colorSets';

import { renderComponent, testSets, withAppStyles } from '../../../tests/components';
import { formatMoney } from '../format';
import { Avatar, Badge, Card, EarningsSummary, Money, ProgressBar, TagDot } from './index';

withAppStyles();

const rgba = (hex: string) => {
  const n = parseInt(hex.slice(1, 7), 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, 1)`;
};
const plain = (s: string) => s.replace(/\u00a0|\u202f/g, ' ');
// Intl puts non-breaking spaces in money; match them as plain spaces.
const moneyText = (s: string) =>
  new RegExp(`^${s.replace(/[$.+]/g, '\\$&').replace(/ /g, '[\\s\\u00a0\\u202f]')}$`);

describe('formatMoney', () => {
  it.each([
    ['pt-BR', 'BRL', 'R$ 1.234,56'],
    ['en', 'BRL', 'R$1,234.56'],
    ['pt-BR', 'USD', 'US$ 1.234,56'],
    ['en', 'USD', '$1,234.56'],
  ] as const)('AC-002-13: %s with %s formats 123456 cents as %s', (locale, currency, expected) => {
    expect(plain(formatMoney(123456, currency, locale))).toBe(expected);
  });

  it('AC-002-13: compact drops the cents, negative keeps its sign', () => {
    expect(plain(formatMoney(118050, 'BRL', 'pt-BR', true))).toBe('R$ 1.181');
    expect(plain(formatMoney(-24000, 'BRL', 'pt-BR'))).toBe('-R$ 240,00');
  });

  it('AC-002-13: only integer cents are accepted', () => {
    expect(() => formatMoney(10.5, 'BRL', 'pt-BR')).toThrow();
  });
});

describe.each(testSets)('display in %s', (set) => {
  const c = colorSets[set];

  it.each([
    ['pt-BR', 'BRL', 'R$ 240,00'],
    ['en', 'BRL', 'R$240.00'],
    ['pt-BR', 'USD', 'US$ 240,00'],
    ['en', 'USD', '$240.00'],
  ] as const)(
    'AC-002-13: Money in %s and %s reads %s with tabular figures',
    async (language, currency, text) => {
      await renderComponent(<Money cents={24000} currency={currency} />, { set, language });
      const money = screen.getByText(moneyText(text));
      expect(money).toHaveStyle({
        fontVariant: ['tabular-nums'],
        fontFamily: 'Nunito_800ExtraBold',
      });
    },
  );

  it.each([
    ['default', 'ink'],
    ['earnings', 'earnings'],
    ['owed', 'danger-ink'],
    ['paid', 'success-ink'],
    ['onHeader', 'earnings-on-header'],
  ] as const)('AC-002-11: Money tone %s uses %s', async (tone, token) => {
    await renderComponent(<Money cents={100} tone={tone} />, { set });
    expect(screen.getByText(/1,00/)).toHaveStyle({ color: rgba(c[token]) });
  });

  it.each([
    ['display', 48],
    ['xl', 34],
    ['lg', 26],
    ['md', 16],
    ['sm', 14],
  ] as const)('AC-002-11: Money size %s is %i px', async (size, fontSize) => {
    await renderComponent(<Money cents={100} size={size} showSign />, { set });
    expect(screen.getByText(moneyText('+ R$ 1,00'))).toHaveStyle({ fontSize });
  });

  it.each([
    ['neutral', 'surface-sunken', 'ink-muted'],
    ['success', 'success-soft', 'success-ink'],
    ['danger', 'danger-soft', 'danger-ink'],
    ['accent', 'accent-soft', 'accent-ink'],
    ['secondary', 'secondary-soft', 'secondary-ink'],
    ['violet', 'tag-violet-soft', 'tag-violet-ink'],
  ] as const)('AC-002-11: Badge %s', async (tone, ground, text) => {
    await renderComponent(
      <Badge tone={tone} icon="check">
        Pago
      </Badge>,
      { set },
    );
    expect(screen.getByTestId('badge')).toHaveStyle({ backgroundColor: rgba(c[ground]) });
    expect(screen.getByText('Pago')).toHaveStyle({ color: rgba(c[text]) });
  });

  it.each(['sm', 'md', 'lg'] as const)(
    'AC-002-11: Avatar %s shows initials on the tag color',
    async (size) => {
      await renderComponent(<Avatar initials="AS" tag="teal" size={size} />, { set });
      expect(screen.getByTestId('avatar-teal', { includeHiddenElements: true })).toHaveStyle({
        width: { sm: 36, md: 44, lg: 48 }[size],
        backgroundColor: rgba(c['tag-teal-soft']),
      });
    },
  );

  it.each(['sm', 'md'] as const)('AC-002-11: TagDot %s', async (size) => {
    await renderComponent(<TagDot tag="red" size={size} />, { set });
    expect(screen.getByTestId('tag-dot-red', { includeHiddenElements: true })).toHaveStyle({
      width: size === 'sm' ? 8 : 10,
      backgroundColor: rgba(c['tag-red']),
    });
  });

  it.each([
    ['accent', 'accent', false],
    ['success', 'danger', false],
    ['primary', 'sunken', true],
    ['secondary', 'sunken', true],
  ] as const)('AC-002-11: ProgressBar %s on %s (thin %s)', async (tone, track, thin) => {
    await renderComponent(
      <ProgressBar value={0.6} tone={tone} track={track} thin={thin} label="Ganho" />,
      {
        set,
      },
    );
    const bar = screen.getByRole('progressbar', { name: 'Ganho' });
    expect(bar.props.accessibilityValue).toEqual({ min: 0, max: 100, now: 60 });
    expect(bar).toHaveStyle({ height: thin ? 4 : 8 });
  });

  it.each(['flat', 'raised', 'outline'] as const)('AC-002-11: Card %s', async (elevation) => {
    await renderComponent(
      <Card elevation={elevation} label="Resumo">
        <></>
      </Card>,
      { set },
    );
    const card = screen.getByLabelText('Resumo');
    expect(card).toHaveStyle({ backgroundColor: rgba(c.surface), borderRadius: 20 });
    if (elevation === 'outline') expect(card).toHaveStyle({ borderColor: rgba(c.line) });
  });

  it('AC-002-11: EarningsSummary shows earned, progress to expected and three compact stats', async () => {
    await renderComponent(
      <EarningsSummary
        title="Ganho hoje"
        earnedCents={24000}
        expectedCents={38000}
        expectedLabel="de R$ 380,00 previstos"
        stats={[
          { label: 'Semana', cents: 118000 },
          { label: 'Outubro', cents: 502000 },
          { label: 'Alunos devem', cents: 56000, tone: 'owed' },
        ]}
      />,
      { set },
    );
    expect(screen.getByText(moneyText('R$ 240,00'))).toHaveStyle({ fontSize: 34 });
    expect(screen.getByRole('progressbar').props.accessibilityValue.now).toBe(63);
    expect(screen.getByText(moneyText('R$ 560'))).toHaveStyle({
      color: rgba(c['danger-ink']),
    });
    expect(screen.getByText('Semana')).toBeTruthy();
  });
});
