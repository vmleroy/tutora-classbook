const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
const prettierRecommended = require('eslint-plugin-prettier/recommended');

const tutora = require('./eslint/tutora-plugin');

module.exports = defineConfig([
  expoConfig,
  prettierRecommended,
  {
    ignores: ['node_modules/', '.expo/', 'android/', 'ios/', 'dist/', 'coverage/', 'expo-env.d.ts'],
  },
  {
    // One import order everywhere: packages, then @/ modules, then relative files.
    rules: {
      'import/order': [
        'warn',
        {
          groups: ['builtin', 'external', 'internal', ['parent', 'sibling', 'index']],
          pathGroups: [{ pattern: '@/**', group: 'internal' }],
          pathGroupsExcludedImportTypes: ['builtin'],
          'newlines-between': 'always',
          alphabetize: { order: 'asc', caseInsensitive: true },
        },
      ],
    },
  },
  {
    // Constitution rule 7: the domain core is pure. tests/boundaries.test.ts checks the same list.
    files: ['src/domain/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              regex: '^(react|expo)($|/|-)',
              message: 'src/domain must stay pure: no React or Expo.',
            },
            { regex: '^@expo/', message: 'src/domain must stay pure: no Expo.' },
            { regex: '^drizzle-orm|sqlite', message: 'src/domain must stay pure: no database.' },
            {
              regex: '^(@/|(\\.\\./)+)(data|state|ui|app|usecases|adapters)(/|$)',
              message: 'src/domain imports nothing from the layers above it.',
            },
          ],
        },
      ],
    },
  },
  {
    // AC-002-14: screens and components use tokens only. Test files may use fixtures.
    files: ['src/app/**/*.{ts,tsx}', 'src/ui/components/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}'],
    plugins: { tutora },
    rules: { 'tutora/no-raw-design-values': 'error' },
  },
]);
