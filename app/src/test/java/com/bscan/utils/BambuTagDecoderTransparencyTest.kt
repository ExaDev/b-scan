package com.bscan.utils

import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for transparent filament detection in utils/BambuTagDecoder
 * This tests the specific functionality added in commit 8f777b5
 */
class BambuTagDecoderTransparencyTest {

    @Test
    fun `extractColorName detects transparent filament correctly`() {
        val tagData = createTagDataWithTransparentColor()
        val result = BambuTagDecoder.parseTag(tagData)
        
        assertNotNull("Should parse transparent color tag data", result)
        result!!
        
        // Should detect transparent filament when alpha = 00
        assertEquals("Should detect clear/transparent filament", "Clear/Transparent", result.colorName)
    }

    @Test
    fun `extractColorName handles semi-transparent filament correctly`() {
        val tagData = createTagDataWithSemiTransparentColor()
        val result = BambuTagDecoder.parseTag(tagData)
        
        assertNotNull("Should parse semi-transparent color tag data", result)
        result!!
        
        // Should NOT detect as clear/transparent when alpha != 00
        assertNotEquals("Should not detect as clear when semi-transparent", "Clear/Transparent", result.colorName)
    }

    @Test
    fun `extractColorName handles opaque colors correctly`() {
        val tagData = createTagDataWithOpaqueColor()
        val result = BambuTagDecoder.parseTag(tagData)
        
        assertNotNull("Should parse opaque color tag data", result)
        result!!
        
        // Should not detect as transparent when alpha = FF
        assertNotEquals("Should not detect opaque color as transparent", "Clear/Transparent", result.colorName)
    }

    @Test
    fun `extractColorName handles various alpha values correctly`() {
        val alphaTestCases = listOf(
            0x00 to true,   // Fully transparent -> should be Clear/Transparent
            0x01 to false,  // Nearly transparent -> should not be Clear/Transparent
            0x7F to false,  // Semi-transparent -> should not be Clear/Transparent
            0xFE to false,  // Nearly opaque -> should not be Clear/Transparent
            0xFF to false   // Fully opaque -> should not be Clear/Transparent
        )

        alphaTestCases.forEach { (alpha, shouldBeTransparent) ->
            val tagData = createTagDataWithSpecificAlpha(alpha)
            val result = BambuTagDecoder.parseTag(tagData)
            
            assertNotNull("Should parse tag data with alpha $alpha", result)
            result!!
            
            if (shouldBeTransparent) {
                assertEquals("Alpha $alpha should be detected as transparent", 
                    "Clear/Transparent", result.colorName)
            } else {
                assertNotEquals("Alpha $alpha should not be detected as transparent", 
                    "Clear/Transparent", result.colorName)
            }
        }
    }

    @Test
    fun `extractColorName maintains RGB values for transparent colors`() {
        val rgbTestCases = listOf(
            Triple(0xFF, 0x00, 0x00), // Red transparent
            Triple(0x00, 0xFF, 0x00), // Green transparent  
            Triple(0x00, 0x00, 0xFF), // Blue transparent
            Triple(0xFF, 0xFF, 0xFF), // White transparent
            Triple(0x80, 0x40, 0x20)  // Custom color transparent
        )

        rgbTestCases.forEach { (r, g, b) ->
            val tagData = createTagDataWithTransparentRGB(r, g, b)
            val result = BambuTagDecoder.parseTag(tagData)
            
            assertNotNull("Should parse transparent RGB($r,$g,$b)", result)
            result!!
            
            assertEquals("Should detect as transparent regardless of RGB", 
                "Clear/Transparent", result.colorName)
            
            // The colorHex should still contain the RGB values
            val expectedHex = "%02X%02X%02X00".format(r, g, b)
            assertTrue("Color hex should contain RGB values for transparent color",
                result.colorHex.contains(expectedHex, ignoreCase = true))
        }
    }

    // Helper methods to create test data

    private fun createTagDataWithTransparentColor(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Block 5: Transparent color RGBA (red with alpha=00)
        val block5Start = 5 * 16
        bytes[block5Start] = 0xFF.toByte()     // Red
        bytes[block5Start + 1] = 0x80.toByte() // Green  
        bytes[block5Start + 2] = 0x40.toByte() // Blue
        bytes[block5Start + 3] = 0x00.toByte() // Alpha = 00 (transparent)
        
        addMinimalValidData(bytes)
        return NfcTagData("TRANSPARENT", bytes, "MifareClassic")
    }

    private fun createTagDataWithSemiTransparentColor(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Block 5: Semi-transparent color RGBA (red with alpha=80)
        val block5Start = 5 * 16
        bytes[block5Start] = 0xFF.toByte()     // Red
        bytes[block5Start + 1] = 0x00.toByte() // Green
        bytes[block5Start + 2] = 0x00.toByte() // Blue  
        bytes[block5Start + 3] = 0x80.toByte() // Alpha = 80 (semi-transparent)
        
        addMinimalValidData(bytes)
        return NfcTagData("SEMI_TRANSPARENT", bytes, "MifareClassic")
    }

    private fun createTagDataWithOpaqueColor(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Block 5: Opaque color RGBA (blue with alpha=FF)
        val block5Start = 5 * 16
        bytes[block5Start] = 0x00.toByte()     // Red
        bytes[block5Start + 1] = 0x00.toByte() // Green
        bytes[block5Start + 2] = 0xFF.toByte() // Blue  
        bytes[block5Start + 3] = 0xFF.toByte() // Alpha = FF (opaque)
        
        addMinimalValidData(bytes)
        return NfcTagData("OPAQUE", bytes, "MifareClassic")
    }

    private fun createTagDataWithSpecificAlpha(alpha: Int): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Block 5: Color with specific alpha value
        val block5Start = 5 * 16
        bytes[block5Start] = 0xFF.toByte()     // Red
        bytes[block5Start + 1] = 0x80.toByte() // Green
        bytes[block5Start + 2] = 0x40.toByte() // Blue
        bytes[block5Start + 3] = alpha.toByte() // Specified alpha
        
        addMinimalValidData(bytes)
        return NfcTagData("ALPHA_$alpha", bytes, "MifareClassic")
    }

    private fun createTagDataWithTransparentRGB(r: Int, g: Int, b: Int): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Block 5: Specific RGB with alpha=00
        val block5Start = 5 * 16
        bytes[block5Start] = r.toByte()        // Red
        bytes[block5Start + 1] = g.toByte()    // Green
        bytes[block5Start + 2] = b.toByte()    // Blue
        bytes[block5Start + 3] = 0x00.toByte() // Alpha = 00 (transparent)
        
        addMinimalValidData(bytes)
        return NfcTagData("TRANSPARENT_RGB", bytes, "MifareClassic")
    }

    private fun addMinimalValidData(bytes: ByteArray) {
        // Add minimal valid data for other required fields
        // Block 2: filament type
        "PLA".toByteArray().copyInto(bytes, 2 * 16)
        
        // Block 4: detailed filament type  
        "PLA Basic".toByteArray().copyInto(bytes, 4 * 16)
        
        // Block 5: Add filament diameter (1.75f as float LE at offset 8)
        val diameter = java.nio.ByteBuffer.allocate(4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putFloat(1.75f)
            .array()
        diameter.copyInto(bytes, 5 * 16 + 8)
        
        // Block 12: Add minimal production date data
        val timestamp = (System.currentTimeMillis() / 1000).toInt()
        val timestampBytes = java.nio.ByteBuffer.allocate(4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putInt(timestamp)
            .array()
        timestampBytes.copyInto(bytes, 12 * 16)
    }
}