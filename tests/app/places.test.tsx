import './mockPhone';

import { router } from 'expo-router';
import { act, fireEvent, renderRouter, screen } from 'expo-router/testing-library';

import { BUILTIN_IDS, builtinPlace } from '@/domain/places';
import { currentDatabase, useSettingsStore } from '@/state/settingsStore';
import { savePlace, saveStudent, saveGroup, type Ctx } from '@/usecases/catalog';

let n = 0;
const ctx = (): Ctx => ({
  db: currentDatabase(),
  now: '2026-10-27T12:00:00.000Z',
  newId: () => `id-${++n}`,
});

function seedSalinha() {
  const { id: _i, archivedAt: _a, ...base } = builtinPlace('online');
  const place = savePlace(ctx(), {
    ...base,
    builtin: null,
    name: 'Salinha Aprender+',
    address: 'Rua das Flores, 120',
    kind: 'salinha',
    payer: 'place',
    rateCents: 6000,
    payCycle: 'monthly',
  });
  if (!place.ok) throw new Error(place.error);
  const pedro = saveStudent(
    ctx(),
    {
      name: 'Pedro Lima',
      subject: 'Física',
      phone: null,
      notes: null,
      color: 'blue',
      rateType: 'hour',
    },
    [{ placeId: place.value.id, isDefault: true, rateCents: null }],
  );
  if (!pedro.ok) throw new Error(pedro.error);
  saveGroup(ctx(), {
    placeId: place.value.id,
    name: 'Turma ENEM',
    subject: 'Redação',
    rateCents: 18000,
    memberIds: [pedro.value.id],
  });
  return place.value.id;
}

async function open(path: string, seed?: () => void) {
  await renderRouter('./src/app', { initialUrl: '/settings' });
  seed?.();
  await act(async () => router.push(path));
}

describe('Locais', () => {
  it('AC-004-1: built-ins first, then your places, with price, duration and students', async () => {
    await open('/registry?view=places', seedSalinha);
    expect(await screen.findByText('Salinha Aprender+')).toBeTruthy();
    expect(screen.getByText('Casa do Aluno')).toBeTruthy();
    expect(screen.getByLabelText('Padrão · alunos herdam o preço')).toBeTruthy();
    expect(screen.queryByText('Nenhum local seu ainda')).toBeNull();
    expect(screen.getByText('Online / Remoto')).toBeTruthy();
    expect(
      screen.getByText(/^Sem preço · 60 min · nenhum aluno · Deslocamento: varia/),
    ).toBeTruthy();
    expect(screen.getByText(/R\$\s60\/h pagos pelo local · 60 min · 1 aluno$/)).toBeTruthy();
    const order = screen.getAllByText(/^(Casa do Aluno|Online \/ Remoto|Salinha Aprender\+)$/);
    expect(order.map((n) => n.props.children)).toEqual([
      'Casa do Aluno',
      'Online / Remoto',
      'Salinha Aprender+',
    ]);
  });

  it('AC-004-17: a place shows who pays, its students with + Ligar aluno, and its groups', async () => {
    let id = '';
    await open('/registry?view=places', () => (id = seedSalinha()));
    await act(async () => router.push(`/catalog/place/${id}`));
    expect(await screen.findByText('Salinha · Rua das Flores, 120')).toBeTruthy();
    expect(screen.getByText('Quem paga: o local')).toBeTruthy();
    expect(screen.getByText('Mensal')).toBeTruthy();
    expect(screen.getByText('Alunos daqui · 1')).toBeTruthy();
    expect(screen.getByText('Pedro Lima')).toBeTruthy();
    expect(screen.getByText('+ Ligar aluno')).toBeTruthy();
    expect(screen.getByText('Turma ENEM')).toBeTruthy();
    await act(async () => fireEvent.press(screen.getByRole('button', { name: /^Pedro Lima/ })));
    expect(await screen.findByTestId('student-detail')).toBeTruthy();
  });

  it('AC-004-26: built-in names follow the language; typed names never change', async () => {
    await open('/registry?view=places', seedSalinha);
    await act(async () => useSettingsStore.getState().setLanguage('en'));
    expect(await screen.findByText("Student's home")).toBeTruthy();
    expect(screen.getByText('Online / Remote')).toBeTruthy();
    expect(screen.getByText('Salinha Aprender+')).toBeTruthy();
    expect(screen.getByText('Your places')).toBeTruthy();
  });

  it('AC-004-23: Arquivados opens from the archive icon in the header', async () => {
    await open('/registry?view=places');
    await act(async () => fireEvent.press(screen.getByRole('button', { name: 'Arquivados' })));
    expect(await screen.findByTestId('archived')).toBeTruthy();
  });

  it('AC-004-29: turning off Mostrar locais padrão hides them from Locais', async () => {
    await open('/registry?view=places', seedSalinha);
    await screen.findByText('Salinha Aprender+');
    await act(async () => useSettingsStore.getState().updateSettings({ showDefaultPlaces: false }));
    expect(screen.queryByText('Casa do Aluno')).toBeNull();
    expect(screen.queryByText('Online / Remoto')).toBeNull();
    expect(screen.getByText('Salinha Aprender+')).toBeTruthy();
  });

  it('AC-004-2: a built-in place has no name field and no archive button', async () => {
    await open(`/catalog/place/edit?id=${BUILTIN_IDS.online}`);
    expect(await screen.findByTestId('place-edit')).toBeTruthy();
    expect(screen.queryByLabelText('Nome')).toBeNull();
    expect(screen.queryByText('Arquivar')).toBeNull();
  });
});
