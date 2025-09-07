module.exports = {
  root: true,
  extends: '@react-native',
  parser: '@typescript-eslint/parser',
  parserOptions: {
    project: './tsconfig.json',
    ecmaVersion: 2020,
    sourceType: 'module',
  },
  rules: {
    // Enforce dot notation over bracket notation
    'dot-notation': 'error',
    
    // Strict unused variable/parameter handling
    '@typescript-eslint/no-unused-vars': [
      'error',
      {
        vars: 'all',
        args: 'all', // Check unused parameters - must be removed
        argsIgnorePattern: '^$', // No underscore prefix allowed - must remove entirely
        ignoreRestSiblings: true,
      }
    ],
    
    // Ban loose typing patterns
    '@typescript-eslint/no-explicit-any': 'error', // Ban 'any' type
    '@typescript-eslint/ban-types': [
      'error',
      {
        types: {
          '{}': {
            message: 'Use explicit interface instead of empty object type',
            fixWith: 'Record<string, unknown>',
          },
          'object': {
            message: 'Use explicit interface or Record<string, T> instead',
            fixWith: 'Record<string, unknown>',
          },
        },
        extendDefaults: true,
      },
    ],

    // Ban dangerous type coercions
    '@typescript-eslint/consistent-type-assertions': [
      'error',
      {
        assertionStyle: 'as',
        objectLiteralTypeAssertions: 'never',
      },
    ],
    '@typescript-eslint/no-unsafe-argument': 'error',
    '@typescript-eslint/no-unsafe-assignment': 'error',
    '@typescript-eslint/no-unsafe-call': 'error',
    '@typescript-eslint/no-unsafe-member-access': 'error',
    '@typescript-eslint/no-unsafe-return': 'error',
    
    // Encourage explicit interfaces over index signatures
    '@typescript-eslint/consistent-indexed-object-style': ['error', 'record'], // Prefer Record<K, V> over { [key: K]: V }
  },
  overrides: [
    {
      files: ['*.js'],
      parser: '@babel/eslint-parser',
      parserOptions: {
        requireConfigFile: false,
        babelOptions: {
          presets: ['@react-native/babel-preset'],
        },
      },
      rules: {
        // Disable TypeScript-specific rules for JS files
        '@typescript-eslint/consistent-type-assertions': 'off',
        '@typescript-eslint/no-unsafe-argument': 'off',
        '@typescript-eslint/no-unsafe-assignment': 'off',
        '@typescript-eslint/no-unsafe-call': 'off',
        '@typescript-eslint/no-unsafe-member-access': 'off',
        '@typescript-eslint/no-unsafe-return': 'off',
      },
    },
    {
      files: ['jest.setup.js'],
      env: {
        jest: true,
      },
    },
    {
      files: ['**/__tests__/**/*', '**/__mocks__/**/*', '**/*.test.ts', '**/*.test.tsx'],
      env: {
        jest: true,
      },
      rules: {
        // Allow more flexibility in test files for mocking purposes
        '@typescript-eslint/no-unsafe-assignment': 'off',
        '@typescript-eslint/no-unsafe-argument': 'off',
        '@typescript-eslint/no-unsafe-call': 'off',
        '@typescript-eslint/no-unsafe-member-access': 'off',
        '@typescript-eslint/no-unsafe-return': 'off',
        // Still ban explicit any and dangerous coercions
        '@typescript-eslint/no-explicit-any': 'error',
        '@typescript-eslint/consistent-type-assertions': [
          'error',
          {
            assertionStyle: 'as',
            objectLiteralTypeAssertions: 'never',
          },
        ],
      },
    },
    {
      files: ['src/types/dependency-fixes.d.ts'],
      rules: {
        // Allow necessary patterns for third-party type augmentations
        '@typescript-eslint/no-explicit-any': 'off',
        '@typescript-eslint/ban-types': 'off',
        '@typescript-eslint/no-unused-vars': 'off',
        '@typescript-eslint/consistent-indexed-object-style': 'off',
      },
    },
  ],
};
