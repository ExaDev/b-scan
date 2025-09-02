package com.bscan.service

import com.bscan.model.graph.entities.*
import com.bscan.model.graph.Edge
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Service for managing unit weight calibration and weight-based quantity inference
 * Perfect for bulk items like screws, nuts, pellets, etc.
 */
class CalibrationService(
    private val graphRepository: GraphRepository
) {
    
    /**
     * Calibrate unit weight from count and total weight
     * Example: "I have 100 screws weighing 247g total, box weighs 47g"
     */
    suspend fun calibrateFromCount(
        inventoryItemId: String,
        totalWeight: Float,
        containerWeight: Float?,
        knownQuantity: Int
    ): CalibrationServiceResult = withContext(Dispatchers.IO) {
        
        val inventoryItem = graphRepository.getEntity(inventoryItemId) as? InventoryItem
            ?: return@withContext CalibrationServiceResult(
                success = false,
                error = "Inventory item not found"
            )
        
        // Perform calibration on entity
        val result = inventoryItem.calibrateUnitWeight(
            totalWeight = totalWeight,
            knownQuantity = knownQuantity.toFloat(),
            containerWeight = containerWeight
        )
        
        if (!result.success) {
            return@withContext CalibrationServiceResult(
                success = false,
                error = result.error
            )
        }
        
        // Create calibration activity for audit trail
        val calibrationActivity = CalibrationActivity(
            label = "Unit Weight Calibration - ${inventoryItem.label}"
        ).apply {
            this.totalWeight = totalWeight
            this.tareWeight = containerWeight
            this.knownQuantity = knownQuantity.toFloat()
            this.calculatedUnitWeight = result.unitWeight
            this.calculatedNetWeight = result.netWeight
            this.calibrationAccuracy = result.accuracy
        }
        
        graphRepository.addEntity(calibrationActivity)
        
        // Link calibration to inventory item
        val edge = Edge(
            fromEntityId = inventoryItem.id,
            toEntityId = calibrationActivity.id,
            relationshipType = InventoryRelationshipTypes.CALIBRATED_BY
        )
        graphRepository.addEdge(edge)
        
        // Update inventory item in repository
        graphRepository.addEntity(inventoryItem)
        
        CalibrationServiceResult(
            success = true,
            unitWeight = result.unitWeight,
            containerWeight = containerWeight,
            calibrationMethod = "COUNTED_AND_WEIGHED",
            confidence = result.accuracy,
            calibrationActivityId = calibrationActivity.id
        )
    }
    
    /**
     * Learn container weight after initial calibration
     * Example: After initial calibration without knowing box weight,
     * user later weighs empty box to improve accuracy
     */
    suspend fun learnContainerWeight(
        inventoryItemId: String,
        emptyContainerWeight: Float
    ): CalibrationServiceResult = withContext(Dispatchers.IO) {
        
        val inventoryItem = graphRepository.getEntity(inventoryItemId) as? InventoryItem
            ?: return@withContext CalibrationServiceResult(
                success = false,
                error = "Inventory item not found"
            )
        
        val result = inventoryItem.learnContainerWeight(emptyContainerWeight)
        
        if (!result.success) {
            return@withContext CalibrationServiceResult(
                success = false,
                error = result.error
            )
        }
        
        // Create learning activity
        val learningActivity = CalibrationActivity(
            label = "Container Weight Learning - ${inventoryItem.label}"
        ).apply {
            this.tareWeight = emptyContainerWeight
            this.calculatedUnitWeight = result.unitWeight
            this.calibrationAccuracy = result.accuracy
            setProperty("notes", "Learned container weight after initial calibration")
        }
        
        graphRepository.addEntity(learningActivity)
        
        // Link to inventory item
        val edge = Edge(
            fromEntityId = inventoryItem.id,
            toEntityId = learningActivity.id,
            relationshipType = InventoryRelationshipTypes.CALIBRATED_BY
        )
        graphRepository.addEdge(edge)
        
        // Update inventory item
        graphRepository.addEntity(inventoryItem)
        
        CalibrationServiceResult(
            success = true,
            unitWeight = result.unitWeight,
            containerWeight = emptyContainerWeight,
            calibrationMethod = "CONTAINER_WEIGHT_LEARNED",
            confidence = result.accuracy,
            calibrationActivityId = learningActivity.id
        )
    }
    
    /**
     * Infer quantity from new weight measurement and record consumption
     * Example: "Box now weighs 187g, how many screws are left?"
     */
    suspend fun inferQuantityFromWeight(
        inventoryItemId: String,
        currentWeight: Float,
        notes: String? = null
    ): QuantityInferenceResult = withContext(Dispatchers.IO) {
        
        val inventoryItem = graphRepository.getEntity(inventoryItemId) as? InventoryItem
            ?: return@withContext QuantityInferenceResult(
                success = false,
                error = "Inventory item not found"
            )
        
        if (!inventoryItem.isProperlyCalibrated()) {
            return@withContext QuantityInferenceResult(
                success = false,
                error = "Item is not properly calibrated. Please calibrate unit weight first."
            )
        }
        
        val updateResult = inventoryItem.updateFromWeightMeasurement(currentWeight)
        
        if (!updateResult.success) {
            return@withContext QuantityInferenceResult(
                success = false,
                error = updateResult.error
            )
        }
        
        // Create measurement activity
        val measurementActivity = MeasurementActivity(
            label = "Weight-based Quantity Update - ${inventoryItem.label}"
        ).apply {
            this.providedWeight = currentWeight
            this.inferredQuantity = updateResult.newQuantity
            this.inferredWeight = currentWeight
            this.confidence = updateResult.confidence
            this.inferenceMethod = updateResult.inferenceMethod
            notes?.let { setProperty("notes", it) }
        }
        
        graphRepository.addEntity(measurementActivity)
        
        // Link measurement to inventory item
        val measurementEdge = Edge(
            fromEntityId = inventoryItem.id,
            toEntityId = measurementActivity.id,
            relationshipType = InventoryRelationshipTypes.MEASURED_BY
        )
        graphRepository.addEdge(measurementEdge)
        
        // If quantity was consumed, create stock movement activity
        if (updateResult.quantityConsumed > 0) {
            val stockMovement = StockMovementActivity(
                movementType = StockMovementType.CONSUMPTION,
                label = "Consumption (inferred from weight) - ${inventoryItem.label}"
            ).apply {
                this.quantityChange = -updateResult.quantityConsumed
                this.weightChange = -(inventoryItem.unitWeight?.times(updateResult.quantityConsumed) ?: 0f)
                this.reason = "Weight-based consumption inference (${updateResult.confidence}% confidence)"
                notes?.let { setProperty("notes", it) }
                // Store confidence as custom property
                setProperty("inferenceConfidence", updateResult.confidence)
            }
            
            graphRepository.addEntity(stockMovement)
            
            // Link stock movement to inventory item
            val stockEdge = Edge(
                fromEntityId = inventoryItem.id,
                toEntityId = stockMovement.id,
                relationshipType = InventoryRelationshipTypes.HAD_MOVEMENT
            )
            graphRepository.addEdge(stockEdge)
        }
        
        // Update inventory item in repository
        graphRepository.addEntity(inventoryItem)
        
        QuantityInferenceResult(
            success = true,
            newQuantity = updateResult.newQuantity,
            quantityConsumed = updateResult.quantityConsumed,
            newWeight = currentWeight,
            confidence = updateResult.confidence,
            inferenceMethod = updateResult.inferenceMethod,
            measurementActivityId = measurementActivity.id
        )
    }
    
    /**
     * Estimate unit weight from bulk item type
     * Used as initial guess before proper calibration
     */
    suspend fun estimateUnitWeight(
        inventoryItemId: String,
        itemType: BulkItemType
    ): CalibrationServiceResult = withContext(Dispatchers.IO) {
        
        val inventoryItem = graphRepository.getEntity(inventoryItemId) as? InventoryItem
            ?: return@withContext CalibrationServiceResult(
                success = false,
                error = "Inventory item not found"
            )
        
        val estimatedWeight = when (itemType) {
            BulkItemType.M3_SCREW_8MM -> 0.5f      // grams
            BulkItemType.M3_SCREW_12MM -> 0.7f
            BulkItemType.M3_SCREW_16MM -> 0.9f
            BulkItemType.M4_SCREW_8MM -> 0.8f
            BulkItemType.M4_SCREW_12MM -> 1.1f
            BulkItemType.M4_SCREW_16MM -> 1.4f
            BulkItemType.M5_SCREW_8MM -> 1.2f
            BulkItemType.M5_SCREW_12MM -> 1.6f
            BulkItemType.M5_SCREW_16MM -> 2.0f
            BulkItemType.M3_NUT -> 0.3f
            BulkItemType.M4_NUT -> 0.6f
            BulkItemType.M5_NUT -> 1.0f
            BulkItemType.SMALL_WASHER -> 0.1f
            BulkItemType.MEDIUM_WASHER -> 0.2f
            BulkItemType.LARGE_WASHER -> 0.4f
            BulkItemType.GENERIC_SMALL -> 0.5f
            BulkItemType.GENERIC_MEDIUM -> 2.0f
            BulkItemType.GENERIC_LARGE -> 10.0f
        }
        
        inventoryItem.unitWeight = estimatedWeight
        inventoryItem.calibrationMethod = "ESTIMATED"
        inventoryItem.calibrationConfidence = 30f  // Low confidence for estimates
        inventoryItem.lastCalibratedAt = LocalDateTime.now().toString()
        
        graphRepository.addEntity(inventoryItem)
        
        CalibrationServiceResult(
            success = true,
            unitWeight = estimatedWeight,
            calibrationMethod = "ESTIMATED",
            confidence = 30f
        )
    }
    
    /**
     * Get calibration recommendations for an item
     */
    suspend fun getCalibrationRecommendations(
        inventoryItemId: String
    ): CalibrationRecommendations = withContext(Dispatchers.IO) {
        
        val inventoryItem = graphRepository.getEntity(inventoryItemId) as? InventoryItem
            ?: return@withContext CalibrationRecommendations(
                needsCalibration = true,
                recommendations = listOf("Item not found")
            )
        
        val recommendations = mutableListOf<String>()
        var needsCalibration = false
        
        when {
            !inventoryItem.isProperlyCalibrated() -> {
                needsCalibration = true
                recommendations.add("No calibration data found")
                recommendations.add("Weigh container with known quantity")
                if (inventoryItem.containerWeight == null) {
                    recommendations.add("Weigh empty container separately for best accuracy")
                }
            }
            
            inventoryItem.calibrationMethod == "ESTIMATED" -> {
                needsCalibration = true
                recommendations.add("Currently using estimated unit weight")
                recommendations.add("Weigh with known count for accurate calibration")
            }
            
            (inventoryItem.calibrationConfidence ?: 0f) < 80f -> {
                recommendations.add("Calibration confidence is low")
                recommendations.add("Consider re-calibrating with larger sample size")
            }
            
            inventoryItem.containerWeight == null -> {
                recommendations.add("Consider weighing empty container for improved accuracy")
            }
            
            else -> {
                recommendations.add("Calibration looks good!")
                recommendations.add("Current status: ${inventoryItem.getCalibrationStatus()}")
            }
        }
        
        CalibrationRecommendations(
            needsCalibration = needsCalibration,
            currentStatus = inventoryItem.getCalibrationStatus(),
            confidence = inventoryItem.calibrationConfidence,
            unitWeight = inventoryItem.unitWeight,
            containerWeight = inventoryItem.containerWeight,
            recommendations = recommendations
        )
    }
}

