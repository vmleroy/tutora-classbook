import { uuidv7 } from './id';

describe('uuidv7', () => {
  it('is a version 7 UUID that sorts by creation time', () => {
    const random = new Uint8Array(10).fill(255);
    const a = uuidv7(1_761_566_400_000, random);
    const b = uuidv7(1_761_566_400_001, new Uint8Array(10));
    expect(a).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    expect(a < b).toBe(true);
  });
});
