module.exports = {
  preset: 'react-native',
  transformIgnorePatterns: [
    'node_modules/(?!(react-native|@react-native|react-native-nfc-manager|@react-navigation|react-native-paper|react-native-vector-icons|react-native-safe-area-context|react-native-screens|@react-native-async-storage)/)',
  ],
  moduleNameMapper: {
    '^react-native-vector-icons/(.*)$': 'react-native-vector-icons/dist/$1',
  },
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
};
