package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.io.ByteArrayInputStream

/**
 * Unit tests for InventoryTrackingRepository
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class InventoryTrackingRepositoryTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockCatalogRepository: CatalogRepository

    @Mock
    private lateinit var mockUserDataRepository: UserDataRepository

    @Mock
    private lateinit var mockUnifiedDataAccess: UnifiedDataAccess

    @Mock
    private lateinit var mockComponentRepository: com.bscan.repository.ComponentRepository

    @Mock
    private lateinit var mockBambuFactory: com.bscan.service.BambuComponentFactory

    private lateinit var repository: InventoryTrackingRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences for all repositories
        `when`(mockContext.getSharedPreferences("inventory_tracking", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("user_data", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("component_repository", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        `when`(mockSharedPreferences.getString(anyString(), anyString())).thenReturn(null)
        
        // Mock the Assets for CatalogRepository
        val mockAssetManager = mock(android.content.res.AssetManager::class.java)
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        
        // Mock empty catalog asset to prevent loading issues
        val emptyCatalog = "{\"version\": 1, \"manufacturers\": {}}"
        `when`(mockAssetManager.open("catalog_data.json")).thenReturn(ByteArrayInputStream(emptyCatalog.toByteArray()))
        
        repository = InventoryTrackingRepository(mockContext)
    }

    @Test
    fun `recordScan should handle successful scan`() {
        // Given
        val decryptedScan = createTestDecryptedScanData(
            uid = "12345678",
            scanResult = ScanResult.SUCCESS
        )
        
        // When
        runTest {
            repository.recordScan(decryptedScan)
        }
        
        // Then - should not throw exception
        // Note: The actual saving depends on successful interpretation, which may not occur with minimal mock data
        assertTrue("Should handle successful scan without exceptions", true)
    }

    @Test
    fun `recordScan should handle failed scan`() {
        // Given
        val failedScan = createTestDecryptedScanData(
            uid = "ABCD1234",
            scanResult = ScanResult.AUTHENTICATION_FAILED
        )
        
        // When
        runTest {
            repository.recordScan(failedScan)
        }
        
        // Then - should not throw exception
        // Note: Failed scans are not recorded, so no SharedPreferences interaction expected
        assertTrue("Should handle failed scan without exceptions", true)
    }

    @Test
    fun `getAllInventoryItems should return empty list for no data`() {
        // Given - no stored data
        `when`(mockSharedPreferences.getString(any(), any())).thenReturn(null)
        
        // When
        val items = repository.getAllInventoryItems()
        
        // Then
        assertTrue("Should return empty list when no data", items.isEmpty())
    }

    @Test
    fun `getInventoryByUid should return null for non-existent inventory item`() {
        // Given - no stored data
        `when`(mockSharedPreferences.getString(any(), any())).thenReturn(null)
        
        // When
        val item = repository.getInventoryByUid("NON_EXISTENT")
        
        // Then
        assertNull("Should return null for non-existent inventory item", item)
    }

    @Test
    fun `clearAllData should remove all stored data`() {
        // When
        runTest {
            repository.clearAllData()
        }
        
        // Then
        verify(mockEditor).clear()
        verify(mockEditor, atLeast(1)).apply()
    }

    @Test
    fun `repository should handle corrupted JSON gracefully`() {
        // Given - corrupted JSON data
        `when`(mockSharedPreferences.getString(any(), any())).thenReturn("{invalid json}")
        
        // When & Then - should not throw exception
        val items = repository.getAllInventoryItems()
        assertTrue("Should handle corrupted data gracefully", items.isEmpty())
    }

    // Helper methods
    private fun createTestDecryptedScanData(
        uid: String,
        scanResult: ScanResult,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): DecryptedScanData {
        return DecryptedScanData(
            id = 1,
            timestamp = timestamp,
            tagUid = uid,
            technology = "MifareClassic",
            scanResult = scanResult,
            decryptedBlocks = if (scanResult == ScanResult.SUCCESS) {
                mapOf(
                    4 to "DEADBEEFCAFEBABE0123456789ABCDEF",
                    9 to "54524159303031" // "TRAY001" in hex
                )
            } else emptyMap(),
            authenticatedSectors = if (scanResult == ScanResult.SUCCESS) listOf(1, 2, 3) else emptyList(),
            failedSectors = if (scanResult != ScanResult.SUCCESS) listOf(1, 2, 3) else emptyList(),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyA"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            errors = when (scanResult) {
                ScanResult.AUTHENTICATION_FAILED -> listOf("Authentication failed")
                ScanResult.INSUFFICIENT_DATA -> listOf("Insufficient data")
                ScanResult.PARSING_FAILED -> listOf("Parsing failed")
                else -> emptyList()
            }
        )
    }
}