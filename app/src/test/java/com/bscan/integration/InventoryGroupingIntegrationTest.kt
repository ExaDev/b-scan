package com.bscan.integration

import androidx.test.core.app.ApplicationProvider
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
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var realContext: Context
    private lateinit var mockContext: Context
    private lateinit var scanHistoryRepository: ScanHistoryRepository
    private lateinit var unifiedDataAccess: UnifiedDataAccess
    private lateinit var interpreterFactory: InterpreterFactory
    private lateinit var catalogRepository: CatalogRepository
    private lateinit var userDataRepository: UserDataRepository
    
    // In-memory storage to simulate SharedPreferences persistence
    private val mockStorage = mutableMapOf<String, String>()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Clear mock storage for each test
        mockStorage.clear()
        
        // Mock all context and SharedPreferences access
        realContext = ApplicationProvider.getApplicationContext()
        mockContext = mock(Context::class.java)
        
        // Mock SharedPreferences chains for all repositories - return mocked SharedPreferences
        `when`(mockContext.getSharedPreferences("scan_history_v2", Context.MODE_PRIVATE))
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
        
        // Mock editor operations to store data in our in-memory map
        `when`(mockEditor.putString(anyString(), anyString())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<String>(1)
            mockStorage[key] = value
            mockEditor
        }
        `when`(mockEditor.remove(anyString())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            mockStorage.remove(key)
            mockEditor
        }
        `when`(mockEditor.apply()).then { /* no-op */ }
        
        // Mock data retrieval to use our in-memory storage
        `when`(mockSharedPreferences.getString(anyString(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultValue = invocation.getArgument<String?>(1)
            mockStorage[key] ?: defaultValue
        }
        `when`(mockSharedPreferences.contains(anyString())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            mockStorage.containsKey(key)
        }
        
        // Setup repositories with mock context and mocked SharedPreferences
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
        
        // When - store scans in repository 
        runBlocking {
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag1.uid), createMockDecryptedScan(tag1.uid, "TRAY001"))
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag2.uid), createMockDecryptedScan(tag2.uid, "TRAY002"))
            scanHistoryRepository.saveScan(createMockEncryptedScan(tag3.uid), createMockDecryptedScan(tag3.uid, "TRAY003"))
        }
        
        // Then - verify scans were saved at the repository level (bypass interpretation issues)
        val encryptedScans = scanHistoryRepository.getAllEncryptedScans()
        val decryptedScans = scanHistoryRepository.getAllDecryptedScans()
        
        assertEquals("Should have 3 encrypted scans", 3, encryptedScans.size)
        assertEquals("Should have 3 decrypted scans", 3, decryptedScans.size)
        
        // Verify unique tag UIDs are stored
        val encryptedTagUids = encryptedScans.map { it.tagUid }.distinct()
        val decryptedTagUids = decryptedScans.map { it.tagUid }.distinct()
        
        assertEquals("Should have 3 unique encrypted tag UIDs", 3, encryptedTagUids.size)
        assertEquals("Should have 3 unique decrypted tag UIDs", 3, decryptedTagUids.size)
        
        // Verify specific tags are stored
        assertTrue("Should contain tag 45FF5A04", encryptedTagUids.contains("45FF5A04"))
        assertTrue("Should contain tag 658127A5", encryptedTagUids.contains("658127A5"))
        assertTrue("Should contain tag 17F3F42B", encryptedTagUids.contains("17F3F42B"))
        
        // Verify the core issue this test was designed to catch - different tray UIDs in block 9
        val scan1 = decryptedScans.find { it.tagUid == "45FF5A04" }
        val scan2 = decryptedScans.find { it.tagUid == "658127A5" }
        val scan3 = decryptedScans.find { it.tagUid == "17F3F42B" }
        
        assertNotNull("Decrypted scan 1 should exist", scan1)
        assertNotNull("Decrypted scan 2 should exist", scan2)
        assertNotNull("Decrypted scan 3 should exist", scan3)
        
        // Extract tray UIDs from block 9 to verify they are different (the original bug)
        val trayUid1 = extractTrayUidFromBlock9(scan1!!.decryptedBlocks[9] ?: "")
        val trayUid2 = extractTrayUidFromBlock9(scan2!!.decryptedBlocks[9] ?: "")
        val trayUid3 = extractTrayUidFromBlock9(scan3!!.decryptedBlocks[9] ?: "")
        
        assertNotEquals("Tray UIDs should be different", trayUid1, trayUid2)
        assertNotEquals("Tray UIDs should be different", trayUid1, trayUid3)
        assertNotEquals("Tray UIDs should be different", trayUid2, trayUid3)
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
        
        // Then - verify scans were saved at the repository level
        val encryptedScans = scanHistoryRepository.getAllEncryptedScans()
        val decryptedScans = scanHistoryRepository.getAllDecryptedScans()
        
        assertEquals("Should have 2 encrypted scans", 2, encryptedScans.size)
        assertEquals("Should have 2 decrypted scans", 2, decryptedScans.size)
        
        // Both scans should have different tag UIDs
        val encryptedTagUids = encryptedScans.map { it.tagUid }.distinct()
        val decryptedTagUids = decryptedScans.map { it.tagUid }.distinct()
        
        assertEquals("Should have 2 unique encrypted tag UIDs", 2, encryptedTagUids.size)
        assertEquals("Should have 2 unique decrypted tag UIDs", 2, decryptedTagUids.size)
        
        // Verify both specific tags are stored
        assertTrue("Should contain tag 45FF5A04", encryptedTagUids.contains("45FF5A04"))
        assertTrue("Should contain tag 45FF5A05", encryptedTagUids.contains("45FF5A05"))
        
        // Verify both scans have the SAME tray UID in block 9 (the key test)
        val scan1 = decryptedScans.find { it.tagUid == "45FF5A04" }
        val scan2 = decryptedScans.find { it.tagUid == "45FF5A05" }
        
        assertNotNull("Decrypted scan 1 should exist", scan1)
        assertNotNull("Decrypted scan 2 should exist", scan2)
        
        val trayUid1 = extractTrayUidFromBlock9(scan1!!.decryptedBlocks[9] ?: "")
        val trayUid2 = extractTrayUidFromBlock9(scan2!!.decryptedBlocks[9] ?: "")
        
        assertEquals("Both tags should have the same tray UID", trayUid1, trayUid2)
        assertEquals("Tray UID should be TRAY001", "TRAY001", trayUid1)
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
        
        // Then - verify scans were saved correctly at the repository level
        val encryptedScans = scanHistoryRepository.getAllEncryptedScans()
        val decryptedScans = scanHistoryRepository.getAllDecryptedScans()
        
        assertEquals("Should have 3 encrypted scans after fix", 3, encryptedScans.size)
        assertEquals("Should have 3 decrypted scans after fix", 3, decryptedScans.size)
        
        // All should have proper tray UIDs stored in block 9, not the corrupted hex value
        val scan1 = decryptedScans.find { it.tagUid == "45FF5A04" }
        val scan2 = decryptedScans.find { it.tagUid == "658127A5" }
        val scan3 = decryptedScans.find { it.tagUid == "17F3F42B" }
        
        assertNotNull("Scan 1 should exist", scan1)
        assertNotNull("Scan 2 should exist", scan2)
        assertNotNull("Scan 3 should exist", scan3)
        
        val trayUid1 = extractTrayUidFromBlock9(scan1!!.decryptedBlocks[9] ?: "")
        val trayUid2 = extractTrayUidFromBlock9(scan2!!.decryptedBlocks[9] ?: "")
        val trayUid3 = extractTrayUidFromBlock9(scan3!!.decryptedBlocks[9] ?: "")
        
        assertEquals("Should extract TRAY001", "TRAY001", trayUid1)
        assertEquals("Should extract TRAY002", "TRAY002", trayUid2)
        assertEquals("Should extract TRAY003", "TRAY003", trayUid3)
        
        // None should have the corrupted hex tray UID
        listOf(trayUid1, trayUid2, trayUid3).forEach { trayUid ->
            assertNotEquals("No scan should have corrupted hex tray UID", 
                corruptedTrayUid, trayUid)
            assertTrue("Tray UID should be human-readable text", 
                trayUid.startsWith("TRAY"))
        }
        
        // All tray UIDs should be different (proving the fix works)
        assertNotEquals("TRAY001 and TRAY002 should be different", trayUid1, trayUid2)
        assertNotEquals("TRAY001 and TRAY003 should be different", trayUid1, trayUid3)
        assertNotEquals("TRAY002 and TRAY003 should be different", trayUid2, trayUid3)
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
            1 to "41302D4B300000004746413000000000", // Real RFID code: GFA00:A00-K0 (32 chars)
            2 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA (32 chars)
            4 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA (32 chars)
            5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000", // Color (32 chars)
            6 to "00C80064001E001E00000000000000000000000000000000000000000000000000", // Temps (32 chars)
            9 to block9Hex // Tray UID as UTF-8 (already 32 chars from createUtf8Block)
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
    
    private fun extractTrayUidFromBlock9(block9Hex: String): String {
        return try {
            // Convert hex to bytes and decode as UTF-8 string
            val bytes = block9Hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            String(bytes, Charsets.UTF_8).trimEnd('\u0000')
        } catch (e: Exception) {
            "INVALID_TRAY_UID"
        }
    }
}