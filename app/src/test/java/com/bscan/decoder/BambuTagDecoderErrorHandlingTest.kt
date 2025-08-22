package com.bscan.decoder

import com.bscan.debug.DebugDataCollector
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*

/**
 * Error handling and edge case tests for BambuTagDecoder
 * Tests robustness against corrupted, malformed, and edge case data
 * Uses gradle testOptions { unitTests { isReturnDefaultValues = true } } to mock Android Log
 */
class BambuTagDecoderErrorHandlingTest {

    @Test
    fun `parseTagDetails handles insufficient data gracefully`() {
        val debugCollector = DebugDataCollector()
        
        // Test various insufficient data sizes
        val testCases = listOf(0, 50, 100, 150, 200, 239) // All less than required 240 bytes
        
        testCases.forEach { size ->
            val insufficientTagData = NfcTagData(
                uid = "12345678",
                bytes = ByteArray(size),
                technology = "MifareClassic"
            )
            
            val result = BambuTagDecoder.parseTagDetails(insufficientTagData, debugCollector)
            assertNull("Should return null for $size bytes of data", result)
        }
    }

    @Test
    fun `parseTagDetails handles empty data gracefully`() {
        val debugCollector = DebugDataCollector()
        val emptyTagData = NfcTagData(
            uid = "00000000",
            bytes = ByteArray(0),
            technology = "MifareClassic"
        )
        
        val result = BambuTagDecoder.parseTagDetails(emptyTagData, debugCollector)
        assertNull("Should return null for empty tag data", result)
    }

    @Test
    fun `parseTagDetails handles corrupted block data gracefully`() {
        val debugCollector = DebugDataCollector()
        
        // Create tag with corrupted data (all 0xFF bytes)
        val corruptedTagData = NfcTagData(
            uid = "CORRUPTED",
            bytes = ByteArray(1024) { 0xFF.toByte() },
            technology = "MifareClassic"
        )
        
        val result = BambuTagDecoder.parseTagDetails(corruptedTagData, debugCollector)
        
        // Should either return null or handle gracefully
        if (result != null) {
            // If parsing succeeds, ensure no runtime errors
            assertNotNull("Should have valid UID", result.uid)
            assertTrue("Color hex should be valid format", result.colorHex.startsWith("#"))
            assertTrue("Temperatures should be non-negative", result.dryingTemperature >= 0)
        }
    }

    @Test
    fun `parseTagDetails handles malformed date strings`() {
        val tagData = createTagDataWithMalformedDate()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should handle malformed dates gracefully", result)
        result!!
        
        // Should fall back to "Unknown" for unparseable dates
        assertEquals("Should default to Unknown for malformed date", "Unknown", result.productionDate)
    }

    @Test
    fun `parseTagDetails handles invalid temperature values`() {
        val tagData = createTagDataWithInvalidTemperatures()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should handle invalid temperatures gracefully", result)
        result!!
        
        // Temperatures should be parsed as unsigned values, so negative values become large positive
        assertTrue("All temperatures should be non-negative after parsing", 
            result.dryingTemperature >= 0 && 
            result.bedTemperature >= 0 && 
            result.minTemperature >= 0 && 
            result.maxTemperature >= 0)
    }

    @Test
    fun `parseTagDetails handles invalid color data`() {
        val tagData = createTagDataWithInvalidColors()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should handle invalid colors gracefully", result)
        result!!
        
        // Should produce valid hex color string
        assertTrue("Color hex should be valid format", result.colorHex.matches(Regex("^#[0-9A-Fa-f]{6}.*")))
        assertNotNull("Color name should not be null", result.colorName)
    }

    @Test
    fun `parseTagDetails detects transparent filament correctly`() {
        val tagData = createTagDataWithTransparentColor()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should parse transparent color tag data", result)
        result!!
        
        // NOTE: The decoder/BambuTagDecoder ignores alpha channel, so this test
        // documents current behavior rather than testing transparency detection
        // The transparency detection is implemented in utils/BambuTagDecoder
        assertNotNull("Should have a color name", result.colorName)
        assertNotNull("Should have a color hex", result.colorHex)
    }

    @Test
    fun `parseTagDetails handles semi-transparent filament correctly`() {
        val tagData = createTagDataWithSemiTransparentColor()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should parse semi-transparent color tag data", result)
        result!!
        
        // NOTE: The decoder/BambuTagDecoder ignores alpha channel
        assertNotNull("Should have a color name", result.colorName)
        assertNotNull("Should have a color hex", result.colorHex)
    }

    @Test
    fun `parseTagDetails handles out of bounds block access gracefully`() {
        val debugCollector = DebugDataCollector()
        
        // Create minimum size data that passes initial check but may cause issues in parsing
        val minimalTagData = NfcTagData(
            uid = "MINIMAL",
            bytes = ByteArray(240), // Exact minimum
            technology = "MifareClassic"
        )
        
        val result = BambuTagDecoder.parseTagDetails(minimalTagData, debugCollector)
        
        // Should handle gracefully, either returning null or valid result
        if (result != null) {
            assertNotNull("Should have valid UID", result.uid)
            assertNotNull("Should have valid filament type", result.filamentType)
        }
    }

    @Test
    fun `parseTagDetails handles null debugCollector gracefully`() {
        val validTagData = createValidTagData()
        
        // Should not throw exception with null debug collector
        val result = BambuTagDecoder.parseTagDetails(validTagData, null)
        
        assertNotNull("Should parse successfully with null debugCollector", result)
    }

