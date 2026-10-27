import { screen } from 'react-native-css-interop/test';

import { Badge, Button, Card, Chip, ListRow, Money, Screen, TextField } from '@/ui/components';
import { renderComponent, withAppStyles } from '@tests/components';

withAppStyles();

describe('className passthrough', () => {
  it('AC-002-3: extra classes reach the root and add layout', async () => {
    await renderComponent(
      <Screen className="px-2">
        <Button className="mt-4">Salvar</Button>
        <Card className="mt-2" label="Resumo">
          <></>
        </Card>
        <Chip className="self-start" label="Online" />
        <ListRow className="mb-1" title="Lembretes" />
      </Screen>,
    );
    expect(screen.getByRole('button', { name: 'Salvar' })).toHaveStyle({
      marginTop: 16,
      height: 48,
    });
    expect(screen.getByLabelText('Resumo')).toHaveStyle({ marginTop: 8, padding: 16 });
    expect(screen.getByRole('button', { name: 'Online' })).toHaveStyle({ alignSelf: 'flex-start' });
    expect(screen.getByRole('button', { name: 'Lembretes' })).toHaveStyle({ marginBottom: 4 });
  });

  it('AC-002-3: a conflicting token class wins over the component default', async () => {
    await renderComponent(
      <>
        <Badge className="bg-danger-soft">Pago</Badge>
        <Money cents={100} className="text-success-ink" />
        <TextField className="gap-0.5" label="Nome" />
      </>,
    );
    // quadro-light danger-soft #fbe9e7, success-ink #276b48
    expect(screen.getByTestId('badge')).toHaveStyle({ backgroundColor: 'rgba(251, 233, 231, 1)' });
    expect(screen.getByText(/1,00/)).toHaveStyle({ color: 'rgba(39, 107, 72, 1)' });
  });
});
