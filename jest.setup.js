// Mock react-native-nfc-manager
jest.mock('react-native-nfc-manager', () => ({
  isSupported: jest.fn(() => Promise.resolve(true)),
  start: jest.fn(() => Promise.resolve()),
  stop: jest.fn(() => Promise.resolve()),
  isEnabled: jest.fn(() => Promise.resolve(true)),
  requestTechnology: jest.fn(() => Promise.resolve()),
  cancelTechnologyRequest: jest.fn(() => Promise.resolve()),
  getTag: jest.fn(() => Promise.resolve({ id: [1, 2, 3, 4], techTypes: ['MifareClassic'] })),
  NfcTech: {
    MifareClassic: 'MifareClassic',
    Ndef: 'Ndef',
    NfcA: 'NfcA',
  },
  Ndef: {
    getNdefMessage: jest.fn(() => Promise.resolve([])),
  },
}));

// Mock @react-native-async-storage/async-storage
jest.mock('@react-native-async-storage/async-storage', () => ({
  getItem: jest.fn(() => Promise.resolve(null)),
  setItem: jest.fn(() => Promise.resolve()),
  removeItem: jest.fn(() => Promise.resolve()),
  clear: jest.fn(() => Promise.resolve()),
}));

// Mock react-native-vector-icons
jest.mock('react-native-vector-icons/MaterialIcons', () => 'Icon');
jest.mock('react-native-vector-icons/MaterialCommunityIcons', () => 'Icon');

// Mock react-native Paper
jest.mock('react-native-paper', () => ({
  Provider: ({ children }) => children,
  MD3LightTheme: {},
  MD3DarkTheme: {},
  Card: ({ children }) => children,
  Title: ({ children }) => children,
  Paragraph: ({ children }) => children,
  Button: ({ children, onPress }) => ({ children, onPress }),
  FAB: ({ onPress, icon, label }) => ({ onPress, icon, label }),
  Surface: ({ children }) => children,
  Text: ({ children }) => children,
  IconButton: ({ icon, onPress }) => ({ icon, onPress }),
  ProgressBar: ({ progress }) => ({ progress }),
  ActivityIndicator: ({ size, color }) => ({ size, color }),
  Searchbar: ({ value, onChangeText, placeholder }) => ({ value, onChangeText, placeholder }),
  Chip: ({ children, selected, onPress }) => ({ children, selected, onPress }),
  List: {
    Item: ({ title, description, left, right, onPress }) => ({ title, description, left, right, onPress }),
    Icon: ({ icon, color }) => ({ icon, color }),
  },
  Divider: () => null,
  Switch: ({ value, onValueChange }) => ({ value, onValueChange }),
  Slider: ({ value, onValueChange, minimumValue, maximumValue, step }) => ({ value, onValueChange, minimumValue, maximumValue, step }),
  Modal: ({ visible, children, onDismiss }) => visible ? children : null,
  Portal: ({ children }) => children,
  TextInput: ({ value, onChangeText, placeholder, multiline }) => ({ value, onChangeText, placeholder, multiline }),
}));