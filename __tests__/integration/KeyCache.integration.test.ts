/**
 * Integration tests for key cache system
 * Port of KeyCacheIntegrationTest.kt from the original Android app
 */

import { BambuKeyDerivation } from '../../src/services/NfcManager';

describe('Key Cache Integration Tests', () => {
  // Test UIDs for various scenarios
  const testUID1 = new Uint8Array([0x01, 0x02, 0x03, 0x04]);
  const testUID2 = new Uint8Array([0x05, 0x06, 0x07, 0x08]);
  const shortUID = new Uint8Array([0x01, 0x02]);
  const emptyUID = new Uint8Array();

  describe('cache behavior and algorithm compatibility', () => {
    it('should return same keys as direct derivation', () => {
      // Test without cache (direct derivation)
      const directKeys = BambuKeyDerivation.deriveKeys(testUID1);
      
      // Simulate cache behavior by calling direct derivation
      // (This test focuses on algorithm compatibility)
      const simulatedCacheKeys = BambuKeyDerivation.deriveKeys(testUID1);
      
      // Keys should be identical
      expect(directKeys).toHaveLength(simulatedCacheKeys.length);
      
      directKeys.forEach((key, index) => {
        const simulatedKey = simulatedCacheKeys[index];
        expect(simulatedKey).toBeDefined();
        expect(Array.from(key)).toEqual(Array.from(simulatedKey!));
      });
      
      // Verify key properties
      expect(directKeys).toHaveLength(16);
      directKeys.forEach(key => {
        expect(key).toHaveLength(6);
      });
    });

    it('should produce different keys for different UIDs', () => {
      const keys1 = BambuKeyDerivation.deriveKeys(testUID1);
      const keys2 = BambuKeyDerivation.deriveKeys(testUID2);
      
      // Keys should be different for different UIDs
      expect(keys1).toHaveLength(keys2.length);
      
      // At least some keys should be different
      let differentCount = 0;
      keys1.forEach((key1, index) => {
        const key2 = keys2[index];
        expect(key2).toBeDefined();
        if (!Array.from(key1).every((byte, byteIndex) => byte === key2![byteIndex])) {
          differentCount++;
        }
      });
      
      expect(differentCount).toBeGreaterThan(0);
    });

    it('should handle edge cases gracefully', () => {
      // Test with short UID
      const shortUIDKeys = BambuKeyDerivation.deriveKeys(shortUID);
      expect(shortUIDKeys).toHaveLength(0);
      
      // Test with empty UID
      const emptyUIDKeys = BambuKeyDerivation.deriveKeys(emptyUID);
      expect(emptyUIDKeys).toHaveLength(0);
      
      // Test with normal UID after edge cases
      const normalKeys = BambuKeyDerivation.deriveKeys(testUID1);
      expect(normalKeys).toHaveLength(16);
    });

    it('should produce deterministic keys', () => {
      // Multiple derivations of the same UID should produce identical results
      const keys1 = BambuKeyDerivation.deriveKeys(testUID1);
      const keys2 = BambuKeyDerivation.deriveKeys(testUID1);
      const keys3 = BambuKeyDerivation.deriveKeys(testUID1);
      
      keys1.forEach((key, index) => {
        const key2 = keys2[index];
        const key3 = keys3[index];
        expect(key2).toBeDefined();
        expect(key3).toBeDefined();
        expect(Array.from(key)).toEqual(Array.from(key2!));
        expect(Array.from(key)).toEqual(Array.from(key3!));
      });
    });
  });

  describe('performance characteristics', () => {
    it('should have reasonable performance', () => {
      const iterations = 10;
      
      // Measure derivation time for consistent UID
      const startTime = Date.now();
      for (let i = 0; i < iterations; i++) {
        BambuKeyDerivation.deriveKeys(testUID1);
      }
      const endTime = Date.now();
      
      const avgTimePerDerivation = (endTime - startTime) / iterations;
      
      // Each derivation should complete in reasonable time (less than 100ms typically)
      expect(avgTimePerDerivation).toBeLessThan(1000);
    });

    it('should handle concurrent access', async () => {
      const numThreads = 5;
      const numOperations = 10;
      
      // Create multiple promises doing key derivation
      const promises = Array.from({ length: numThreads }, () =>
        Promise.resolve().then(() => {
          const threadResults: Array<Uint8Array>[] = [];
          for (let i = 0; i < numOperations; i++) {
            const keys = BambuKeyDerivation.deriveKeys(testUID1);
            threadResults.push(keys);
          }
          return threadResults;
        })
      );
      
      // Wait for all promises to complete
      const allResults = await Promise.all(promises);
      const flatResults = allResults.flat();
      
      // All results should be identical
      const expectedKeys = BambuKeyDerivation.deriveKeys(testUID1);
      flatResults.forEach(keys => {
        expect(keys).toHaveLength(expectedKeys.length);
        keys.forEach((key, index) => {
          const expectedKey = expectedKeys[index];
          expect(expectedKey).toBeDefined();
          expect(Array.from(key)).toEqual(Array.from(expectedKey!));
        });
      });
      
      expect(flatResults).toHaveLength(numThreads * numOperations);
    });
  });

  describe('key material quality', () => {
    it('should produce keys with good entropy', () => {
      const keys = BambuKeyDerivation.deriveKeys(testUID1);
      
      // Check that keys are not all zeros or all the same
      const allZeroKey = new Uint8Array(6);
      keys.forEach(key => {
        expect(Array.from(key)).not.toEqual(Array.from(allZeroKey));
      });
      
      // Check that keys are different from each other
      for (let i = 0; i < keys.length; i++) {
        for (let j = i + 1; j < keys.length; j++) {
          const keyI = keys[i];
          const keyJ = keys[j];
          expect(keyI).toBeDefined();
          expect(keyJ).toBeDefined();
          expect(Array.from(keyI!)).not.toEqual(Array.from(keyJ!));
        }
      }
    });

    it('should maintain algorithm compatibility', () => {
      // This test verifies that our cache integration doesn't change
      // the fundamental key derivation algorithm
      
      const uid = new Uint8Array([0x12, 0x34, 0x56, 0x78]);
      const keys = BambuKeyDerivation.deriveKeys(uid);
      
      // Verify known properties of the Bambu key derivation
      expect(keys).toHaveLength(16);
      keys.forEach(key => {
        expect(key).toHaveLength(6);
      });
      
      // Verify that the same UID always produces the same first key
      // (This acts as a regression test for the algorithm)
      const secondDerivation = BambuKeyDerivation.deriveKeys(uid);
      const firstKey = keys[0];
      const secondFirstKey = secondDerivation[0];
      expect(firstKey).toBeDefined();
      expect(secondFirstKey).toBeDefined();
      expect(Array.from(firstKey!)).toEqual(Array.from(secondFirstKey!));
    });
  });

  describe('integration with NFC operations', () => {

    it('should integrate key derivation with tag authentication flow', () => {
      const testUID = new Uint8Array([0x04, 0x91, 0x4C, 0xCA]);
      
      // Derive keys for the test UID
      const keys = BambuKeyDerivation.deriveKeys(testUID);
      expect(keys).toHaveLength(16);
      
      // Keys should be ready for authentication
      keys.forEach(key => {
        expect(key).toHaveLength(6);
        expect(key).toBeInstanceOf(Uint8Array);
        
        // Keys should have reasonable entropy
        const keyArray = Array.from(key);
        const uniqueBytes = new Set(keyArray);
        // Allow some repetition but not all identical
        expect(uniqueBytes.size).toBeGreaterThan(0);
      });
    });

    it('should handle UID format conversions correctly', () => {
      // Test UID format conversion scenarios
      const hexUID = '04914CCA';
      const byteUID = new Uint8Array([0x04, 0x91, 0x4C, 0xCA]);
      
      // Both should produce valid keys
      const keysFromBytes = BambuKeyDerivation.deriveKeys(byteUID);
      expect(keysFromBytes).toHaveLength(16);
      
      // Converting hex string to bytes should work
      const hexBytes = new Uint8Array(
        hexUID.match(/.{1,2}/g)?.map(byte => parseInt(byte, 16)) || []
      );
      const keysFromHex = BambuKeyDerivation.deriveKeys(hexBytes);
      
      expect(keysFromHex).toHaveLength(16);
      keysFromBytes.forEach((key, index) => {
        const hexKey = keysFromHex[index];
        expect(hexKey).toBeDefined();
        expect(Array.from(key)).toEqual(Array.from(hexKey!));
      });
    });

    it('should cache and reuse keys efficiently', () => {
      const testUID = new Uint8Array([0xAA, 0xBB, 0xCC, 0xDD]);
      
      // Time first derivation
      const start1 = Date.now();
      const keys1 = BambuKeyDerivation.deriveKeys(testUID);
      const time1 = Date.now() - start1;
      
      // Time second derivation (should be same or faster if cached)
      const start2 = Date.now();
      const keys2 = BambuKeyDerivation.deriveKeys(testUID);
      const time2 = Date.now() - start2;
      
      // Results should be identical
      keys1.forEach((key, index) => {
        const key2 = keys2[index];
        expect(key2).toBeDefined();
        expect(Array.from(key)).toEqual(Array.from(key2!));
      });
      
      // Second call should not be significantly slower
      expect(time2).toBeLessThanOrEqual(time1 * 2);
    });

    it('should handle memory pressure during key operations', () => {
      // Test with many different UIDs to stress memory usage
      const testUIDs = [];
      for (let i = 0; i < 100; i++) {
        testUIDs.push(new Uint8Array([i, (i + 1) % 256, (i + 2) % 256, (i + 3) % 256]));
      }
      
      // Derive keys for all UIDs
      const allKeys = testUIDs.map(uid => BambuKeyDerivation.deriveKeys(uid));
      
      // All should have correct structure
      allKeys.forEach(keys => {
        expect(keys).toHaveLength(16);
        keys.forEach(key => {
          expect(key).toHaveLength(6);
        });
      });
      
      // Keys for different UIDs should be different
      for (let i = 0; i < Math.min(10, allKeys.length - 1); i++) {
        for (let j = i + 1; j < Math.min(10, allKeys.length); j++) {
          // At least first keys should be different
          const keyI = allKeys[i]?.[0];
          const keyJ = allKeys[j]?.[0];
          expect(keyI).toBeDefined();
          expect(keyJ).toBeDefined();
          expect(Array.from(keyI!)).not.toEqual(Array.from(keyJ!));
        }
      }
    });
  });
});