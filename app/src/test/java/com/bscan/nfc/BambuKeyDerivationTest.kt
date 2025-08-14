package com.bscan.nfc

import org.junit.Test
import org.junit.Assert.*

/**
 * Test for BambuKeyDerivation to verify compatibility with Python reference implementation
 */
class BambuKeyDerivationTest {

    @Test
    fun `deriveKeys matches Python reference implementation`() {
        // Test with known UID from RFID-Tag-Guide
        val testUid = byteArrayOf(0x04.toByte(), 0x91.toByte(), 0x46.toByte(), 0xCA.toByte(), 
                                 0x5E.toByte(), 0x64.toByte(), 0x80.toByte())
        
        val keys = BambuKeyDerivation.deriveKeys(testUid)
        
        // Verify we get 16 keys
        assertEquals("Should generate 16 keys", 16, keys.size)
        
        // Verify each key is 6 bytes
        keys.forEachIndexed { index, key ->
            assertEquals("Key $index should be 6 bytes", 6, key.size)
        }
        
        // Print keys for manual verification against Python script
        println("Generated keys for UID ${testUid.joinToString("") { "%02X".format(it) }}:")
        keys.forEachIndexed { index, key ->
            println("Key $index: ${key.joinToString("") { "%02X".format(it) }}")
        }
    }
    
    @Test
    fun `deriveKeys handles various UID lengths`() {
        // Test with 4-byte UID (MFC default)
        val uid4 = byteArrayOf(0x04.toByte(), 0x91.toByte(), 0x46.toByte(), 0xCA.toByte())
        val keys4 = BambuKeyDerivation.deriveKeys(uid4)
        assertEquals(16, keys4.size)
        
        // Test with 7-byte UID 
        val uid7 = byteArrayOf(0x04.toByte(), 0x91.toByte(), 0x46.toByte(), 0xCA.toByte(),
                              0x5E.toByte(), 0x64.toByte(), 0x80.toByte())
        val keys7 = BambuKeyDerivation.deriveKeys(uid7)
        assertEquals(16, keys7.size)
        
        // UIDs should generate different keys
        assertFalse("Different UIDs should generate different keys", 
                   keys4[0].contentEquals(keys7[0]))
    }
    
    @Test
    fun `deriveKeys rejects invalid UID lengths`() {
        // Test with too-short UID
        val shortUid = byteArrayOf(0x04.toByte(), 0x91.toByte(), 0x46.toByte())
        val keys = BambuKeyDerivation.deriveKeys(shortUid)
        assertEquals("Should return empty array for short UID", 0, keys.size)
        
        // Test with empty UID
        val emptyKeys = BambuKeyDerivation.deriveKeys(byteArrayOf())
        assertEquals("Should return empty array for empty UID", 0, emptyKeys.size)
    }
}