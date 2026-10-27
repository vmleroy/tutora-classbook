/**
 * A UUIDv7: 48 bits of Unix milliseconds, then random bits, so ids sort by creation time.
 * `random` must hold at least 10 bytes; the caller supplies them, keeping this pure.
 */
export function uuidv7(nowMs: number, random: Uint8Array): string {
  const bytes = new Uint8Array(16);
  let ms = nowMs;
  for (let i = 5; i >= 0; i--) {
    bytes[i] = ms % 256;
    ms = Math.floor(ms / 256);
  }
  bytes.set(random.subarray(0, 10), 6);
  bytes[6] = 0x70 | (bytes[6]! & 0x0f); // version 7
  bytes[8] = 0x80 | (bytes[8]! & 0x3f); // RFC 4122 variant
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
