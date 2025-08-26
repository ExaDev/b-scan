package com.bscan.repository

import android.content.Context
import android.util.Log
import com.bscan.model.FilamentInfo
import com.bscan.model.PhysicalComponent

/**
 * Diagnostic tools for troubleshooting SKU lookup and component setup issues.
 * This class provides validation and testing functions for the inventory pipeline.
 */
class InventoryDiagnostics(private val context: Context) {
    
    private val catalogRepository by lazy { CatalogRepository(context) }
    private val userDataRepository by lazy { UserDataRepository(context) }
    private val unifiedDataAccess by lazy { UnifiedDataAccess(catalogRepository, userDataRepository) }
    private val inventoryRepository by lazy { InventoryRepository(context) }
    private val physicalComponentRepository by lazy { PhysicalComponentRepository(context) }
    
    companion object {
        private const val TAG = "InventoryDiagnostics"
    }
    
    /**
     * Comprehensive diagnostic report for SKU lookup and component setup
     */
    fun runFullDiagnostic(): DiagnosticReport {
        Log.i(TAG, "Running comprehensive inventory diagnostic")
        
        val report = DiagnosticReport()
        
        // Test mappings loading
        report.mappingsStatus = testMappingsLoading()
        
        // Test SKU lookup
        report.skuLookupTests = testSkuLookupScenarios()
        
        // Test component creation
        report.componentTests = testComponentCreation()
        
        // Test full pipeline
        report.pipelineTests = testFullPipeline()
        
        // Validate existing inventory
        report.existingInventoryStatus = validateExistingInventory()
        
        Log.i(TAG, "Diagnostic complete: ${if (report.allTestsPassed()) "ALL TESTS PASSED" else "SOME TESTS FAILED"}")
        return report
    }
    
    /**
     * Test mappings loading functionality
     */
    fun testMappingsLoading(): MappingsStatus {
        Log.d(TAG, "Testing mappings loading...")
        
        val status = MappingsStatus()
        
        try {
            val mappings = unifiedDataAccess.getCurrentMappings()
            status.loaded = true
            status.productCount = mappings.productCatalog.size
            status.materialMappingCount = mappings.materialMappings.size
            status.brandMappingCount = mappings.brandMappings.size
            status.version = mappings.version
            
            Log.d(TAG, "Mappings loaded: ${status.productCount} products, ${status.materialMappingCount} materials")
            
            if (status.productCount == 0) {
                status.warnings.add("No products in catalog - SKU lookup will fail")
            }
            
        } catch (e: Exception) {
            status.loaded = false
            status.error = e.message
            Log.e(TAG, "Failed to load mappings", e)
        }
        
        return status
    }
    
    /**
     * Test SKU lookup with various scenarios
     */
    fun testSkuLookupScenarios(): List<SkuLookupTest> {
        Log.d(TAG, "Testing SKU lookup scenarios...")
        
        val testScenarios = listOf(
            SkuLookupTest("PLA_BASIC", "Black", "Common PLA Basic Black"),
            SkuLookupTest("PLA_BASIC", "White", "Common PLA Basic White"),
            SkuLookupTest("PETG", "Clear", "Common PETG Clear"),
            SkuLookupTest("ABS", "Black", "Common ABS Black"),
            SkuLookupTest("UNKNOWN_MATERIAL", "Unknown Color", "Unknown material test"),
            SkuLookupTest("", "", "Empty input test")
        )
        
        return testScenarios.map { test ->
            try {
                val result = unifiedDataAccess.findBestProductMatch(test.filamentType, test.colorName)
                test.found = result != null
                test.productName = result?.productName
                test.massFound = result?.filamentWeightGrams != null
                test.mass = result?.filamentWeightGrams
                
                Log.d(TAG, "SKU test ${test.description}: ${if (test.found) "FOUND" else "NOT_FOUND"}")
                
            } catch (e: Exception) {
                test.error = e.message
                Log.e(TAG, "SKU lookup test failed: ${test.description}", e)
            }
            test
        }
    }
    
