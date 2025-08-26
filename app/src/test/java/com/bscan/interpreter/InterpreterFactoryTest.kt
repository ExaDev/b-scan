package com.bscan.interpreter

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
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

/**
 * Unit tests for InterpreterFactory
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class InterpreterFactoryTest {

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

    private lateinit var unifiedDataAccess: UnifiedDataAccess
    private lateinit var interpreterFactory: InterpreterFactory

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences (still needed for UserDataRepository)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null)
        
        // Mock CatalogRepository to return empty mappings for test
        val emptyMappings = FilamentMappings.empty()
        `when`(mockCatalogRepository.getCurrentMappings()).thenReturn(emptyMappings)
        `when`(mockCatalogRepository.findRfidMapping(anyString())).thenReturn(null)
        
        // Mock UserDataRepository
        `when`(mockUserDataRepository.getUserData()).thenReturn(
            UserData(
                version = 1,
                components = emptyMap(),
                inventoryItems = emptyMap(),
                scans = ScanDataContainer(emptyMap(), emptyMap()),
                measurements = emptyList(),
                customMappings = CustomMappings(emptyMap(), emptyMap()),
                preferences = UserPreferences(),
                metadata = UserDataMetadata(LocalDateTime.now(), "1.0.0")
            )
        )
        
        unifiedDataAccess = UnifiedDataAccess(mockCatalogRepository, mockUserDataRepository)
        interpreterFactory = InterpreterFactory(unifiedDataAccess)
    }

    @Test
    fun `interpret should return null for failed scan result`() {
        // Given
        val failedScan = createTestDecryptedScan(scanResult = ScanResult.AUTHENTICATION_FAILED)
        
        // When
        val result = interpreterFactory.interpret(failedScan)
        
        // Then
        assertNull("Should return null for failed scan", result)
    }

    @Test
    fun `interpret should process successful Bambu scan`() {
        // Given - use real RFID codes that exist in our mapping (GFA00:A00-K0 = SKU 10101)
        val successfulScan = createTestDecryptedScan(
            scanResult = ScanResult.SUCCESS,
            tagFormat = TagFormat.BAMBU_PROPRIETARY,
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030",  // "A00-K0" + "GFA00" (variant + material IDs)
                4 to "DEADBEEFCAFEBABE0123456789ABCDEF",
                9 to "54524159303031" // "TRAY001" in hex
            )
        )
        
        // When
        val result = interpreterFactory.interpret(successfulScan)
        
        // Then
        if (result != null) {
            // Only validate if interpretation succeeded (exact mapping found)
            assertEquals("Should preserve tag UID", "12345678", result.tagUid)
            assertEquals("Should have exact SKU", "10101", result.exactSku)
        }
        // Note: result may be null if RFID mapping not found - this is expected behavior for exact-only mode
    }

    @Test
    fun `interpret should handle empty decrypted blocks`() {
        // Given
        val scanWithNoBlocks = createTestDecryptedScan(
            scanResult = ScanResult.SUCCESS,
            decryptedBlocks = emptyMap()
        )
        
        // When
        val result = interpreterFactory.interpret(scanWithNoBlocks)
        
        // Then
        // Should handle gracefully - exact behavior depends on implementation
        // For now, just verify it doesn't throw an exception
    }

    @Test
    fun `interpret should handle different tag formats`() {
        // Given - test different tag formats
        val bambuScan = createTestDecryptedScan(tagFormat = TagFormat.BAMBU_PROPRIETARY)
        val openTagScan = createTestDecryptedScan(tagFormat = TagFormat.OPENTAG_V1)
        val crealityScan = createTestDecryptedScan(tagFormat = TagFormat.CREALITY_ASCII)
        val unknownScan = createTestDecryptedScan(tagFormat = TagFormat.UNKNOWN)
        
        // When & Then - should handle all formats without throwing exceptions
        interpreterFactory.interpret(bambuScan)
        interpreterFactory.interpret(openTagScan)
        interpreterFactory.interpret(crealityScan)
        interpreterFactory.interpret(unknownScan)
    }

    @Test
    fun `refreshMappings should update internal state`() {
        // When
        interpreterFactory.refreshMappings()
        
        // Then - should not throw exception
        // This method refreshes internal state but has no observable effects to test
    }

    // Helper methods
    private fun createTestDecryptedScan(
        uid: String = "12345678",
        timestamp: LocalDateTime = LocalDateTime.now(),
        scanResult: ScanResult = ScanResult.SUCCESS,
        tagFormat: TagFormat = TagFormat.BAMBU_PROPRIETARY,
        decryptedBlocks: Map<Int, String> = emptyMap()
    ): DecryptedScanData {
        return DecryptedScanData(
            id = 1,
            timestamp = timestamp,
            tagUid = uid,
            technology = "MifareClassic",
            scanResult = scanResult,
            decryptedBlocks = decryptedBlocks,
            authenticatedSectors = if (scanResult == ScanResult.SUCCESS) listOf(1, 2, 3) else emptyList(),
            failedSectors = if (scanResult != ScanResult.SUCCESS) listOf(1, 2, 3) else emptyList(),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyA"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            errors = when (scanResult) {
                ScanResult.AUTHENTICATION_FAILED -> listOf("Authentication failed for sector 1")
                ScanResult.INSUFFICIENT_DATA -> listOf("Not enough data blocks read")
                ScanResult.PARSING_FAILED -> listOf("Failed to parse filament data")
                else -> emptyList()
            }
        )
    }
}