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
        
        // Then - we can't directly assert internal state, but we can test through createScanHistory
        val scanHistory = debugCollector.createScanHistory(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS,
            filamentInfo = null
        )
        
        assertEquals("Should record correct tag size", 1024, scanHistory.debugInfo.tagSizeBytes)
        assertEquals("Should record correct sector count", 16, scanHistory.debugInfo.sectorCount)
    }

    @Test
    fun `recordSectorAuthentication stores authentication data`() {
        // When
        debugCollector.recordTagInfo(1024, 16)
        debugCollector.recordSectorAuthentication(1, true, "KeyA")
        debugCollector.recordSectorAuthentication(2, false, "KeyB")
        
        // Then
        val scanHistory = debugCollector.createScanHistory(
            uid = "12345678",
            technology = "MifareClassic", 
            result = ScanResult.SUCCESS,
            filamentInfo = null
        )
        
        assertEquals("Should record sector count", 16, scanHistory.debugInfo.sectorCount)
        assertTrue("Should record authenticated sector", 1 in scanHistory.debugInfo.authenticatedSectors)
        assertTrue("Should record failed sector", 2 in scanHistory.debugInfo.failedSectors)
        assertEquals("Should record key type for authenticated sector", "KeyA", scanHistory.debugInfo.usedKeyTypes[1])
    }

    @Test
    fun `recordBlockData stores hex data correctly`() {
        // Given
        val testBlockData = "00112233445566778899AABBCCDDEEFF"
        
        // When
        debugCollector.recordBlockData(4, testBlockData)
        
        // Then
        val scanHistory = debugCollector.createScanHistory(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS, 
            filamentInfo = null
        )
        
        assertEquals("Should store block data correctly", testBlockData, scanHistory.debugInfo.blockData[4])
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
        val scanHistory = debugCollector.createScanHistory(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS,
            filamentInfo = null
        )
        
        assertTrue("Should contain first key", "AABBCCDDEEFF" in scanHistory.debugInfo.derivedKeys)
        assertTrue("Should contain second key", "112233445566" in scanHistory.debugInfo.derivedKeys)
        assertEquals("Should have both keys", 2, scanHistory.debugInfo.derivedKeys.size)
    }

    @Test
    fun `recordColorBytes stores raw color data`() {
        // Given
        val colorBytes = byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x00.toByte())
        
        // When  
        debugCollector.recordColorBytes(colorBytes)
        
        // Then
        val scanHistory = debugCollector.createScanHistory(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS,
            filamentInfo = null
        )
        
        assertEquals("Should store raw color bytes", "FF0000", scanHistory.debugInfo.rawColorBytes)
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
        val scanHistory = debugCollector.createScanHistory(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.AUTHENTICATION_FAILED,
            filamentInfo = null
        )
        
        assertTrue("Should contain first error", error1 in scanHistory.debugInfo.errorMessages)
        assertTrue("Should contain second error", error2 in scanHistory.debugInfo.errorMessages)
        assertEquals("Should have both errors", 2, scanHistory.debugInfo.errorMessages.size)
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
        
        // Then
        val scanHistory = debugCollector.createScanHistory(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS,
            filamentInfo = null
        )
        
        details.forEach { (key, value) ->
            assertEquals("Should store parsing detail: $key", value, scanHistory.debugInfo.parsingDetails[key])
        }
    }

    @Test
    fun `createScanHistory integrates all debug data`() {
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
        val scanHistory = debugCollector.createScanHistory(
            uid = "ABCDEF12",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS,
            filamentInfo = filamentInfo
        )
        
        // Then - verify all data is correctly integrated
        assertEquals("Should have correct UID", "ABCDEF12", scanHistory.uid)
        assertEquals("Should have correct technology", "MifareClassic", scanHistory.technology)
        assertEquals("Should have correct result", ScanResult.SUCCESS, scanHistory.scanResult)
        assertEquals("Should include filament info", filamentInfo, scanHistory.filamentInfo)
        
        with(scanHistory.debugInfo) {
            assertEquals("Should have tag size", 1024, tagSizeBytes)
            assertEquals("Should have sector count", 16, sectorCount)
            assertTrue("Should have authenticated sector", 1 in authenticatedSectors)
            assertTrue("Should have failed sector", 15 in failedSectors)
            assertEquals("Should have key type", "KeyA", usedKeyTypes[1])
            assertEquals("Should have block data", "DEADBEEFCAFEBABE0123456789ABCDEF", blockData[4])
            assertTrue("Should have derived key", "AABBCCDDEEFF" in derivedKeys)
            assertEquals("Should have color bytes", "00FF00", rawColorBytes)
            assertTrue("Should have error message", "Test error message" in errorMessages)
            assertEquals("Should have parsing detail", "testValue", parsingDetails["testField"])
        }
    }

    @Test
    fun `multiple createScanHistory calls maintain separate debug info`() {
        // Given
        debugCollector.recordError("First error")
        val firstScan = debugCollector.createScanHistory(
            uid = "12345678",
            technology = "MifareClassic",
            result = ScanResult.AUTHENTICATION_FAILED,
            filamentInfo = null
        )
        
        // When - create a second scan with different error
        debugCollector.recordError("Second error")  
        val secondScan = debugCollector.createScanHistory(
            uid = "87654321",
            technology = "MifareClassic", 
            result = ScanResult.PARSING_FAILED,
            filamentInfo = null
        )
        
        // Then - each should have cumulative but distinct error lists
        assertTrue("First scan should have first error", "First error" in firstScan.debugInfo.errorMessages)
        assertTrue("Second scan should have both errors", "First error" in secondScan.debugInfo.errorMessages)
        assertTrue("Second scan should have both errors", "Second error" in secondScan.debugInfo.errorMessages)
    }
}