    /**
     * Test component creation functionality
     */
    fun testComponentCreation(): List<ComponentTest> {
        Log.d(TAG, "Testing component creation...")
        
        val tests = listOf(
            ComponentTest("createFilamentComponent", "Test filament component creation"),
            ComponentTest("getBambuCoreComponent", "Test core component creation"),
            ComponentTest("getBambuSpoolComponent", "Test spool component creation")
        )
        
        tests.forEach { test ->
            try {
                when (test.testName) {
                    "createFilamentComponent" -> {
                        val component = physicalComponentRepository.createFilamentComponent(
                            filamentType = "PLA_BASIC",
                            colorName = "Test Color",
                            colorHex = "#FF0000",
                            massGrams = 1000f,
                            manufacturer = "Test Manufacturer",
                            fullMassGrams = 1000f
                        )
                        test.success = component.id.isNotBlank()
                        test.componentId = component.id
                    }
                    "getBambuCoreComponent" -> {
                        val component = physicalComponentRepository.getBambuCoreComponent()
                        test.success = component.id.isNotBlank()
                        test.componentId = component.id
                    }
                    "getBambuSpoolComponent" -> {
                        val component = physicalComponentRepository.getBambuSpoolComponent()
                        test.success = component.id.isNotBlank()
                        test.componentId = component.id
                    }
                }
                Log.d(TAG, "Component test ${test.testName}: ${if (test.success) "SUCCESS" else "FAILED"}")
            } catch (e: Exception) {
                test.success = false
                test.error = e.message
                Log.e(TAG, "Component creation test failed: ${test.testName}", e)
            }
        }
        
        return tests
    }
    
    /**
     * Test the full pipeline with mock FilamentInfo
     */
    fun testFullPipeline(): List<PipelineTest> {
        Log.d(TAG, "Testing full pipeline...")
        
        val testCases = listOf(
            createMockFilamentInfo("TEST_PLA_001", "PLA_BASIC", "Black", "#000000"),
            createMockFilamentInfo("TEST_PETG_001", "PETG", "Clear", "#FFFFFF"),
            createMockFilamentInfo("TEST_UNKNOWN_001", "UNKNOWN", "Unknown", "#808080")
        )
        
        return testCases.map { filamentInfo ->
            val test = PipelineTest(filamentInfo.trayUid, filamentInfo.filamentType, filamentInfo.colorName)
            
            try {
                val components = inventoryRepository.setupBambuComponents(
                    trayUid = filamentInfo.trayUid,
                    filamentInfo = filamentInfo,
                    includeRefillableSpool = false
                )
                
                test.setupSuccess = components.isNotEmpty()
                test.componentCount = components.size
                test.componentIds = components.map { it.id }
                
                // Check if inventory item was created
                val inventoryItem = inventoryRepository.getInventoryItem(filamentInfo.trayUid)
                test.inventoryItemCreated = inventoryItem != null
                
                Log.d(TAG, "Pipeline test ${filamentInfo.trayUid}: ${if (test.setupSuccess) "SUCCESS" else "FAILED"}")
                
            } catch (e: Exception) {
                test.setupSuccess = false
                test.error = e.message
                Log.e(TAG, "Full pipeline test failed: ${filamentInfo.trayUid}", e)
            }
            
            test
        }
    }
    
    /**
     * Validate existing inventory items
     */
    fun validateExistingInventory(): InventoryValidationStatus {
        Log.d(TAG, "Validating existing inventory...")
        
        val status = InventoryValidationStatus()
        
        try {
            val inventoryItems = inventoryRepository.getInventoryItems()
            status.totalItems = inventoryItems.size
            
            inventoryItems.forEach { item ->
                val components = inventoryRepository.getInventoryItemComponents(item.trayUid)
                val filamentStatus = inventoryRepository.calculateFilamentStatus(item.trayUid)
                
                val validation = InventoryItemValidation(
                    trayUid = item.trayUid,
                    componentCount = components.size,
                    hasFilamentComponent = components.any { it.variableMass },
                    statusCalculationSuccess = filamentStatus.calculationSuccess,
                    statusError = filamentStatus.errorMessage
                )
                
                if (!validation.hasFilamentComponent) {
                    status.itemsWithoutFilament++
                }
                
                if (!validation.statusCalculationSuccess) {
                    status.itemsWithCalculationErrors++
                }
                
                status.itemValidations.add(validation)
            }
            
            status.validationSuccess = true
            Log.d(TAG, "Inventory validation: ${status.totalItems} items, ${status.itemsWithoutFilament} without filament, ${status.itemsWithCalculationErrors} with errors")
            
        } catch (e: Exception) {
            status.validationSuccess = false
            status.error = e.message
            Log.e(TAG, "Inventory validation failed", e)
        }
        
        return status
    }
    
