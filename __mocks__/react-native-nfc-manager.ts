/**
 * Mock for react-native-nfc-manager
 * Provides type-safe mocks for NFC operations in testing
 */

export const mockNfcLib = {
  isSupported: jest.fn() as jest.MockedFunction<() => Promise<boolean>>,
  start: jest.fn() as jest.MockedFunction<() => Promise<void>>,
  stop: jest.fn() as jest.MockedFunction<() => Promise<void>>,
  isEnabled: jest.fn() as jest.MockedFunction<() => Promise<boolean>>,
  requestTechnology: jest.fn() as jest.MockedFunction<(tech: any) => Promise<void>>,
  cancelTechnologyRequest: jest.fn() as jest.MockedFunction<() => Promise<void>>,
  getTag: jest.fn() as jest.MockedFunction<() => Promise<any>>,
  mifareClassicAuthenticateA: jest.fn() as jest.MockedFunction<(sector: number, key: Uint8Array) => Promise<boolean>>,
  mifareClassicReadBlock: jest.fn() as jest.MockedFunction<(blockIndex: number) => Promise<Uint8Array>>,
  ndefHandler: {
    getNdefMessage: jest.fn() as jest.MockedFunction<() => Promise<any[]>>,
  },
};

// Export as default
const NfcManager = mockNfcLib;
export default NfcManager;

// Export NfcTech enum mock
export enum NfcTech {
  Ndef = 'Ndef',
  NfcA = 'NfcA',
  NfcB = 'NfcB',
  NfcF = 'NfcF',
  NfcV = 'NfcV',
  IsoDep = 'IsoDep',
  MifareClassic = 'MifareClassic',
  MifareUltralight = 'MifareUltralight',
}