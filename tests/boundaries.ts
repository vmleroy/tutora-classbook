// Module specifiers the pure domain core must never reach: UI frameworks, Expo, the
// database, and any layer above or beside it (constitution rules 6 and 7).
const forbidden = [
  /^react($|\/|-)/,
  /^expo($|\/|-)/,
  /^@expo\//,
  /^drizzle-orm($|\/)/,
  /sqlite/,
  /^(@\/|(\.\.\/)+)(data|state|ui|app|usecases|adapters)(\/|$)/,
];

const specifier =
  /(?:import|export)\s+(?:type\s+)?(?:[^'"]*?\sfrom\s+)?['"]([^'"]+)['"]|(?:require|import)\(\s*['"]([^'"]+)['"]\s*\)/g;

export function forbiddenDomainImports(source: string): string[] {
  return [...source.matchAll(specifier)]
    .map((m) => m[1] ?? m[2] ?? '')
    .filter((spec) => forbidden.some((re) => re.test(spec)));
}
