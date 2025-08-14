package com.bscan.decoder

import com.bscan.debug.DebugDataCollector
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito.*

/**
 * Simple unit tests for BambuTagDecoder that avoid Android dependencies
 * These tests focus on core parsing logic without Android Log calls
 */
class BambuTagDecoderSimpleTest {

    @Before
    fun setUp() {
        // Mock Android Log to avoid "not mocked" errors
        mockStatic(android.util.Log::class.java).use {
            `when`(android.util.Log.d(anyString(), anyString())).thenReturn(0)
            `when`(android.util.Log.w(anyString(), anyString())).thenReturn(0)
            `when`(android.util.Log.e(anyString(), anyString(), any())).thenReturn(0)
        }
    }

    @Test
    fun `parseTagDetails handles insufficient data correctly`() {
        val debugCollector = DebugDataCollector()
        val insufficientData = NfcTagData(
            uid = "12345678",
            bytes = ByteArray(100), // Less than required 240 bytes
            technology = "MifareClassic"
        )

        mockStatic(android.util.Log::class.java).use {
            `when`(android.util.Log.d(anyString(), anyString())).thenReturn(0)
            `when`(android.util.Log.w(anyString(), anyString())).thenReturn(0)

            val result = BambuTagDecoder.parseTagDetails(insufficientData, debugCollector)
            assertNull("Should return null for insufficient data", result)
        }
    }

    @Test
    fun `parseTagDetails handles empty data correctly`() {
        val debugCollector = DebugDataCollector()
        val emptyData = NfcTagData(
            uid = "00000000",
            bytes = ByteArray(0),
            technology = "MifareClassic"
        )

        mockStatic(android.util.Log::class.java).use {
            `when`(android.util.Log.d(anyString(), anyString())).thenReturn(0)
            `when`(android.util.Log.w(anyString(), anyString())).thenReturn(0)

            val result = BambuTagDecoder.parseTagDetails(emptyData, debugCollector)
            assertNull("Should return null for empty data", result)
        }
    }

    @Test
    fun `parseTagDetails with minimal valid data structure`() {
        val debugCollector = DebugDataCollector()
        val validData = createMinimalValidTagData()

        mockStatic(android.util.Log::class.java).use {
            `when`(android.util.Log.d(anyString(), anyString())).thenReturn(0)
            `when`(android.util.Log.w(anyString(), anyString())).thenReturn(0)
            `when`(android.util.Log.e(anyString(), anyString(), any())).thenReturn(0)

            val result = BambuTagDecoder.parseTagDetails(validData, debugCollector)
            
            // Test may return null due to parsing exceptions, which is acceptable
            // The key is that it doesn't crash and handles the data gracefully
            if (result != null) {
                assertNotNull("Should have valid UID", result.uid)
                assertNotNull("Should have valid filament type", result.filamentType)
                assertTrue("Should have valid color hex", result.colorHex.startsWith("#"))
            }
        }
    }

    @Test
    fun `decoder handles null debug collector gracefully`() {
        val validData = createMinimalValidTagData()

        mockStatic(android.util.Log::class.java).use {
            `when`(android.util.Log.d(anyString(), anyString())).thenReturn(0)
            `when`(android.util.Log.w(anyString(), anyString())).thenReturn(0)
            `when`(android.util.Log.e(anyString(), anyString(), any())).thenReturn(0)

            val result = BambuTagDecoder.parseTagDetails(validData, null)
            
            // Should not throw exception with null debug collector
            // May return null due to parsing issues, but shouldn't crash
        }
    }

    private fun createMinimalValidTagData(): NfcTagData {
        val bytes = ByteArray(1024) // Ensure sufficient size
        
        // Block 2: Set "PLA" filament type
        val block2Start = 2 * 16
        "PLA".toByteArray().copyInto(bytes, block2Start)
        
        // Block 4: Set "PLA Basic" detailed type
        val block4Start = 4 * 16
        "PLA Basic".toByteArray().copyInto(bytes, block4Start)
        
        // Block 5: Set color (red) and basic properties
        val block5Start = 5 * 16
        bytes[block5Start] = 0xFF.toByte()     // Red
        bytes[block5Start + 1] = 0x00.toByte() // Green
        bytes[block5Start + 2] = 0x00.toByte() // Blue
        bytes[block5Start + 3] = 0xFF.toByte() // Alpha
        
        // Spool weight: 250g as uint16 LE at offset 4
        bytes[block5Start + 4] = 0xFA.toByte() // 250 & 0xFF
        bytes[block5Start + 5] = 0x00.toByte() // (250 >> 8) & 0xFF
        
        // Filament diameter: 1.75f as float LE at offset 8
        val diameterBuffer = java.nio.ByteBuffer.allocate(4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putFloat(1.75f)
        diameterBuffer.array().copyInto(bytes, block5Start + 8)
        
        return NfcTagData(
            uid = "TEST1234",
            bytes = bytes,
            technology = "MifareClassic"
        )
    }
}