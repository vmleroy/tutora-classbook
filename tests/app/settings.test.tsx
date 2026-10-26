import './mockPhone';

import { renderRouter, screen } from 'expo-router/testing-library';

describe('Ajustes', () => {
  it('AC-001-22: the Sobre row shows app version, build number and schema version', async () => {
    await renderRouter('./src/app', { initialUrl: '/settings' });
    expect(screen.getByText('Sobre')).toBeTruthy();
    expect(screen.getByText('Versão 1.1.0 · build 7 · banco v1')).toBeTruthy();
  });
});
