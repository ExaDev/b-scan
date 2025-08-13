package com.bscan

import com.bscan.decoder.BambuTagDecoder
import com.bscan.model.NfcTagData
import com.bscan.model.ScanResult
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BambuTagDecoder
 */
class BambuTagDecoderTest {

    @Test
    fun testDecodeWithInsufficientData() {
        // Given incomplete tag data (no block data)
        val tagData = NfcTagData(
            uid = "04:5A:B2:C1:DE:34:80",
            technology = "MIFARE_CLASSIC",
            bytes = emptyMap(),
            isAuthenticated = true
        )

        // When decoding
        val result = BambuTagDecoder.decode(tagData)

        // Then should return insufficient data result
        assertEquals(ScanResult.INSUFFICIENT_DATA, result.scanResult)
        assertNull(result.filamentInfo)
    }

    @Test
    fun testDecodeWithUnauthenticatedTag() {
        // Given unauthenticated tag data
        val tagData = NfcTagData(
            uid = "04:5A:B2:C1:DE:34:80",
            technology = "MIFARE_CLASSIC",
            bytes = mapOf(5 to byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0xFF.toByte())),
            isAuthenticated = false
        )

        // When decoding
        val result = BambuTagDecoder.decode(tagData)

        // Then should return authentication failed
        assertEquals(ScanResult.AUTHENTICATION_FAILED, result.scanResult)
        assertNull(result.filamentInfo)
    }

    @Test
    fun testDecodeWithValidData() {
        // Given valid RFID tag data
        val blockData = mapOf(
            5 to byteArrayOf(
                0xFF.toByte(), 0x00, 0x00, 0xFF.toByte(),  // RGBA color
                0x03, 0xE8.toByte(),                        // Spool weight (1000g)
                0x00, 0x00,                                 // Reserved
                0x41, 0xE0.toByte(), 0x00, 0x00,           // Filament diameter (28.0 as float)
                0x00, 0x00, 0x00, 0x00                      // Padding
            ),
            6 to byteArrayOf(
                0x28, 0x00,                                 // Drying temp (40°C)
                0x06, 0x00,                                 // Drying time (6h)
                0x00, 0x00,                                 // Reserved
                0x3C, 0x00,                                 // Bed temp (60°C)
                0xD2, 0x00,                                 // Max temp (210°C)
                0xBE, 0x00,                                 // Min temp (190°C)
                0x00, 0x00, 0x00, 0x00                      // Padding
            )
        )

        val tagData = NfcTagData(
            uid = "04:5A:B2:C1:DE:34:80",
            technology = "MIFARE_CLASSIC",
            bytes = blockData,
            isAuthenticated = true
        )

        // When decoding
        val result = BambuTagDecoder.decode(tagData)

        // Then should successfully decode
        assertEquals(ScanResult.SUCCESS, result.scanResult)
        assertNotNull(result.filamentInfo)
        
        val filament = result.filamentInfo!!
        assertEquals("04:5A:B2:C1:DE:34:80", filament.uid)
        assertEquals(1000, filament.spoolWeight)
        assertEquals(40, filament.dryingTemperature)
        assertEquals(6, filament.dryingTime)
        assertEquals(60, filament.bedTemperature)
        assertEquals(210, filament.maxTemperature)
        assertEquals(190, filament.minTemperature)
    }

    @Test
    fun testColorNameGeneration() {
        // Test that different colors get different names
        val testColors = listOf(
            byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0xFF.toByte()), // Red
            byteArrayOf(0x00, 0xFF.toByte(), 0x00, 0xFF.toByte()), // Green  
            byteArrayOf(0x00, 0x00, 0xFF.toByte(), 0xFF.toByte()), // Blue
            byteArrayOf(0x00, 0x00, 0x00, 0xFF.toByte())           // Black
        )

        val colorNames = mutableSetOf<String>()
        
        testColors.forEach { colorBytes ->
            val blockData = mapOf(
                5 to colorBytes + ByteArray(12) { 0 }, // Color + padding
                6 to ByteArray(16) { 0 }               // Temperature block
            )

            val tagData = NfcTagData(
                uid = "04:5A:B2:C1:DE:34:80",
                technology = "MIFARE_CLASSIC", 
                bytes = blockData,
                isAuthenticated = true
            )

            val result = BambuTagDecoder.decode(tagData)
            if (result.scanResult == ScanResult.SUCCESS) {
                colorNames.add(result.filamentInfo!!.colorName)
            }
        }

        // Should have generated different color names (at least some variation)
        assertTrue("Should generate different color names", colorNames.size >= 2)
    }
}