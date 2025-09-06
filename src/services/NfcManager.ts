import NfcManager, { NfcTech, Ndef } from 'react-native-nfc-manager';
import { 
  FilamentInfo, 
  TagFormat, 
  TagReadResult, 
  ScanProgress, 
  ScanStage 
} from '../types/FilamentInfo';

export class NfcManagerService {
  private static instance: NfcManagerService;
  private isInitialized = false;
  private scanProgressCallback?: (progress: ScanProgress) => void;

  private constructor() {}

  static getInstance(): NfcManagerService {
    if (!NfcManagerService.instance) {
      NfcManagerService.instance = new NfcManagerService();
    }
    return NfcManagerService.instance;
  }

  async initialize(): Promise<boolean> {
    try {
      const supported = await NfcManager.isSupported();
      if (!supported) {
        return false;
      }

      await NfcManager.start();
      this.isInitialized = true;
      return true;
    } catch (error) {
      console.error('Failed to initialize NFC:', error);
      return false;
    }
  }

  async isNfcEnabled(): Promise<boolean> {
    try {
      return await NfcManager.isEnabled();
    } catch (error) {
      console.error('Failed to check NFC status:', error);
      return false;
    }
  }

  setScanProgressCallback(callback: (progress: ScanProgress) => void): void {
    this.scanProgressCallback = callback;
  }

  private updateScanProgress(stage: ScanStage, percentage: number, currentSector: number = 0, statusMessage: string = ''): void {
    if (this.scanProgressCallback) {
      this.scanProgressCallback({
        stage,
        percentage,
        currentSector,
        statusMessage
      });
    }
  }

  async scanTag(): Promise<TagReadResult> {
    if (!this.isInitialized) {
      const initialized = await this.initialize();
      if (!initialized) {
        return { type: 'NO_NFC' };
      }
    }

    try {
      this.updateScanProgress(ScanStage.INITIALIZING, 0, 0, 'Initializing NFC scan...');

      // Request NFC technology
      await NfcManager.requestTechnology(NfcTech.MifareClassic);
      
      this.updateScanProgress(ScanStage.AUTHENTICATING, 20, 0, 'Authenticating tag...');

      // Get tag information
      const tag = await NfcManager.getTag();
      
      if (!tag || !tag.id) {
        await NfcManager.cancelTechnologyRequest();
        return { type: 'INVALID_TAG' };
      }

      this.updateScanProgress(ScanStage.READING_DATA, 40, 0, 'Reading tag data...');

      // Read tag data based on technology
      let filamentInfo: FilamentInfo | null = null;
      
      if (tag.techTypes?.includes(NfcTech.MifareClassic)) {
        filamentInfo = await this.readMifareClassicTag(tag);
      } else if (tag.techTypes?.includes(NfcTech.Ndef)) {
        filamentInfo = await this.readNdefTag(tag);
      }

      await NfcManager.cancelTechnologyRequest();

      if (!filamentInfo) {
        return { type: 'PARSING_ERROR', error: 'Failed to parse filament data' };
      }

      this.updateScanProgress(ScanStage.COMPLETED, 100, 0, 'Scan completed successfully');

      return { type: 'SUCCESS', filamentInfo };

    } catch (error) {
      await NfcManager.cancelTechnologyRequest();
      console.error('NFC scan error:', error);
      return { type: 'READ_ERROR', error: error instanceof Error ? error.message : 'Unknown error' };
    }
  }

  private async readMifareClassicTag(tag: any): Promise<FilamentInfo | null> {
    try {
      const tagUid = this.bytesToHex(tag.id);
      
      // Derive authentication keys from UID (Bambu Lab method)
      const keys = this.deriveKeysFromUid(tagUid);
      
      let authenticatedSectors = 0;
      const totalSectors = 16;
      
      // Try to authenticate and read sectors
      for (let sector = 0; sector < totalSectors; sector++) {
        this.updateScanProgress(
          ScanStage.AUTHENTICATING, 
          20 + (sector / totalSectors) * 30, 
          sector, 
          `Authenticating sector ${sector}...`
        );

        try {
          // Try authentication with derived key A
          const authenticated = await this.authenticateWithKeyA(sector, keys.keyA);
          if (authenticated) {
            authenticatedSectors++;
            // Read sector data here
            await this.readSector(sector);
            // Process sector data...
          }
        } catch (error) {
          console.warn(`Failed to authenticate sector ${sector}:`, error);
        }
      }

      if (authenticatedSectors === 0) {
        return null;
      }

      // Parse the data - this is a simplified version
      // In the real app, you'd parse the actual MIFARE data format
      return this.createMockFilamentInfo(tagUid);

    } catch (error) {
      console.error('Error reading MIFARE Classic tag:', error);
      return null;
    }
  }

