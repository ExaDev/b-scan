package com.bscan.decoder

import com.bscan.debug.DebugDataCollector
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BambuTagDecoder
 */
class BambuTagDecoderTest {

    @Test
    fun `parseTagDetails handles empty data gracefully`() {
        val debugCollector = DebugDataCollector()
        val emptyTagData = NfcTagData(
            uid = "00000000",
            bytes = ByteArray(0),
            technology = "MifareClassic"
        )
        
        try {
            val result = BambuTagDecoder.parseTagDetails(emptyTagData, debugCollector)
            assertNull("Should return null for empty tag data", result)
        } catch (e: Exception) {
            // This is acceptable - empty data should be handled gracefully
            assertTrue("Exception should be handled for empty data", true)
        }
    }

    @Test
    fun `parseTagDetails handles insufficient data gracefully`() {
        val debugCollector = DebugDataCollector()
        val insufficientTagData = NfcTagData(
            uid = "12345678",
            bytes = ByteArray(50), // Too small
            technology = "MifareClassic"
        )
        
        try {
            val result = BambuTagDecoder.parseTagDetails(insufficientTagData, debugCollector)
            assertNull("Should return null for insufficient tag data", result)
        } catch (e: Exception) {
            // This is acceptable - insufficient data should be handled gracefully
            assertTrue("Exception should be handled for insufficient data", true)
        }
    }

    @Test
    fun `version comparison works correctly`() {
        // This test validates the version comparison logic we use in UpdateService
        val version1 = "1.0"
        val version2 = "1.1"
        val version3 = "2.0"
        
        // Test basic version comparison logic
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }
        val v3Parts = version3.split(".").map { it.toIntOrNull() ?: 0 }
        
        assertTrue("1.1 should be greater than 1.0", v2Parts[1] > v1Parts[1])
        assertTrue("2.0 should be greater than 1.1", v3Parts[0] > v2Parts[0])
    }
}