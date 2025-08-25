package com.bscan

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bscan.model.FilamentInfo
import com.bscan.repository.InventoryRepository
import com.bscan.repository.PhysicalComponentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Working instrumented tests that verify component fixes on actual device
 */
@RunWith(AndroidJUnit4::class)
class DeviceComponentTest {

    private lateinit var context: Context
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var physicalComponentRepository: PhysicalComponentRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        inventoryRepository = InventoryRepository(context)
        physicalComponentRepository = PhysicalComponentRepository(context)
    }

    @Test
    fun deviceTest_componentAddition_shouldNotCrash() {
        val testTrayUid = "DEVICE_TEST_001"
        
        runBlocking {
            // Clean up any existing test data
            try {
                inventoryRepository.deleteInventoryItem(testTrayUid)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            
            // Create test filament info with correct parameters
            val testFilamentInfo = FilamentInfo(
                tagUid = "${testTrayUid}_TAG",
                trayUid = testTrayUid,
                manufacturerName = "Bambu Lab",
                filamentType = "PLA_BASIC",
                detailedFilamentType = "PLA Basic",
                colorHex = "#000000",
                colorName = "Black",
                spoolWeight = 1000,
                filamentDiameter = 1.75f,
                filamentLength = 330,
                productionDate = "",
                minTemperature = 190,
                maxTemperature = 220,
                bedTemperature = 60,
                dryingTemperature = 45,
                dryingTime = 12
            )
            
            // Test component setup
            val components = inventoryRepository.setupBambuComponents(testTrayUid, testFilamentInfo)
            assert(components.isNotEmpty()) { "Should create components successfully" }
            
            // Test component retrieval
            val retrievedComponents = inventoryRepository.getInventoryItemComponents(testTrayUid)
            assert(retrievedComponents.isNotEmpty()) { "Should retrieve components successfully" }
            
            val initialCount = retrievedComponents.size
            
            // Test component addition (this was previously crashing)
            inventoryRepository.addComponentToInventoryItem(testTrayUid, "bambu_refillable_spool")
            
            // Verify addition succeeded
            val updatedComponents = inventoryRepository.getInventoryItemComponents(testTrayUid)
            assert(updatedComponents.size == initialCount + 1) { 
                "Component addition should succeed. Expected: ${initialCount + 1}, Got: ${updatedComponents.size}" 
            }
            
            val hasSpoolComponent = updatedComponents.any { it.id == "bambu_refillable_spool" }
            assert(hasSpoolComponent) { "Should have refillable spool component after addition" }
            
            // Test mass calculations still work
            val filamentStatus = inventoryRepository.calculateFilamentStatus(testTrayUid)
            assert(filamentStatus.calculationSuccess) { 
                "Mass calculation should work after component addition: ${filamentStatus.errorMessage}" 
            }
            
            // Test component removal
            inventoryRepository.removeComponentFromInventoryItem(testTrayUid, "bambu_refillable_spool")
            
            val afterRemoval = inventoryRepository.getInventoryItemComponents(testTrayUid)
            assert(afterRemoval.size == initialCount) { 
                "Component removal should work. Expected: $initialCount, Got: ${afterRemoval.size}" 
            }
            
            // Cleanup
            inventoryRepository.deleteInventoryItem(testTrayUid)
        }
    }
    
    @Test
    fun deviceTest_componentRepository_basicOperations() {
        runBlocking {
            // Test component repository basic functionality
            val components = physicalComponentRepository.getComponents()
            assert(components.isNotEmpty()) { "Should have default components" }
            
            val bambuCore = physicalComponentRepository.getBambuCoreComponent()
            assert(bambuCore.name.contains("Cardboard Core")) { "Core component should have correct name" }
            assert(bambuCore.massGrams == 33f) { "Core component should have 33g mass" }
            assert(!bambuCore.variableMass) { "Core should be fixed mass" }
            
            val bambuSpool = physicalComponentRepository.getBambuSpoolComponent()
            assert(bambuSpool.name.contains("Refillable Spool")) { "Spool component should have correct name" }
            assert(bambuSpool.massGrams == 212f) { "Spool component should have 212g mass" }
            assert(!bambuSpool.variableMass) { "Spool should be fixed mass" }
            
            // Test component creation with full mass (the key fix)
            val testFilament = physicalComponentRepository.createFilamentComponent(
                filamentType = "PLA_BASIC",
                colorName = "Test Black",
                colorHex = "#000000",
                massGrams = 1000f,
                manufacturer = "Test Manufacturer",
                fullMassGrams = 1000f
            )
            
            assert(testFilament.variableMass) { "Filament should be variable mass" }
            assert(testFilament.massGrams == 1000f) { "Filament should have correct current mass" }
            assert(testFilament.fullMassGrams == 1000f) { "Filament should have correct full mass" }
            assert(testFilament.getRemainingPercentage() == 1.0f) { "Should be 100% remaining when full (1.0 = 100%)" }
        }
    }
    
    @Test
    fun deviceTest_errorHandling_invalidComponent() {
        val testTrayUid = "ERROR_TEST_001"
        
        runBlocking {
            // Clean up
            try {
                inventoryRepository.deleteInventoryItem(testTrayUid)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            
            // Setup inventory
            val testFilamentInfo = FilamentInfo(
                tagUid = "${testTrayUid}_TAG",
                trayUid = testTrayUid,
                manufacturerName = "Bambu Lab",
                filamentType = "PLA_BASIC", 
                detailedFilamentType = "PLA Basic",
                colorHex = "#000000",
                colorName = "Black",
                spoolWeight = 1000,
                filamentDiameter = 1.75f,
                filamentLength = 330,
                productionDate = "",
                minTemperature = 190,
                maxTemperature = 220,
                bedTemperature = 60,
                dryingTemperature = 45,
                dryingTime = 12
            )
            
            inventoryRepository.setupBambuComponents(testTrayUid, testFilamentInfo)
            
            // Test error handling for invalid component (this should throw exception, not crash)
            var exceptionThrown = false
            try {
                inventoryRepository.addComponentToInventoryItem(testTrayUid, "non_existent_component")
            } catch (e: IllegalArgumentException) {
                exceptionThrown = true
                assert(e.message?.contains("not found") == true) { 
                    "Exception should mention component not found: ${e.message}" 
                }
            }
            
            assert(exceptionThrown) { "Should throw exception for non-existent component" }
            
            // Verify no changes were made
            val components = inventoryRepository.getInventoryItemComponents(testTrayUid)
            val hasInvalidComponent = components.any { it.id == "non_existent_component" }
            assert(!hasInvalidComponent) { "Should not have invalid component in inventory" }
            
            // Cleanup
            inventoryRepository.deleteInventoryItem(testTrayUid)
        }
    }
}