package com.bscan.integration

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.bscan.repository.*
import com.bscan.interpreter.InterpreterFactory
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
        
        // Mock SharedPreferences
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn("{}")
        `when`(mockSharedPreferences.contains(anyString())).thenReturn(false)
        
        // Setup repositories
        catalogRepository = CatalogRepository(mockContext)
        userDataRepository = UserDataRepository(mockContext)
        unifiedDataAccess = UnifiedDataAccess(catalogRepository, userDataRepository)
        scanHistoryRepository = ScanHistoryRepository(mockContext, unifiedDataAccess)
        interpreterFactory = InterpreterFactory(unifiedDataAccess)
    }

    @Test
    fun `unrelated tags should create separate inventory items`() {
        // Given - three different tags with different tray UIDs (simulating the bug scenario)
        val tag1 = createMockInterpretedScan("45FF5A04", "TRAY001", "PLA", "Red")
        val tag2 = createMockInterpretedScan("658127A5", "TRAY002", "PETG", "Blue") 
        val tag3 = createMockInterpretedScan("17F3F42B", "TRAY003", "ABS", "Green")
        
        // When - store scans in repository
        scanHistoryRepository.storeEncryptedScan(createMockEncryptedScan(tag1.uid))
        scanHistoryRepository.storeDecryptedScan(createMockDecryptedScan(tag1.uid, "TRAY001"))
        scanHistoryRepository.storeInterpretedScan(tag1)
        
        scanHistoryRepository.storeEncryptedScan(createMockEncryptedScan(tag2.uid))
        scanHistoryRepository.storeDecryptedScan(createMockDecryptedScan(tag2.uid, "TRAY002"))
        scanHistoryRepository.storeInterpretedScan(tag2)
        
        scanHistoryRepository.storeEncryptedScan(createMockEncryptedScan(tag3.uid))
        scanHistoryRepository.storeDecryptedScan(createMockDecryptedScan(tag3.uid, "TRAY003"))
        scanHistoryRepository.storeInterpretedScan(tag3)
        
        // Then - should have 3 separate inventory items, not 1 with 3 related tags
        val inventoryItems = scanHistoryRepository.getUniqueFilamentReels()
        
        assertEquals("Should have 3 separate inventory items", 3, inventoryItems.size)
        
        // Verify each inventory item has only its own tag
        val tray1Details = scanHistoryRepository.getFilamentReelDetails("TRAY001")
        val tray2Details = scanHistoryRepository.getFilamentReelDetails("TRAY002")
        val tray3Details = scanHistoryRepository.getFilamentReelDetails("TRAY003")
        
        assertNotNull("TRAY001 should exist", tray1Details)
        assertNotNull("TRAY002 should exist", tray2Details)
        assertNotNull("TRAY003 should exist", tray3Details)
        
        tray1Details?.let {
            assertEquals("TRAY001 should have 1 tag", 1, it.tagUids.size)
            assertTrue("TRAY001 should contain tag 45FF5A04", it.tagUids.contains("45FF5A04"))
            assertFalse("TRAY001 should NOT contain tag 658127A5", it.tagUids.contains("658127A5"))
            assertFalse("TRAY001 should NOT contain tag 17F3F42B", it.tagUids.contains("17F3F42B"))
        }
        
        tray2Details?.let {
            assertEquals("TRAY002 should have 1 tag", 1, it.tagUids.size)
            assertTrue("TRAY002 should contain tag 658127A5", it.tagUids.contains("658127A5"))
            assertFalse("TRAY002 should NOT contain tag 45FF5A04", it.tagUids.contains("45FF5A04"))
            assertFalse("TRAY002 should NOT contain tag 17F3F42B", it.tagUids.contains("17F3F42B"))
        }
        
        tray3Details?.let {
            assertEquals("TRAY003 should have 1 tag", 1, it.tagUids.size)
            assertTrue("TRAY003 should contain tag 17F3F42B", it.tagUids.contains("17F3F42B"))
            assertFalse("TRAY003 should NOT contain tag 45FF5A04", it.tagUids.contains("45FF5A04"))
            assertFalse("TRAY003 should NOT contain tag 658127A5", it.tagUids.contains("658127A5"))
        }
    }

    @Test
    fun `related tags should be grouped under same inventory item`() {
        // Given - two tags with the SAME tray UID (legitimate Bambu Lab spool scenario)
        val tag1 = createMockInterpretedScan("45FF5A04", "TRAY001", "PLA", "Red")
        val tag2 = createMockInterpretedScan("45FF5A05", "TRAY001", "PLA", "Red") // Same tray, different tag
        
        // When - store scans in repository
        scanHistoryRepository.storeEncryptedScan(createMockEncryptedScan(tag1.uid))
        scanHistoryRepository.storeDecryptedScan(createMockDecryptedScan(tag1.uid, "TRAY001"))
        scanHistoryRepository.storeInterpretedScan(tag1)
        
        scanHistoryRepository.storeEncryptedScan(createMockEncryptedScan(tag2.uid))
        scanHistoryRepository.storeDecryptedScan(createMockDecryptedScan(tag2.uid, "TRAY001"))
        scanHistoryRepository.storeInterpretedScan(tag2)
        
        // Then - should have 1 inventory item with 2 related tags
        val inventoryItems = scanHistoryRepository.getUniqueFilamentReels()
        
        assertEquals("Should have 1 inventory item", 1, inventoryItems.size)
        
        val trayDetails = scanHistoryRepository.getFilamentReelDetails("TRAY001")
        assertNotNull("TRAY001 should exist", trayDetails)
        
        trayDetails?.let {
            assertEquals("TRAY001 should have 2 tags", 2, it.tagUids.size)
            assertTrue("TRAY001 should contain tag 45FF5A04", it.tagUids.contains("45FF5A04"))
            assertTrue("TRAY001 should contain tag 45FF5A05", it.tagUids.contains("45FF5A05"))
        }
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
        scanHistoryRepository.storeEncryptedScan(createMockEncryptedScan(tag1.uid))
        scanHistoryRepository.storeDecryptedScan(createMockDecryptedScan(tag1.uid, "TRAY001"))
        scanHistoryRepository.storeInterpretedScan(tag1)
        
        scanHistoryRepository.storeEncryptedScan(createMockEncryptedScan(tag2.uid))
        scanHistoryRepository.storeDecryptedScan(createMockDecryptedScan(tag2.uid, "TRAY002"))
        scanHistoryRepository.storeInterpretedScan(tag2)
        
        scanHistoryRepository.storeEncryptedScan(createMockEncryptedScan(tag3.uid))
        scanHistoryRepository.storeDecryptedScan(createMockDecryptedScan(tag3.uid, "TRAY003"))
        scanHistoryRepository.storeInterpretedScan(tag3)
        
        // Then - should have 3 separate inventory items (fix working correctly)
        val inventoryItems = scanHistoryRepository.getUniqueFilamentReels()
        assertEquals("Should have 3 separate inventory items after fix", 3, inventoryItems.size)
        
        // None should have the corrupted hex tray UID
        inventoryItems.forEach { inventoryItem ->
            assertNotEquals("No inventory item should have corrupted hex tray UID", 
                corruptedTrayUid, inventoryItem.uid)
            assertTrue("Tray UID should be human-readable text", 
                inventoryItem.uid.startsWith("TRAY"))
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
        
        return InterpretedScan(
            id = tagUid.hashCode().toLong(),
            timestamp = LocalDateTime.now(),
            uid = tagUid,
            technology = "MifareClassic",
            scanResult = ScanResult.SUCCESS,
            filamentInfo = filamentInfo
        )
    }
    
    private fun createMockEncryptedScan(tagUid: String): EncryptedScanData {
        return EncryptedScanData(
            id = tagUid.hashCode().toLong(),
            timestamp = LocalDateTime.now(),
            tagUid = tagUid,
            technology = "MifareClassic",
            encryptedBytes = ByteArray(1024) { 0x00 }
        )
    }
    
    private fun createMockDecryptedScan(tagUid: String, trayUid: String): DecryptedScanData {
        // Create block 9 with proper UTF-8 encoding of tray UID
        val trayUidBytes = trayUid.toByteArray(Charsets.UTF_8)
        val paddedBytes = ByteArray(16)
        System.arraycopy(trayUidBytes, 0, paddedBytes, 0, trayUidBytes.size)
        val block9Hex = paddedBytes.joinToString("") { "%02X".format(it) }
        
        return DecryptedScanData(
            id = tagUid.hashCode().toLong(),
            timestamp = LocalDateTime.now(),
            tagUid = tagUid,
            technology = "MifareClassic",
            scanResult = ScanResult.SUCCESS,
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030", // Valid RFID codes
                2 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA
                4 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA
                5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000", // Color
                6 to "00C80064001E001E00000000000000000000000000000000000000000000000000", // Temps
                9 to block9Hex // Tray UID as UTF-8
            ),
            authenticatedSectors = listOf(1, 2, 3),
            failedSectors = emptyList(),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyA"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            errors = emptyList()
        )
    }
}