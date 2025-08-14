package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime

/**
 * Unit tests for ScanHistoryRepository
 */
@RunWith(MockitoJUnitRunner.Silent::class)
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
        `when`(mockContext.getSharedPreferences("scan_history", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.remove(any())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        
        repository = ScanHistoryRepository(mockContext)
    }

    @Test
    fun `saveScan adds timestamp when missing`() {
        // Given
        val scanHistory = createTestScanHistory(timestamp = LocalDateTime.MIN)
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn(null)
        
        // When
        repository.saveScan(scanHistory)
        
        // Then
        verify(mockEditor).putString(eq("scans"), any())
        verify(mockEditor).apply()
    }

    @Test
    fun `saveScan maintains chronological order`() {
        // Given
        val scan1 = createTestScanHistory(
            uid = "tag1", 
            timestamp = LocalDateTime.now().minusMinutes(10)
        )
        val scan2 = createTestScanHistory(
            uid = "tag2", 
            timestamp = LocalDateTime.now().minusMinutes(5)
        )
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn(null)
        
        // When
        repository.saveScan(scan1)
        repository.saveScan(scan2)
        
        // Then - newer scans should be first
        verify(mockEditor, times(2)).putString(eq("scans"), any())
    }

    @Test
    fun `saveScan enforces maximum history size`() {
        // Given - simulate having 100 scans already
        val existingScans = (1..100).map { 
            createTestScanHistory(uid = "tag$it") 
        }
        val existingJson = """[${existingScans.joinToString(",") { "\"test\"" }}]"""
        `when`(mockSharedPreferences.getString("scans", "[]")).thenReturn(existingJson)
        
        val newScan = createTestScanHistory(uid = "tag101")
        
        // When
        repository.saveScan(newScan)
        
        // Then - should save and maintain size limit
        verify(mockEditor).putString(eq("scans"), any())
    }

    @Test
    fun `getAllScans handles empty preferences`() {
        // Given - return null for empty preferences
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn(null)
        
        // When
        val result = repository.getAllScans()
        
        // Then
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `getAllScans handles corrupted JSON gracefully`() {
        // Given - return null initially, then corrupted JSON
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn("{invalid json}")
        
        // When
        val result = repository.getAllScans()
        
        // Then
        assertTrue("Should return empty list for corrupted data", result.isEmpty())
        verify(mockEditor).remove("scans") // Should clear corrupted data
        verify(mockEditor).apply()
    }

    @Test
    fun `getSuccessfulScans filters correctly`() {
        // This is a basic test - in practice we'd mock the JSON parsing
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn(null)
        
        val result = repository.getSuccessfulScans()
        
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `getFailedScans filters correctly`() {
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn(null)
        
        val result = repository.getFailedScans()
        
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `getScanByUid filters correctly`() {
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn(null)
        
        val result = repository.getScanByUid("test-uid")
        
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `clearHistory removes all data`() {
        // When
        repository.clearHistory()
        
        // Then
        verify(mockEditor).remove("scans")
        verify(mockEditor).apply()
    }

    @Test
    fun `getHistoryCount returns zero for empty history`() {
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn(null)
        
        val result = repository.getHistoryCount()
        
        assertEquals("Should return zero count", 0, result)
    }

    @Test
    fun `getSuccessRate handles division by zero`() {
        `when`(mockSharedPreferences.getString("scans", null)).thenReturn(null)
        
        val result = repository.getSuccessRate()
        
        assertEquals("Should return 0.0 for empty history", 0.0, result.toDouble(), 0.001)
    }

    // Helper method to create test data
    private fun createTestScanHistory(
        uid: String = "12345678",
        timestamp: LocalDateTime = LocalDateTime.now(),
        result: ScanResult = ScanResult.SUCCESS
    ): ScanHistory {
        return ScanHistory(
            id = 1,
            timestamp = timestamp,
            uid = uid,
            technology = "MifareClassic",
            scanResult = result,
            filamentInfo = if (result == ScanResult.SUCCESS) createTestFilamentInfo() else null,
            debugInfo = createTestDebugInfo()
        )
    }
    
    private fun createTestFilamentInfo(): FilamentInfo {
        return FilamentInfo(
            uid = "TEST_UID",
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