/**
 * Bulk item types with estimated weights
 */
enum class BulkItemType {
    // Screws
    M3_SCREW_8MM, M3_SCREW_12MM, M3_SCREW_16MM,
    M4_SCREW_8MM, M4_SCREW_12MM, M4_SCREW_16MM,
    M5_SCREW_8MM, M5_SCREW_12MM, M5_SCREW_16MM,
    
    // Nuts
    M3_NUT, M4_NUT, M5_NUT,
    
    // Washers
    SMALL_WASHER, MEDIUM_WASHER, LARGE_WASHER,
    
    // Generic sizes
    GENERIC_SMALL, GENERIC_MEDIUM, GENERIC_LARGE
}

/**
 * Result of calibration service operations
 */
data class CalibrationServiceResult(
    val success: Boolean,
    val unitWeight: Float? = null,
    val containerWeight: Float? = null,
    val calibrationMethod: String? = null,
    val confidence: Float? = null,
    val calibrationActivityId: String? = null,
    val error: String? = null
)

/**
 * Result of quantity inference from weight
 */
data class QuantityInferenceResult(
    val success: Boolean,
    val newQuantity: Float = 0f,
    val quantityConsumed: Float = 0f,
    val newWeight: Float = 0f,
    val confidence: Float = 0f,
    val inferenceMethod: String = "",
    val measurementActivityId: String? = null,
    val error: String? = null
)

/**
 * Calibration recommendations for an item
 */
data class CalibrationRecommendations(
    val needsCalibration: Boolean,
    val currentStatus: String? = null,
    val confidence: Float? = null,
    val unitWeight: Float? = null,
    val containerWeight: Float? = null,
    val recommendations: List<String> = emptyList()
)