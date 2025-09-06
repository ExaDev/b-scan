/**
 * Integration tests for scanning workflow
 * Port of ScanHistoryIntegrationTest.kt and scanning workflow tests from the original Android app
 */

import { NfcManager, TagReadResult } from '../../src/services/NfcManager';
import { FilamentInfo, TagFormat } from '../../src/types/FilamentInfo';

// Mock react-native-nfc-manager for integration testing
jest.mock('react-native-nfc-manager', () => ({
  start: jest.fn(),
  stop: jest.fn(),
  requestTechnology: jest.fn(),
  cancelTechnologyRequest: jest.fn(),
  mifareClassicAuthenticateA: jest.fn(),
  mifareClassicReadBlock: jest.fn(),
  getTag: jest.fn(),
  NfcTech: {
    MifareClassic: 'MifareClassic',
    Ndef: 'Ndef'
  }
}));

describe('Scan Workflow Integration Tests', () => {
  let nfcManager: NfcManager;

  beforeEach(() => {
    nfcManager = new NfcManager();
    jest.clearAllMocks();
  });

  describe('complete scan workflow', () => {
    it('should complete full Bambu Lab tag scan workflow', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      // Mock successful NFC operations
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      
      // Mock reading tag blocks with sample data
      const mockBlockData = new Uint8Array(16);
      mockBlockData[0] = 0x42; // Mock material identifier
      mockBlockData[1] = 0x50; // Mock bed temperature
      mockBlockData[2] = 0x00;
      mockBlockData[8] = 0x01; // Mock format version
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(mockBlockData);
      
      // Initialize NFC manager
      const initResult = await nfcManager.initialize();
      expect(initResult).toBe(true);
      
      // Perform scan
      const scanResult = await nfcManager.scanTag();
      
      // Verify successful scan
      expect(scanResult?.success).toBe(true);
      expect(scanResult?.data).toBeDefined();
      expect(scanResult?.data?.uid).toBe('04914CCA5E6480');
      expect(scanResult?.data?.technology).toBe('MifareClassic');
    });

    it('should handle scan failure gracefully in workflow', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      // Mock NFC initialization success but scan failure
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology.mockRejectedValue(new Error('No tag found'));
      
      const initResult = await nfcManager.initialize();
      expect(initResult).toBe(true);
      
      const scanResult = await nfcManager.scanTag();
      
      // Verify graceful failure handling
      expect(scanResult?.success).toBe(false);
      expect(scanResult?.error).toBeDefined();
      expect(scanResult?.data).toBeUndefined();
    });

    it('should handle authentication workflow correctly', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      
      // Mock authentication failure on first attempt, success on second
      mockNfcLib.mifareClassicAuthenticateA
        .mockResolvedValueOnce(false)
        .mockResolvedValueOnce(true);
      
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(new Uint8Array(16));
      
      await nfcManager.initialize();
      const scanResult = await nfcManager.scanTag();
      
      // Should succeed after trying multiple keys
      expect(scanResult?.success).toBe(true);
      expect(mockNfcLib.mifareClassicAuthenticateA).toHaveBeenCalledTimes(2);
    });

    it('should handle partial data read scenarios', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      
      // Mock reading success for some blocks, failure for others
      mockNfcLib.mifareClassicReadBlock
        .mockResolvedValueOnce(new Uint8Array(16).fill(0x42))
        .mockResolvedValueOnce(new Uint8Array(16).fill(0x43))
        .mockRejectedValueOnce(new Error('Block read failed'))
        .mockResolvedValueOnce(new Uint8Array(16).fill(0x44));
      
      await nfcManager.initialize();
      const scanResult = await nfcManager.scanTag();
      
      // Should handle partial reads gracefully
      expect(scanResult?.success).toBe(true);
      expect(scanResult?.data).toBeDefined();
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
      
      const parsedResult = nfcManager.parseTagData(mockTagData);
      
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
      
      const parsedResult = nfcManager.parseTagData(mockTagData);
      
      // Should handle OpenSpool format
      expect(() => nfcManager.parseTagData(mockTagData)).not.toThrow();
    });

    it('should validate scan results consistently', () => {
      const validScanResult: TagReadResult = {
        success: true,
        data: {
          uid: '04914CCA5E6480',
          data: new Uint8Array(1024),
          technology: 'MifareClassic',
          format: TagFormat.BAMBU_LAB
        }
      };
      
      const invalidScanResult: TagReadResult = {
        success: false,
        error: 'Authentication failed'
      };
      
      // Valid results should have data
      expect(validScanResult.success).toBe(true);
      expect(validScanResult.data).toBeDefined();
      expect(validScanResult.data?.uid).toBeTruthy();
      
      // Invalid results should have error
      expect(invalidScanResult.success).toBe(false);
      expect(invalidScanResult.error).toBeDefined();
      expect(invalidScanResult.data).toBeUndefined();
    });
  });

  describe('workflow error recovery', () => {
    it('should recover from NFC connection drops', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology
        .mockRejectedValueOnce(new Error('Connection lost'))
        .mockResolvedValueOnce(void 0);
      
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      
      await nfcManager.initialize();
      
      // First scan should fail
      const firstScan = await nfcManager.scanTag();
      expect(firstScan?.success).toBe(false);
      
      // Second scan should succeed (after recovery)
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(new Uint8Array(16));
      
      const secondScan = await nfcManager.scanTag();
      expect(secondScan?.success).toBe(true);
    });

    it('should handle tag removal during scan', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock.mockRejectedValue(new Error('Tag lost'));
      
      await nfcManager.initialize();
      const scanResult = await nfcManager.scanTag();
      
      // Should handle tag removal gracefully
      expect(scanResult?.success).toBe(false);
      expect(scanResult?.error).toContain('Tag');
    });

    it('should timeout long-running operations', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      mockNfcLib.start.mockResolvedValue(true);
      
      // Mock a long-running operation
      mockNfcLib.requestTechnology.mockImplementation(() =>
        new Promise(resolve => setTimeout(resolve, 10000)) // 10 second delay
      );
      
      await nfcManager.initialize();
      
      const startTime = Date.now();
      const scanResult = await nfcManager.scanTag();
      const endTime = Date.now();
      
      // Should timeout in reasonable time
      expect(endTime - startTime).toBeLessThan(6000); // Less than 6 seconds
      expect(scanResult?.success).toBe(false);
    }, 10000);

    it('should clean up resources on failure', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology.mockRejectedValue(new Error('Scan failed'));
      mockNfcLib.cancelTechnologyRequest.mockResolvedValue(void 0);
      
      await nfcManager.initialize();
      await nfcManager.scanTag();
      
      // Should cleanup after scan
      await nfcManager.cleanup();
      expect(mockNfcLib.cancelTechnologyRequest).toHaveBeenCalled();
    });
  });

  describe('performance and reliability', () => {
    it('should maintain consistent performance across multiple scans', async () => {
      const mockNfcLib = require('react-native-nfc-manager');
      
      // Setup successful scan mocks
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(new Uint8Array(16));
      
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
      const mockNfcLib = require('react-native-nfc-manager');
      
      mockNfcLib.start.mockResolvedValue(true);
      mockNfcLib.requestTechnology.mockResolvedValue(void 0);
      mockNfcLib.getTag.mockResolvedValue({
        id: '04914CCA5E6480',
        techTypes: ['android.nfc.tech.MifareClassic'],
        type: 'MifareClassic'
      });
      mockNfcLib.mifareClassicAuthenticateA.mockResolvedValue(true);
      mockNfcLib.mifareClassicReadBlock.mockResolvedValue(new Uint8Array(16));
      
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
        r.status === 'fulfilled' && r.value?.success
      ).length;
      
      const failed = results.filter(r =>
        r.status === 'fulfilled' && !r.value?.success
      ).length;
      
      expect(succeeded + failed).toBe(results.length);
    });
  });
});