  private async readNdefTag(tag: any): Promise<FilamentInfo | null> {
    try {
      this.updateScanProgress(ScanStage.READING_DATA, 60, 0, 'Reading NDEF data...');
      
      const ndefRecords = await NfcManager.ndefHandler.getNdefMessage();
      
      if (!ndefRecords || ndefRecords.length === 0) {
        return null;
      }

      // Parse NDEF records - simplified for demo
      const tagUid = this.bytesToHex(tag.id);
      return this.createMockFilamentInfo(tagUid);

    } catch (error) {
      console.error('Error reading NDEF tag:', error);
      return null;
    }
  }

  private authenticateWithKeyA(_sector: number, _key: number[]): Promise<boolean> {
    // This would implement actual MIFARE authentication
    // For now, return a mock result
    return Promise.resolve(Math.random() > 0.1); // 90% success rate for demo
  }

  private readSector(_sector: number): Promise<number[][]> {
    // This would read actual sector data
    // For now, return mock data
    return Promise.resolve([
      Array.from({length: 16}, () => Math.floor(Math.random() * 256)),
      Array.from({length: 16}, () => Math.floor(Math.random() * 256)),
      Array.from({length: 16}, () => Math.floor(Math.random() * 256)),
      Array.from({length: 16}, () => Math.floor(Math.random() * 256))
    ]);
  }

  private deriveKeysFromUid(uid: string): { keyA: number[], keyB: number[] } {
    // Simplified key derivation - in real app this would implement
    // the actual Bambu Lab key derivation algorithm
    const uidBytes = this.hexToBytes(uid);
    const keyA = uidBytes.concat([0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]).slice(0, 6);
    const keyB = uidBytes.concat([0x00, 0x00, 0x00, 0x00, 0x00, 0x00]).slice(0, 6);
    
    return { keyA, keyB };
  }

  private bytesToHex(bytes: number[]): string {
    return bytes.map(byte => byte.toString(16).padStart(2, '0')).join('').toUpperCase();
  }

  private hexToBytes(hex: string): number[] {
    const result = [];
    for (let i = 0; i < hex.length; i += 2) {
      result.push(parseInt(hex.substr(i, 2), 16));
    }
    return result;
  }

  private createMockFilamentInfo(tagUid: string): FilamentInfo {
    // Create mock filament info for demonstration
    return {
      tagUid: tagUid,
      trayUid: `TRAY_${tagUid.slice(-8)}`,
      tagFormat: TagFormat.BAMBU_LAB,
      manufacturerName: 'Bambu Lab',
      filamentType: 'PLA Basic',
      colorHex: '#FF5733',
      colorName: 'Orange',
      spoolWeight: 243,
      filamentDiameter: 1.75,
      filamentLength: 330000,
      productionDate: '2024-01-15',
      minTemperature: 190,
      maxTemperature: 220,
      bedTemperature: 60,
      dryingTemperature: 40,
      dryingTime: 8
    };
  }

  async stopScan(): Promise<void> {
    try {
      await NfcManager.cancelTechnologyRequest();
    } catch (error) {
      console.error('Error stopping NFC scan:', error);
    }
  }

  async cleanup(): Promise<void> {
    try {
      if (this.isInitialized) {
        await NfcManager.cancelTechnologyRequest();
        await NfcManager.unregisterTagEvent();
        this.isInitialized = false;
      }
    } catch (error) {
      console.error('Error cleaning up NFC:', error);
    }
  }
}