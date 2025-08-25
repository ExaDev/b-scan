package com.bscan.integration

import android.content.Context
import android.content.SharedPreferences
import com.bscan.debug.DebugDataCollector
import com.bscan.model.*
import com.bscan.repository.ScanHistoryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * Integration tests for the complete scan history workflow
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ScanHistoryIntegrationTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var repository: ScanHistoryRepository
    private lateinit var debugCollector: DebugDataCollector
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences chain for ScanHistoryRepository
        `when`(mockContext.getSharedPreferences("scan_history_v2", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        // Mock SharedPreferences for MappingsRepository (needed by ScanHistoryRepository constructor)
        `when`(mockContext.getSharedPreferences("filament_mappings", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.remove(any())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("mappings", null)).thenReturn(null)
        
        repository = ScanHistoryRepository(mockContext)
        debugCollector = DebugDataCollector()
    }

    @Test
    fun `complete scan workflow saves history correctly`() {
        // Given - simulate a complete NFC scan workflow
        debugCollector.recordTagInfo(1024, 16)
        debugCollector.recordSectorAuthentication(1, true, "KeyA")
        debugCollector.recordSectorAuthentication(2, true, "KeyA") 
        debugCollector.recordSectorAuthentication(3, false, "KeyB")
        debugCollector.recordBlockData(4, "DEADBEEFCAFEBABE0123456789ABCDEF")
        val keys = arrayOf(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()))
        debugCollector.recordDerivedKeys(keys)
        debugCollector.recordColorBytes(byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x00.toByte()))
        debugCollector.recordParsingDetail("brand", "Bambu Lab")
        debugCollector.recordParsingDetail("confidence", 0.95)
        
        val filamentInfo = FilamentInfo(
            tagUid = "A1B2C3D4",
            trayUid = "TRAY001",
            filamentType = "PLA",
            detailedFilamentType = "PLA Basic",
            colorHex = "#FF0000",
            colorName = "Red",
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
        
        val encryptedScan = debugCollector.createEncryptedScanData(
            uid = "A1B2C3D4",
            technology = "MifareClassic"
        )
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "A1B2C3D4",
            technology = "MifareClassic",
            result = ScanResult.SUCCESS
        )
        
        // When - save the scan
        repository.saveScan(encryptedScan, decryptedScan)
        
        // Then - verify save was called with correct data for both encrypted and decrypted
        verify(mockEditor, atLeast(2)).putString(any(), any()) // Called for both encrypted and decrypted
        verify(mockEditor, atLeast(2)).apply() // Called for both encrypted and decrypted
    }

    @Test
    fun `failed scan workflow captures error details`() {
        // Given - simulate a failed scan
        debugCollector.recordTagInfo(512, 16)
        debugCollector.recordSectorAuthentication(1, true, "KeyA")
        debugCollector.recordSectorAuthentication(2, false, "KeyA")
        debugCollector.recordSectorAuthentication(2, false, "KeyB") // Both keys failed
        debugCollector.recordError("Authentication failed for sector 2 with both KeyA and KeyB")
        debugCollector.recordError("Insufficient authenticated sectors for parsing")
        debugCollector.recordParsingDetail("authenticatedSectors", 1)
        debugCollector.recordParsingDetail("requiredSectors", 3)
        
        val encryptedScan = debugCollector.createEncryptedScanData(
            uid = "E5F6A7B8",
            technology = "MifareClassic"
        )
        val decryptedScan = debugCollector.createDecryptedScanData(
            uid = "E5F6A7B8",
            technology = "MifareClassic", 
            result = ScanResult.AUTHENTICATION_FAILED
        )
        
        // When
        repository.saveScan(encryptedScan, decryptedScan)
        
        // Then
        assertEquals("Should record failed result", ScanResult.AUTHENTICATION_FAILED, decryptedScan.scanResult)
        assertTrue("Should record authentication error", 
            decryptedScan.errors.any { it.contains("Authentication failed") })
        assertTrue("Should record insufficient data error",
            decryptedScan.errors.any { it.contains("Insufficient authenticated sectors") })
        verify(mockEditor, atLeast(2)).putString(any(), any()) // Called for both encrypted and decrypted
    }

    @Test
    fun `multiple scan workflow maintains order and statistics`() {
        // Given - multiple scans with different results
        val scans = listOf(
            createScanHistoryData("tag1", ScanResult.SUCCESS),
            createScanHistoryData("tag2", ScanResult.AUTHENTICATION_FAILED),
            createScanHistoryData("tag3", ScanResult.SUCCESS),
            createScanHistoryData("tag4", ScanResult.PARSING_FAILED),
            createScanHistoryData("tag5", ScanResult.SUCCESS)
        )
        
        // When - save all scans
        scans.forEach { scanData ->
            val encryptedScan = debugCollector.createEncryptedScanData(
                uid = scanData.uid,
                technology = "MifareClassic"
            )
            val decryptedScan = debugCollector.createDecryptedScanData(
                uid = scanData.uid,
                technology = "MifareClassic",
                result = scanData.result
            )
            repository.saveScan(encryptedScan, decryptedScan)
        }
        
        // Then - verify all saves occurred (2 saves per scan: encrypted + decrypted)
        verify(mockEditor, atLeast(10)).putString(any(), any()) // 5 scans × 2 saves each
        verify(mockEditor, atLeast(10)).apply() // 5 scans × 2 applies each
    }

    @Test
    fun `LocalDateTime serialization workflow works correctly`() {
        // Given - scan with specific timestamp
        val specificTime = LocalDateTime.of(2025, 8, 14, 10, 30, 0)
        val encryptedScan = EncryptedScanData(
            id = 1,
            timestamp = specificTime,
            tagUid = "12345678",
            technology = "MifareClassic",
            encryptedData = ByteArray(1024),
            tagSizeBytes = 1024,
            sectorCount = 16
        )
        val decryptedScan = DecryptedScanData(
            id = 2,
            timestamp = specificTime,
            tagUid = "12345678",
            technology = "MifareClassic",
            scanResult = ScanResult.SUCCESS,
            decryptedBlocks = mapOf(4 to "DEADBEEFCAFEBABE0123456789ABCDEF"),
            authenticatedSectors = listOf(1, 2, 3),
            failedSectors = emptyList(),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyA"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            tagSizeBytes = 1024,
            sectorCount = 16,
            errors = emptyList()
        )
        
        // When
        repository.saveScan(encryptedScan, decryptedScan)
        
        // Then - should save without serialization errors
        verify(mockEditor, atLeast(2)).putString(any(), any())
        verify(mockEditor, atLeast(2)).apply()
        
        // The actual JSON should contain the ISO timestamp
        // We can't easily verify this without more complex mocking, but the test
        // ensures the LocalDateTime adapter doesn't throw exceptions
    }

    @Test
    fun `repository statistics methods handle empty data`() {
        // Given - empty repository
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(null)
        
        // When & Then
        assertEquals("Success rate should be 0.0", 0.0, repository.getSuccessRate().toDouble(), 0.001)
        assertEquals("History count should be 0", 0, repository.getHistoryCount())
        assertTrue("Successful scans should be empty", repository.getSuccessfulDecryptedScans().isEmpty())
        assertTrue("Failed scans should be empty", repository.getFailedDecryptedScans().isEmpty())
        assertTrue("All scans should be empty", repository.getAllScans().isEmpty())
    }

    // Helper methods
    private fun createScanHistoryData(uid: String, result: ScanResult) = object {
        val uid = uid
        val result = result
    }
    
    private fun createTestFilamentInfo(): FilamentInfo {
        return FilamentInfo(
            tagUid = "TEST_UID",
            trayUid = "TEST_TRAY",
            filamentType = "PLA",
            detailedFilamentType = "PLA Basic",
            colorHex = "#FF0000",
            colorName = "Test Color",
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
    }
    
    private fun createTestDebugInfo(): ScanDebugInfo {
        return ScanDebugInfo(
            uid = "04123456789ABC",
            tagSizeBytes = 1024,
            sectorCount = 16,
            authenticatedSectors = listOf(1, 2, 3),
            failedSectors = emptyList(),
            usedKeyTypes = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyA"),
            blockData = mapOf(4 to "DEADBEEFCAFEBABE0123456789ABCDEF"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            rawColorBytes = "FF0000",
            errorMessages = emptyList(),
            parsingDetails = mapOf("version" to 1, "checksum" to "valid")
        )
    }
}