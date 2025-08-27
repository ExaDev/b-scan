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
 * Test to verify proper tray UID extraction from RFID blocks.
 * This test would have caught the bug where different tags were incorrectly grouped together.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TrayUidExtractionTest {

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
        
        // Mock SharedPreferences
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null)
        
        // Mock CatalogRepository with a valid RFID mapping for test data
        val testRfidMapping = RfidMapping(
            rfidCode = "GFL99:A00-K0",
            sku = "test-sku",
            material = "PLA_BASIC",
            color = "Test Color", 
            hex = "#FF0000"
        )
        `when`(mockCatalogRepository.findRfidMapping(anyString())).thenReturn(Pair("bambu", testRfidMapping))
        
        // Mock manufacturer data for UnifiedDataAccess.getCurrentMappings()
        val testManufacturer = ManufacturerCatalog(
            name = "bambu",
            displayName = "Bambu Lab",
            tagFormat = TagFormat.BAMBU_PROPRIETARY,
            materials = mapOf(
                "PLA_BASIC" to MaterialDefinition(
                    displayName = "PLA Basic",
                    temperatureProfile = "lowTempPLA",
                    properties = MaterialCatalogProperties()
                )
            ),
            temperatureProfiles = mapOf(
                "lowTempPLA" to TemperatureProfile(
                    minNozzle = 190,
                    maxNozzle = 220,
                    bed = 60
                )
            ),
            colorPalette = mapOf("#FF0000" to "Red"),
            rfidMappings = mapOf("GFL99:A00-K0" to testRfidMapping),
            componentDefaults = emptyMap(),
            products = emptyList()
        )
        `when`(mockCatalogRepository.getManufacturers()).thenReturn(mapOf("bambu" to testManufacturer))
        
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
    fun `tray UID should be extracted as UTF-8 string not raw hex`() {
        // Given - block 9 contains "TRAY001" as UTF-8 bytes
        val trayUidAsBytes = "TRAY001".toByteArray(Charsets.UTF_8)
        val paddedBytes = ByteArray(16)
        System.arraycopy(trayUidAsBytes, 0, paddedBytes, 0, trayUidAsBytes.size)
        val block9Hex = paddedBytes.joinToString("") { "%02X".format(it) }
        
        val scanData = createTestDecryptedScan(
            tagUid = "12345678",
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030",  // Valid RFID codes
                2 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA
                4 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA
                5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000", // Color
                6 to "00C80064001E001E00000000000000000000000000000000000000000000000000", // Temps
                9 to block9Hex // "TRAY001" as UTF-8 bytes
            )
        )
        
        // When
        val result = interpreterFactory.interpret(scanData)
        
        // Then
        assertNotNull("Interpretation should succeed", result)
        result?.let {
            assertEquals("Tray UID should be extracted as UTF-8 string", "TRAY001", it.trayUid)
            assertNotEquals("Tray UID should NOT be raw hex", block9Hex, it.trayUid)
        }
    }

    @Test
    fun `different tray UIDs should produce different filament reels`() {
        // Given - two tags with different tray UIDs
        val trayUid1 = "TRAY001"
        val trayUid2 = "TRAY002"
        
        val block9Hex1 = createUtf8Block(trayUid1, 16)
        val block9Hex2 = createUtf8Block(trayUid2, 16)
        
        val scanData1 = createTestDecryptedScan(
            tagUid = "12345678",
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030",
                2 to "504C41000000000000000000000000000000000000000000000000000000000000",
                4 to "504C41000000000000000000000000000000000000000000000000000000000000",
                5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000",
                6 to "00C80064001E001E00000000000000000000000000000000000000000000000000",
                9 to block9Hex1
            )
        )
        
        val scanData2 = createTestDecryptedScan(
            tagUid = "87654321",
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030",
                2 to "504C41000000000000000000000000000000000000000000000000000000000000",
                4 to "504C41000000000000000000000000000000000000000000000000000000000000",
                5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000",
                6 to "00C80064001E001E00000000000000000000000000000000000000000000000000",
                9 to block9Hex2
            )
        )
        
        // When
        val result1 = interpreterFactory.interpret(scanData1)
        val result2 = interpreterFactory.interpret(scanData2)
        
        // Then
        assertNotNull("First interpretation should succeed", result1)
        assertNotNull("Second interpretation should succeed", result2)
        
        result1?.let { r1 ->
            result2?.let { r2 ->
                assertEquals("First tag should have tray UID 1", trayUid1, r1.trayUid)
                assertEquals("Second tag should have tray UID 2", trayUid2, r2.trayUid)
                assertNotEquals("Tray UIDs should be different", r1.trayUid, r2.trayUid)
            }
        }
    }

    @Test
    fun `same tray UID should produce same tray identifier for related tags`() {
        // Given - two tags with the SAME tray UID (legitimate case)
        val sharedTrayUid = "TRAY001"
        val block9Hex = createUtf8Block(sharedTrayUid, 16)
        
        val scanData1 = createTestDecryptedScan(
            tagUid = "TAG00001", // Different tag UIDs
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030",
                2 to "504C41000000000000000000000000000000000000000000000000000000000000",
                4 to "504C41000000000000000000000000000000000000000000000000000000000000", 
                5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000",
                6 to "00C80064001E001E00000000000000000000000000000000000000000000000000",
                9 to block9Hex // Same tray UID
            )
        )
        
        val scanData2 = createTestDecryptedScan(
            tagUid = "TAG00002", // Different tag UIDs
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030",
                2 to "504C41000000000000000000000000000000000000000000000000000000000000",
                4 to "504C41000000000000000000000000000000000000000000000000000000000000",
                5 to "00FF00FF0000000000000000000000000000000000000000000000000000000000", // Different color
                6 to "00C80064001E001E00000000000000000000000000000000000000000000000000",
                9 to block9Hex // Same tray UID
            )
        )
        
        // When
        val result1 = interpreterFactory.interpret(scanData1)
        val result2 = interpreterFactory.interpret(scanData2)
        
        // Then
        assertNotNull("First interpretation should succeed", result1)
        assertNotNull("Second interpretation should succeed", result2)
        
        result1?.let { r1 ->
            result2?.let { r2 ->
                assertEquals("Both tags should have same tray UID", sharedTrayUid, r1.trayUid)
                assertEquals("Both tags should have same tray UID", sharedTrayUid, r2.trayUid)
                assertEquals("Tray UIDs should match", r1.trayUid, r2.trayUid)
                assertNotEquals("Tag UIDs should be different", r1.tagUid, r2.tagUid)
            }
        }
    }

    @Test
    fun `sample data generation consistency test`() {
        // This test verifies that sample data generation creates proper UTF-8 encoded tray UIDs
        // that can be correctly extracted by the interpreter
        
        // Given - simulate how SampleDataGenerator creates block 9
        val trayUid = "TRAY042"
        val block9Hex = createUtf8Block(trayUid, 16) // Same method as SampleDataGenerator.stringToHexBlock
        
        val scanData = createTestDecryptedScan(
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030",
                2 to "504C41000000000000000000000000000000000000000000000000000000000000",
                4 to "504C41000000000000000000000000000000000000000000000000000000000000",
                5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000",
                6 to "00C80064001E001E00000000000000000000000000000000000000000000000000",
                9 to block9Hex
            )
        )
        
        // When
        val result = interpreterFactory.interpret(scanData)
        
        // Then
        assertNotNull("Sample data should be interpretable", result)
        result?.let {
            assertEquals("Should extract original tray UID from sample data", trayUid, it.trayUid)
        }
    }

    // Helper methods
    private fun createUtf8Block(text: String, blockSize: Int): String {
        // Replicate SampleDataGenerator.stringToHexBlock logic
        val bytes = text.toByteArray(Charsets.UTF_8)
        val paddedBytes = bytes.take(blockSize).toByteArray()
        val result = ByteArray(blockSize)
        System.arraycopy(paddedBytes, 0, result, 0, paddedBytes.size)
        return result.joinToString("") { "%02X".format(it) }
    }
    
    private fun createTestDecryptedScan(
        tagUid: String = "12345678",
        timestamp: LocalDateTime = LocalDateTime.now(),
        scanResult: ScanResult = ScanResult.SUCCESS,
        tagFormat: TagFormat = TagFormat.BAMBU_PROPRIETARY,
        decryptedBlocks: Map<Int, String> = emptyMap()
    ): DecryptedScanData {
        return DecryptedScanData(
            id = 1,
            timestamp = timestamp,
            tagUid = tagUid,
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