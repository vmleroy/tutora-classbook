import './mockPhone';

import { act, renderRouter, screen } from 'expo-router/testing-library';

import { loadSettings } from '@/data/repos/settingsRepo';
import { currentDatabase, useSettingsStore } from '@/state/settingsStore';

const store = () => useSettingsStore.getState();
const rgba = (hex: string) => {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, 1)`;
};

describe('theme switching', () => {
  it('AC-002-5: changing theme or mode repaints the open screen at once, without remounting it', async () => {
    await renderRouter('./src/app');
    const before = screen.getByTestId('today-screen');
    expect(screen.getByTestId('today-screen')).toHaveStyle({ backgroundColor: rgba('#faf7f0') });

    await act(() => store().updateSettings({ themeId: 'biblioteca' }));
    expect(screen.getByTestId('today-screen')).toHaveStyle({ backgroundColor: rgba('#fbf6ef') });

    await act(() => store().updateSettings({ themeMode: 'dark' }));
    expect(screen.getByTestId('today-screen')).toHaveStyle({ backgroundColor: rgba('#1c1416') });

    // Same host view: the screen was repainted, not rebuilt.
    expect(screen.getByTestId('today-screen')).toBe(before);
    expect(loadSettings(currentDatabase())).toMatchObject({
      themeId: 'biblioteca',
      themeMode: 'dark',
    });
  });

  it('AC-002-8: a custom color under 4.5:1 is not saved until the teacher confirms it', async () => {
    await renderRouter('./src/app');
    const refused = await act(() => store().setCustomColor('cards', '#5c6a70', 'light'));
    expect(refused).toEqual({ saved: false, ratio: expect.any(Number) });
    expect(refused.ratio).toBeLessThan(4.5);
    expect(store().settings?.customColors).toEqual({});

    const confirmed = await act(() =>
      store().setCustomColor('cards', '#5c6a70', 'light', { confirmed: true }),
    );
    expect(confirmed.saved).toBe(true);
    expect(store().settings?.customColors).toEqual({ cards: '#5c6a70' });
    expect(loadSettings(currentDatabase()).customColors).toEqual({ cards: '#5c6a70' });
  });

  it('AC-002-8: a readable custom color is saved straight away and paints the app', async () => {
    await renderRouter('./src/app');
    const result = await act(() => store().setCustomColor('background', '#f3f9f8', 'light'));
    expect(result.saved).toBe(true);
    expect(screen.getByTestId('today-screen')).toHaveStyle({ backgroundColor: rgba('#f3f9f8') });
  });

  it('AC-002-9: Restaurar cores removes every custom color and returns to the theme', async () => {
    await renderRouter('./src/app');
    await act(() => store().setCustomColor('background', '#f3f9f8', 'light'));
    await act(() => store().setCustomColor('primaryBtn', '#7a2e3b', 'light'));
    await act(() => store().resetCustomColors());
    expect(store().settings?.customColors).toEqual({});
    expect(loadSettings(currentDatabase()).customColors).toEqual({});
    expect(screen.getByTestId('today-screen')).toHaveStyle({ backgroundColor: rgba('#faf7f0') });
  });
});
