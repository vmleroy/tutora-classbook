module.exports = {
  preset: 'jest-expo',
  roots: ['<rootDir>/src', '<rootDir>/tests', '<rootDir>/scripts'],
  setupFiles: ['<rootDir>/tests/setup.ts'],
  // Reanimated and Worklets load their JS implementations in Node.
  resolver: 'react-native-reanimated/jest/resolver',
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    // global.css is compiled by Metro; tests register the compiled CSS themselves.
    '\\.css$': '<rootDir>/tests/styleStub.js',
    // The package's default entry is ESM (.mjs); Jest loads its CommonJS build instead.
    '^lucide-react-native$':
      '<rootDir>/node_modules/lucide-react-native/dist/cjs/lucide-react-native.js',
  },
};
