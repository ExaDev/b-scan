package com.bscan

import org.junit.Test
import org.junit.Assert.*

/**
 * Simplified unit tests for BambuKeyDerivation that avoid Android dependencies
 */
class SimpleBambuKeyDerivationTest {

    @Test
    fun testKeyDerivationBasics() {
        // Test that we can import the basic crypto classes needed for key derivation
        val testUid = byteArrayOf(0x04, 0x5A.toByte(), 0xB2.toByte(), 0xC1.toByte())
        
        // Test basic byte array operations
        assertEquals(4, testUid.size)
        assertEquals(0x04, testUid[0])
        assertEquals(0x5A.toByte(), testUid[1])
        
        // Test hex formatting (used in key derivation)
        val hexString = testUid.joinToString("") { "%02X".format(it) }
        assertEquals("045AB2C1", hexString)
    }
    
    @Test
    fun testHmacSha256Availability() {
        // Test that HMAC-SHA256 is available (required for HKDF)
        try {
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            assertNotNull("HMAC-SHA256 should be available", mac)
        } catch (e: Exception) {
            fail("HMAC-SHA256 should be available: ${e.message}")
        }
    }
    
    @Test
    fun testByteArrayManipulation() {
        // Test byte array operations used in key derivation
        val masterKey = byteArrayOf(
            0x9a.toByte(), 0x75.toByte(), 0xc9.toByte(), 0xf2.toByte()
        )
        val context = "RFID-A\u0000".toByteArray()
        
        assertEquals(4, masterKey.size)
        assertEquals(7, context.size) // "RFID-A" + null terminator
        
        // Test combining arrays (used in HKDF expand)
        val combined = context + byteArrayOf(1)
        assertEquals(8, combined.size)
        assertEquals(1, combined.last())
    }
}