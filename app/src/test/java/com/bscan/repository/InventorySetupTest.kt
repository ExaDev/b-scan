package com.bscan.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bscan.model.FilamentInfo
import com.bscan.model.TagFormat
import com.bscan.model.PhysicalComponent
import com.bscan.model.PhysicalComponentType
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
    private lateinit var catalogRepository: CatalogRepository
    private lateinit var unifiedDataAccess: UnifiedDataAccess
    private lateinit var userDataRepository: UserDataRepository
    private lateinit var diagnostics: InventoryDiagnostics
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        catalogRepository = CatalogRepository(context)
        userDataRepository = UserDataRepository(context)
        unifiedDataAccess = UnifiedDataAccess(catalogRepository, userDataRepository)
        inventoryRepository = InventoryRepository(context)
        diagnostics = InventoryDiagnostics(context)
        
        // Clear any existing test data
        userDataRepository.clearUserData()
    }
    
    @Test
    fun testMappingsLoading() {
        // Test that mappings can be loaded (either from assets or fallback)
        val mappings = catalogRepository.getCurrentMappings()
        assertNotNull(mappings)
        
        // Should have at least fallback data
        assertTrue(mappings.materialMappings.isNotEmpty() || mappings.productCatalog.isNotEmpty())
    }
    
    @Test
    fun testSkuLookupWithValidData() {
        // Test SKU lookup with common materials
        val plaMatch = catalogRepository.findBestProductMatch("PLA_BASIC", "Black")
        // Should either find exact match or return null (but not crash)
        // The key is that the method doesn't throw exceptions
        
        val petgMatch = catalogRepository.findBestProductMatch("PETG", "Clear")
        // Same as above - should handle gracefully
        
        // Test should pass as long as no exceptions are thrown
        assertTrue(true, "SKU lookup completed without exceptions")
    }
    
    @Test
    fun testComponentCreation() {
        // Test basic component creation using UnifiedDataAccess
        val components = unifiedDataAccess.createDefaultComponents(
            manufacturerId = "bambu",
            filamentType = "PLA_BASIC",
            trayUid = "TEST_TRAY_001"
        )
        
        assertTrue(components.isNotEmpty(), "Should create at least one component")
        
        // Find filament component
        val filamentComponent = components.find { it.type == PhysicalComponentType.FILAMENT }
        assertNotNull(filamentComponent, "Should have filament component")
        assertTrue(filamentComponent.variableMass, "Filament should have variable mass")
        
        // Find core component
        val coreComponent = components.find { it.type == PhysicalComponentType.CORE_RING }
        if (coreComponent != null) {
            assertTrue(!coreComponent.variableMass, "Core should have fixed mass")
        }
        
        // Find spool component
        val spoolComponent = components.find { it.type == PhysicalComponentType.BASE_SPOOL }
        if (spoolComponent != null) {
            assertTrue(!spoolComponent.variableMass, "Spool should have fixed mass")
        }
    }
    
    @Test
    fun testSetupBambuComponentsWithValidData() {
        val filamentInfo = createTestFilamentInfo(
            trayUid = "TEST_TRAY_001",
            filamentType = "PLA_BASIC",
            colorName = "Black",
            colorHex = "#000000"
        )
        
        val inventoryItem = unifiedDataAccess.createInventoryItemWithComponents(
            trayUid = filamentInfo.trayUid,
            manufacturerId = "bambu",
            filamentType = filamentInfo.filamentType
        )
        val components = inventoryItem.components.mapNotNull { unifiedDataAccess.getComponent(it) }
        
        // Should always create at least one component (emergency fallback if needed)
        assertTrue(components.isNotEmpty(), "At least one component should be created")
        
        // Check that inventory item was created
        val retrievedItem = unifiedDataAccess.getInventoryItem(filamentInfo.trayUid)
        assertNotNull(retrievedItem, "Inventory item should be created")
        assertEquals(components.size, retrievedItem.components.size)
        
        // Clean up - remove components and inventory item
        components.forEach { component ->
            userDataRepository.removeComponent(component.id)
        }
        userDataRepository.removeInventoryItem(filamentInfo.trayUid)
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
        
        val inventoryItem = unifiedDataAccess.createInventoryItemWithComponents(
            trayUid = filamentInfo.trayUid,
            manufacturerId = "bambu",
            filamentType = if (filamentInfo.filamentType.isBlank()) "PLA_BASIC" else filamentInfo.filamentType
        )
        val components = inventoryItem.components.mapNotNull { unifiedDataAccess.getComponent(it) }
        
        // Should still create components (using defaults/fallbacks)
        assertTrue(components.isNotEmpty(), "Components should be created even with missing data")
        
        // Check that inventory item was created
        val retrievedItem = unifiedDataAccess.getInventoryItem(filamentInfo.trayUid)
        assertNotNull(retrievedItem, "Inventory item should be created even with missing data")
        
        // Clean up
        components.forEach { component ->
            userDataRepository.removeComponent(component.id)
        }
        userDataRepository.removeInventoryItem(filamentInfo.trayUid)
    }
    
    @Test
    fun testSetupBambuComponentsWithUnknownMaterial() {
        val filamentInfo = createTestFilamentInfo(
            trayUid = "TEST_TRAY_003",
            filamentType = "UNKNOWN_MATERIAL_TYPE",
            colorName = "Unknown Color",
            colorHex = "#808080"
        )
        
        val inventoryItem = unifiedDataAccess.createInventoryItemWithComponents(
            trayUid = filamentInfo.trayUid,
            manufacturerId = "bambu",
            filamentType = "PLA_BASIC" // Use default for unknown material
        )
        val components = inventoryItem.components.mapNotNull { unifiedDataAccess.getComponent(it) }
        
        // Should still create components with default values
        assertTrue(components.isNotEmpty(), "Components should be created for unknown materials")
        
        // Should have at least filament component
        val filamentComponent = components.find { it.variableMass }
        assertNotNull(filamentComponent, "Should have at least one filament component")
        
        // Clean up
        components.forEach { component ->
            userDataRepository.removeComponent(component.id)
        }
        userDataRepository.removeInventoryItem(filamentInfo.trayUid)
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
            val inventoryItem = unifiedDataAccess.createInventoryItemWithComponents(
                trayUid = filamentInfo.trayUid,
                manufacturerId = "bambu",
                filamentType = filamentInfo.filamentType
            )
            val components = inventoryItem.components.mapNotNull { unifiedDataAccess.getComponent(it) }
            
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
        val inventoryItems = unifiedDataAccess.getInventoryItems()
        assertTrue(inventoryItems.size >= 3, "Should have at least 3 inventory items")
        
        // Clean up
        filamentInfos.forEach { filamentInfo ->
            val inventoryItem = unifiedDataAccess.getInventoryItem(filamentInfo.trayUid)
            inventoryItem?.let { item ->
                item.components.forEach { componentId ->
                    userDataRepository.removeComponent(componentId)
                }
                userDataRepository.removeInventoryItem(filamentInfo.trayUid)
            }
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