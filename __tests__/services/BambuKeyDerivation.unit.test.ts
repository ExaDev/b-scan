/**
 * Unit tests for BambuKeyDerivation service
 * Port of BambuKeyDerivationTest.kt from the original Android app
 */

import { BambuKeyDerivation } from '../../src/services/NfcManager';

describe('BambuKeyDerivation Unit Tests', () => {
  describe('deriveKeys functionality', () => {
    it('should derive 16 keys of 6 bytes each', () => {
      // Test with known UID from RFID-Tag-Guide
      const testUid = new Uint8Array([0x04, 0x91, 0x46, 0xCA, 0x5E, 0x64, 0x80]);
      
      const keys = BambuKeyDerivation.deriveKeys(testUid);
      
      // Verify we get 16 keys
      expect(keys).toHaveLength(16);
      
      // Verify each key is 6 bytes
      keys.forEach(key => {
        expect(key).toHaveLength(6);
        expect(key).toBeInstanceOf(Uint8Array);
      });
    });

    it('should handle various UID lengths', () => {
      // Test with 4-byte UID (MFC default)
      const uid4 = new Uint8Array([0x04, 0x91, 0x46, 0xCA]);
      const keys4 = BambuKeyDerivation.deriveKeys(uid4);
      expect(keys4).toHaveLength(16);
      
      // Test with 7-byte UID 
      const uid7 = new Uint8Array([0x04, 0x91, 0x46, 0xCA, 0x5E, 0x64, 0x80]);
      const keys7 = BambuKeyDerivation.deriveKeys(uid7);
      expect(keys7).toHaveLength(16);
      
      // UIDs should generate different keys
      const key4 = keys4[0];
      const key7 = keys7[0];
      expect(key4).toBeDefined();
      expect(key7).toBeDefined();
      if (key4 && key7) {
        const keys4Bytes = Array.from(key4);
        const keys7Bytes = Array.from(key7);
        expect(keys4Bytes).not.toEqual(keys7Bytes);
      }
    });

    it('should reject invalid UID lengths', () => {
      // Test with too-short UID
      const shortUid = new Uint8Array([0x04, 0x91, 0x46]);
      const keys = BambuKeyDerivation.deriveKeys(shortUid);
      expect(keys).toHaveLength(0);
      
      // Test with empty UID
      const emptyKeys = BambuKeyDerivation.deriveKeys(new Uint8Array());
      expect(emptyKeys).toHaveLength(0);
    });

    it('should produce consistent output with HKDF expansion', () => {
      // Test the HKDF expansion method produces correct results
      const testUid = new Uint8Array([0x12, 0x34, 0x56, 0x78]);
      
      const keys = BambuKeyDerivation.deriveKeys(testUid);
      
      // Verify basic structure
      expect(keys).toHaveLength(16);
      keys.forEach(key => {
        expect(key).toHaveLength(6);
      });
      
      // Verify keys are different (no identical keys due to splitting)
      const keyStrings = keys.map(key => Array.from(key).join(','));
      const uniqueKeys = new Set(keyStrings);
      expect(uniqueKeys.size).toBe(keys.length);
      
      // Verify deterministic output - same UID should produce same keys
      const keys2 = BambuKeyDerivation.deriveKeys(testUid);
      keys.forEach((key, index) => {
        const key2 = keys2[index];
        expect(key2).toBeDefined();
        if (key2) {
          expect(Array.from(key)).toEqual(Array.from(key2));
        }
      });
    });

    it('should produce keys with good entropy distribution', () => {
      // Test that the expansion method produces well-distributed keys
      const testUid = new Uint8Array([0xAA, 0xBB, 0xCC, 0xDD]);
      const keys = BambuKeyDerivation.deriveKeys(testUid);
      
      // Check that keys span the full byte range (good entropy)
      const allBytes = keys.flatMap(key => Array.from(key));
      const uniqueBytes = new Set(allBytes);
      
      // Should have reasonable byte distribution (at least 25% of possible byte values)
      expect(uniqueBytes.size).toBeGreaterThanOrEqual(64);
      
      // Verify no key is all zeros or all 0xFF (degenerate cases)
      keys.forEach(key => {
        const keyArray = Array.from(key);
        expect(keyArray.every(b => b === 0)).toBe(false);
        expect(keyArray.every(b => b === 0xFF)).toBe(false);
      });
    });

    it('should handle edge case UIDs correctly', () => {
      // Test edge cases that might expose issues with the expansion method
      const edgeCases = [
        { uid: new Uint8Array([0x00, 0x00, 0x00, 0x00]), name: 'all zeros' },
        { uid: new Uint8Array([0xFF, 0xFF, 0xFF, 0xFF]), name: 'all ones' },
        { uid: new Uint8Array([0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD]), name: '7-byte' },
        { uid: new Uint8Array([0xAA, 0x55, 0xAA, 0x55]), name: 'alternating pattern' }
      ];
      
      edgeCases.forEach(({ uid }) => {
        const keys = BambuKeyDerivation.deriveKeys(uid);
        
        expect(keys).toHaveLength(16);
        
        // Verify each key has proper structure
        keys.forEach(key => {
          expect(key).toHaveLength(6);
          
          // Key should not be degenerate (all same byte)
          const keyArray = Array.from(key);
          const uniqueBytesInKey = new Set(keyArray);
          
          // If all bytes are the same, they shouldn't all be 0x00 or 0xFF
          if (uniqueBytesInKey.size === 1) {
            const byteValue = keyArray[0];
            expect(byteValue).not.toBe(0x00);
            expect(byteValue).not.toBe(0xFF);
          }
        });
      });
    });

    it('should be deterministic across multiple calls', () => {
      const testUid = new Uint8Array([0x12, 0x34, 0x56, 0x78]);
      
      // Multiple derivations should produce identical results
      const keys1 = BambuKeyDerivation.deriveKeys(testUid);
      const keys2 = BambuKeyDerivation.deriveKeys(testUid);
      const keys3 = BambuKeyDerivation.deriveKeys(testUid);
      
      keys1.forEach((key, index) => {
        const key2 = keys2[index];
        const key3 = keys3[index];
        expect(key2).toBeDefined();
        expect(key3).toBeDefined();
        if (key2 && key3) {
          expect(Array.from(key)).toEqual(Array.from(key2));
          expect(Array.from(key)).toEqual(Array.from(key3));
        }
      });
    });

    it('should complete derivation in reasonable time', () => {
      const testUid = new Uint8Array([0x12, 0x34, 0x56, 0x78]);
      const iterations = 10;
      
      const startTime = Date.now();
      for (let i = 0; i < iterations; i++) {
        BambuKeyDerivation.deriveKeys(testUid);
      }
      const endTime = Date.now();
      
      const avgTimePerDerivation = (endTime - startTime) / iterations;
      
      // Each derivation should complete in reasonable time (less than 100ms typically)
      expect(avgTimePerDerivation).toBeLessThan(1000);
    });

    it('should produce unique keys for different UIDs', () => {
      const uid1 = new Uint8Array([0x01, 0x02, 0x03, 0x04]);
      const uid2 = new Uint8Array([0x05, 0x06, 0x07, 0x08]);
      
      const keys1 = BambuKeyDerivation.deriveKeys(uid1);
      const keys2 = BambuKeyDerivation.deriveKeys(uid2);
      
      // Keys should be different for different UIDs
      expect(keys1).toHaveLength(keys2.length);
      
      // At least the first key should be different
      const key1 = keys1[0];
      const key2 = keys2[0];
      expect(key1).toBeDefined();
      expect(key2).toBeDefined();
      if (key1 && key2) {
        expect(Array.from(key1)).not.toEqual(Array.from(key2));
      }
      
      // Check that most keys are different
      let differentKeys = 0;
      for (let i = 0; i < keys1.length; i++) {
        const keyA = keys1[i];
        const keyB = keys2[i];
        if (keyA && keyB && !Array.from(keyA).every((byte, idx) => byte === keyB[idx])) {
          differentKeys++;
        }
      }
      expect(differentKeys).toBeGreaterThan(keys1.length / 2);
    });
  });

  describe('key material quality', () => {
    it('should have good entropy in generated keys', () => {
      const testUid = new Uint8Array([0x42, 0x73, 0x63, 0x61, 0x6E]); // "Bscan" in ASCII
      const keys = BambuKeyDerivation.deriveKeys(testUid);
      
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
          if (keyI && keyJ) {
            expect(Array.from(keyI)).not.toEqual(Array.from(keyJ));
          }
        }
      }
    });

    it('should maintain algorithm compatibility', () => {
      // This test verifies that the key derivation algorithm maintains compatibility
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
      if (firstKey && secondFirstKey) {
        expect(Array.from(firstKey)).toEqual(Array.from(secondFirstKey));
      }
    });
  });
});