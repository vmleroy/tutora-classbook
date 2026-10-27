import { getRandomBytes } from 'expo-crypto';

import { uuidv7 } from '@/domain/id';

/** A new time-ordered id for a row. */
export function newId(): string {
  return uuidv7(Date.now(), getRandomBytes(10));
}
