package com.bscan

import com.bscan.model.*
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

/**
 * Unit tests for ScanHistory data model
 */
class ScanHistoryTest {

    @Test
    fun testScanHistoryCreation() {
        // Given test data
        val timestamp = LocalDateTime.now()
        val uid = "04:5A:B2:C1:DE:34:80"
        val filamentInfo = FilamentInfo(
            uid = uid,
            trayUid = "test-tray",
            filamentType = "PLA",
            detailedFilamentType = "Basic PLA",
            colorHex = "#FF0000",
            colorName = "Red",
            spoolWeight = 1000,
            filamentDiameter = 1.75f,
            filamentLength = 250000,
            productionDate = "2024-01-15",
            minTemperature = 190,
            maxTemperature = 210,
            bedTemperature = 60,
            dryingTemperature = 40,
            dryingTime = 6
        )
        val debugInfo = ScanDebugInfo(
            tagSizeBytes = 1024,
            sectorCount = 16,
            authenticatedSectors = listOf(1, 2, 3),
            failedSectors = listOf(4, 5),
            usedKeyTypes = mapOf(1 to "KeyA", 2 to "KeyB"),
            blockData = mapOf(5 to "FF0000FF12345678", 6 to "28003C00D2C2BE"),
            derivedKeys = listOf("A1B2C3D4E5F6", "F6E5D4C3B2A1"),
            rawColorBytes = "FF0000FF",
            errorMessages = emptyList(),
            parsingDetails = mapOf("spoolWeight" to 1000, "diameter" to 1.75f)
        )

        // When creating ScanHistory
        val scanHistory = ScanHistory(
            id = 1L,
            timestamp = timestamp,
            uid = uid,
            technology = "MIFARE_CLASSIC",
            scanResult = ScanResult.SUCCESS,
            filamentInfo = filamentInfo,
            debugInfo = debugInfo
        )

        // Then all properties should be set correctly
        assertEquals(1L, scanHistory.id)
        assertEquals(timestamp, scanHistory.timestamp)
        assertEquals(uid, scanHistory.uid)
        assertEquals("MIFARE_CLASSIC", scanHistory.technology)
        assertEquals(ScanResult.SUCCESS, scanHistory.scanResult)
        assertEquals(filamentInfo, scanHistory.filamentInfo)
        assertEquals(debugInfo, scanHistory.debugInfo)
    }

    @Test
    fun testScanDebugInfoCreation() {
        // Given test debug data
        val blockData = mapOf(
            5 to "FF0000FF12345678",
            6 to "28003C00D2C2BE"
        )

        // When creating ScanDebugInfo
        val debugInfo = ScanDebugInfo(
            tagSizeBytes = 1024,
            sectorCount = 16,
            authenticatedSectors = listOf(1, 2, 3),
            failedSectors = listOf(4, 5),
            usedKeyTypes = mapOf(1 to "KeyA", 2 to "KeyB"),
            blockData = blockData,
            derivedKeys = listOf("A1B2C3D4E5F6", "F6E5D4C3B2A1"),
            rawColorBytes = "FF0000FF",
            errorMessages = emptyList(),
            parsingDetails = mapOf("test" to "value")
        )

        // Then all properties should be set correctly
        assertEquals(1024, debugInfo.tagSizeBytes)
        assertEquals(16, debugInfo.sectorCount)
        assertEquals(listOf(1, 2, 3), debugInfo.authenticatedSectors)
        assertEquals(listOf(4, 5), debugInfo.failedSectors)
        assertEquals("FF0000FF", debugInfo.rawColorBytes)
        assertEquals(blockData, debugInfo.blockData)
        assertEquals(2, debugInfo.derivedKeys.size)
    }

    @Test
    fun testScanResultEnumValues() {
        // Test that all expected ScanResult values exist
        val expectedResults = arrayOf(
            ScanResult.SUCCESS,
            ScanResult.AUTHENTICATION_FAILED,
            ScanResult.INSUFFICIENT_DATA,
            ScanResult.PARSING_FAILED,
            ScanResult.NO_NFC_TAG,
            ScanResult.UNKNOWN_ERROR
        )

        // Verify enum has expected number of values
        assertEquals("ScanResult should have 6 values", 6, expectedResults.size)
        assertEquals("ScanResult should have 6 values", 6, ScanResult.values().size)
    }
}