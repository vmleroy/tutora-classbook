import { fireEvent, screen } from 'react-native-css-interop/test';
import { Text as RNText } from 'react-native';

import { colorSets } from '@/domain/theme/colorSets';

import {
  pressableProblems,
  renderComponent,
  testSets,
  withAppStyles,
} from '../../../tests/components';
import {
  AgendaBlock,
  AlertBanner,
  LessonRow,
  ListGroup,
  ListRow,
  ScreenHeader,
  SectionHeader,
  TabBar,
  WeekStrip,
} from './index';

withAppStyles();

const rgba = (hex: string) => {
  const n = parseInt(hex.slice(1, 7), 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, 1)`;
};

const days = [
  { weekday: 'Seg', day: 26, dot: 'amber' as const },
  { weekday: 'Ter', day: 27 },
  { weekday: 'Qua', day: 28, dot: 'blue' as const },
  { weekday: 'Qui', day: 29 },
  { weekday: 'Sex', day: 30 },
  { weekday: 'Sáb', day: 31 },
  { weekday: 'Dom', day: 1 },
];

describe.each(testSets)('lists and navigation in %s', (set) => {
  const c = colorSets[set];

  it.each(['muted', 'primary'] as const)('AC-002-11: SectionHeader aside %s', async (asideTone) => {
    await renderComponent(
      <SectionHeader title="Aulas de hoje" aside="4 aulas" asideTone={asideTone} />,
      {
        set,
      },
    );
    expect(screen.getByRole('header', { name: 'Aulas de hoje' })).toBeTruthy();
    expect(screen.getByText('4 aulas')).toHaveStyle({
      color: rgba(asideTone === 'primary' ? c.primary : c['ink-muted']),
    });
  });

  it.each([
    ['status', { status: { tone: 'success' as const, label: 'Concluída' } }],
    ['trailing', { trailing: 'R$ 180' }],
  ])('AC-002-11: LessonRow with %s', async (_, extra) => {
    const onPress = jest.fn();
    await renderComponent(
      <LessonRow
        time="14:00"
        tag="amber"
        title="Ana Souza"
        subtitle="Matemática · Online"
        onPress={onPress}
        {...extra}
      />,
      { set },
    );
    await fireEvent.press(screen.getByText('Ana Souza'));
    expect(onPress).toHaveBeenCalled();
    expect(screen.getByText('14:00')).toHaveStyle({ fontVariant: ['tabular-nums'] });
    expect(pressableProblems()).toEqual([]);
  });

  it('AC-002-11: ListGroup separates its rows with hairlines', async () => {
    await renderComponent(
      <ListGroup title="Seus dados">
        <ListRow title="Backup e planilha" subtitle="Automático, toda semana" icon="cloud" />
        <ListRow
          title="Cancelamentos e faltas"
          subtitle="Cobra 50%"
          icon="alert"
          iconTone="danger"
        />
      </ListGroup>,
      { set },
    );
    expect(screen.getByText('Seus dados')).toHaveStyle({ color: rgba(c['ink-muted']) });
    expect(screen.getAllByTestId('list-divider')).toHaveLength(1);
    expect(pressableProblems()).toEqual([]);
  });

  it.each([
    ['chevron', {}],
    ['switch', { trailing: 'switch' as const, switchValue: true }],
    ['none', { trailing: 'none' as const, badge: { tone: 'success' as const, label: 'Ligado' } }],
  ])('AC-002-11: ListRow trailing %s', async (kind, extra) => {
    const onPress = jest.fn();
    await renderComponent(
      <ListRow title="Lembretes" subtitle="15 min antes da aula" onPress={onPress} {...extra} />,
      { set },
    );
    if (kind === 'switch') {
      expect(screen.getByRole('switch', { name: 'Lembretes' })).toBeTruthy();
      expect(screen.queryByRole('button', { name: /Lembretes/ })).toBeNull();
    } else {
      await fireEvent.press(screen.getByRole('button', { name: /Lembretes/ }));
      expect(onPress).toHaveBeenCalled();
    }
    if (kind === 'none') expect(screen.getByText('Ligado')).toBeTruthy();
    expect(pressableProblems()).toEqual([]);
  });

  it.each([
    ['danger', 'danger-soft'],
    ['accent', 'accent-soft'],
    ['success', 'success-soft'],
    ['secondary', 'secondary-soft'],
  ] as const)('AC-002-11: AlertBanner %s', async (tone, ground) => {
    await renderComponent(
      <AlertBanner
        tone={tone}
        message="3 alunos com pagamento pendente"
        value="R$ 560"
        onPress={() => {}}
      />,
      { set },
    );
    expect(screen.getByRole('button', { name: /3 alunos/ })).toHaveStyle({
      backgroundColor: rgba(c[ground]),
    });
    expect(pressableProblems()).toEqual([]);
  });

  it('AC-002-8b: the hero header uses the header token, a deep shade in dark mode', async () => {
    await renderComponent(
      <ScreenHeader
        variant="hero"
        eyebrow="Segunda, 26 de outubro"
        title="Boa tarde, Victor"
        actions={[{ icon: 'gear', label: 'Ajustes', onPress: () => {} }]}
      />,
      { set },
    );
    const hero = screen.getByTestId('screen-header-hero');
    expect(hero).toHaveStyle({ backgroundColor: rgba(c.header) });
    if (set.endsWith('dark')) expect(c.header).not.toBe(c.primary);
    expect(screen.getByRole('header', { name: 'Boa tarde, Victor' })).toHaveStyle({
      color: rgba(c['on-header']),
    });
    expect(pressableProblems()).toEqual([]);
  });

  it.each([
    ['tab', null],
    ['push', 'Voltar'],
    ['modal', 'Fechar'],
  ] as const)('AC-002-11: ScreenHeader %s', async (variant, back) => {
    const onBack = jest.fn();
    await renderComponent(
      <ScreenHeader
        variant={variant}
        title="Registrar aula"
        onBack={onBack}
        action={<RNText>Editar</RNText>}
      />,
      { set },
    );
    expect(screen.getByRole('header', { name: 'Registrar aula' })).toHaveStyle({
      fontSize: variant === 'tab' ? 26 : 22,
    });
    if (back) {
      await fireEvent.press(screen.getByRole('button', { name: back }));
      expect(onBack).toHaveBeenCalled();
    }
    expect(screen.getByText('Editar')).toBeTruthy();
    expect(pressableProblems()).toEqual([]);
  });

  it('AC-002-11: TabBar shows five tabs in order with the active one on a soft pill', async () => {
    const onSelect = jest.fn();
    await renderComponent(<TabBar active="students" onSelect={onSelect} />, { set });
    expect(screen.getAllByRole('tab').map((t) => t.props.accessibilityLabel)).toEqual([
      'Hoje',
      'Agenda',
      'Alunos',
      'Locais',
      'Ganhos',
    ]);
    expect(screen.getByRole('tab', { name: 'Alunos' }).props.accessibilityState).toMatchObject({
      selected: true,
    });
    expect(screen.getByTestId('tab-pill-students')).toHaveStyle({
      backgroundColor: rgba(c['primary-soft']),
    });
    await fireEvent.press(screen.getByRole('tab', { name: 'Ganhos' }));
    expect(onSelect).toHaveBeenCalledWith('earnings');
    expect(pressableProblems()).toEqual([]);
  });

  it('AC-002-11: WeekStrip selects a day and marks days with lessons', async () => {
    const onSelect = jest.fn();
    await renderComponent(<WeekStrip days={days} selected={0} onSelect={onSelect} />, { set });
    expect(screen.getAllByRole('tab')).toHaveLength(7);
    expect(screen.getByRole('tab', { name: 'Seg 26' })).toHaveStyle({
      backgroundColor: rgba(c.primary),
    });
    await fireEvent.press(screen.getByRole('tab', { name: 'Qua 28' }));
    expect(onSelect).toHaveBeenCalledWith(2);
    expect(pressableProblems()).toEqual([]);
  });

  it.each(['default', 'done', 'next'] as const)('AC-002-11: AgendaBlock %s', async (state) => {
    await renderComponent(
      <AgendaBlock
        tag="blue"
        title="Pedro Lima"
        subtitle="Física"
        state={state}
        onPress={() => {}}
      />,
      { set },
    );
    expect(screen.getByRole('button', { name: /Pedro Lima/ })).toHaveStyle({
      backgroundColor: rgba(c['tag-blue-soft']),
    });
    if (state === 'done') expect(screen.getByLabelText('Concluída')).toBeTruthy();
    if (state === 'next') expect(screen.getByText('Próxima')).toBeTruthy();
    expect(pressableProblems()).toEqual([]);
  });
});
