module.exports = {
  preset: 'react-native',
  transformIgnorePatterns: [
    'node_modules/(?!(react-native|@react-native|react-native-nfc-manager|@react-navigation|react-native-paper|react-native-vector-icons|react-native-safe-area-context|react-native-screens|@react-native-async-storage|react-native-tab-view|react-native-pager-view)/)',
  ],
  moduleNameMapper: {
    '^react-native-vector-icons/(.*)$': 'react-native-vector-icons/dist/$1',
  },
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  projects: [
    {
      displayName: 'unit',
      testMatch: ['<rootDir>/__tests__/**/*.unit.test.{js,ts,tsx}'],
      testEnvironment: 'node',
      preset: 'react-native',
      transformIgnorePatterns: [
        'node_modules/(?!(react-native|@react-native|react-native-nfc-manager|@react-navigation|react-native-paper|react-native-vector-icons|react-native-safe-area-context|react-native-screens|@react-native-async-storage|react-native-tab-view|react-native-pager-view)/)',
      ],
      setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
    },
    {
      displayName: 'integration', 
      testMatch: ['<rootDir>/__tests__/**/*.integration.test.{js,ts,tsx}'],
      testEnvironment: 'node',
      preset: 'react-native',
      transformIgnorePatterns: [
        'node_modules/(?!(react-native|@react-native|react-native-nfc-manager|@react-navigation|react-native-paper|react-native-vector-icons|react-native-safe-area-context|react-native-screens|@react-native-async-storage|react-native-tab-view|react-native-pager-view)/)',
      ],
      setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
    },
    {
      displayName: 'component',
      testMatch: ['<rootDir>/__tests__/**/*.component.test.{js,ts,tsx}'],
      testEnvironment: 'jsdom',
      preset: 'react-native',
      transformIgnorePatterns: [
        'node_modules/(?!(react-native|@react-native|react-native-nfc-manager|@react-navigation|react-native-paper|react-native-vector-icons|react-native-safe-area-context|react-native-screens|@react-native-async-storage|react-native-tab-view|react-native-pager-view)/)',
      ],
      setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
    },
    {
      displayName: 'e2e',
      testMatch: ['<rootDir>/__tests__/**/*.e2e.test.{js,ts,tsx}'],
      testEnvironment: 'jsdom',
      preset: 'react-native',
      transformIgnorePatterns: [
        'node_modules/(?!(react-native|@react-native|react-native-nfc-manager|@react-navigation|react-native-paper|react-native-vector-icons|react-native-safe-area-context|react-native-screens|@react-native-async-storage|react-native-tab-view|react-native-pager-view)/)',
      ],
      setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
    },
    {
      displayName: 'default',
      testMatch: ['<rootDir>/__tests__/**/!(*.unit|*.integration|*.component|*.e2e).test.{js,ts,tsx}'],
      testEnvironment: 'node',
      preset: 'react-native',
      transformIgnorePatterns: [
        'node_modules/(?!(react-native|@react-native|react-native-nfc-manager|@react-navigation|react-native-paper|react-native-vector-icons|react-native-safe-area-context|react-native-screens|@react-native-async-storage|react-native-tab-view|react-native-pager-view)/)',
      ],
      setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
    }
  ],
  coverageDirectory: 'coverage',
  collectCoverageFrom: [
    'src/**/*.{js,ts,tsx}',
    '!src/**/*.d.ts',
    '!src/**/*.stories.{js,ts,tsx}',
    '!src/**/__tests__/**',
  ],
  coverageReporters: ['text', 'lcov', 'html'],
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80,
    },
  },
};
