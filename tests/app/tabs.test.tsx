import { mockPhone } from './mockPhone';

import { fireEvent, renderRouter, screen } from 'expo-router/testing-library';

const tabNames = (language: 'pt' | 'en') =>
  language === 'pt'
    ? ['Hoje', 'Agenda', 'Alunos', 'Locais', 'Ganhos']
    : ['Today', 'Schedule', 'Students', 'Places', 'Earnings'];

// Bottom tab buttons carry testID tab-<route> and the tab name as their label.
function tabBar() {
  return screen.getAllByTestId(/^tab-/).map((tab) => tab.props.accessibilityLabel as string);
}

const tab = (name: string) => screen.getByRole('button', { name });

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
      ['Alunos', '/students', 'Nenhum aluno ainda'],
      ['Locais', '/places', 'Nenhum local ainda'],
      ['Ganhos', '/earnings', 'Nada ganho ainda'],
      ['Hoje', '/today', 'Nenhuma aula hoje'],
    ];
    for (const [name, path, title] of expected) {
      await fireEvent.press(tab(name));
      expect(screen).toHavePathname(path);
      expect(screen.getByText(title)).toBeTruthy();
    }
  });

  it('AC-001-3: the gear on Hoje opens an empty Ajustes with a back button', async () => {
    await renderRouter('./src/app');
    await fireEvent.press(screen.getByRole('button', { name: 'Ajustes' }));
    expect(screen).toHavePathname('/settings');
    // The native header title is not in the JS tree; .maestro/shell.yaml checks it on screen.
    expect(screen.getByTestId('settings-screen')).toBeTruthy();

    await fireEvent.press(screen.getByRole('button', { name: 'Voltar' }));
    expect(screen).toHavePathname('/today');
  });
});
