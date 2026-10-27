import { processColor } from 'react-native';
import { fireEvent, screen } from 'react-native-css-interop/test';

import { colorSets } from '@/domain/theme/colorSets';
import { BottomSheet, Button, EmptyState, TimerRing } from '@/ui/components';

import {
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

describe.each(testSets)('feedback in %s', (set) => {
  const c = colorSets[set];

  it('AC-002-11: BottomSheet shows title, content and outline + primary actions over a scrim', async () => {
    const onClose = jest.fn();
    await renderComponent(
      <BottomSheet
        title="Restaurar backup"
        subtitle="Os dados atuais serão substituídos"
        onClose={onClose}
        actions={
          <>
            <Button variant="outline">Cancelar</Button>
            <Button variant="danger">Restaurar</Button>
          </>
        }
      />,
      { set },
    );
    expect(screen.getByRole('header', { name: 'Restaurar backup' })).toBeTruthy();
    expect(screen.getByTestId('sheet')).toHaveStyle({ backgroundColor: rgba(c.surface) });
    expect(screen.getByTestId('sheet-scrim')).toHaveStyle({
      opacity: set.endsWith('dark') ? 0.6 : 0.5,
    });
    // Outline + primary side by side, 1 : 2.
    expect(screen.getByTestId('sheet-action-secondary')).toHaveStyle({ flexGrow: 1 });
    expect(screen.getByTestId('sheet-action-primary')).toHaveStyle({ flexGrow: 2 });
    await fireEvent.press(screen.getByRole('button', { name: 'Fechar' }));
    expect(onClose).toHaveBeenCalled();
    expect(pressableProblems()).toEqual([]);
  });

  it('AC-002-11: BottomSheet closed renders nothing', async () => {
    await renderComponent(<BottomSheet title="Opções" open={false} />, { set });
    expect(screen.queryByTestId('sheet')).toBeNull();
  });

  it.each([
    ['icon and action', { icon: 'users' as const, action: <Button>Adicionar aluno</Button> }],
    ['title only', {}],
  ])('AC-002-11: EmptyState with %s', async (_, extra) => {
    await renderComponent(
      <EmptyState title="Nenhum aluno ainda" body="Seus alunos aparecem aqui." {...extra} />,
      {
        set,
      },
    );
    expect(screen.getByRole('header', { name: 'Nenhum aluno ainda' })).toBeTruthy();
    expect(pressableProblems()).toEqual([]);
  });

  it.each([0, 0.5, 1.4])('AC-002-11: TimerRing at progress %s', async (progress) => {
    await renderComponent(<TimerRing elapsed="47:12" progress={progress} caption="R$ 2,00/min" />, {
      set,
    });
    const ring = screen.getByLabelText('Tempo de aula: 47:12');
    expect(ring).toBeTruthy();
    expect(screen.getByText('47:12')).toHaveStyle({ fontVariant: ['tabular-nums'], fontSize: 48 });
    // react-native-svg stores the stroke as a processed color.
    expect(screen.getByTestId('timer-fill').props.stroke).toEqual({
      type: 0,
      payload: processColor(c.accent),
    });
  });
});
