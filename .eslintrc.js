module.exports = {
  root: true,
  extends: '@react-native',
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
    
    // Encourage explicit interfaces over index signatures
    '@typescript-eslint/consistent-indexed-object-style': ['error', 'record'], // Prefer Record<K, V> over { [key: K]: V }
  },
  overrides: [
    {
      files: ['jest.setup.js'],
      env: {
        jest: true,
      },
    },
  ],
};
