package com.bscan.service

import com.bscan.model.graph.Edge
import com.bscan.model.graph.Graph
import com.bscan.model.graph.entities.*
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Service for managing inventory operations with bidirectional weight/quantity inference.
 * Handles persistent inventory tracking, stock movements, and measurement activities.
 */
class InventoryService(private val graphRepository: GraphRepository) {
    
    /**
     * Create an inventory item to track a component
     */
    suspend fun createInventoryItem(
        component: PhysicalComponent?,
        sku: String?,
        label: String,
        trackingMode: TrackingMode,
        initialQuantity: Float = 0f,
        initialWeight: Float? = null,
        tareWeight: Float? = null,
        unitWeight: Float? = null,
        location: String? = null
    ): InventoryItem = withContext(Dispatchers.IO) {
        
        val inventoryItem = InventoryItem(
            label = label,
            trackingMode = trackingMode
        ).apply {
            currentQuantity = initialQuantity
            currentWeight = initialWeight
            this.tareWeight = tareWeight
            this.unitWeight = unitWeight
            this.location = location
        }
        
        graphRepository.addEntity(inventoryItem)
        
        // Link to tracked component if provided
        component?.let { comp ->
            val existingComponent = graphRepository.getEntity(comp.id)
            if (existingComponent == null) {
                graphRepository.addEntity(comp)
            }
            val edge = Edge(
                fromEntityId = inventoryItem.id,
                toEntityId = comp.id,
                relationshipType = InventoryRelationshipTypes.TRACKS
            )
            graphRepository.addEdge(edge)
        }
        
        // Link to SKU virtual entity if provided
        sku?.let { skuValue ->
            val skuEntity = Virtual(
                virtualType = "sku",
                label = skuValue
            ).apply {
                setProperty("sku", skuValue)
            }
            
            graphRepository.addEntity(skuEntity)
            val edge = Edge(
                fromEntityId = inventoryItem.id,
                toEntityId = skuEntity.id,
                relationshipType = InventoryRelationshipTypes.TRACKS
            )
            graphRepository.addEdge(edge)
        }
        inventoryItem
    }
    
    /**
     * Perform calibration to establish weight/unit relationships
     */
    suspend fun calibrateInventoryItem(
        inventoryItem: InventoryItem,
        totalWeight: Float,
        tareWeight: Float?,
        knownQuantity: Float,
        notes: String = "Calibration"
    ): CalibrationResult = withContext(Dispatchers.IO) {
        
        val calibrationActivity = CalibrationActivity(
            label = "Calibrate ${inventoryItem.label}"
        ).apply {
            this.totalWeight = totalWeight
            this.tareWeight = tareWeight
            this.knownQuantity = knownQuantity
        }
        
        val result = calibrationActivity.performCalibration()
        
        if (result.success) {
            // Update inventory item with calibration results
            inventoryItem.tareWeight = tareWeight
            inventoryItem.unitWeight = result.unitWeight
            inventoryItem.currentWeight = totalWeight
            inventoryItem.currentQuantity = knownQuantity
        }
        
        graphRepository.addEntity(calibrationActivity)
        
        // Link calibration to inventory item
        val edge = Edge(
            fromEntityId = inventoryItem.id,
            toEntityId = calibrationActivity.id,
            relationshipType = InventoryRelationshipTypes.CALIBRATED_BY
        )
        graphRepository.addEdge(edge)
        result
    }
    
    /**
     * Record a measurement with bidirectional inference
     */
    suspend fun recordMeasurement(
        inventoryItem: InventoryItem,
        providedWeight: Float? = null,
        providedQuantity: Float? = null,
        notes: String = "Measurement"
    ): MeasurementResult = withContext(Dispatchers.IO) {
        
        if (providedWeight == null && providedQuantity == null) {
            return@withContext MeasurementResult(
                success = false,
                error = "Must provide either weight or quantity"
            )
        }
        
        val previousWeight = inventoryItem.currentWeight
        val previousQuantity = inventoryItem.currentQuantity
        
        val measurementActivity = MeasurementActivity(
            label = "Measure ${inventoryItem.label}"
        ).apply {
            this.providedWeight = providedWeight
            this.providedQuantity = providedQuantity
            this.previousWeight = previousWeight
            this.previousQuantity = previousQuantity
        }
        
        // Perform inference
        val inferenceResult = when {
            providedWeight != null -> {
                inventoryItem.inferFromWeight(providedWeight)
            }
            providedQuantity != null -> {
                inventoryItem.inferFromQuantity(providedQuantity)
            }
            else -> null
        }
        
        if (inferenceResult != null) {
            // Update measurement with inference results
            measurementActivity.inferredWeight = inferenceResult.inferredWeight
            measurementActivity.inferredQuantity = inferenceResult.inferredQuantity
            measurementActivity.confidence = inferenceResult.confidence
            measurementActivity.inferenceMethod = inferenceResult.method
            
            // Calculate changes
            previousWeight?.let { prevWeight ->
                measurementActivity.weightChange = inferenceResult.inferredWeight - prevWeight
            }
            measurementActivity.quantityChange = inferenceResult.inferredQuantity - previousQuantity
            
            // Update inventory item
            inventoryItem.currentWeight = inferenceResult.inferredWeight
            inventoryItem.currentQuantity = inferenceResult.inferredQuantity
        }
        
        graphRepository.addEntity(measurementActivity)
        
        // Link measurement to inventory item
        val edge = Edge(
            fromEntityId = inventoryItem.id,
            toEntityId = measurementActivity.id,
            relationshipType = InventoryRelationshipTypes.MEASURED_BY
        )
        graphRepository.addEdge(edge)
        
        MeasurementResult(
            success = true,
            newQuantity = inventoryItem.currentQuantity,
            newWeight = inventoryItem.currentWeight,
            quantityChange = measurementActivity.quantityChange,
            weightChange = measurementActivity.weightChange,
            confidence = measurementActivity.confidence,
            method = measurementActivity.inferenceMethod
        )
    }
    
