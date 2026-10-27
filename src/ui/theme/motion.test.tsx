import { fireEvent, screen } from 'react-native-css-interop/test';
import * as Reanimated from 'react-native-reanimated';

import { BottomSheet, SegmentedControl, Switch } from '@/ui/components';
import { MOTION_MS } from '@/ui/theme/motion';

import { renderComponent, withAppStyles } from '../../../tests/components';

jest.mock('react-native-reanimated', () => {
  const actual = jest.requireActual('react-native-reanimated');
  return {
    ...actual,
    __esModule: true,
    default: actual.default,
    useReducedMotion: jest.fn(() => false),
    withTiming: jest.fn((to: number, config: unknown) => actual.withTiming(to, config)),
  };
});

withAppStyles();

const reduced = jest.mocked(Reanimated.useReducedMotion);
const timing = jest.mocked(Reanimated.withTiming);

beforeEach(() => {
  timing.mockClear();
});

const options = [
  { value: 'a', label: 'Dia' },
  { value: 'b', label: 'Semana' },
];

describe('motion', () => {
  it('AC-002-17: switches, segment thumbs and sheets animate for 150–250 ms normally', async () => {
    reduced.mockReturnValue(false);
    await renderComponent(
      <>
        <Switch label="Lembretes" />
        <SegmentedControl label="Vista" options={options} />
        <BottomSheet title="Opções" />
      </>,
    );
    timing.mockClear();
    await fireEvent.press(screen.getByRole('switch', { name: 'Lembretes' }));
    await fireEvent.press(screen.getByRole('radio', { name: 'Semana' }));
    expect(timing).toHaveBeenCalled();
    for (const [, config] of timing.mock.calls) expect(config).toEqual({ duration: MOTION_MS });
    expect(MOTION_MS).toBeGreaterThanOrEqual(150);
    expect(MOTION_MS).toBeLessThanOrEqual(250);
  });

  it('AC-002-17: with reduce motion on, they change state without animation', async () => {
    reduced.mockReturnValue(true);
    await renderComponent(
      <>
        <Switch label="Lembretes" />
        <SegmentedControl label="Vista" options={options} />
        <BottomSheet title="Opções" />
      </>,
    );
    await fireEvent.press(screen.getByRole('switch', { name: 'Lembretes' }));
    await fireEvent.press(screen.getByRole('radio', { name: 'Semana' }));
    expect(
      screen.getByRole('switch', { name: 'Lembretes' }).props.accessibilityState,
    ).toMatchObject({
      checked: true,
    });
    expect(timing).not.toHaveBeenCalled();
  });
});