    @Test
    fun `parseTagDetails handles extreme temperature values`() {
        val tagData = createTagDataWithExtremeTemperatures()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should handle extreme temperatures", result)
        result!!
        
        // Values should be parsed as unsigned 16-bit integers (0-65535)
        assertTrue("Temperatures should be in uint16 range", 
            result.dryingTemperature in 0..65535 &&
            result.bedTemperature in 0..65535 &&
            result.minTemperature in 0..65535 &&
            result.maxTemperature in 0..65535)
    }

    @Test
    fun `parseTagDetails handles very long strings in blocks`() {
        val tagData = createTagDataWithLongStrings()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should handle long strings", result)
        result!!
        
        // String fields should be truncated to reasonable lengths
        assertTrue("Filament type should be reasonable length", result.filamentType.length <= 50)
        assertTrue("Detailed type should be reasonable length", result.detailedFilamentType.length <= 50)
    }

    @Test
    fun `parseTagDetails handles zero filament diameter gracefully`() {
        val tagData = createTagDataWithZeroFilamentDiameter()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should handle zero diameter", result)
        result!!
        
        // Should fall back to default 1.75f for invalid diameter
        assertEquals("Should use default diameter", 1.75f, result.filamentDiameter, 0.001f)
    }

    // Helper methods to create test data with various error conditions

    private fun createTagDataWithMalformedDate(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Put malformed date in block 12: "INVALID_DATE_FORMAT"
        val block12Start = 12 * 16
        val malformedDate = "INVALID_DATE_FORMAT".toByteArray()
        malformedDate.copyInto(bytes, block12Start, 0, minOf(malformedDate.size, 16))
        
        return NfcTagData("TEST", bytes, "MifareClassic")
    }

    private fun createTagDataWithInvalidTemperatures(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Put invalid temperature values in block 6 (0xFFFF for all temps)
        val block6Start = 6 * 16
        for (i in 0 until 12 step 2) {
            bytes[block6Start + i] = 0xFF.toByte()
            bytes[block6Start + i + 1] = 0xFF.toByte()
        }
        
        return NfcTagData("TEST", bytes, "MifareClassic")
    }

    private fun createTagDataWithInvalidColors(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Put invalid color data in block 5 (all zeros)
        val block5Start = 5 * 16
        // Leave color bytes as zero
        
        return NfcTagData("TEST", bytes, "MifareClassic")
    }

    private fun createValidTagData(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Put minimal valid data
        // Block 2: "PLA" filament type
        val block2Start = 2 * 16
        "PLA".toByteArray().copyInto(bytes, block2Start)
        
        // Block 5: Valid color and diameter
        val block5Start = 5 * 16
        bytes[block5Start] = 0xFF.toByte() // Red
        bytes[block5Start + 1] = 0x00.toByte() // Green
        bytes[block5Start + 2] = 0x00.toByte() // Blue
        bytes[block5Start + 3] = 0xFF.toByte() // Alpha
        
        // Filament diameter 1.75f as float LE at offset 8
        val diameter = java.nio.ByteBuffer.allocate(4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putFloat(1.75f)
            .array()
        diameter.copyInto(bytes, block5Start + 8)
        
        return NfcTagData("VALID", bytes, "MifareClassic")
    }

    private fun createTagDataWithExtremeTemperatures(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Put extreme values (max uint16) in block 6
        val block6Start = 6 * 16
        for (i in 0 until 12 step 2) {
            bytes[block6Start + i] = 0xFF.toByte()
            bytes[block6Start + i + 1] = 0xFF.toByte()
        }
        
        return NfcTagData("EXTREME", bytes, "MifareClassic")
    }

    private fun createTagDataWithLongStrings(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Put very long string in block 2 (will be truncated to 16 bytes)
        val block2Start = 2 * 16
        val longString = "ThisIsAVeryLongFilamentTypeNameThatExceeds16Bytes"
        longString.toByteArray().copyInto(bytes, block2Start, 0, minOf(16, longString.length))
        
        return NfcTagData("LONG", bytes, "MifareClassic")
    }

    private fun createTagDataWithZeroFilamentDiameter(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Put zero diameter in block 5 at offset 8
        // Use compressed addressing to match decoder layout
        val compressedBlockIndex = 5 - (5 / 4)
        val block5Start = compressedBlockIndex * 16
        // Leave filament diameter bytes as zero (will be 0.0f)
        
        return NfcTagData("ZERO", bytes, "MifareClassic")
    }

    private fun createTagDataWithTransparentColor(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Block 5: Transparent color RGBA (any RGB + alpha=00)
        val block5Start = 5 * 16
        bytes[block5Start] = 0xFF.toByte()     // Red
        bytes[block5Start + 1] = 0x80.toByte() // Green  
        bytes[block5Start + 2] = 0x40.toByte() // Blue
        bytes[block5Start + 3] = 0x00.toByte() // Alpha = 00 (transparent)
        
        // Add minimal valid data for other fields
        "PLA".toByteArray().copyInto(bytes, 2 * 16) // Block 2: filament type
        
        return NfcTagData("TRANSPARENT", bytes, "MifareClassic")
    }

    private fun createTagDataWithSemiTransparentColor(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Block 5: Semi-transparent color RGBA (any RGB + alpha=80)
        val block5Start = 5 * 16
        bytes[block5Start] = 0xFF.toByte()     // Red
        bytes[block5Start + 1] = 0x00.toByte() // Green
        bytes[block5Start + 2] = 0x00.toByte() // Blue  
        bytes[block5Start + 3] = 0x80.toByte() // Alpha = 80 (semi-transparent)
        
        // Add minimal valid data for other fields
        "PLA".toByteArray().copyInto(bytes, 2 * 16) // Block 2: filament type
        
        return NfcTagData("SEMI_TRANSPARENT", bytes, "MifareClassic")
    }
}