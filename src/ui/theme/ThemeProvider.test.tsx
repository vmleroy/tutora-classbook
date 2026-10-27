import { useEffect } from 'react';
import { View } from 'react-native';
import { act, render, screen } from 'react-native-css-interop/test';

import { Text } from '@/ui/components/Text';
import { ThemeProvider } from '@/ui/theme/ThemeProvider';

import { withAppStyles } from '../../../tests/nativewind';

let mockScheme: 'light' | 'dark' | null = 'light';
const mockListeners = new Set<() => void>();
jest.mock('@/ui/theme/useSystemScheme', () => {
  const { useSyncExternalStore } = jest.requireActual('react');
  return {
    useSystemScheme: () =>
      useSyncExternalStore(
        (l: () => void) => (mockListeners.add(l), () => mockListeners.delete(l)),
        () => mockScheme,
      ),
  };
});
function setPhoneScheme(scheme: 'light' | 'dark') {
  mockScheme = scheme;
  mockListeners.forEach((l) => l());
}

withAppStyles();
beforeEach(() => setPhoneScheme('light'));

describe('ThemeProvider', () => {
  it('AC-001-9: follows the phone switching light and dark while open, without remounting', async () => {
    let mounts = 0;
    function Probe() {
      useEffect(() => {
        mounts += 1;
      }, []);
      return <View testID="probe" className="bg-primary" />;
    }
    await render(
      <ThemeProvider theme="quadro" mode="system">
        <Probe />
      </ThemeProvider>,
    );
    expect(screen.getByTestId('probe')).toHaveStyle({ backgroundColor: 'rgba(46, 94, 78, 1)' });

    const switchedAt = Date.now();
    await act(() => setPhoneScheme('dark'));
    expect(screen.getByTestId('probe')).toHaveStyle({ backgroundColor: 'rgba(111, 179, 154, 1)' });
    expect(Date.now() - switchedAt).toBeLessThan(1000);

    await act(() => setPhoneScheme('light'));
    expect(screen.getByTestId('probe')).toHaveStyle({ backgroundColor: 'rgba(46, 94, 78, 1)' });
    expect(mounts).toBe(1);
  });

  it('AC-001-9: an explicit mode ignores the phone', async () => {
    await render(
      <ThemeProvider theme="quadro" mode="dark">
        <View testID="probe" className="bg-background" />
      </ThemeProvider>,
    );
    expect(screen.getByTestId('probe')).toHaveStyle({ backgroundColor: 'rgba(20, 28, 25, 1)' });
    await act(() => setPhoneScheme('light'));
    expect(screen.getByTestId('probe')).toHaveStyle({ backgroundColor: 'rgba(20, 28, 25, 1)' });
  });
});

describe('Text', () => {
  it('AC-001-11: body text uses Nunito, and headings use the Nunito weight file', async () => {
    await render(
      <ThemeProvider theme="quadro" mode="light">
        <Text>Hoje</Text>
        <Text variant="title-xl">Boa tarde</Text>
        <Text variant="row-title">Ana</Text>
      </ThemeProvider>,
    );
    expect(screen.getByText('Hoje')).toHaveStyle({ fontFamily: 'Nunito_400Regular' });
    expect(screen.getByText('Boa tarde')).toHaveStyle({ fontFamily: 'Nunito_800ExtraBold' });
    expect(screen.getByText('Ana')).toHaveStyle({ fontFamily: 'Nunito_700Bold' });
  });

  it('AC-001-11: numbers styled as money use tabular figures', async () => {
    await render(
      <ThemeProvider theme="quadro" mode="light">
        <Text variant="money-xl">R$ 240,00</Text>
        <Text variant="body" tabular>
          14:00
        </Text>
        <Text variant="body">Ana Souza</Text>
      </ThemeProvider>,
    );
    expect(screen.getByText('R$ 240,00')).toHaveStyle({
      fontFamily: 'Nunito_800ExtraBold',
      fontVariant: ['tabular-nums'],
    });
    expect(screen.getByText('14:00')).toHaveStyle({ fontVariant: ['tabular-nums'] });
    expect(screen.getByText('Ana Souza').props.style).not.toEqual(
      expect.objectContaining({ fontVariant: ['tabular-nums'] }),
    );
  });
});
