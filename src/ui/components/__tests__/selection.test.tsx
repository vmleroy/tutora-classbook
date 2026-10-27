import { useState } from 'react';
import { fireEvent, screen } from 'react-native-css-interop/test';

import { colorSets } from '@/domain/theme/colorSets';
import {
  Checkbox,
  Chip,
  SegmentedControl,
  SelectField,
  Stepper,
  Switch,
  TextField,
} from '@/ui/components';

import {
  pressableProblems,
  renderComponent,
  testSets,
  withAppStyles,
} from '../../../../tests/components';

withAppStyles();

const rgba = (hex: string) => {
  const n = parseInt(hex.slice(1, 7), 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, 1)`;
};

const periods = [
  { value: 'month', label: 'Mês' },
  { value: 'quarter', label: 'Trimestre' },
  { value: 'year', label: 'Ano' },
];

describe.each(testSets)('selection and inputs in %s', (set) => {
  const c = colorSets[set];

  it.each(['md', 'sm'] as const)(
    'AC-002-11: SegmentedControl %s selects one option',
    async (size) => {
      const onChange = jest.fn();
      await renderComponent(
        <SegmentedControl label="Período" options={periods} size={size} onChange={onChange} />,
        { set },
      );
      // The group is named for screen readers but is not itself a focus stop.
      expect(screen.getByLabelText('Período').props.accessibilityRole).toBe('radiogroup');
      expect(screen.getByRole('radio', { name: 'Mês' }).props.accessibilityState).toMatchObject({
        checked: true,
      });
      await fireEvent.press(screen.getByRole('radio', { name: 'Ano' }));
      expect(onChange).toHaveBeenCalledWith('year');
      expect(screen.getByRole('radio', { name: 'Ano' }).props.accessibilityState).toMatchObject({
        checked: true,
      });
      expect(pressableProblems()).toEqual([]);
    },
  );

  it.each([false, true])('AC-002-11: Chip selected=%s', async (selected) => {
    const onPress = jest.fn();
    await renderComponent(<Chip label="Online" selected={selected} onPress={onPress} />, { set });
    const chip = screen.getByRole('button', { name: 'Online' });
    expect(chip).toHaveStyle({
      backgroundColor: rgba(selected ? c.primary : c.surface),
      height: 36,
    });
    await fireEvent.press(chip);
    expect(onPress).toHaveBeenCalledWith(!selected);
    expect(pressableProblems()).toEqual([]);
  });

  it.each([false, true])('AC-002-11: Switch value=%s', async (value) => {
    const onChange = jest.fn();
    await renderComponent(<Switch label="Cronômetro ao vivo" value={value} onChange={onChange} />, {
      set,
    });
    const sw = screen.getByRole('switch', { name: 'Cronômetro ao vivo' });
    expect(sw.props.accessibilityState).toMatchObject({ checked: value });
    expect(sw).toHaveStyle({ backgroundColor: rgba(value ? c.primary : c['line-strong']) });
    await fireEvent.press(sw);
    expect(onChange).toHaveBeenCalledWith(!value);
    expect(pressableProblems()).toEqual([]);
  });

  it('AC-002-11: Switch disabled does not change', async () => {
    const onChange = jest.fn();
    await renderComponent(<Switch label="Lembretes" disabled onChange={onChange} />, { set });
    await fireEvent.press(screen.getByRole('switch', { name: 'Lembretes' }));
    expect(onChange).not.toHaveBeenCalled();
  });

  it.each([false, true])('AC-002-11: Checkbox checked=%s, whole row pressable', async (checked) => {
    const onChange = jest.fn();
    await renderComponent(
      <Checkbox label="Incluir pagamentos" checked={checked} onChange={onChange} />,
      {
        set,
      },
    );
    const box = screen.getByRole('checkbox', { name: 'Incluir pagamentos' });
    expect(box.props.accessibilityState).toMatchObject({ checked });
    await fireEvent.press(box);
    expect(onChange).toHaveBeenCalledWith(!checked);
    expect(pressableProblems()).toEqual([]);
  });

  it.each(['md', 'sm'] as const)('AC-002-11: Stepper %s steps within bounds', async (size) => {
    const onChange = jest.fn();
    function Harness() {
      const [value, setValue] = useState(60);
      return (
        <Stepper
          value={value}
          step={15}
          min={45}
          max={75}
          size={size}
          format={(v) => `${v} min`}
          decrementLabel="Menos 15 minutos"
          incrementLabel="Mais 15 minutos"
          onChange={(v) => {
            setValue(v);
            onChange(v);
          }}
        />
      );
    }
    await renderComponent(<Harness />, { set });
    expect(screen.getByText('60 min')).toHaveStyle({ fontVariant: ['tabular-nums'] });
    await fireEvent.press(screen.getByRole('button', { name: 'Mais 15 minutos' }));
    expect(onChange).toHaveBeenLastCalledWith(75);
    expect(
      screen.getByRole('button', { name: 'Mais 15 minutos' }).props.accessibilityState,
    ).toMatchObject({
      disabled: true,
    });
    await fireEvent.press(screen.getByRole('button', { name: 'Menos 15 minutos' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Menos 15 minutos' }));
    expect(onChange).toHaveBeenLastCalledWith(45);
    expect(pressableProblems()).toEqual([]);
  });

  it.each([
    ['default', {}],
    ['helper', { helper: 'Ex.: Ana Souza' }],
    ['error', { error: 'Número incompleto: faltam 8 dígitos' }],
    ['onPrimary', { tone: 'onPrimary' as const }],
  ])('AC-002-11: TextField %s', async (_, extra) => {
    const onChangeText = jest.fn();
    await renderComponent(
      <TextField label="Nome" placeholder="Ex.: Ana" onChangeText={onChangeText} {...extra} />,
      { set },
    );
    const input = screen.getByLabelText('Nome');
    await fireEvent.changeText(input, 'Ana');
    expect(onChangeText).toHaveBeenCalledWith('Ana');
    if ('error' in extra) {
      expect(screen.getByText(extra.error)).toHaveStyle({ color: rgba(c['danger-ink']) });
      expect(input).toHaveStyle({ borderColor: rgba(c.danger), borderWidth: 2 });
    } else {
      expect(input).toHaveStyle({ height: 48 });
    }
    if ('helper' in extra) expect(screen.getByText(extra.helper)).toBeTruthy();
  });

  it.each([
    ['avatar', { avatar: { initials: 'AS', tag: 'amber' as const }, sub: 'Matemática' }],
    ['icon', { icon: 'calendar' as const, actionLabel: 'Trocar' }],
    ['plain', {}],
  ])('AC-002-11: SelectField with %s shows the current choice', async (_, extra) => {
    const onPress = jest.fn();
    await renderComponent(
      <SelectField label="Aluno" value="Ana Souza" onPress={onPress} {...extra} />,
      {
        set,
      },
    );
    const field = screen.getByRole('button', { name: 'Aluno: Ana Souza' });
    await fireEvent.press(field);
    expect(onPress).toHaveBeenCalled();
    expect(pressableProblems()).toEqual([]);
  });
});
