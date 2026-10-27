import { render, screen } from 'react-native-css-interop/test';

import { createI18n } from '@/i18n';
import { StartupError } from '@/ui/StartupError';
import { ThemeProvider } from '@/ui/theme/ThemeProvider';

import { withAppStyles } from '../../tests/nativewind';

withAppStyles();

async function show(language: 'pt-BR' | 'en', backupPath: string | null) {
  const i18n = await createI18n(language);
  await render(
    <ThemeProvider theme="quadro" mode="light">
      <StartupError t={i18n.t} backupPath={backupPath} message="no such table: nope" />
    </ThemeProvider>,
  );
}

describe('StartupError', () => {
  it('AC-001-14: shows the copied database path in Portuguese', async () => {
    await show('pt-BR', '/data/user/0/com.vmleroy.tutora/files/SQLite/tutora-pre-v2.db');
    expect(screen.getByText('Não foi possível atualizar seus dados')).toBeTruthy();
    expect(
      screen.getByText(
        'Cópia salva em: /data/user/0/com.vmleroy.tutora/files/SQLite/tutora-pre-v2.db',
      ),
    ).toBeTruthy();
  });

  it('AC-001-14: shows the copied database path in English', async () => {
    await show('en', '/files/SQLite/tutora-pre-v2.db');
    expect(screen.getByText('Your data could not be updated')).toBeTruthy();
    expect(screen.getByText('Copy saved at: /files/SQLite/tutora-pre-v2.db')).toBeTruthy();
    expect(screen.getByText('Error: no such table: nope')).toBeTruthy();
  });

  it('AC-001-14: says the database was not changed when there was no copy to make', async () => {
    await show('en', null);
    expect(screen.getByText('The database was not changed.')).toBeTruthy();
  });
});
