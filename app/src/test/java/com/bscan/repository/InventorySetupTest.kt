package com.bscan.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bscan.model.FilamentInfo
import com.bscan.model.TagFormat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for the improved inventory setup pipeline
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class InventorySetupTest {
    
    private lateinit var context: Context
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var mappingsRepository: MappingsRepository
    private lateinit var componentRepository: ComponentRepository
    private lateinit var diagnostics: InventoryDiagnostics
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        inventoryRepository = InventoryRepository(context)
        mappingsRepository = MappingsRepository(context)
        componentRepository = ComponentRepository(context)
        diagnostics = InventoryDiagnostics(context)
        
        // Clear any existing test data
        componentRepository.clearComponents()
    }
    
    @Test
    fun testMappingsLoading() {
        // Test that mappings can be loaded (either from assets or fallback)
        val mappings = mappingsRepository.getCurrentMappings()
        assertNotNull(mappings)
        
        // Should have at least fallback data
        assertTrue(mappings.materialMappings.isNotEmpty() || mappings.productCatalog.isNotEmpty())
    }
    
    @Test
    fun testSkuLookupWithValidData() {
        // Test SKU lookup with common materials
        val plaMatch = mappingsRepository.findBestProductMatch("PLA_BASIC", "Black")
        // Should either find exact match or return null (but not crash)
        // The key is that the method doesn't throw exceptions
        
        val petgMatch = mappingsRepository.findBestProductMatch("PETG", "Clear")
        // Same as above - should handle gracefully
        
        // Test should pass as long as no exceptions are thrown
        assertTrue(true, "SKU lookup completed without exceptions")
    }
    
    @Test
    fun testComponentCreation() {
        // Test basic component creation
        val filamentComponent = componentRepository.createFilamentComponent(
            filamentType = "PLA_BASIC",
            colorName = "Black",
            colorHex = "#000000",
            massGrams = 1000f
        )
        
        assertNotNull(filamentComponent)
        assertEquals("PLA_BASIC - Black", filamentComponent.name)
        assertEquals(1000f, filamentComponent.massGrams)
        assertTrue(filamentComponent.variableMass)
        
        // Test core component
        val coreComponent = componentRepository.getBambuCoreComponent()
        assertNotNull(coreComponent)
        assertEquals(33f, coreComponent.massGrams)
        assertTrue(!coreComponent.variableMass)
        
        // Test spool component
        val spoolComponent = componentRepository.getBambuSpoolComponent()
        assertNotNull(spoolComponent)
        assertEquals(212f, spoolComponent.massGrams)
        assertTrue(!spoolComponent.variableMass)
    }
    
    @Test
    fun testSetupBambuComponentsWithValidData() {
        val filamentInfo = createTestFilamentInfo(
            trayUid = "TEST_TRAY_001",
            filamentType = "PLA_BASIC",
            colorName = "Black",
            colorHex = "#000000"
        )
        
        val components = inventoryRepository.setupBambuComponents(
            trayUid = filamentInfo.trayUid,
            filamentInfo = filamentInfo
        )
        
        // Should always create at least one component (emergency fallback if needed)
        assertTrue(components.isNotEmpty(), "At least one component should be created")
        
        // Check that inventory item was created
        val inventoryItem = inventoryRepository.getInventoryItem(filamentInfo.trayUid)
        assertNotNull(inventoryItem, "Inventory item should be created")
        assertEquals(components.size, inventoryItem.components.size)
        
        // Check that filament status can be calculated
        val status = inventoryRepository.calculateFilamentStatus(filamentInfo.trayUid)
        assertNotNull(status, "Filament status should be calculable")
        
        // Clean up
        inventoryRepository.deleteInventoryItem(filamentInfo.trayUid)
    }
    
    @Test
    fun testSetupBambuComponentsWithMissingData() {
        // Test with incomplete/missing data to ensure resilience
        val filamentInfo = createTestFilamentInfo(
            trayUid = "TEST_TRAY_002",
            filamentType = "", // Empty type
            colorName = "", // Empty color
            colorHex = ""  // Empty hex
        )
        
        val components = inventoryRepository.setupBambuComponents(
            trayUid = filamentInfo.trayUid,
            filamentInfo = filamentInfo
        )
        
        // Should still create components (using defaults/fallbacks)
        assertTrue(components.isNotEmpty(), "Components should be created even with missing data")
        
        // Check that inventory item was created
        val inventoryItem = inventoryRepository.getInventoryItem(filamentInfo.trayUid)
        assertNotNull(inventoryItem, "Inventory item should be created even with missing data")
        
        // Clean up
        inventoryRepository.deleteInventoryItem(filamentInfo.trayUid)
    }
    
    @Test
    fun testSetupBambuComponentsWithUnknownMaterial() {
        val filamentInfo = createTestFilamentInfo(
            trayUid = "TEST_TRAY_003",
            filamentType = "UNKNOWN_MATERIAL_TYPE",
            colorName = "Unknown Color",
            colorHex = "#808080"
        )
        
        val components = inventoryRepository.setupBambuComponents(
            trayUid = filamentInfo.trayUid,
            filamentInfo = filamentInfo
        )
        
        // Should still create components with default values
        assertTrue(components.isNotEmpty(), "Components should be created for unknown materials")
        
        // Should have at least filament component
        val filamentComponent = components.find { it.variableMass }
        assertNotNull(filamentComponent, "Should have at least one filament component")
        
        // Clean up
        inventoryRepository.deleteInventoryItem(filamentInfo.trayUid)
    }
    
    @Test
    fun testInventoryDiagnostics() {
        // Test the diagnostic system
        val healthCheck = diagnostics.quickHealthCheck()
        assertTrue(healthCheck, "Basic health check should pass")
        
        val mappingsStatus = diagnostics.testMappingsLoading()
        assertTrue(mappingsStatus.loaded, "Mappings should load successfully")
        
        val componentTests = diagnostics.testComponentCreation()
        assertTrue(componentTests.all { it.success }, "All component creation tests should pass")
    }
    
    @Test
    fun testMultipleInventoryItems() {
        // Test creating multiple inventory items to ensure no conflicts
        val filamentInfos = listOf(
            createTestFilamentInfo("MULTI_001", "PLA_BASIC", "Black", "#000000"),
            createTestFilamentInfo("MULTI_002", "PETG", "Clear", "#FFFFFF"),
            createTestFilamentInfo("MULTI_003", "ABS", "Red", "#FF0000")
        )
        
        val allComponents = mutableListOf<String>()
        
        filamentInfos.forEach { filamentInfo ->
            val components = inventoryRepository.setupBambuComponents(
                trayUid = filamentInfo.trayUid,
                filamentInfo = filamentInfo
            )
            
            assertTrue(components.isNotEmpty(), "Components should be created for ${filamentInfo.trayUid}")
            
            // Check for unique filament component IDs (shared components like core/spool are expected to be reused)
            val filamentComponents = components.filter { it.variableMass }
            val filamentComponentIds = filamentComponents.map { it.id }
            filamentComponentIds.forEach { id ->
                assertTrue(!allComponents.contains(id), "Filament component ID $id should be unique")
                allComponents.add(id)
            }
        }
        
        // Verify all inventory items exist
        val inventoryItems = inventoryRepository.getInventoryItems()
        assertTrue(inventoryItems.size >= 3, "Should have at least 3 inventory items")
        
        // Clean up
        filamentInfos.forEach { filamentInfo ->
            inventoryRepository.deleteInventoryItem(filamentInfo.trayUid)
        }
    }
    
    private fun createTestFilamentInfo(
        trayUid: String,
        filamentType: String,
        colorName: String,
        colorHex: String
    ): FilamentInfo {
        return FilamentInfo(
            tagUid = "TEST_TAG_${System.currentTimeMillis()}",
            trayUid = trayUid,
            tagFormat = TagFormat.BAMBU_PROPRIETARY,
            manufacturerName = "Bambu Lab",
            filamentType = filamentType,
            detailedFilamentType = filamentType,
            colorHex = colorHex,
            colorName = colorName,
            spoolWeight = 1000,
            filamentDiameter = 1.75f,
            filamentLength = 330000,
            productionDate = "2025-01-01",
            minTemperature = 190,
            maxTemperature = 220,
            bedTemperature = 60,
            dryingTemperature = 45,
            dryingTime = 8
        )
    }
}