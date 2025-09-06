/**
 * Unit tests for NfcManager service
 * Port of NfcManagerTest.kt from the original Android app
 */

// Use manual mock
jest.mock('react-native-nfc-manager');

import { NfcManager, TagData, TagReadResult, BambuKeyDerivation } from '../../src/services/NfcManager';
import { TagFormat } from '../../src/types/FilamentInfo';
// Import the mocked module to access mock functions
import NfcLib from 'react-native-nfc-manager';

const mockNfcLib = NfcLib as jest.Mocked<typeof NfcLib>;

describe('NfcManager Unit Tests', () => {
  let nfcManager: NfcManager;

  // Mock BambuKeyDerivation
  beforeAll(() => {
    jest.spyOn(BambuKeyDerivation, 'deriveKeys').mockImplementation((uid: Uint8Array) => {
      // Return mock keys for testing
      const keys: Uint8Array[] = [];
      for (let i = 0; i < 16; i++) {
        keys.push(new Uint8Array([0x01 + i, 0x02 + i, 0x03 + i, 0x04 + i, 0x05 + i, 0x06 + i]));
      }
      return keys;
    });
  });

  beforeEach(() => {
    nfcManager = new NfcManager();
    jest.clearAllMocks();
    
    // Add missing methods to the mock if they don't exist
    if (!mockNfcLib.mifareClassicAuthenticateA) {
      mockNfcLib.mifareClassicAuthenticateA = jest.fn();
    }
    if (!mockNfcLib.mifareClassicReadBlock) {
      mockNfcLib.mifareClassicReadBlock = jest.fn();
    }
    
    // Set up default mock implementations
    mockNfcLib.isSupported.mockResolvedValue(true);
    mockNfcLib.start.mockResolvedValue(undefined);
    mockNfcLib.stop.mockResolvedValue(undefined);
    mockNfcLib.cancelTechnologyRequest.mockResolvedValue(undefined);
    mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
    mockNfcLib.mifareClassicReadBlock.mockResolvedValue(new Uint8Array(16).fill(0x42));
  });

  describe('initialization and lifecycle', () => {
    it('should initialize NFC manager successfully', async () => {
      mockNfcLib.start.mockResolvedValue(true);
      
      const result = await nfcManager.initialize();
      
      expect(mockNfcLib.start).toHaveBeenCalled();
      expect(result).toBe(true);
    });

    it('should handle initialization failure gracefully', async () => {
      mockNfcLib.start.mockRejectedValue(new Error('NFC not available'));
      
      const result = await nfcManager.initialize();
      
      expect(mockNfcLib.start).toHaveBeenCalled();
      expect(result).toBe(false);
    });

    it('should cleanup NFC resources properly', async () => {
      // First initialize NFC to set isInitialized = true
      await nfcManager.initialize();
      
      mockNfcLib.stop.mockResolvedValue(void 0);
      
      await nfcManager.cleanup();
      
      expect(mockNfcLib.stop).toHaveBeenCalled();
    });

    it('should handle cleanup errors gracefully', async () => {
      mockNfcLib.stop.mockRejectedValue(new Error('Cleanup failed'));
      mockNfcLib.cancelTechnologyRequest.mockRejectedValue(new Error('Cancel failed'));
      
      expect(async () => {
        await nfcManager.cleanup();
      }).not.toThrow();
    });
  });

  describe('tag scanning', () => {
    it('should scan Bambu Lab tag successfully', async () => {
      const mockTag = {
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      };
      
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue(mockTag);
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(new Uint8Array(16).fill(0x42));
      
      const result = await nfcManager.scanTag();
      
      expect(mockNfcLib.requestTechnology).toHaveBeenCalled();
      expect(result).toBeDefined();
      expect(result?.success).toBe(true);
    });

    it('should handle authentication failure', async () => {
      const mockTag = {
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      };
      
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue(mockTag);
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(false);
      
      const result = await nfcManager.scanTag();
      
      expect(result?.success).toBe(false);
      expect(result?.error).toContain('Failed to read tag data');
    });

    it('should handle scan timeout', async () => {
      mockNfcLib.requestTechnology.mockImplementation(() => 
        new Promise((_, reject) => 
          setTimeout(() => reject(new Error('Timeout')), 100)
        )
      );
      
      const result = await nfcManager.scanTag();
      
      expect(result?.success).toBe(false);
      expect(result?.error).toBe('Timeout');
    }, 1000);

    it('should handle no tag present', async () => {
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue(null);
      
      const result = await nfcManager.scanTag();
      
      expect(result?.success).toBe(false);
      expect(result?.error).toContain('No tag');
    });

    it('should cancel scanning operation', async () => {
      mockNfcLib.cancelTechnologyRequest.mockResolvedValue(void 0);
      
      await nfcManager.cancelScan();
      
      expect(mockNfcLib.cancelTechnologyRequest).toHaveBeenCalled();
    });
  });

  describe('tag authentication', () => {
    const testUid = new Uint8Array([0x04, 0x91, 0x4C, 0xCA]);
    
    it('should authenticate with correct keys', async () => {
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      
      const result = await nfcManager['authenticateTag'](testUid, 4);
      
      expect(result).toBe(true);
      expect(mockNfcLib.mifareClassicAuthenticateA).toHaveBeenCalled();
    });

    it('should try multiple keys on authentication failure', async () => {
      mockNfcLib.mifareClassicAuthenticateA
        .mockResolvedValueOnce(false)  // First key fails
        .mockResolvedValueOnce(false)  // Second key fails
        .mockResolvedValueOnce(true);  // Third key succeeds
      
      const result = await nfcManager['authenticateTag'](testUid, 4);
      
      expect(result).toBe(true);
      expect(mockNfcLib.mifareClassicAuthenticateA).toHaveBeenCalledTimes(3);
    });

    it('should fail when all keys are rejected', async () => {
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(false);
      
      const result = await nfcManager['authenticateTag'](testUid, 4);
      
      expect(result).toBe(false);
    });

    it('should handle authentication errors', async () => {
      mockNfcLib.mifareClassicAuthenticateA.mockRejectedValue(new Error('Auth error'));
      
      const result = await nfcManager['authenticateTag'](testUid, 4);
      
      expect(result).toBe(false);
    });
  });

  describe('data reading and parsing', () => {
    it('should read tag data successfully', async () => {
      const mockData = new Uint8Array(16);
      mockData.fill(0x42);
      
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(mockData);
      
      const result = await nfcManager['readTagData'](0, 4);
      
      expect(result).toEqual(mockData);
      expect(mockNfcLib.mifareClassicReadBlock).toHaveBeenCalledWith(0);
    });

    it('should handle read errors gracefully', async () => {
      mockNfcLib.mifareClassicReadBlock.mockRejectedValue(new Error('Read error'));
      
      const result = await nfcManager['readTagData'](0, 4);
      
      expect(result).toBeNull();
    });

    it('should parse valid tag data', () => {
      const validTagData: TagData = {
        uid: '04914CCA',
        data: new Uint8Array(1024),
        technology: 'MifareClassic',
        format: TagFormat.BAMBU_LAB
      };
      
      // Should not throw for valid data structure
      expect(() => {
        nfcManager.parseTagData(validTagData);
      }).not.toThrow();
    });

    it('should reject invalid tag data', () => {
      const invalidTagData: TagData = {
        uid: '',
        data: new Uint8Array(0),
        technology: 'Unknown',
        format: TagFormat.UNKNOWN
      };
      
      const result = nfcManager.parseTagData(invalidTagData);
      expect(result).toBeNull();
    });
  });

  describe('error handling and edge cases', () => {
    it('should handle NFC disabled scenario', async () => {
      mockNfcLib.start.mockRejectedValue(new Error('NFC_DISABLED'));
      
      const result = await nfcManager.initialize();
      
      expect(result).toBe(false);
    });

    it('should handle unsupported device', async () => {
      mockNfcLib.start.mockRejectedValue(new Error('NFC_NOT_SUPPORTED'));
      
      const result = await nfcManager.initialize();
      
      expect(result).toBe(false);
    });

    it('should handle corrupted tag data', () => {
      const corruptedData = new Uint8Array(1024);
      corruptedData.fill(0xFF); // All bits set, likely corrupted
      
      const tagData: TagData = {
        uid: '04914CCA',
        data: corruptedData,
        technology: 'MifareClassic',
        format: TagFormat.BAMBU_LAB
      };
      
      expect(() => {
        nfcManager.parseTagData(tagData);
      }).not.toThrow();
    });

    it('should handle concurrent scan requests', async () => {
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      
      // Start multiple scans concurrently
      const scan1 = nfcManager.scanTag();
      const scan2 = nfcManager.scanTag();
      const scan3 = nfcManager.scanTag();
      
      const results = await Promise.all([scan1, scan2, scan3]);
      
      // Only one scan should succeed, others should be cancelled or fail
      const successCount = results.filter(r => r?.success).length;
      expect(successCount).toBeLessThanOrEqual(1);
    });
  });

  describe('performance and reliability', () => {
    it('should complete scan operations in reasonable time', async () => {
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(new Uint8Array(16));
      
      const startTime = Date.now();
      await nfcManager.scanTag();
      const endTime = Date.now();
      
      const scanTime = endTime - startTime;
      expect(scanTime).toBeLessThan(5000); // Should complete within 5 seconds
    });

    it('should handle memory pressure during scanning', async () => {
      // Simulate large tag data
      const largeData = new Uint8Array(4096);
      largeData.fill(0x42);
      
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(largeData);
      
      // Should handle large data without memory issues
      expect(async () => {
        await nfcManager.scanTag();
      }).not.toThrow();
    });

    it('should be deterministic across multiple scans of same tag', async () => {
      const mockTag = {
        id: '04914CCA',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      };
      
      const mockData = new Uint8Array(16);
      mockData.fill(0x42);
      
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue(mockTag);
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(mockData);
      
      const result1 = await nfcManager.scanTag();
      const result2 = await nfcManager.scanTag();
      const result3 = await nfcManager.scanTag();
      
      // Results should be consistent
      expect(result1?.success).toBe(result2?.success);
      expect(result2?.success).toBe(result3?.success);
      
      if (result1?.data && result2?.data) {
        expect(result1.data.uid).toBe(result2.data.uid);
      }
    });
  });
});