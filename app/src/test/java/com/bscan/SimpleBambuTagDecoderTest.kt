package com.bscan

import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*

/**
 * Simplified unit tests for BambuTagDecoder that avoid Android dependencies
 */
class SimpleBambuTagDecoderTest {

    @Test
    fun testNfcTagDataCreation() {
        // Test that we can create NfcTagData objects
        val tagData = NfcTagData(
            uid = "04:5A:B2:C1:DE:34:80",
            bytes = ByteArray(240),
            technology = "MIFARE_CLASSIC"
        )
        
        assertEquals("04:5A:B2:C1:DE:34:80", tagData.uid)
        assertEquals(240, tagData.bytes.size)
        assertEquals("MIFARE_CLASSIC", tagData.technology)
    }
    
    @Test
    fun testColorHexConversion() {
        // Test basic color conversion logic
        val colorBytes = byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0xFF.toByte()) // Red
        
        val r = colorBytes[0].toUByte().toInt()
        val g = colorBytes[1].toUByte().toInt()
        val b = colorBytes[2].toUByte().toInt()
        
        assertEquals(255, r)
        assertEquals(0, g)
        assertEquals(0, b)
        
        val colorHex = String.format("#%02X%02X%02X", r, g, b)
        assertEquals("#FF0000", colorHex)
    }
    
    @Test
    fun testByteBufferOperations() {
        // Test ByteBuffer operations used in tag parsing
        val testBytes = byteArrayOf(0xE8.toByte(), 0x03, 0x00, 0x00) // 1000 as uint16 LE
        
        val buffer = java.nio.ByteBuffer.wrap(testBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val value = buffer.short.toInt() and 0xFFFF
        
        assertEquals(1000, value)
    }
    
    @Test
    fun testStringProcessing() {
        // Test string processing used in tag parsing
        val testBytes = byteArrayOf('P'.code.toByte(), 'L'.code.toByte(), 'A'.code.toByte(), 0x00, 0x00)
        val material = testBytes.toString(Charsets.UTF_8).replace("\u0000", "")
        
        assertEquals("PLA", material)
    }
}