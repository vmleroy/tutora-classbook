import { fireEvent, renderRouter, screen } from 'expo-router/testing-library';

import { mockPhone } from './mockPhone';

const tabNames = (language: 'pt' | 'en') =>
  language === 'pt'
    ? ['Hoje', 'Agenda', 'Cadastro', 'Ganhos', 'Ajustes']
    : ['Today', 'Schedule', 'Directory', 'Earnings', 'Settings'];

// Bottom tabs have the tab role and the tab name as their label.
function tabBar() {
  return screen.getAllByRole('tab').map((tab) => tab.props.accessibilityLabel as string);
}

const tab = (name: string) => screen.getByRole('tab', { name });

describe('tabs', () => {
  beforeEach(() => {
    mockPhone.languageCode = 'pt';
    mockPhone.languageTag = 'pt-BR';
  });

  it('AC-001-1: a fresh install opens on Hoje with five tabs in order', async () => {
    await renderRouter('./src/app');
    expect(screen).toHavePathname('/today');
    expect(tabBar()).toEqual(tabNames('pt'));
    expect(tab('Hoje').props.accessibilityState).toEqual({ selected: true });
    expect(screen.getByText('Nenhuma aula hoje')).toBeTruthy();
  });

  it('AC-001-1: the tab labels follow the app language', async () => {
    mockPhone.languageCode = 'en';
    mockPhone.languageTag = 'en-US';
    await renderRouter('./src/app');
    expect(tabBar()).toEqual(tabNames('en'));
  });

  it('AC-001-1: each tab opens its empty state', async () => {
    await renderRouter('./src/app');
    const expected: [string, string, string][] = [
      ['Agenda', '/schedule', 'Agenda vazia'],
      ['Cadastro', '/registry', 'Nenhum aluno ainda'],
      ['Ganhos', '/earnings', 'Nada ganho ainda'],
      ['Ajustes', '/settings', 'Sobre'],
      ['Hoje', '/today', 'Nenhuma aula hoje'],
    ];
    for (const [name, path, title] of expected) {
      await fireEvent.press(tab(name));
      expect(screen).toHavePathname(path);
      expect(screen.getByText(title)).toBeTruthy();
    }
  });

  it('AC-001-3: Ajustes is a tab, reachable from every screen', async () => {
    await renderRouter('./src/app');
    await fireEvent.press(tab('Ajustes'));
    expect(screen).toHavePathname('/settings');
    // The native header title is not in the JS tree; .maestro/shell.yaml checks it on screen.
    expect(screen.getByTestId('settings-screen')).toBeTruthy();
  });

  it('AC-005-27: Cadastro holds Alunos and Locais as sub-pages', async () => {
    await renderRouter('./src/app');
    await fireEvent.press(tab('Cadastro'));
    expect(screen.getByTestId('students-screen')).toBeTruthy();
    await fireEvent.press(screen.getByRole('radio', { name: 'Locais' }));
    expect(await screen.findByTestId('places-screen')).toBeTruthy();
    expect(screen.getByText('Casa do Aluno')).toBeTruthy();
  });
});
