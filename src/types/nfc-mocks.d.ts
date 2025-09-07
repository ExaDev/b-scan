/**
 * TypeScript declarations for react-native-nfc-manager mock types
 */

declare module 'react-native-nfc-manager' {
  export interface NdefRecord {
    tnf: number;
    type: Uint8Array;
    payload: Uint8Array;
  }

  export interface TagEvent {
    techTypes: string[];
    type: string;
    ndefMessage?: NdefRecord[];
    [key: string]: unknown;
  }

  export const NfcTech: {
    MifareClassic: 'MifareClassic';
    Ndef: 'Ndef';
    NfcA: 'NfcA';
    NfcB: 'NfcB';
    NfcF: 'NfcF';
    NfcV: 'NfcV';
    IsoDep: 'IsoDep';
    Iso15693: 'Iso15693';
    MifareUltralight: 'MifareUltralight';
  };

  export interface NfcManagerInterface {
    isSupported(): Promise<boolean>;
    start(): Promise<void>;
    stop(): Promise<void>;
    isEnabled(): Promise<boolean>;
    requestTechnology(techs: string | string[]): Promise<void>;
    cancelTechnologyRequest(): Promise<void>;
    getTag(): Promise<TagEvent | null>;
    mifareClassicAuthenticateA?(sector: number, key: Uint8Array): Promise<boolean>;
    mifareClassicReadBlock?(blockIndex: number): Promise<Uint8Array>;
    ndefHandler?: {
      getNdefMessage(): Promise<NdefRecord[]>;
    };
  }

  const manager: NfcManagerInterface;
  export default manager;

  export function isSupported(): Promise<boolean>;
  export function start(): Promise<void>;
  export function stop(): Promise<void>;
  export function isEnabled(): Promise<boolean>;
  export function requestTechnology(techs: string | string[]): Promise<void>;
  export function cancelTechnologyRequest(): Promise<void>;
  export function getTag(): Promise<TagEvent | null>;
  export function mifareClassicAuthenticateA(sector: number, key: Uint8Array): Promise<boolean>;
  export function mifareClassicReadBlock(blockIndex: number): Promise<Uint8Array>;
}