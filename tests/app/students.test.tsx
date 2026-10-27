import './mockPhone';

import { router } from 'expo-router';
import { act, fireEvent, renderRouter, screen } from 'expo-router/testing-library';

import { BUILTIN_IDS } from '@/domain/places';
import { currentDatabase } from '@/state/settingsStore';
import { saveGroup, saveStudent, type Ctx } from '@/usecases/catalog';

let n = 0;
const ctx = (): Ctx => ({
  db: currentDatabase(),
  now: '2026-10-27T12:00:00.000Z',
  newId: () => `id-${++n}`,
});
const base = { phone: null, notes: null, color: 'amber' as const, rateType: 'hour' as const };

function seed() {
  const ana = saveStudent(ctx(), { ...base, name: 'Ana Souza', subject: 'Matemática' }, [
    { placeId: BUILTIN_IDS.online, isDefault: true, rateCents: 12000 },
    { placeId: BUILTIN_IDS.student_home, isDefault: false, rateCents: null },
  ]);
  saveStudent(ctx(), { ...base, name: 'Lucas Mendes', subject: 'Química' }, [
    { placeId: BUILTIN_IDS.student_home, isDefault: true, rateCents: null },
  ]);
  saveGroup(ctx(), {
    placeId: BUILTIN_IDS.online,
    name: 'Turma ENEM',
    subject: 'Redação',
    rateCents: 18000,
    memberIds: [],
  });
  if (!ana.ok) throw new Error(ana.error);
  return ana.value.id;
}

async function open(path: string) {
  let id = '';
  await renderRouter('./src/app', { initialUrl: '/settings' });
  id = seed();
  await act(async () => router.push(path.replace(':ana', id)));
}

describe('Alunos', () => {
  it('AC-004-14: lists students and groups by name with search and Por local', async () => {
    await open('/students');
    expect(await screen.findByText('Matemática · Online / Remoto, Casa do Aluno')).toBeTruthy();
    expect(
      screen.getByText(/^Redação · Online \/ Remoto · nenhum aluno · R\$\s180\/aula$/),
    ).toBeTruthy();
    expect(screen.getByText('Todos · 3')).toBeTruthy();

    await act(async () => fireEvent.changeText(screen.getByLabelText('Buscar aluno'), 'lucas'));
    expect(screen.queryByText('Ana Souza')).toBeNull();
    expect(screen.getByText('Lucas Mendes')).toBeTruthy();

    await act(async () => fireEvent.changeText(screen.getByLabelText('Buscar aluno'), ''));
    await act(async () => fireEvent.press(screen.getByText('Por local')));
    expect(screen.getByLabelText('Casa do Aluno')).toBeTruthy();
    expect(screen.getByLabelText('Online / Remoto')).toBeTruthy();
  });

  it("AC-004-15: a student's detail shows contact, rate and places, which open the place", async () => {
    await open('/catalog/student/:ana');
    expect(await screen.findByText('Ana Souza')).toBeTruthy();
    expect(screen.getByText('Por hora')).toBeTruthy();
    expect(screen.getByText(/^R\$\s120\/h$/)).toBeTruthy();
    expect(screen.getByText('Sem preço')).toBeTruthy();
    await act(async () => fireEvent.press(screen.getByRole('button', { name: /^Casa do Aluno/ })));
    expect(await screen.findByTestId('place-detail')).toBeTruthy();
  });
});
