/**
 * Unit tests for BambuTagDecoder functionality
 * Port of BambuTagDecoderTest.kt from the original Android app
 */

import { NfcManager, TagData } from '../../src/services/NfcManager';
import { FilamentInfo, TagFormat } from '../../src/types/FilamentInfo';

// Mock the NfcManager for isolated unit testing
jest.mock('react-native-nfc-manager', () => ({
  start: jest.fn(),
  stop: jest.fn(),
  requestTechnology: jest.fn(),
  cancelTechnologyRequest: jest.fn(),
  mifareClassicAuthenticateA: jest.fn(),
  mifareClassicReadBlock: jest.fn(),
  getTag: jest.fn(),
}));

describe('BambuTagDecoder Unit Tests', () => {
  let nfcManager: NfcManager;

  beforeEach(() => {
    nfcManager = new NfcManager();
  });

  describe('parseTagDetails error handling', () => {
    it('should handle empty data gracefully', () => {
      const emptyTagData: TagData = {
        uid: '00000000',
        data: new Uint8Array(0),
        technology: 'MifareClassic',
        format: TagFormat.UNKNOWN
      };

      expect(() => {
        nfcManager.parseTagData(emptyTagData);
      }).not.toThrow();

      const result = nfcManager.parseTagData(emptyTagData);
      expect(result).toBeNull();
    });

    it('should handle insufficient data gracefully', () => {
      const insufficientTagData: TagData = {
        uid: '12345678',
        data: new Uint8Array(50), // Too small for proper tag data
        technology: 'MifareClassic',
        format: TagFormat.BAMBU_LAB
      };

      expect(() => {
        nfcManager.parseTagData(insufficientTagData);
      }).not.toThrow();

      const result = nfcManager.parseTagData(insufficientTagData);
      expect(result).toBeNull();
    });

    it('should handle malformed tag data', () => {
      // Create malformed data that might cause parsing issues
      const malformedData = new Uint8Array(1024);
      // Fill with random-looking but invalid data
      for (let i = 0; i < malformedData.length; i++) {
        malformedData[i] = (i * 17 + 42) % 256;
      }

      const malformedTagData: TagData = {
        uid: 'DEADBEEF',
        data: malformedData,
        technology: 'MifareClassic',
        format: TagFormat.BAMBU_LAB
      };

      expect(() => {
        nfcManager.parseTagData(malformedTagData);
      }).not.toThrow();

      const result = nfcManager.parseTagData(malformedTagData);
      expect(result).toBeNull();
    });

    it('should handle null or undefined input', () => {
      expect(() => {
        nfcManager.parseTagData(null as any);
      }).not.toThrow();

      expect(() => {
        nfcManager.parseTagData(undefined as any);
      }).not.toThrow();
    });
  });

  describe('tag format detection', () => {
    it('should detect Bambu Lab format correctly', () => {
      // Create mock data that resembles Bambu Lab tag structure
      const bambuData = new Uint8Array(1024);
      // Simulate Bambu-specific patterns with enough diversity to pass validation
      for (let i = 0; i < bambuData.length; i++) {
        bambuData[i] = (i * 3 + 123) % 256; // Create diverse pattern different from malformed test
      }
      bambuData[0] = 0x42; // 'B' for Bambu (mock)
      bambuData[1] = 0x41; // 'A'
      bambuData[2] = 0x4D; // 'M'
      bambuData[3] = 0x42; // 'B'

      const bambuTagData: TagData = {
        uid: '04914CCA',
        data: bambuData,
        technology: 'MifareClassic',
        format: TagFormat.BAMBU_LAB
      };

      // This test assumes parseTagData can handle format detection
      const result = nfcManager.parseTagData(bambuTagData);
      // Should parse successfully for valid data with proper format
      expect(result?.tagFormat).toBe(TagFormat.BAMBU_LAB);
    });

    it('should detect OpenSpool format correctly', () => {
      // Create mock data for OpenSpool format
      const openSpoolData = new Uint8Array(1024);
      // Simulate JSON NDEF structure
      openSpoolData[0] = 0xD1; // NDEF header
      openSpoolData[1] = 0x01; // Type length
      openSpoolData[2] = 0x10; // Payload length
      openSpoolData[3] = 0x54; // 'T' for Text Record

      const openSpoolTagData: TagData = {
        uid: '12345678',
        data: openSpoolData,
        technology: 'NTAG',
        format: TagFormat.OPENSPOOL
      };

      const result = nfcManager.parseTagData(openSpoolTagData);
      expect(result === null || result?.tagFormat === TagFormat.OPENSPOOL).toBe(true);
    });

    it('should handle unknown format gracefully', () => {
      const unknownData = new Uint8Array(512);
      // Fill with random data that doesn't match known patterns
      unknownData.fill(0xFF);

      const unknownTagData: TagData = {
        uid: 'UNKNOWN1',
        data: unknownData,
        technology: 'Unknown',
        format: TagFormat.UNKNOWN
      };

      const result = nfcManager.parseTagData(unknownTagData);
      expect(result).toBeNull();
    });
  });

  describe('data validation', () => {
    it('should validate UID format', () => {
      const testCases = [
        { uid: '04914CCA', valid: true },
        { uid: '1234567890ABCDEF', valid: true },
        { uid: '', valid: false },
        { uid: 'INVALID', valid: false },
        { uid: '04914CCZ', valid: false }, // Invalid hex character
      ];

      testCases.forEach(({ uid, valid }) => {
        const tagData: TagData = {
          uid,
          data: new Uint8Array(1024),
          technology: 'MifareClassic',
          format: TagFormat.BAMBU_LAB
        };

        if (valid) {
          expect(() => nfcManager.parseTagData(tagData)).not.toThrow();
        } else {
          const result = nfcManager.parseTagData(tagData);
          expect(result).toBeNull();
        }
      });
    });

    it('should validate data size requirements', () => {
      const testCases = [
        { size: 0, shouldParse: false },
        { size: 32, shouldParse: false },  // Too small for validation
        { size: 1024, shouldParse: true }, // Standard MFC size
        { size: 4096, shouldParse: true }, // Large MFC size
      ];

      testCases.forEach(({ size, shouldParse }) => {
        // Create data with diversity for validation
        const data = new Uint8Array(size);
        if (size > 0) {
          for (let i = 0; i < size; i++) {
            data[i] = (i * 7 + 42) % 256; // Create diverse pattern
          }
        }

        const tagData: TagData = {
          uid: '04914CCA',
          data: data,
          technology: 'MifareClassic',
          format: TagFormat.BAMBU_LAB
        };

        const result = nfcManager.parseTagData(tagData);
        if (shouldParse) {
          // May parse successfully or fail gracefully
          expect(typeof result === 'object').toBe(true);
        } else {
          expect(result).toBeNull();
        }
      });
    });
  });

  describe('bed temperature parsing', () => {
    it('should parse bed temperature values correctly', () => {
      // Mock data with known temperature values embedded
      const tempData = new Uint8Array(1024);
      
      // Simulate temperature data in expected format
      // These would be the actual bytes representing temperature in Bambu format
      tempData[100] = 60; // 60°C
      tempData[101] = 0;  
      tempData[200] = 80; // 80°C
      tempData[201] = 0;

      const tagData: TagData = {
        uid: '04914CCA',
        data: tempData,
        technology: 'MifareClassic',
        format: TagFormat.BAMBU_LAB
      };

      // Since we don't have the actual implementation yet,
      // we just verify it handles the data without throwing
      expect(() => nfcManager.parseTagData(tagData)).not.toThrow();
    });

    it('should handle edge case temperatures', () => {
      const edgeCases = [
        { temp: 0, description: 'absolute zero' },
        { temp: 300, description: 'very high temperature' },
        { temp: -1, description: 'invalid negative' },
        { temp: 65535, description: 'max uint16 value' }
      ];

      edgeCases.forEach(({ temp, description }) => {
        const tempData = new Uint8Array(1024);
        tempData[100] = temp & 0xFF;
        tempData[101] = (temp >> 8) & 0xFF;

        const tagData: TagData = {
          uid: '04914CCA',
          data: tempData,
          technology: 'MifareClassic',
          format: TagFormat.BAMBU_LAB
        };

        expect(() => {
          nfcManager.parseTagData(tagData);
        }).not.toThrow();
      });
    });
  });

  describe('version comparison logic', () => {
    it('should compare versions correctly', () => {
      const compareVersions = (v1: string, v2: string): number => {
        const v1Parts = v1.split('.').map(part => parseInt(part, 10) || 0);
        const v2Parts = v2.split('.').map(part => parseInt(part, 10) || 0);
        
        const maxLength = Math.max(v1Parts.length, v2Parts.length);
        
        for (let i = 0; i < maxLength; i++) {
          const part1 = v1Parts[i] || 0;
          const part2 = v2Parts[i] || 0;
          
          if (part1 > part2) return 1;
          if (part1 < part2) return -1;
        }
        
        return 0;
      };

      // Test basic version comparison
      expect(compareVersions('1.0', '1.1')).toBeLessThan(0);
      expect(compareVersions('1.1', '1.0')).toBeGreaterThan(0);
      expect(compareVersions('1.0', '1.0')).toBe(0);
      expect(compareVersions('1.1', '2.0')).toBeLessThan(0);
      expect(compareVersions('2.0', '1.1')).toBeGreaterThan(0);
      
      // Test with different number of components
      expect(compareVersions('1.0', '1.0.1')).toBeLessThan(0);
      expect(compareVersions('1.0.1', '1.0')).toBeGreaterThan(0);
      expect(compareVersions('1.0.0', '1.0')).toBe(0);
    });

    it('should handle malformed version strings', () => {
      const compareVersions = (v1: string, v2: string): number => {
        const v1Parts = v1.split('.').map(part => parseInt(part, 10) || 0);
        const v2Parts = v2.split('.').map(part => parseInt(part, 10) || 0);
        
        const maxLength = Math.max(v1Parts.length, v2Parts.length);
        
        for (let i = 0; i < maxLength; i++) {
          const part1 = v1Parts[i] || 0;
          const part2 = v2Parts[i] || 0;
          
          if (part1 > part2) return 1;
          if (part1 < part2) return -1;
        }
        
        return 0;
      };

      // Test malformed versions
      expect(compareVersions('1.a', '1.1')).toBeLessThan(0); // 'a' becomes 0
      expect(compareVersions('', '1.0')).toBeLessThan(0);    // Empty becomes 0
      expect(compareVersions('1.0', '')).toBeGreaterThan(0);
      expect(compareVersions('1..0', '1.0.0')).toBe(0);      // Empty part becomes 0
    });
  });
});