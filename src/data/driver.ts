/** The few SQLite calls start-up needs, so expo-sqlite (app) and better-sqlite3 (tests) both fit. */
export interface SqlDriver {
  /** Absolute path of the database file. */
  readonly path: string;
  exec(sql: string): void;
  get<T>(sql: string, ...params: unknown[]): T | undefined;
  run(sql: string, ...params: unknown[]): void;
}

export interface FileOps {
  exists(path: string): boolean;
}
