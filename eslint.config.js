const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
const prettierRecommended = require('eslint-plugin-prettier/recommended');

module.exports = defineConfig([
  expoConfig,
  prettierRecommended,
  {
    ignores: ['node_modules/', '.expo/', 'android/', 'ios/', 'dist/', 'coverage/', 'expo-env.d.ts'],
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
]);
