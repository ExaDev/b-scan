/**
 * Mock for react-native-nfc-manager
 * Provides type-safe mocks for NFC operations in testing
 */

// NFC Tech enum
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

// NFC Tag interface for mocks
interface MockNfcTag {
  id: number[] | string;
  techTypes: string[];
  [key: string]: unknown;
}

// NDEF Message interface
interface MockNdefMessage {
  records: Array<{
    tnf: number;
    type: Uint8Array;
    id: Uint8Array;
    payload: Uint8Array;
  }>;
}

export const mockNfcLib = {
  isSupported: jest.fn() as jest.MockedFunction<() => Promise<boolean>>,
  start: jest.fn() as jest.MockedFunction<() => Promise<void>>,
  stop: jest.fn() as jest.MockedFunction<() => Promise<void>>,
  isEnabled: jest.fn() as jest.MockedFunction<() => Promise<boolean>>,
  requestTechnology: jest.fn() as jest.MockedFunction<(tech: NfcTech) => Promise<void>>,
  cancelTechnologyRequest: jest.fn() as jest.MockedFunction<() => Promise<void>>,
  getTag: jest.fn() as jest.MockedFunction<() => Promise<MockNfcTag | null>>,
  mifareClassicAuthenticateA: jest.fn() as jest.MockedFunction<(sector: number, key: Uint8Array) => Promise<boolean>>,
  mifareClassicReadBlock: jest.fn() as jest.MockedFunction<(blockIndex: number) => Promise<Uint8Array>>,
  ndefHandler: {
    getNdefMessage: jest.fn() as jest.MockedFunction<() => Promise<MockNdefMessage | null>>,
  },
};

// Export as default
const NfcManager = mockNfcLib;
export default NfcManager;

// Export types for external use
export type { MockNfcTag, MockNdefMessage };