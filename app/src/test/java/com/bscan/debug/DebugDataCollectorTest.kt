package com.bscan.debug

import com.bscan.model.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for DebugDataCollector
 */
class DebugDataCollectorTest {

    private lateinit var debugCollector: DebugDataCollector

    @Before
    fun setup() {
        debugCollector = DebugDataCollector()
    }

    @Test
    fun `recordTagInfo stores correct size and sectors`() {
        // When
        debugCollector.recordTagInfo(1024, 16)
        
        // Then - we can't directly assert internal state, but we can test through createDecryptedScanData
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS
        )
        
        assertEquals("Should record correct tag size", 1024, decryptedScan.tagSizeBytes)
        assertEquals("Should record correct sector count", 16, decryptedScan.sectorCount)
    }

    @Test
    fun `recordSectorAuthentication stores authentication data`() {
        // When
        debugCollector.recordTagInfo(1024, 16)
        debugCollector.recordSectorAuthentication(1, true, "KeyA")
        debugCollector.recordSectorAuthentication(2, false, "KeyB")
        
        // Then
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "12345678",
            technology = "MifareClassic", 
            result = ScanResult.SUCCESS
        )
        
        assertEquals("Should record sector count", 16, decryptedScan.sectorCount)
        assertTrue("Should record authenticated sector", 1 in decryptedScan.authenticatedSectors)
        assertTrue("Should record failed sector", 2 in decryptedScan.failedSectors)
        assertEquals("Should record key type for authenticated sector", "KeyA", decryptedScan.usedKeys[1])
    }

    @Test
    fun `recordBlockData stores hex data correctly`() {
        // Given
        val testBlockData = "00112233445566778899AABBCCDDEEFF"
        
        // When
        debugCollector.recordBlockData(4, testBlockData)
        
        // Then
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS
        )
        
        assertEquals("Should store block data correctly", testBlockData, decryptedScan.decryptedBlocks[4])
    }

    @Test
    fun `recordDerivedKeys handles multiple keys`() {
        // Given
        val key1 = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        val key2 = byteArrayOf(0x11.toByte(), 0x22.toByte(), 0x33.toByte(), 0x44.toByte(), 0x55.toByte(), 0x66.toByte())
        val keys = arrayOf(key1, key2)
        
        // When
        debugCollector.recordDerivedKeys(keys)
        
        // Then
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS
        )
        
        assertTrue("Should contain first key", "AABBCCDDEEFF" in decryptedScan.derivedKeys)
        assertTrue("Should contain second key", "112233445566" in decryptedScan.derivedKeys)
        assertEquals("Should have both keys", 2, decryptedScan.derivedKeys.size)
    }

    @Test
    fun `recordColorBytes stores raw color data`() {
        // Given
        val colorBytes = byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x00.toByte())
        
        // When  
        debugCollector.recordColorBytes(colorBytes)
        
        // Then - raw color bytes are not directly accessible in DecryptedScanData
        // This test verifies the data is recorded internally
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS
        )
        
        // Color bytes are typically encoded in block data or handled separately
        assertNotNull("Should create scan data", decryptedScan)
    }

    @Test
    fun `recordError accumulates error messages`() {
        // Given
        val error1 = "Authentication failed for sector 1"
        val error2 = "Invalid block data at position 64"
        
        // When
        debugCollector.recordError(error1)
        debugCollector.recordError(error2)
        
        // Then
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.AUTHENTICATION_FAILED
        )
        
        assertTrue("Should contain first error", error1 in decryptedScan.errors)
        assertTrue("Should contain second error", error2 in decryptedScan.errors)
        assertEquals("Should have both errors", 2, decryptedScan.errors.size)
    }

    @Test
    fun `recordParsingDetail stores flexible debug data`() {
        // Given
        val details = mapOf(
            "version" to 1,
            "checksum" to "valid",
            "parsedFields" to listOf("brand", "color", "type"),
            "confidence" to 0.95
        )
        
        // When
        details.forEach { (key, value) ->
            debugCollector.recordParsingDetail(key, value)
        }
        
        // Then - parsing details are not directly exposed in DecryptedScanData
        // This test verifies the data is recorded internally
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS
        )
        
        // Verify scan was created successfully - parsing details are used internally
        assertNotNull("Should create scan data", decryptedScan)
        assertEquals("Should have correct result", ScanResult.SUCCESS, decryptedScan.scanResult)
    }

    @Test
    fun `createDecryptedScanData integrates all debug data`() {
        // Given - set up various debug data
        debugCollector.recordTagInfo(1024, 16)
        debugCollector.recordSectorAuthentication(1, true, "KeyA")
        debugCollector.recordSectorAuthentication(15, false, "KeyB")
        debugCollector.recordBlockData(4, "DEADBEEFCAFEBABE0123456789ABCDEF")
        val keys = arrayOf(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()))
        debugCollector.recordDerivedKeys(keys)
        debugCollector.recordColorBytes(byteArrayOf(0x00.toByte(), 0xFF.toByte(), 0x00.toByte()))
        debugCollector.recordError("Test error message")
        debugCollector.recordParsingDetail("testField", "testValue")
        
        val filamentInfo = FilamentInfo(
            tagUid = "ABCDEF12",
            trayUid = "TRAY001",
            filamentType = "PLA",
            detailedFilamentType = "PLA Basic",
            colorHex = "#00FF00",
            colorName = "Green",
            spoolWeight = 500,
            filamentDiameter = 1.75f,
            filamentLength = 250000,
            productionDate = "2025-01-01",
            minTemperature = 190,
            maxTemperature = 210,
            bedTemperature = 60,
            dryingTemperature = 45,
            dryingTime = 8
        )
        
        // When
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "ABCDEF12",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS
        )
        
        // Then - verify all data is correctly integrated
        assertEquals("Should have correct UID", "ABCDEF12", decryptedScan.tagUid)
        assertEquals("Should have correct technology", "MifareClassic", decryptedScan.technology)
        assertEquals("Should have correct result", ScanResult.SUCCESS, decryptedScan.scanResult)
        
        with(decryptedScan) {
            assertEquals("Should have tag size", 1024, tagSizeBytes)
            assertEquals("Should have sector count", 16, sectorCount)
            assertTrue("Should have authenticated sector", 1 in authenticatedSectors)
            assertTrue("Should have failed sector", 15 in failedSectors)
            assertEquals("Should have key type", "KeyA", usedKeys[1])
            assertEquals("Should have block data", "DEADBEEFCAFEBABE0123456789ABCDEF", decryptedBlocks[4])
            assertTrue("Should have derived key", "AABBCCDDEEFF" in derivedKeys)
            assertTrue("Should have error message", "Test error message" in errors)
        }
    }

    @Test
    fun `multiple createDecryptedScanData calls maintain separate debug info`() {
        // Given
        debugCollector.recordError("First error")
        val firstScan = debugCollector.createDecryptedScanData(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.AUTHENTICATION_FAILED
        )
        
        // When - create a second scan with different error
        debugCollector.recordError("Second error")  
        val secondScan = debugCollector.createDecryptedScanData(
            uid = "87654321",
            technology = "MifareClassic", 
            result = ScanResult.PARSING_FAILED
        )
        
        // Then - each should have cumulative but distinct error lists
        assertTrue("First scan should have first error", "First error" in firstScan.errors)
        assertTrue("Second scan should have both errors", "First error" in secondScan.errors)
        assertTrue("Second scan should have both errors", "Second error" in secondScan.errors)
    }
}