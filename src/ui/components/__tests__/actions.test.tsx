import { fireEvent, screen } from 'react-native-css-interop/test';

import { colorSets } from '@/domain/theme/colorSets';
import { Button, Fab, Icon, IconButton } from '@/ui/components';

import {
  pressableCount,
  pressableProblems,
  renderComponent,
  testSets,
  withAppStyles,
} from '../../../../tests/components';

withAppStyles();

const rgba = (hex: string) => {
  const n = parseInt(hex.slice(1, 7), 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, 1)`;
};

describe.each(testSets)('actions in %s', (set) => {
  const c = colorSets[set];

  it('AC-002-11: Icon draws in the token color and is hidden unless labelled', async () => {
    await renderComponent(
      <>
        <Icon name="gear" label="Ajustes" tone="primary" />
        <Icon name="play" tone="accent" />
      </>,
      { set },
    );
    const gear = screen.getByLabelText('Ajustes');
    expect(gear.props.stroke ?? gear.props.color).toBe(c.primary);
  });

  it.each(['primary', 'accent', 'outline', 'ghost', 'danger'] as const)(
    'AC-002-11: Button %s paints its variant',
    async (variant) => {
      await renderComponent(<Button variant={variant}>Salvar aula</Button>, { set });
      const ground = {
        primary: c.primary,
        accent: c.accent,
        outline: c.surface,
        ghost: null,
        danger: c['danger-soft'],
      }[variant];
      const button = screen.getByRole('button', { name: 'Salvar aula' });
      if (ground) expect(button).toHaveStyle({ backgroundColor: rgba(ground) });
      expect(pressableProblems()).toEqual([]);
    },
  );

  it.each([
    ['sm', 36],
    ['md', 48],
    ['lg', 56],
  ] as const)('AC-002-11: Button size %s is %i px tall', async (size, height) => {
    await renderComponent(<Button size={size}>Salvar</Button>, { set });
    expect(screen.getByRole('button', { name: 'Salvar' })).toHaveStyle({ height });
    expect(pressableProblems()).toEqual([]);
  });

  it('AC-002-11: Button loading keeps its label, shows progress and does not press', async () => {
    const onPress = jest.fn();
    await renderComponent(
      <Button loading onPress={onPress}>
        Gerar PDF
      </Button>,
      { set },
    );
    const button = screen.getByRole('button', { name: 'Gerar PDF' });
    expect(button.props.accessibilityState).toMatchObject({ busy: true, disabled: true });
    await fireEvent.press(button);
    expect(onPress).not.toHaveBeenCalled();
  });

  it.each(['sunken', 'plain', 'onPrimary', 'accent'] as const)(
    'AC-002-11: IconButton %s is a 44 px circle with a label',
    async (variant) => {
      const onPress = jest.fn();
      await renderComponent(
        <IconButton icon="close" label="Fechar" variant={variant} onPress={onPress} />,
        { set },
      );
      const button = screen.getByRole('button', { name: 'Fechar' });
      expect(button).toHaveStyle({ width: 44, height: 44, borderRadius: 999 });
      await fireEvent.press(button);
      expect(onPress).toHaveBeenCalled();
      expect(pressableProblems()).toEqual([]);
    },
  );

  it('AC-002-11: Fab is the 60 px accent button labelled Nova aula by default', async () => {
    await renderComponent(<Fab />, { set });
    const fab = screen.getByRole('button', { name: 'Nova aula' });
    expect(fab).toHaveStyle({ width: 60, height: 60, backgroundColor: rgba(c.accent) });
    expect(pressableProblems()).toEqual([]);
  });
});

describe('custom colors on actions', () => {
  it('AC-002-6: primary buttons read the main-buttons part, not primary', async () => {
    const { ThemeProvider } = jest.requireActual('@/ui/theme/ThemeProvider');
    const { render } = jest.requireActual('react-native-css-interop/test');
    await render(
      <ThemeProvider theme="quadro" mode="light" customColors={{ primaryBtn: '#7a2e3b' }}>
        <Button>Salvar</Button>
      </ThemeProvider>,
    );
    expect(screen.getByRole('button', { name: 'Salvar' })).toHaveStyle({
      backgroundColor: rgba('#7a2e3b'),
    });
  });
});

describe('labels', () => {
  it('AC-002-16: a button label stays on one line and may shrink to 85% with Large text', async () => {
    await renderComponent(<Button variant="outline">Cancelar</Button>, { textSize: 'large' });
    const label = screen.getByText('Cancelar');
    expect(label.props).toMatchObject({
      numberOfLines: 1,
      adjustsFontSizeToFit: true,
      minimumFontScale: 0.85,
    });
    expect(label).toHaveStyle({ fontSize: 15 * 1.15 });
  });
});

describe('pressed state', () => {
  it('AC-002-11: a pressed button dims to opacity-pressed and keeps its class styles', async () => {
    await renderComponent(<Button>Salvar</Button>);
    const button = screen.getByRole('button', { name: 'Salvar' });
    await fireEvent(button, 'pressIn');
    expect(screen.getByRole('button', { name: 'Salvar' })).toHaveStyle({
      opacity: 0.85,
      height: 48,
    });
    await fireEvent(button, 'pressOut');
    expect(screen.getByRole('button', { name: 'Salvar' })).not.toHaveStyle({ opacity: 0.85 });
  });
});

describe('a11y walker', () => {
  it('AC-002-12: the walker finds pressables and flags a small unlabelled one', async () => {
    const { Pressable } = jest.requireActual('react-native');
    await renderComponent(<Pressable onPress={() => {}} style={{ height: 20, width: 20 }} />);
    expect(pressableCount()).toBe(1);
    expect(pressableProblems()).toEqual([
      'pressable: no accessible name',
      'pressable: height 20 + slop < 44',
      'pressable: width 20 + slop < 44',
    ]);
  });
});