    /**
     * Record a stock movement (consumption, addition, etc.)
     */
    suspend fun recordStockMovement(
        inventoryItem: InventoryItem,
        movementType: StockMovementType,
        quantityChange: Float,
        weightChange: Float? = null,
        reason: String? = null,
        batchNumber: String? = null,
        cost: Float? = null,
        supplier: String? = null
    ): StockMovementResult = withContext(Dispatchers.IO) {
        
        val newQuantity = inventoryItem.currentQuantity + quantityChange
        if (newQuantity < 0) {
            return@withContext StockMovementResult(
                success = false,
                error = "Insufficient stock: current ${inventoryItem.currentQuantity}, requested change $quantityChange"
            )
        }
        
        val stockMovement = StockMovementActivity(
            movementType = movementType,
            label = "${movementType.name} ${inventoryItem.label}"
        ).apply {
            this.quantityChange = quantityChange
            this.weightChange = weightChange
            this.newQuantity = newQuantity
            this.reason = reason
            this.batchNumber = batchNumber
            this.cost = cost
            this.supplier = supplier
            
            // Infer weight if not provided
            if (weightChange == null) {
                inventoryItem.inferFromQuantity(newQuantity)?.let { inference ->
                    this.newWeight = inference.inferredWeight
                    inventoryItem.currentWeight?.let { currentWeight ->
                        this.weightChange = inference.inferredWeight - currentWeight
                    }
                }
            } else {
                this.newWeight = (inventoryItem.currentWeight ?: 0f) + weightChange
            }
        }
        
        // Update inventory item
        inventoryItem.currentQuantity = newQuantity
        stockMovement.newWeight?.let { inventoryItem.currentWeight = it }
        
        graphRepository.addEntity(stockMovement)
        
        // Link movement to inventory item
        val edge = Edge(
            fromEntityId = inventoryItem.id,
            toEntityId = stockMovement.id,
            relationshipType = InventoryRelationshipTypes.HAD_MOVEMENT
        )
        graphRepository.addEdge(edge)
        
        StockMovementResult(
            success = true,
            newQuantity = newQuantity,
            newWeight = inventoryItem.currentWeight,
            quantityChange = quantityChange,
            weightChange = stockMovement.weightChange
        )
    }
    
    /**
     * Get all inventory items
     */
    suspend fun getAllInventoryItems(): List<InventoryItem> = withContext(Dispatchers.IO) {
        graphRepository.getEntitiesByType("inventory_item").filterIsInstance<InventoryItem>()
    }
    
    /**
     * Get inventory items below reorder level
     */
    suspend fun getItemsBelowReorderLevel(): List<InventoryItem> = withContext(Dispatchers.IO) {
        getAllInventoryItems().filter { item ->
            val reorderLevel = item.reorderLevel
            reorderLevel != null && item.currentQuantity <= reorderLevel
        }
    }
    
    /**
     * Get stock movement history for an inventory item
     */
    suspend fun getStockMovementHistory(inventoryItem: InventoryItem): List<StockMovementActivity> = withContext(Dispatchers.IO) {
        val movements = graphRepository.getConnectedEntities(inventoryItem.id, InventoryRelationshipTypes.HAD_MOVEMENT)
        movements.filterIsInstance<StockMovementActivity>()
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Get measurement history for an inventory item
     */
    suspend fun getMeasurementHistory(inventoryItem: InventoryItem): List<MeasurementActivity> = withContext(Dispatchers.IO) {
        val measurements = graphRepository.getConnectedEntities(inventoryItem.id, InventoryRelationshipTypes.MEASURED_BY)
        measurements.filterIsInstance<MeasurementActivity>()
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Get the component or SKU being tracked by an inventory item
     */
    suspend fun getTrackedEntity(inventoryItem: InventoryItem): com.bscan.model.graph.Entity? = withContext(Dispatchers.IO) {
        val trackedEntities = graphRepository.getConnectedEntities(inventoryItem.id, InventoryRelationshipTypes.TRACKS)
        trackedEntities.firstOrNull()
    }
}

/**
 * Result of measurement operation
 */
data class MeasurementResult(
    val success: Boolean,
    val newQuantity: Float? = null,
    val newWeight: Float? = null,
    val quantityChange: Float? = null,
    val weightChange: Float? = null,
    val confidence: Float? = null,
    val method: String? = null,
    val error: String? = null
)

/**
 * Result of stock movement operation
 */
data class StockMovementResult(
    val success: Boolean,
    val newQuantity: Float? = null,
    val newWeight: Float? = null,
    val quantityChange: Float? = null,
    val weightChange: Float? = null,
    val error: String? = null
)