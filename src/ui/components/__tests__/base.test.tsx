import { fireEvent, render, screen } from 'react-native-css-interop/test';

import { Button, Card, EmptyState, Screen, Text } from '@/ui/components';
import { ThemeProvider } from '@/ui/theme/ThemeProvider';

import { withAppStyles } from '../../../../tests/nativewind';

withAppStyles();

const inTheme = (ui: React.ReactElement) => (
  <ThemeProvider theme="quadro" mode="light">
    {ui}
  </ThemeProvider>
);

describe('base components', () => {
  it('AC-001-10: Button paints with semantic classes and reports presses', async () => {
    const onPress = jest.fn();
    await render(inTheme(<Button onPress={onPress}>Salvar aula</Button>));
    const button = screen.getByRole('button', { name: 'Salvar aula' });
    expect(button).toHaveStyle({ backgroundColor: 'rgba(46, 94, 78, 1)', height: 48 });
    fireEvent.press(button);
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('AC-001-10: Card and EmptyState render inside a Screen', async () => {
    await render(
      inTheme(
        <Screen>
          <Card label="Resumo">
            <Text>Nada por aqui</Text>
          </Card>
          <EmptyState title="Nenhuma aula hoje" body="Suas aulas aparecem aqui." />
        </Screen>,
      ),
    );
    expect(screen.getByLabelText('Resumo')).toHaveStyle({
      backgroundColor: 'rgba(255, 255, 255, 1)',
      borderRadius: 20,
      padding: 16,
    });
    expect(screen.getByRole('header', { name: 'Nenhuma aula hoje' })).toBeTruthy();
  });
});
