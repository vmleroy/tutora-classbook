import { render, screen } from '@testing-library/react-native';
import * as fc from 'fast-check';
import { Text } from 'react-native';

describe('test harness', () => {
  it('AC-001-18: component tests render React Native in Node, with no emulator', async () => {
    expect(typeof process.versions.node).toBe('string');
    await render(<Text>Tutora</Text>);
    expect(screen.getByText('Tutora')).toBeTruthy();
  });

  it('AC-001-18: property tests run with fast-check', () => {
    fc.assert(fc.property(fc.integer(), fc.integer(), (a, b) => a + b === b + a));
  });
});
