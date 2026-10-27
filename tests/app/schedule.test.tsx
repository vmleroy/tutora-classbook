import './mockPhone';

import { router } from 'expo-router';
import { act, fireEvent, renderRouter, screen } from 'expo-router/testing-library';

import { BUILTIN_IDS } from '@/domain/places';
import { localToUtc } from '@/domain/recurrence';
import { deviceZone } from '@/state/schedule';
import { currentDatabase, useSettingsStore } from '@/state/settingsStore';
import { saveStudent, type Ctx } from '@/usecases/catalog';
import { scheduleLesson } from '@/usecases/schedule';

// Monday 26 Oct 2026, 09:00 on this machine's clock: seeding and screens share the zone.
const ZONE = deviceZone();
const NOW = localToUtc('2026-10-26', '09:00', ZONE);
let n = 0;

beforeEach(() => jest.useFakeTimers({ advanceTimers: true, now: new Date(NOW) }));
afterEach(() => jest.useRealTimers());

const ctx = () => ({
  db: currentDatabase(),
  now: NOW,
  zone: ZONE,
  newId: () => `id-${String(++n).padStart(5, '0')}`,
});

function seed() {
  const ana = saveStudent(
    ctx() as Ctx,
    {
      name: 'Ana Souza',
      subject: 'Matemática',
      phone: null,
      notes: null,
      color: 'violet',
      rateType: 'hour',
    },
    [{ placeId: BUILTIN_IDS.online, isDefault: true, rateCents: 12000 }],
  );
  if (!ana.ok) throw new Error();
  const base = {
    who: { studentId: ana.value.id },
    placeId: BUILTIN_IDS.online,
    time: '14:00',
    plannedMin: 60,
    valueCents: null,
    travelFeeCents: null,
  };
  scheduleLesson(ctx(), { ...base, date: '2026-10-26', repeat: 'weekly' });
  scheduleLesson(ctx(), { ...base, date: '2026-10-28', time: '16:30', repeat: null });
}

async function open(path: string, withData = true) {
  await renderRouter('./src/app', { initialUrl: '/settings' });
  if (withData) seed();
  await act(async () => router.push(path));
}

describe('Agenda', () => {
  it('AC-005-1: the + on Agenda and Hoje opens Nova aula to schedule', async () => {
    await open('/schedule');
    await act(async () => fireEvent.press(screen.getByRole('button', { name: 'Nova aula' })));
    expect(await screen.findByText('Agendar')).toBeTruthy();
    await act(async () => router.push('/today'));
    await act(async () => fireEvent.press(screen.getByRole('button', { name: 'Nova aula' })));
    expect(await screen.findByText('Agendar')).toBeTruthy();
  });

  it('AC-005-17: the month and a Monday-to-Sunday week strip, moving by week', async () => {
    await open('/schedule');
    expect(await screen.findByText('Outubro 2026')).toBeTruthy();
    // The first seven tabs are the days; the tab bar's own tabs follow.
    const tabs = screen.getAllByRole('tab').map((d) => d.props.accessibilityLabel);
    expect(tabs.slice(0, 7)).toEqual([
      'Seg 26',
      'Ter 27',
      'Qua 28',
      'Qui 29',
      'Sex 30',
      'Sáb 31',
      'Dom 1',
    ]);
    await act(async () => fireEvent.press(screen.getByRole('button', { name: 'Próxima semana' })));
    expect(screen.getByText('Novembro 2026')).toBeTruthy();
  });

  it('AC-005-18: Dia shows each class as a block, the next one marked, with the day header', async () => {
    await open('/schedule');
    expect(await screen.findByText('Ana Souza')).toBeTruthy();
    expect(screen.getByText('Matemática · Online / Remoto')).toBeTruthy();
    expect(screen.getByText('Próxima')).toBeTruthy();
    expect(screen.getByText('Hoje · 1 aula')).toBeTruthy();
    expect(screen.getByText(/^Previsto R\$\s120$/)).toBeTruthy();
  });

  it('AC-005-20: Semana shows the seven days side by side', async () => {
    await open('/schedule');
    await screen.findByText('Ana Souza');
    await act(async () => fireEvent.press(screen.getByRole('radio', { name: 'Semana' })));
    expect(screen.getAllByText('Ana Souza')).toHaveLength(2); // Monday and Wednesday
  });

  it('AC-005-25: with no classes, Agenda and Hoje keep their empty states', async () => {
    await open('/schedule', false);
    expect(await screen.findByText('Agenda vazia')).toBeTruthy();
    await act(async () => router.push('/today'));
    expect(await screen.findByText('Nenhuma aula hoje')).toBeTruthy();
  });

  it('AC-005-26: dates and times follow the language; switching never moves a class', async () => {
    await open('/schedule');
    await screen.findByText('Ana Souza');
    await act(async () => useSettingsStore.getState().setLanguage('en'));
    expect(await screen.findByText('October 2026')).toBeTruthy();
    expect(screen.getByText('Today · 1 lesson')).toBeTruthy();
    expect(screen.getAllByRole('tab')[0]?.props.accessibilityLabel).toBe('Mon 26');
    await act(async () => router.push('/today'));
    // The next-class card and the list both show it.
    const times = await screen.findAllByText(/^2:00\sPM$/);
    expect(times).toHaveLength(2);
    // "2:00 PM" is wider than "14:00": it stays on one line, never "2:00 P / M".
    expect(times.every((el) => el.props.numberOfLines === 1)).toBe(true);
  });
});

describe('Hoje', () => {
  it("AC-005-24: today's classes in order with time, place, duration and value", async () => {
    await open('/today');
    expect(await screen.findByText('Aulas de hoje')).toBeTruthy();
    expect(screen.getByText('1 aula')).toBeTruthy();
    expect(screen.getByText('Matemática · Online / Remoto · 60 min')).toBeTruthy();
    expect(screen.getByText(/^R\$\s120,00$/)).toBeTruthy();
    expect(screen.getByText('Segunda, 26 de outubro')).toBeTruthy();
    expect(screen.getByText('Bom dia')).toBeTruthy();
    expect(screen.getByText('Próxima aula · em 5h')).toBeTruthy();
  });
});
