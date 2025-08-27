package com.bscan.debug

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class InterpreterDebugTest {

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
        
        // Basic mocking for repositories
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null)
        
        // Mock assets
        val mockAssetManager = mock(android.content.res.AssetManager::class.java)
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        val emptyCatalog = "{\"version\": 1, \"manufacturers\": {}}"
        `when`(mockAssetManager.open("catalog_data.json")).thenReturn(java.io.ByteArrayInputStream(emptyCatalog.toByteArray()))
        
        // Mock RFID mapping
        val testRfidMapping = RfidMapping(
            rfidCode = "GFA00:A00-K0",
            sku = "test-sku",
            material = "PLA_BASIC",
            color = "Test Color", 
            hex = "#FF0000"
        )
        `when`(mockCatalogRepository.findRfidMapping("GFA00:A00-K0")).thenReturn(Pair("bambu", testRfidMapping))
        
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
    fun `debug block extraction from test data`() {
        // Create test data that matches TrayUidExtractionTest
        val scanData = DecryptedScanData(
            id = 1,
            timestamp = LocalDateTime.now(),
            tagUid = "12345678",
            technology = "MifareClassic",
            scanResult = ScanResult.SUCCESS,
            decryptedBlocks = mapOf(
                1 to "4130302D4B30004746413030000000000000000000000000000000000000000000",  // Valid RFID codes (32 chars)
                2 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA (32 chars)
                4 to "504C41000000000000000000000000000000000000000000000000000000000000", // PLA (32 chars)
                5 to "FF0000FF0000000000000000000000000000000000000000000000000000000000", // Color (32 chars)
                6 to "00C80064001E001E00000000000000000000000000000000000000000000000000", // Temps (32 chars)
                9 to "54524159303031000000000000000000000000000000000000000000000000000000", // "TRAY001" as UTF-8 (32 chars)
                // Add more blocks to reach 16 sectors (up to block 60 = sector 15, block 0)
                48 to "00000000000000000000000000000000000000000000000000000000000000000000", // Sector 12, block 0
                52 to "00000000000000000000000000000000000000000000000000000000000000000000", // Sector 13, block 0
                56 to "00000000000000000000000000000000000000000000000000000000000000000000", // Sector 14, block 0
                60 to "00000000000000000000000000000000000000000000000000000000000000000000"  // Sector 15, block 0
            ),
            authenticatedSectors = listOf(1, 2, 3),
            failedSectors = emptyList(),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyA"),
            derivedKeys = listOf("AABBCCDDEEFF00112233445566778899"),
            errors = emptyList()
        )
        
        println("=== DEBUG INFO ===")
        println("Tag UID: ${scanData.tagUid}")
        println("Technology: ${scanData.technology}")
        println("Tag Format: ${scanData.tagFormat}")
        println("Scan Result: ${scanData.scanResult}")
        println("Sector Count: ${scanData.sectorCount}")
        println("Tag Size Bytes: ${scanData.tagSizeBytes}")
        println("Block count: ${scanData.decryptedBlocks.size}")
        println("Blocks: ${scanData.decryptedBlocks}")
        
        // Parse block 1 manually to understand the extraction
        val block1 = scanData.decryptedBlocks[1]!!
        println("Block 1 hex: $block1")
        
        // Extract variant ID (first 8 bytes) and material ID (next 8 bytes)
        val variantIdBytes = hexStringToByteArray(block1.substring(0, 16)) // First 8 bytes
        val materialIdBytes = hexStringToByteArray(block1.substring(16, 32)) // Next 8 bytes
        
        val variantId = String(variantIdBytes, Charsets.UTF_8).replace("\u0000", "").trim()
        val materialId = String(materialIdBytes, Charsets.UTF_8).replace("\u0000", "").trim()
        
        println("Variant ID: '$variantId'")
        println("Material ID: '$materialId'")
        println("RFID Code: '${materialId}:${variantId}'")
        
        // Try interpretation
        val result = interpreterFactory.interpret(scanData)
        
        println("Interpretation result: $result")
        
        assertNotNull("Should be able to interpret the data", result)
    }
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }
}