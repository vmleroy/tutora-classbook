import { hexToChannels } from './channels';

it('AC-001-8: palette hex values become space-separated RGB channels', () => {
  expect(hexToChannels('#2e5e4e')).toBe('46 94 78');
  expect(hexToChannels('#FFFFFF')).toBe('255 255 255');
  expect(hexToChannels('#141c1980')).toBe('20 28 25');
  expect(() => hexToChannels('red')).toThrow();
});
