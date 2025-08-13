package com.bscan

import com.bscan.nfc.BambuKeyDerivation
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BambuKeyDerivation
 */
class BambuKeyDerivationTest {

    @Test
    fun testKeyDerivationProducesCorrectNumberOfKeys() {
        // Given a test UID
        val testUid = byteArrayOf(0x04, 0x5A, 0xB2, 0xC1, 0xDE, 0x34, 0x80)
        
        // When deriving keys
        val keys = BambuKeyDerivation.deriveKeys(testUid)
        
        // Then we should get 16 keys (for 16 sectors)
        assertEquals("Should generate 16 keys for MIFARE Classic", 16, keys.size)
    }
    
    @Test
    fun testKeyDerivationIsConsistent() {
        // Given the same UID
        val testUid = byteArrayOf(0x04, 0x5A, 0xB2, 0xC1, 0xDE, 0x34, 0x80)
        
        // When deriving keys multiple times
        val keys1 = BambuKeyDerivation.deriveKeys(testUid)
        val keys2 = BambuKeyDerivation.deriveKeys(testUid)
        
        // Then the results should be identical
        assertEquals("Key count should be consistent", keys1.size, keys2.size)
        for (i in keys1.indices) {
            assertArrayEquals("Key $i should be identical", keys1[i], keys2[i])
        }
    }
    
    @Test
    fun testKeyDerivationProducesValidKeyLength() {
        // Given a test UID
        val testUid = byteArrayOf(0x04, 0x5A, 0xB2, 0xC1, 0xDE, 0x34, 0x80)
        
        // When deriving keys
        val keys = BambuKeyDerivation.deriveKeys(testUid)
        
        // Then each key should be 6 bytes (MIFARE Classic key length)
        keys.forEachIndexed { index, key ->
            assertEquals("Key $index should be 6 bytes", 6, key.size)
        }
    }
    
    @Test
    fun testDifferentUidsProduceDifferentKeys() {
        // Given two different UIDs
        val uid1 = byteArrayOf(0x04, 0x5A, 0xB2, 0xC1, 0xDE, 0x34, 0x80)
        val uid2 = byteArrayOf(0x04, 0x1B, 0x3C, 0x4D, 0x5E, 0x6F, 0x70)
        
        // When deriving keys
        val keys1 = BambuKeyDerivation.deriveKeys(uid1)
        val keys2 = BambuKeyDerivation.deriveKeys(uid2)
        
        // Then the keys should be different
        assertFalse("Different UIDs should produce different keys", 
                   keys1.contentDeepEquals(keys2))
    }
}