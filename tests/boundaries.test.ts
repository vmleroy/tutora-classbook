import { forbiddenDomainImports } from './boundaries';
import { read, sourceFiles } from './sourceFiles';

describe('domain core boundaries', () => {
  it('AC-001-16: no file under src/domain imports React, React Native, Expo, Drizzle or SQLite', () => {
    const files = sourceFiles('src/domain');
    expect(files.length).toBeGreaterThan(0);
    const offenders = files.flatMap((file) =>
      forbiddenDomainImports(read(file)).map((spec) => `${file} imports ${spec}`),
    );
    expect(offenders).toEqual([]);
  });

  it('AC-001-16: the check catches every forbidden kind of import', () => {
    const source = [
      "import React from 'react';",
      "import { View } from 'react-native';",
      "import * as SQLite from 'expo-sqlite';",
      "import { sql } from 'drizzle-orm';",
      "const db = require('better-sqlite3');",
      "export { x } from '../data/db';",
      "import type { Settings } from '@/state/settingsStore';",
      "const m = await import('@expo/vector-icons');",
      "import { formatISO } from 'date-fns';",
      "import { add } from './money';",
    ].join('\n');
    expect(forbiddenDomainImports(source)).toEqual([
      'react',
      'react-native',
      'expo-sqlite',
      'drizzle-orm',
      'better-sqlite3',
      '../data/db',
      '@/state/settingsStore',
      '@expo/vector-icons',
    ]);
  });
});
