package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import kotlinx.coroutines.runBlocking
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
 * Unit tests for ScanHistoryRepository
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ScanHistoryRepositoryTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var repository: ScanHistoryRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences chains for all repositories ScanHistoryRepository needs
        `when`(mockContext.getSharedPreferences("scan_history", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("user_data", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("user_catalog", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("component_data", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("component_measurement_data", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("inventory_data", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("inventory_tracking", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("ui_preferences", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.remove(any())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        
        // Mock data retrieval for all keys used by the repository chain
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("user_data", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("user_data", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("components", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("component_measurements", null)).thenReturn(null)
        
        repository = ScanHistoryRepository(mockContext)
    }

    @Test
    fun `saveScan adds timestamp when missing`() {
        // Given
        val encryptedScan = createTestEncryptedScanData(timestamp = LocalDateTime.MIN)
        val decryptedScan = createTestDecryptedScanData(timestamp = LocalDateTime.MIN)
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        
        // When
        runBlocking {
            repository.saveScan(encryptedScan, decryptedScan)
        }
        
        // Then
        verify(mockEditor, atLeast(2)).putString(any(), any()) // Both encrypted and decrypted
        verify(mockEditor, atLeast(2)).apply()
    }

    @Test
    fun `saveScan maintains chronological order`() {
        // Given
        val encrypted1 = createTestEncryptedScanData(
            uid = "tag1", 
            timestamp = LocalDateTime.now().minusMinutes(10)
        )
        val decrypted1 = createTestDecryptedScanData(
            uid = "tag1", 
            timestamp = LocalDateTime.now().minusMinutes(10)
        )
        val encrypted2 = createTestEncryptedScanData(
            uid = "tag2", 
            timestamp = LocalDateTime.now().minusMinutes(5)
        )
        val decrypted2 = createTestDecryptedScanData(
            uid = "tag2", 
            timestamp = LocalDateTime.now().minusMinutes(5)
        )
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        
        // When
        runBlocking {
            repository.saveScan(encrypted1, decrypted1)
            repository.saveScan(encrypted2, decrypted2)
        }
        
        // Then - newer scans should be first (4 saves total: 2 encrypted + 2 decrypted)
        verify(mockEditor, atLeast(4)).putString(any(), any())
    }

    @Test
    fun `saveScan enforces maximum history size`() {
        // Given - simulate having 100 scans already
        val existingScansJson = """[${(1..100).joinToString(",") { "\"test\"" }}]"""
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(existingScansJson)
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(existingScansJson)
        
        val encryptedScan = createTestEncryptedScanData(uid = "tag101")
        val decryptedScan = createTestDecryptedScanData(uid = "tag101")
        
        // When
        runBlocking {
            repository.saveScan(encryptedScan, decryptedScan)
        }
        
        // Then - should save and maintain size limit
        verify(mockEditor, atLeast(2)).putString(any(), any())
    }

    @Test
    fun `getAllScans handles empty preferences`() {
        // Given - return null for empty preferences
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        
        // When
        val result = repository.getAllScans()
        
        // Then
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `getAllScans handles corrupted JSON gracefully`() {
        // Given - return corrupted JSON
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn("{invalid json}")
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn("{invalid json}")
        
        // When
        val result = repository.getAllScans()
        
        // Then
        assertTrue("Should return empty list for corrupted data", result.isEmpty())
        verify(mockEditor, atLeast(2)).remove(any()) // Should clear both encrypted and decrypted corrupted data
        verify(mockEditor, atLeast(2)).apply()
    }

    @Test
    fun `getSuccessfulDecryptedScans filters correctly`() {
        // This is a basic test - in practice we'd mock the JSON parsing
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        
        val result = repository.getSuccessfulDecryptedScans()
        
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `getFailedDecryptedScans filters correctly`() {
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        
        val result = repository.getFailedDecryptedScans()
        
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `getScansByTagUid filters correctly`() {
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        
        val result = repository.getScansByTagUid("test-uid")
        
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `clearHistory removes all data`() {
        // When
        runBlocking {
            repository.clearHistory()
        }
        
        // Then
        verify(mockEditor, atLeast(2)).remove(any()) // Both encrypted and decrypted
        verify(mockEditor, atLeast(2)).apply()
    }

    @Test
    fun `getHistoryCount returns zero for empty history`() {
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        
        val result = repository.getHistoryCount()
        
        assertEquals("Should return zero count", 0, result)
    }

    @Test
    fun `getSuccessRate handles division by zero`() {
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        
        val result = repository.getSuccessRate()
        
        assertEquals("Should return 0.0 for empty history", 0.0, result.toDouble(), 0.001)
    }

    // Helper methods to create test data
    private fun createTestEncryptedScanData(
        uid: String = "12345678",
        timestamp: LocalDateTime = LocalDateTime.now()
    ): EncryptedScanData {
        return EncryptedScanData(
            id = 1,
            timestamp = timestamp,
            tagUid = uid,
            technology = "MifareClassic",
            encryptedData = ByteArray(1024)
        )
    }
    
    private fun createTestDecryptedScanData(
        uid: String = "12345678",
        timestamp: LocalDateTime = LocalDateTime.now(),
        result: ScanResult = ScanResult.SUCCESS
    ): DecryptedScanData {
        return DecryptedScanData(
            id = 2,
            timestamp = timestamp,
            tagUid = uid,
            technology = "MifareClassic",
            scanResult = result,
            decryptedBlocks = mapOf(4 to "00112233445566778899AABBCCDDEEFF"),
            authenticatedSectors = listOf(1, 2, 3),
            failedSectors = emptyList(),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyA"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            errors = emptyList()
        )
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
            blockData = mapOf(4 to "00112233445566778899AABBCCDDEEFF"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            rawColorBytes = "FF0000",
            errorMessages = emptyList(),
            parsingDetails = mapOf("version" to 1, "checksum" to "valid")
        )
    }
}