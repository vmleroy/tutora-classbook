import { cn, tv } from '@/ui/cn';

describe('cn: clsx + tailwind-merge that knows the design tokens', () => {
  it('AC-002-3: a token font size and a token color are different groups, both kept', () => {
    expect(cn('text-body', 'text-ink')).toBe('text-body text-ink');
    expect(cn('text-money-xl', 'text-earnings-on-header')).toBe(
      'text-money-xl text-earnings-on-header',
    );
  });

  it('AC-002-3: the later class of the same group wins', () => {
    expect(cn('bg-primary', 'bg-surface-sunken')).toBe('bg-surface-sunken');
    expect(cn('text-ink', 'text-danger-ink')).toBe('text-danger-ink');
    expect(cn('text-body', 'text-caption')).toBe('text-caption');
    expect(cn('font-regular', 'font-black')).toBe('font-black');
    expect(cn('p-4', 'p-2.5')).toBe('p-2.5');
    expect(cn('h-control-md', 'h-control-sm')).toBe('h-control-sm');
    expect(cn('rounded-lg', 'rounded-2xl')).toBe('rounded-2xl');
    expect(cn('border-line', 'border-btn-primary')).toBe('border-btn-primary');
  });

  it('AC-002-3: conditions read like clsx', () => {
    const on = true;
    expect(
      cn('rounded-full', on && 'bg-btn-primary', !on && 'bg-surface', { 'opacity-40': false }),
    ).toBe('rounded-full bg-btn-primary');
  });

  it('AC-002-3: tv() variants merge with the same token rules', () => {
    const box = tv({ base: 'bg-surface p-4', variants: { tone: { danger: 'bg-danger-soft' } } });
    expect(box({ tone: 'danger', className: 'p-2' })).toBe('bg-danger-soft p-2');
  });
});
