import fs from 'node:fs';
import path from 'node:path';

export const repoRoot = path.resolve(__dirname, '..');

/** Every .ts/.tsx file under `dir` (relative to the repo root). */
export function sourceFiles(dir: string): string[] {
  const abs = path.join(repoRoot, dir);
  if (!fs.existsSync(abs)) return [];
  return fs.readdirSync(abs, { recursive: true, withFileTypes: true }).flatMap((entry) => {
    if (!entry.isFile() || !/\.tsx?$/.test(entry.name)) return [];
    return [path.relative(repoRoot, path.join(entry.parentPath, entry.name))];
  });
}

export function read(file: string): string {
  return fs.readFileSync(path.join(repoRoot, file), 'utf8');
}