    /**
     * Quick health check - returns true if basic functionality works
     */
    fun quickHealthCheck(): Boolean {
        return try {
            // Test basic functionality
            val mappings = unifiedDataAccess.getCurrentMappings()
            val core = physicalComponentRepository.getBambuCoreComponent()
            
            val testFilamentInfo = createMockFilamentInfo("HEALTH_CHECK", "PLA_BASIC", "Black", "#000000")
            val components = inventoryRepository.setupBambuComponents(testFilamentInfo.trayUid, testFilamentInfo)
            
            // Clean up test data
            inventoryRepository.deleteInventoryItem(testFilamentInfo.trayUid)
            
            components.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            false
        }
    }
    
    /**
     * Create mock FilamentInfo for testing
     */
    private fun createMockFilamentInfo(
        trayUid: String,
        filamentType: String,
        colorName: String,
        colorHex: String
    ): FilamentInfo {
        return FilamentInfo(
            tagUid = "TEST_TAG_${System.currentTimeMillis()}",
            trayUid = trayUid,
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

/**
 * Comprehensive diagnostic report
 */
data class DiagnosticReport(
    var mappingsStatus: MappingsStatus = MappingsStatus(),
    var skuLookupTests: List<SkuLookupTest> = emptyList(),
    var componentTests: List<ComponentTest> = emptyList(),
    var pipelineTests: List<PipelineTest> = emptyList(),
    var existingInventoryStatus: InventoryValidationStatus = InventoryValidationStatus()
) {
    fun allTestsPassed(): Boolean {
        return mappingsStatus.loaded &&
               skuLookupTests.isNotEmpty() &&
               componentTests.all { it.success } &&
               pipelineTests.all { it.setupSuccess } &&
               existingInventoryStatus.validationSuccess
    }
}

/**
 * Mappings loading status
 */
data class MappingsStatus(
    var loaded: Boolean = false,
    var productCount: Int = 0,
    var materialMappingCount: Int = 0,
    var brandMappingCount: Int = 0,
    var version: Int = 0,
    var error: String? = null,
    var warnings: MutableList<String> = mutableListOf()
)

/**
 * SKU lookup test result
 */
data class SkuLookupTest(
    val filamentType: String,
    val colorName: String,
    val description: String,
    var found: Boolean = false,
    var productName: String? = null,
    var massFound: Boolean = false,
    var mass: Float? = null,
    var error: String? = null
)

/**
 * Component creation test result
 */
data class ComponentTest(
    val testName: String,
    val description: String,
    var success: Boolean = false,
    var componentId: String? = null,
    var error: String? = null
)

/**
 * Full pipeline test result
 */
data class PipelineTest(
    val trayUid: String,
    val filamentType: String,
    val colorName: String,
    var setupSuccess: Boolean = false,
    var componentCount: Int = 0,
    var componentIds: List<String> = emptyList(),
    var inventoryItemCreated: Boolean = false,
    var error: String? = null
)

/**
 * Existing inventory validation status
 */
data class InventoryValidationStatus(
    var validationSuccess: Boolean = false,
    var totalItems: Int = 0,
    var itemsWithoutFilament: Int = 0,
    var itemsWithCalculationErrors: Int = 0,
    var itemValidations: MutableList<InventoryItemValidation> = mutableListOf(),
    var error: String? = null
)

/**
 * Individual inventory item validation
 */
data class InventoryItemValidation(
    val trayUid: String,
    val componentCount: Int,
    val hasFilamentComponent: Boolean,
    val statusCalculationSuccess: Boolean,
    val statusError: String?
)