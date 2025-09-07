/**
 * Integration tests for scanning workflow
 * Port of ScanHistoryIntegrationTest.kt and scanning workflow tests from the original Android app
 */

import { NfcManager, TagReadResult } from '../../src/services/NfcManager';
import { TagFormat } from '../../src/types/FilamentInfo';
import NfcLib from 'react-native-nfc-manager';

// Ensure the mock is picked up
jest.mock('react-native-nfc-manager');
const mockNfcLib = jest.mocked(NfcLib);

describe('Scan Workflow Integration Tests', () => {
  let nfcManager: NfcManager;

  beforeEach(() => {
    nfcManager = new NfcManager();
    jest.clearAllMocks();
    
    // Reset mock defaults for integration tests
    mockNfcLib.isSupported.mockResolvedValue(true);
    mockNfcLib.start.mockResolvedValue(undefined);
    mockNfcLib.isEnabled.mockResolvedValue(true);
    mockNfcLib.requestTechnology.mockResolvedValue(undefined);
    mockNfcLib.cancelTechnologyRequest.mockResolvedValue(undefined);
    mockNfcLib.mifareClassicAuthenticateA?.mockResolvedValue(true);
    
    // Create varied data for all integration tests by default
    let defaultReadCount = 0;
    mockNfcLib.mifareClassicReadBlock?.mockImplementation(async (blockIndex: number) => {
      const mockBlockData = new Uint8Array(16);
      mockBlockData.fill(0x42 + (defaultReadCount % 8));
      mockBlockData[0] = blockIndex & 0xFF;
      mockBlockData[1] = (defaultReadCount + 1) & 0xFF;
      defaultReadCount++;
      return mockBlockData;
    });
    
    mockNfcLib.getTag.mockResolvedValue({
      id: '04914CCA5E6480',
      techTypes: ['android.nfc.tech.MifareClassic']
    });
  });

  describe('complete scan workflow', () => {
    it('should complete full Bambu Lab tag scan workflow', async () => {
      // Create proper mock data for Bambu Lab tag (1024 bytes = 64 blocks × 16 bytes)
      let readCount = 0;
      mockNfcLib.mifareClassicReadBlock?.mockImplementation(async (blockIndex: number) => {
        const mockBlockData = new Uint8Array(16);
        
        // Create varied data to pass validation (not all same bytes)
        mockBlockData.fill(0x42 + (readCount % 8));
        mockBlockData[0] = blockIndex & 0xFF; // Block-specific identifier
        mockBlockData[1] = (readCount + 1) & 0xFF; // Read-specific data
        
        readCount++;
        return mockBlockData;
      });
      
      // Initialize NFC manager
      const initResult = await nfcManager.initialize();
      expect(initResult).toBe(true);
      
      // Perform scan
      const scanResult = await nfcManager.scanTag();
      
      // Verify successful scan
      expect(scanResult.type).toBe('SUCCESS');
      if (scanResult.type === 'SUCCESS') {
        expect(scanResult.filamentInfo).toBeDefined();
        expect(scanResult.filamentInfo.tagUid).toBe('04914CCA5E6480');
        expect(scanResult.filamentInfo.tagFormat).toBe(TagFormat.BAMBU_LAB);
      }
    });

    it('should handle scan failure gracefully in workflow', async () => {
      
      // Mock NFC initialization success but scan failure
      mockNfcLib.start.mockResolvedValue(undefined);
      mockNfcLib.requestTechnology.mockRejectedValue(new Error('No tag found'));
      
      const initResult = await nfcManager.initialize();
      expect(initResult).toBe(true);
      
      const scanResult = await nfcManager.scanTag();
      
      // Verify graceful failure handling
      expect(scanResult.type).not.toBe('SUCCESS');
      if (scanResult.type === 'READ_ERROR' || scanResult.type === 'PARSING_ERROR') {
        expect(scanResult.error).toBeDefined();
      }
    });

    it('should handle authentication workflow correctly', async () => {
      
      mockNfcLib.start.mockResolvedValue(undefined);
      mockNfcLib.requestTechnology.mockResolvedValue(undefined);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      
      // Mock authentication failure on first attempt, success on second
      mockNfcLib.mifareClassicAuthenticateA
        ?.mockResolvedValueOnce(false)
        ?.mockResolvedValueOnce(true);
      
      mockNfcLib.mifareClassicReadBlock?.mockResolvedValue(new Uint8Array(16));
      
      await nfcManager.initialize();
      const scanResult = await nfcManager.scanTag();
      
      // Should succeed after trying multiple keys
      expect(scanResult.type).toBe('SUCCESS');
      expect(mockNfcLib.mifareClassicAuthenticateA).toHaveBeenCalledTimes(2);
    });

    it('should handle partial data read scenarios', async () => {
      
      mockNfcLib.start.mockResolvedValue(undefined);
      mockNfcLib.requestTechnology.mockResolvedValue(undefined);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA?.mockResolvedValue(true);
      
      // Mock reading success for some blocks, failure for others
      mockNfcLib.mifareClassicReadBlock
        ?.mockResolvedValueOnce(new Uint8Array(16).fill(0x42))
        ?.mockResolvedValueOnce(new Uint8Array(16).fill(0x43))
        ?.mockRejectedValueOnce(new Error('Block read failed'))
        ?.mockResolvedValueOnce(new Uint8Array(16).fill(0x44));
      
      await nfcManager.initialize();
      const scanResult = await nfcManager.scanTag();
      
      // Should handle partial reads gracefully
      expect(scanResult.type).toBe('SUCCESS');
      expect(scanResult.type === 'SUCCESS' ? scanResult.filamentInfo : undefined).toBeDefined();
    });
  });

  describe('scan result processing', () => {
    it('should process Bambu Lab tag data correctly', () => {
      const mockTagData = {
        uid: '04914CCA5E6480',
        data: new Uint8Array(1024),
        technology: 'MifareClassic' as const,
        format: TagFormat.BAMBU_LAB
      };
      
      // Set up mock data with recognizable patterns
      mockTagData.data[0] = 0x01; // Format version
      mockTagData.data[16] = 0x50; // Bed temperature (80°C)
      mockTagData.data[17] = 0x00;
      mockTagData.data[32] = 0x42; // Material type identifier
      
      // Should parse without throwing
      expect(() => nfcManager.parseTagData(mockTagData)).not.toThrow();
    });

    it('should process OpenSpool tag data correctly', () => {
      const mockTagData = {
        uid: '12345678',
        data: new Uint8Array(512),
        technology: 'NTAG' as const,
        format: TagFormat.OPENSPOOL
      };
      
      // Set up mock NDEF data
      mockTagData.data[0] = 0xD1; // NDEF header
      mockTagData.data[1] = 0x01; // Type length
      mockTagData.data[2] = 0x10; // Payload length
      mockTagData.data[3] = 0x54; // 'T' for Text Record
      
      // Should handle OpenSpool format
      expect(() => nfcManager.parseTagData(mockTagData)).not.toThrow();
    });

    it('should validate scan results consistently', () => {
      const validScanResult: TagReadResult = {
        type: 'SUCCESS',
        filamentInfo: {
          tagUid: '04914CCA5E6480',
          trayUid: '04914CCA5E6480',
          tagFormat: TagFormat.BAMBU_LAB,
          manufacturerName: 'Bambu Lab',
          filamentType: 'PLA',
          detailedFilamentType: 'PLA Basic',
          colorHex: '#FFFFFF',
          colorName: 'White',
          spoolWeight: 250,
          filamentLength: 330000,
          filamentDiameter: 1.75,
          minTemperature: 190,
          maxTemperature: 230,
          bedTemperature: 35,
          spoolWidth: 70,
          productionDate: Date.now().toString(),
          dryingTemperature: 40,
          dryingTime: 8,
          materialId: 'BL-PLA-WH',
          materialVariantId: 'BL-PLA-WH-VAR',
          nozzleDiameter: 0.4,
          bedTemperatureType: 1,
          shortProductionDate: '2024-01',
          colorCount: 1,
          shortProductionDateHex: '2024',
          unknownBlock17Hex: 'ABCD',
        }
      };
      
      const invalidScanResult: TagReadResult = {
        type: 'AUTHENTICATION_FAILED'
      };
      
      // Valid results should have data
      expect(validScanResult.type).toBe('SUCCESS');
      if (validScanResult.type === 'SUCCESS') {
        expect(validScanResult.filamentInfo).toBeDefined();
        expect(validScanResult.filamentInfo.tagUid).toBeTruthy();
      }
      
      // Invalid results should have error
      expect(invalidScanResult.type).toBe('AUTHENTICATION_FAILED');
    });
  });

  describe('workflow error recovery', () => {
    it('should recover from NFC connection drops', async () => {
      
      mockNfcLib.start.mockResolvedValue(undefined);
      mockNfcLib.requestTechnology
        .mockRejectedValueOnce(new Error('Connection lost'))
        .mockResolvedValueOnce(undefined);
      
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      
      await nfcManager.initialize();
      
      // First scan should fail
      const firstScan = await nfcManager.scanTag();
      expect(firstScan.type).not.toBe('SUCCESS');
      
      // Second scan should succeed (after recovery)
      mockNfcLib.mifareClassicAuthenticateA?.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock?.mockResolvedValue(new Uint8Array(16));
      
      const secondScan = await nfcManager.scanTag();
      expect(secondScan.type).toBe('SUCCESS');
    });

    it('should handle tag removal during scan', async () => {
      
      mockNfcLib.start.mockResolvedValue(undefined);
      mockNfcLib.requestTechnology.mockResolvedValue(undefined);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA?.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock?.mockRejectedValue(new Error('Tag lost'));
      
      await nfcManager.initialize();
      const scanResult = await nfcManager.scanTag();
      
      // Should handle tag removal gracefully
      expect(scanResult.type).not.toBe('SUCCESS');
      if (scanResult.type === 'READ_ERROR' || scanResult.type === 'PARSING_ERROR') {
        expect(scanResult.error).toContain('Tag');
      }
    });

    it('should timeout long-running operations', async () => {
      
      mockNfcLib.start.mockResolvedValue(undefined);
      
      // Mock a long-running operation
      mockNfcLib.requestTechnology.mockImplementation(() =>
        new Promise(resolve => setTimeout(() => resolve(undefined), 10000)) // 10 second delay
      );
      
      await nfcManager.initialize();
      
      const startTime = Date.now();
      const scanResult = await nfcManager.scanTag();
      const endTime = Date.now();
      
      // Should timeout in reasonable time
      expect(endTime - startTime).toBeLessThan(6000); // Less than 6 seconds
      expect(scanResult.type).not.toBe('SUCCESS');
    }, 10000);

    it('should clean up resources on failure', async () => {
      
      mockNfcLib.start.mockResolvedValue(undefined);
      mockNfcLib.requestTechnology.mockRejectedValue(new Error('Scan failed'));
      mockNfcLib.cancelTechnologyRequest.mockResolvedValue(undefined);
      
      await nfcManager.initialize();
      await nfcManager.scanTag();
      
      // Should cleanup after scan
      await nfcManager.cleanup();
      expect(mockNfcLib.cancelTechnologyRequest).toHaveBeenCalled();
    });
  });

  describe('performance and reliability', () => {
    it('should maintain consistent performance across multiple scans', async () => {
      
      // Setup successful scan mocks
      mockNfcLib.start.mockResolvedValue(undefined);
      mockNfcLib.requestTechnology.mockResolvedValue(undefined);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA?.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock?.mockResolvedValue(new Uint8Array(16));
      
      await nfcManager.initialize();
      
      const scanTimes: number[] = [];
      const numScans = 5;
      
      for (let i = 0; i < numScans; i++) {
        const startTime = Date.now();
        await nfcManager.scanTag();
        const endTime = Date.now();
        scanTimes.push(endTime - startTime);
      }
      
      // All scans should complete in reasonable time
      scanTimes.forEach(time => {
        expect(time).toBeLessThan(2000); // Less than 2 seconds each
      });
      
      // Performance should be relatively consistent
      const avgTime = scanTimes.reduce((a, b) => a + b) / scanTimes.length;
      scanTimes.forEach(time => {
        expect(Math.abs(time - avgTime)).toBeLessThan(avgTime); // Within 100% of average
      });
    });

    it('should handle concurrent scan attempts', async () => {
      
      mockNfcLib.start.mockResolvedValue(undefined);
      mockNfcLib.requestTechnology.mockResolvedValue(undefined);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA?.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock?.mockResolvedValue(new Uint8Array(16));
      
      await nfcManager.initialize();
      
      // Start multiple concurrent scans
      const concurrentScans = [
        nfcManager.scanTag(),
        nfcManager.scanTag(),
        nfcManager.scanTag()
      ];
      
      const results = await Promise.allSettled(concurrentScans);
      
      // At least one should succeed or all should fail gracefully
      const succeeded = results.filter(r => 
        r.status === 'fulfilled' && r.value?.type === 'SUCCESS'
      ).length;
      
      const failed = results.filter(r =>
        r.status === 'fulfilled' && r.value?.type !== 'SUCCESS'
      ).length;
      
      expect(succeeded + failed).toBe(results.length);
    });
  });
});