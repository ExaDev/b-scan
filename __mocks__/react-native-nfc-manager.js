// Manual mock for react-native-nfc-manager
const mockNfcManager = {
  start: jest.fn(),
  stop: jest.fn(),
  isSupported: jest.fn(() => Promise.resolve(true)),
  isEnabled: jest.fn(() => Promise.resolve(true)),
  requestTechnology: jest.fn(() => Promise.resolve()),
  cancelTechnologyRequest: jest.fn(() => Promise.resolve()),
  mifareClassicAuthenticateA: jest.fn(() => Promise.resolve(true)),
  mifareClassicReadBlock: jest.fn(() => Promise.resolve(new Uint8Array(16).fill(0x42))),
  getTag: jest.fn(),
  ndefHandler: {
    getNdefMessage: jest.fn(() => Promise.resolve([]))
  },
  unregisterTagEvent: jest.fn()
};

const NfcTech = {
  MifareClassic: 'MifareClassic',
  Ndef: 'Ndef'
};

module.exports = mockNfcManager;
module.exports.default = mockNfcManager;
module.exports.NfcTech = NfcTech;