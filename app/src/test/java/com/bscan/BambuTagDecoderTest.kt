package com.bscan

import com.bscan.decoder.BambuTagDecoder
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BambuTagDecoder
 */
class BambuTagDecoderTest {

    @Test
    fun testDecodeWithInsufficientData() {
        // Given incomplete tag data (not enough bytes)
        val tagData = NfcTagData(
            uid = "04:5A:B2:C1:DE:34:80",
            bytes = ByteArray(100), // Less than 240 bytes required
            technology = "MIFARE_CLASSIC"
        )

        // When decoding
        val result = BambuTagDecoder.parseTagDetails(tagData)

        // Then should return null for insufficient data
        assertNull("Should return null for insufficient data", result)
    }

    @Test
    fun testDecodeWithValidData() {
        // Given valid RFID tag data (240 bytes minimum)
        val tagBytes = ByteArray(240) // Initialize with zeros
        
        // Set up some test data in the expected blocks
        // Block 2: Material (16 bytes) - "PLA"
        tagBytes[32] = 'P'.code.toByte()
        tagBytes[33] = 'L'.code.toByte()
        tagBytes[34] = 'A'.code.toByte()
        
        // Block 4: Detailed filament type (16 bytes) - "Basic PLA"
        tagBytes[64] = 'B'.code.toByte()
        tagBytes[65] = 'a'.code.toByte()
        tagBytes[66] = 's'.code.toByte()
        tagBytes[67] = 'i'.code.toByte()
        tagBytes[68] = 'c'.code.toByte()
        tagBytes[69] = ' '.code.toByte()
        tagBytes[70] = 'P'.code.toByte()
        tagBytes[71] = 'L'.code.toByte()
        tagBytes[72] = 'A'.code.toByte()
        
        // Block 5: Color (RGBA), weight, diameter
        tagBytes[80] = 0xFF.toByte() // Red
        tagBytes[81] = 0x00 // Green
        tagBytes[82] = 0x00 // Blue  
        tagBytes[83] = 0xFF.toByte() // Alpha
        tagBytes[84] = 0xE8.toByte() // Weight low byte (1000)
        tagBytes[85] = 0x03 // Weight high byte
        // Diameter as float at bytes 88-91
        tagBytes[88] = 0x00
        tagBytes[89] = 0x00
        tagBytes[90] = 0xE0.toByte()
        tagBytes[91] = 0x3F

        val tagData = NfcTagData(
            uid = "04:5A:B2:C1:DE:34:80",
            bytes = tagBytes,
            technology = "MIFARE_CLASSIC"
        )

        // When decoding
        val result = BambuTagDecoder.parseTagDetails(tagData)

        // Then should successfully decode
        assertNotNull("Should successfully decode valid data", result)
        
        val filament = result!!
        assertEquals("04:5A:B2:C1:DE:34:80", filament.uid)
        assertEquals("PLA", filament.filamentType)
        assertEquals("Basic PLA", filament.detailedFilamentType)
        assertEquals(1000, filament.spoolWeight)
        assertTrue("Should parse filament diameter", filament.filamentDiameter > 0)
    }

    @Test
    fun testColorNameGeneration() {
        // Test basic color detection works
        val tagBytes = ByteArray(240)
        
        // Set red color
        tagBytes[80] = 0xFF.toByte() // Red
        tagBytes[81] = 0x00 // Green
        tagBytes[82] = 0x00 // Blue
        tagBytes[83] = 0xFF.toByte() // Alpha

        val tagData = NfcTagData(
            uid = "04:5A:B2:C1:DE:34:80",
            bytes = tagBytes,
            technology = "MIFARE_CLASSIC"
        )

        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should parse color data", result)
        assertEquals("#FF0000", result!!.colorHex)
        assertEquals("Red", result.colorName)
    }

    @Test
    fun testNullReturnOnException() {
        // Given corrupted or invalid data structure
        val tagData = NfcTagData(
            uid = "04:5A:B2:C1:DE:34:80", 
            bytes = ByteArray(240) { 0xFF.toByte() }, // All 0xFF which may cause parsing issues
            technology = "MIFARE_CLASSIC"
        )

        // When decoding (this may throw internal exceptions)
        val result = BambuTagDecoder.parseTagDetails(tagData)

        // Then should handle gracefully (may return null or valid result)
        // This test mainly ensures no unhandled exceptions are thrown
        assertTrue("Should not throw unhandled exceptions", 
                  result == null || result.uid.isNotEmpty())
    }
}