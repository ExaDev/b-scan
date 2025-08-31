package com.bscan.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bscan.model.*
import com.bscan.model.graph.*
import com.bscan.model.graph.entities.*
import com.bscan.service.ScanDataService
import com.bscan.service.InventoryService
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for the graph-based inventory system
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class GraphInventoryTest {
    
    private lateinit var context: Context
    private lateinit var graphRepository: GraphRepository
    private lateinit var scanDataService: ScanDataService
    private lateinit var inventoryService: InventoryService
    private lateinit var unifiedDataAccess: UnifiedDataAccess
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        graphRepository = GraphRepository(context)
        scanDataService = ScanDataService(graphRepository)
        inventoryService = InventoryService(graphRepository)
        
        val catalogRepository = CatalogRepository(context)
        val userDataRepository = UserDataRepository(context)
        val scanHistoryRepository = ScanHistoryRepository(context)
        
        unifiedDataAccess = UnifiedDataAccess(
            catalogRepository, 
            userDataRepository, 
            scanHistoryRepository,
            null,
            context
        )
        
        // Clear any existing test data
        runTest {
            graphRepository.clearAll()
        }
    }
    
    @Test
    fun testGraphRepositoryInitialization() = runTest {
        // Test basic graph repository functionality
        val statistics = graphRepository.getStatistics()
        assertNotNull(statistics, "Graph statistics should be available")
        assertEquals(0, statistics.entityCount, "Graph should start empty")
        assertEquals(0, statistics.edgeCount, "Graph should start with no edges")
    }
    
    @Test
    fun testScanDataService() = runTest {
        // Test scan data persistence and deduplication
        val scanResult = scanDataService.recordScan(
            rawData = "0011223344556677",
            scanFormat = "bambu_rfid",
            deviceInfo = "Test Device",
            scanLocation = "Test Location",
            userNotes = "Test scan"
        )
        
        assertTrue(scanResult.success, "Scan should be recorded successfully")
        assertNotNull(scanResult.scanOccurrence, "Scan occurrence should be created")
        assertNotNull(scanResult.rawScanData, "Raw scan data should be created")
        
        // Test deduplication by scanning same data again
        val duplicateScanResult = scanDataService.recordScan(
            rawData = "0011223344556677",
            scanFormat = "bambu_rfid",
            deviceInfo = "Another Device",
            userNotes = "Duplicate scan"
        )
        
        assertTrue(duplicateScanResult.success, "Duplicate scan should be recorded successfully")
        assertTrue(duplicateScanResult.wasRawDataDeduplicated, "Raw data should be deduplicated")
        assertEquals(
            scanResult.rawScanData?.id, 
            duplicateScanResult.rawScanData?.id,
            "Same raw data should reference same entity"
        )
    }
    
    @Test
    fun testInventoryItemCreation() = runTest {
        // Test inventory item creation using new inventory service
        val component = PhysicalComponent(label = "Test Component")
        
        val inventoryItem = inventoryService.createInventoryItem(
            component = component,
            sku = "TEST-SKU-001",
            label = "Test Inventory Item",
            trackingMode = TrackingMode.DISCRETE,
            initialQuantity = 10.0f,
            location = "Test Location"
        )
        
        assertNotNull(inventoryItem, "Inventory item should be created")
        assertEquals("Test Inventory Item", inventoryItem.label)
        assertEquals(10.0f, inventoryItem.currentQuantity)
        assertEquals(TrackingMode.DISCRETE, inventoryItem.trackingMode)
        
        // Verify it exists in the graph
        val retrievedItem = graphRepository.getEntity(inventoryItem.id)
        assertNotNull(retrievedItem, "Inventory item should exist in graph")
        assertTrue(retrievedItem is com.bscan.model.graph.entities.InventoryItem, "Retrieved entity should be InventoryItem")
    }
    
    @Test
    fun testInventoryCalibration() = runTest {
        // Test inventory calibration functionality
        val inventoryItem = inventoryService.createInventoryItem(
            component = null,
            sku = "CAL-TEST-001",
            label = "Calibration Test Item",
            trackingMode = TrackingMode.CONTINUOUS,
            initialQuantity = 0.0f
        )
        
        val calibrationResult = inventoryService.calibrateInventoryItem(
            inventoryItem = inventoryItem,
            totalWeight = 250.0f,
            tareWeight = 50.0f,
            knownQuantity = 200.0f,
            notes = "Test calibration"
        )
        
        assertTrue(calibrationResult.success, "Calibration should succeed")
        assertNotNull(calibrationResult.unitWeight, "Unit weight should be calculated")
        assertEquals(1.0f, calibrationResult.unitWeight!!, 0.01f) // (250-50)/200 = 1.0
        
        // Verify inventory item was updated
        assertEquals(50.0f, inventoryItem.tareWeight)
        assertEquals(1.0f, inventoryItem.unitWeight)
        assertEquals(250.0f, inventoryItem.currentWeight)
        assertEquals(200.0f, inventoryItem.currentQuantity)
    }
    
    @Test
    fun testBidirectionalInference() = runTest {
        // Test weight ↔ quantity inference
        val inventoryItem = inventoryService.createInventoryItem(
            component = null,
            sku = "INF-TEST-001",
            label = "Inference Test Item",
            trackingMode = TrackingMode.CONTINUOUS,
            initialQuantity = 0.0f,
            unitWeight = 2.5f,
            tareWeight = 100.0f
        )
        
        // Test weight → quantity inference
        val weightMeasurement = inventoryService.recordMeasurement(
            inventoryItem = inventoryItem,
            providedWeight = 150.0f,
            notes = "Weight measurement"
        )
        
        assertTrue(weightMeasurement.success, "Weight measurement should succeed")
        assertEquals(20.0f, weightMeasurement.newQuantity!!, 0.01f) // (150-100)/2.5 = 20
        
        // Test quantity → weight inference
        val quantityMeasurement = inventoryService.recordMeasurement(
            inventoryItem = inventoryItem,
            providedQuantity = 30.0f,
            notes = "Quantity measurement"
        )
        
        assertTrue(quantityMeasurement.success, "Quantity measurement should succeed")
        assertEquals(175.0f, quantityMeasurement.newWeight!!, 0.01f) // 30*2.5+100 = 175
    }
    
    @Test
    fun testStockMovements() = runTest {
        // Test stock movement tracking
        val inventoryItem = inventoryService.createInventoryItem(
            component = null,
            sku = "STOCK-TEST-001",
            label = "Stock Test Item",
            trackingMode = TrackingMode.DISCRETE,
            initialQuantity = 100.0f
        )
        
        // Test consumption
        val consumptionResult = inventoryService.recordStockMovement(
            inventoryItem = inventoryItem,
            movementType = StockMovementType.CONSUMPTION,
            quantityChange = -25.0f,
            reason = "Used in production"
        )
        
        assertTrue(consumptionResult.success, "Consumption should succeed")
        assertEquals(75.0f, consumptionResult.newQuantity!!, 0.01f)
        
        // Test addition
        val additionResult = inventoryService.recordStockMovement(
            inventoryItem = inventoryItem,
            movementType = StockMovementType.ADDITION,
            quantityChange = 50.0f,
            reason = "Restocked"
        )
        
        assertTrue(additionResult.success, "Addition should succeed")
        assertEquals(125.0f, additionResult.newQuantity!!, 0.01f)
        
        // Test insufficient stock protection
        val insufficientResult = inventoryService.recordStockMovement(
            inventoryItem = inventoryItem,
            movementType = StockMovementType.CONSUMPTION,
            quantityChange = -200.0f,
            reason = "Attempt to overconsume"
        )
        
        assertTrue(!insufficientResult.success, "Insufficient stock should be prevented")
        assertNotNull(insufficientResult.error, "Error message should be provided")
    }
    
    @Test
    fun testGraphDataPersistence() = runTest {
        // Test that graph data persists across repository instances
        val entity1 = PhysicalComponent(label = "Persistent Test Entity")
        
        // Add entity to first repository instance
        val added = graphRepository.addEntity(entity1)
        assertTrue(added, "Entity should be added successfully")
        
        // Create new repository instance
        val newGraphRepository = GraphRepository(context)
        val retrievedEntity = newGraphRepository.getEntity(entity1.id)
        
        assertNotNull(retrievedEntity, "Entity should persist across repository instances")
        assertEquals(entity1.label, retrievedEntity.label)
    }
    
    @Test
    fun testCacheStatistics() = runTest {
        // Test ephemeral entity caching
        val scanResult = scanDataService.recordScan(
            rawData = "AABBCCDDEEFF0011",
            scanFormat = "bambu_rfid"
        )
        
        assertTrue(scanResult.success, "Scan should be recorded")
        
        // Generate ephemeral entities to populate cache
        val decodedEncrypted = scanDataService.getDecodedEncrypted(scanResult.rawScanData!!)
        val encodedDecrypted = scanDataService.getEncodedDecrypted(scanResult.rawScanData!!)
        val decodedDecrypted = scanDataService.getDecodedDecrypted(scanResult.rawScanData!!)
        
        assertNotNull(decodedEncrypted, "Decoded encrypted should be generated")
        assertNotNull(encodedDecrypted, "Encoded decrypted should be generated")
        assertNotNull(decodedDecrypted, "Decoded decrypted should be generated")
        
        // Check cache statistics
        val stats = scanDataService.getCacheStatistics()
        assertTrue(stats.totalEntries > 0, "Cache should contain entries")
        assertTrue(stats.hitRate >= 0.0f, "Hit rate should be non-negative")
    }
}