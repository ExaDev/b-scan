package com.bscan.integration

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.bscan.repository.*
import com.bscan.interpreter.InterpreterFactory
import kotlinx.coroutines.runBlocking
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
 * Integration test to verify that unrelated RFID tags don't get incorrectly grouped together
 * as related inventory items. This test would have caught the tray UID extraction bug.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class InventoryGroupingIntegrationTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var scanHistoryRepository: ScanHistoryRepository
    private lateinit var unifiedDataAccess: UnifiedDataAccess
    private lateinit var interpreterFactory: InterpreterFactory
    private lateinit var catalogRepository: CatalogRepository
    private lateinit var userDataRepository: UserDataRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences chains for all repositories
        `when`(mockContext.getSharedPreferences("scan_history_v2", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("catalog_data", Context.MODE_PRIVATE))
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
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        
        // Mock Assets for CatalogRepository
        val mockAssetManager = mock(android.content.res.AssetManager::class.java)
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        
        // Mock empty catalog asset to prevent loading issues
        val emptyCatalog = "{\"version\": 1, \"manufacturers\": {}}"
        `when`(mockAssetManager.open("catalog_data.json")).thenReturn(java.io.ByteArrayInputStream(emptyCatalog.toByteArray()))
        
        // Mock data retrieval for all keys used by the repository chain
        `when`(mockSharedPreferences.getString("encrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("decrypted_scans", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("catalog_data", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("user_data", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("user_data_v1", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("components", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString("component_measurements", null)).thenReturn(null)
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn("{}")
        `when`(mockSharedPreferences.contains(anyString())).thenReturn(false)
        
        // Setup repositories
        catalogRepository = CatalogRepository(mockContext)
        userDataRepository = UserDataRepository(mockContext)
        unifiedDataAccess = UnifiedDataAccess(catalogRepository, userDataRepository)
        scanHistoryRepository = ScanHistoryRepository(mockContext)
        interpreterFactory = InterpreterFactory(unifiedDataAccess)
    }

    @Test
    fun `unrelated tags should create separate inventory items`() {
        // Given - three different tags with different tray UIDs (simulating the bug scenario)
        val tag1 = createMockInterpretedScan("45FF5A04", "TRAY001", "PLA", "Red")
        val tag2 = createMockInterpretedScan("658127A5", "TRAY002", "PETG", "Blue") 
        val tag3 = createMockInterpretedScan("17F3F42B", "TRAY003", "ABS", "Green")
        
        // When - store scans in repository using new method
        runBlocking {
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag1.uid), createMockDecryptedScan(tag1.uid, "TRAY001"))
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag2.uid), createMockDecryptedScan(tag2.uid, "TRAY002"))
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag3.uid), createMockDecryptedScan(tag3.uid, "TRAY003"))
        }
        
        // Then - verify scans were saved correctly (3 separate scan interpretations)
        val allScans = scanHistoryRepository.getAllScans()
        
        assertEquals("Should have 3 separate scan interpretations", 3, allScans.size)
        
        // Verify each scan has unique tray UID and tag UID
        val trayUids = allScans.map { it.filamentInfo?.trayUid }.distinct()
        val tagUids = allScans.map { it.filamentInfo?.tagUid }.distinct()
        
        assertEquals("Should have 3 unique tray UIDs", 3, trayUids.size)
        assertEquals("Should have 3 unique tag UIDs", 3, tagUids.size)
        
        // Verify specific tray-tag relationships
        val scan1 = allScans.find { it.filamentInfo?.tagUid == "45FF5A04" }
        val scan2 = allScans.find { it.filamentInfo?.tagUid == "658127A5" }
        val scan3 = allScans.find { it.filamentInfo?.tagUid == "17F3F42B" }
        
        assertNotNull("Tag 45FF5A04 scan should exist", scan1)
        assertNotNull("Tag 658127A5 scan should exist", scan2)
        assertNotNull("Tag 17F3F42B scan should exist", scan3)
        
        assertEquals("Tag 45FF5A04 should link to TRAY001", "TRAY001", scan1?.filamentInfo?.trayUid)
        assertEquals("Tag 658127A5 should link to TRAY002", "TRAY002", scan2?.filamentInfo?.trayUid)
        assertEquals("Tag 17F3F42B should link to TRAY003", "TRAY003", scan3?.filamentInfo?.trayUid)
    }

    @Test
    fun `related tags should be grouped under same inventory item`() {
        // Given - two tags with the SAME tray UID (legitimate Bambu Lab spool scenario)
        val tag1 = createMockInterpretedScan("45FF5A04", "TRAY001", "PLA", "Red")
        val tag2 = createMockInterpretedScan("45FF5A05", "TRAY001", "PLA", "Red") // Same tray, different tag
        
        // When - store scans in repository
        runBlocking {
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag1.uid), createMockDecryptedScan(tag1.uid, "TRAY001"))
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag2.uid), createMockDecryptedScan(tag2.uid, "TRAY001"))
        }
        
        // Then - should have 2 scans with same tray UID (related tags)
        val allScans = scanHistoryRepository.getAllScans()
        
        assertEquals("Should have 2 scan interpretations", 2, allScans.size)
        
        // Both scans should have same tray UID but different tag UIDs
        val trayUids = allScans.map { it.filamentInfo?.trayUid }.distinct()
        val tagUids = allScans.map { it.filamentInfo?.tagUid }.distinct()
        
        assertEquals("Should have 1 unique tray UID", 1, trayUids.size)
        assertEquals("Should have 2 unique tag UIDs", 2, tagUids.size)
        assertEquals("Both should belong to TRAY001", "TRAY001", trayUids.first())
        
        // Verify both tags are properly linked
        assertTrue("Should contain tag 45FF5A04", tagUids.contains("45FF5A04"))
        assertTrue("Should contain tag 45FF5A05", tagUids.contains("45FF5A05"))
    }

    @Test
    fun `tags with corrupted tray UIDs should not be grouped together`() {
        // Given - simulate the original bug where hex extraction caused same tray UID
        val corruptedTrayUid = "00000000000008787876900000000000000" // The hex value from the screenshot
        
        // These should be separate tags but the bug made them appear to have the same tray UID
        val tag1 = createMockInterpretedScan("45FF5A04", "TRAY001", "PLA", "Red")
        val tag2 = createMockInterpretedScan("658127A5", "TRAY002", "PETG", "Blue") 
        val tag3 = createMockInterpretedScan("17F3F42B", "TRAY003", "ABS", "Green")
        
        // When - store scans with properly extracted tray UIDs (post-fix)
        runBlocking {
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag1.uid), createMockDecryptedScan(tag1.uid, "TRAY001"))
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag2.uid), createMockDecryptedScan(tag2.uid, "TRAY002"))
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag3.uid), createMockDecryptedScan(tag3.uid, "TRAY003"))
        }
        
        // Then - should have 3 separate scan interpretations (fix working correctly)
        val allScans = scanHistoryRepository.getAllScans()
        assertEquals("Should have 3 separate scan interpretations after fix", 3, allScans.size)
        
        // All should have proper tray UIDs, not the corrupted hex value
        val trayUids = allScans.mapNotNull { it.filamentInfo?.trayUid }
        
        assertEquals("Should have 3 tray UIDs", 3, trayUids.size)
        assertTrue("Should contain TRAY001", trayUids.contains("TRAY001"))
        assertTrue("Should contain TRAY002", trayUids.contains("TRAY002"))
        assertTrue("Should contain TRAY003", trayUids.contains("TRAY003"))
        
        // None should have the corrupted hex tray UID
        trayUids.forEach { trayUid ->
            assertNotEquals("No scan should have corrupted hex tray UID", 
                corruptedTrayUid, trayUid)
            assertTrue("Tray UID should be human-readable text", 
                trayUid.startsWith("TRAY"))
        }
    }

    // Helper methods
    private fun createMockInterpretedScan(
        tagUid: String,
        trayUid: String, 
        material: String,
        color: String
    ): InterpretedScan {
        val filamentInfo = FilamentInfo(
            tagUid = tagUid,
            trayUid = trayUid,
            tagFormat = TagFormat.BAMBU_PROPRIETARY,
            manufacturerName = "Bambu Lab",
            filamentType = material,
            detailedFilamentType = material,
            colorHex = when(color) {
                "Red" -> "#FF0000"
                "Blue" -> "#0000FF"
                "Green" -> "#00FF00"
                else -> "#808080"
            },
            colorName = color,
            spoolWeight = 1000,
            filamentDiameter = 1.75f,
            filamentLength = 330000,
            productionDate = "2024-08-01",
            minTemperature = 190,
            maxTemperature = 220,
            bedTemperature = 60,
            dryingTemperature = 40,
            dryingTime = 8
        )
        
        val encryptedScan = createMockEncryptedScan(tagUid)
        val decryptedScan = createMockDecryptedScan(tagUid, trayUid)
        
        return InterpretedScan(
            encryptedData = encryptedScan,
            decryptedData = decryptedScan,
            filamentInfo = filamentInfo
        )
    }
    
    private fun createMockEncryptedScan(tagUid: String): EncryptedScanData {
        return EncryptedScanData(
            id = tagUid.hashCode().toLong(),
            timestamp = LocalDateTime.now(),
            tagUid = tagUid,
            technology = "MifareClassic",
            encryptedData = ByteArray(1024) { 0x00 }
        )
    }
    
    private fun createMockDecryptedScan(tagUid: String, trayUid: String): DecryptedScanData {
        // Create block 9 with proper UTF-8 encoding of tray UID
        val trayUidBytes = trayUid.toByteArray(Charsets.UTF_8)
        val paddedBytes = ByteArray(16)
        System.arraycopy(trayUidBytes, 0, paddedBytes, 0, trayUidBytes.size)
        val block9Hex = paddedBytes.joinToString("") { "%02X".format(it) }
        
        // Create a complete 16-sector structure for proper interpreter validation
        val baseBlocks = mapOf(
            1 to "4130302D4B30004746413030", // Valid RFID codes
            2 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA
            4 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA
            5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000", // Color
            6 to "00C80064001E001E00000000000000000000000000000000000000000000000000", // Temps
            9 to block9Hex // Tray UID as UTF-8
        )
        
        val completeBlocks = baseBlocks.toMutableMap()
        
        // Add dummy blocks to create proper 16-sector structure
        for (sector in 0..15) {
            for (blockInSector in 0..2) { // Only data blocks (skip sector trailers)
                val blockNumber = sector * 4 + blockInSector
                if (blockNumber !in completeBlocks) {
                    completeBlocks[blockNumber] = "00".repeat(16) // 16 bytes of zeros
                }
            }
        }
        
        return DecryptedScanData(
            id = tagUid.hashCode().toLong(),
            timestamp = LocalDateTime.now(),
            tagUid = tagUid,
            technology = "MifareClassic",
            scanResult = ScanResult.SUCCESS,
            decryptedBlocks = completeBlocks,
            authenticatedSectors = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            failedSectors = emptyList(),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyA"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            errors = emptyList()
        )
    }
}