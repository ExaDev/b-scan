package com.bscan.service

import com.bscan.model.graph.Edge
import com.bscan.model.graph.entities.*
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.max

/**
 * Service for managing composite consumption tracking with intelligent distribution
 * across multiple related entities. Supports scenarios like filament spools where
 * total weight includes packaging, core, and consumable material.
 */
class CompositeConsumptionService(
    private val graphRepository: GraphRepository,
    private val inventoryService: InventoryService
) {
    
    /**
     * Calculate consumption from composite weight measurement
     */
    suspend fun calculateCompositeConsumption(
        compositeEntityId: String,
        measuredWeight: Float,
        unit: String = "g"
    ): ConsumptionCalculation = withContext(Dispatchers.IO) {
        
        val compositeEntity = graphRepository.getEntity(compositeEntityId)
            ?: return@withContext ConsumptionCalculation(
                success = false,
                error = "Composite entity not found"
            )
        
        // Get all components of the composite entity
        val components = getCompositeComponents(compositeEntityId)
        
        // Separate fixed mass and consumable components
        val fixedComponents = components.filter { !it.isConsumable }
        val consumableComponents = components.filter { it.isConsumable }
        
        if (consumableComponents.isEmpty()) {
            return@withContext ConsumptionCalculation(
                success = false,
                error = "No consumable components found in composite"
            )
        }
        
        // Calculate total fixed mass
        val totalFixedMass = fixedComponents.mapNotNull { it.fixedMass }.sum()
        
        // Get previous composite weight if available
        val previousCompositeWeight = compositeEntity.getProperty<Float>("lastCompositeWeight")
        
        // Calculate consumption
        val consumableMass = measuredWeight - totalFixedMass
        val totalConsumption = if (previousCompositeWeight != null) {
            val previousConsumableMass = previousCompositeWeight - totalFixedMass
            max(0f, previousConsumableMass - consumableMass)
        } else {
            0f // First measurement, no consumption yet
        }
        
        ConsumptionCalculation(
            success = true,
            compositeEntityId = compositeEntityId,
            measuredWeight = measuredWeight,
            previousCompositeWeight = previousCompositeWeight,
            totalFixedMass = totalFixedMass,
            consumableMass = consumableMass,
            totalConsumption = totalConsumption,
            fixedComponents = fixedComponents.associate { 
                it.id to (it.fixedMass ?: 0f) 
            },
            consumableComponents = consumableComponents.associate { 
                it.id to it.currentQuantity 
            }
        )
    }
    
    /**
     * Distribute consumption across multiple consumable entities
     */
    suspend fun distributeConsumption(
        calculation: ConsumptionCalculation,
        distributionMethod: DistributionMethod = DistributionMethod.PROPORTIONAL,
        userDistributions: Map<String, Float>? = null
    ): DistributionResult = withContext(Dispatchers.IO) {
        
        if (calculation.totalConsumption <= 0f) {
            return@withContext DistributionResult(
                success = false,
                error = "No consumption to distribute"
            )
        }
        
        val consumableEntities = calculation.consumableComponents.keys.mapNotNull { entityId ->
            graphRepository.getEntity(entityId) as? InventoryItem
        }
        
        if (consumableEntities.isEmpty()) {
            return@withContext DistributionResult(
                success = false,
                error = "No consumable inventory items found"
            )
        }
        
        val distributions = when (distributionMethod) {
            DistributionMethod.PROPORTIONAL -> calculateProportionalDistribution(
                consumableEntities, calculation.totalConsumption
            )
            DistributionMethod.EQUAL_SPLIT -> calculateEqualSplitDistribution(
                consumableEntities, calculation.totalConsumption
            )
            DistributionMethod.USER_SPECIFIED -> userDistributions ?: run {
                return@withContext DistributionResult(
                    success = false,
                    error = "User distributions required for USER_SPECIFIED method"
                )
            }
            DistributionMethod.WEIGHTED -> calculateWeightedDistribution(
                consumableEntities, calculation.totalConsumption
            )
            DistributionMethod.INFERRED -> calculateInferredDistribution(
                consumableEntities, calculation.totalConsumption
            )
        }
        
        // Validate distributions
        val totalDistributed = distributions.values.sum()
        val distributionError = abs(totalDistributed - calculation.totalConsumption)
        val confidence = calculateDistributionConfidence(
            distributions, consumableEntities, distributionMethod
        )
        
        if (distributionError > 0.01f) { // Allow small rounding errors
            return@withContext DistributionResult(
                success = false,
                error = "Distribution total ($totalDistributed) doesn't match consumption (${calculation.totalConsumption})"
            )
        }
        
        DistributionResult(
            success = true,
            distributions = distributions,
            confidence = confidence,
            method = distributionMethod,
            totalDistributed = totalDistributed
        )
    }
    
    /**
     * Record the consumption distribution as a persistent activity
     */
    suspend fun recordConsumptionDistribution(
        calculation: ConsumptionCalculation,
        distribution: DistributionResult,
        notes: String? = null
    ): RecordingResult = withContext(Dispatchers.IO) {
        
        // Create consumption distribution activity
        val distributionActivity = ConsumptionDistributionActivity(
            label = "Consumption Distribution - ${calculation.compositeEntityId}"
        ).apply {
            compositeEntityId = calculation.compositeEntityId
            measuredWeight = calculation.measuredWeight
            previousCompositeWeight = calculation.previousCompositeWeight
            totalConsumption = calculation.totalConsumption
            distributionMethod = distribution.method
            distributionConfidence = distribution.confidence
            distributions = distribution.distributions
            fixedComponents = calculation.fixedComponents
            consumableComponents = calculation.consumableComponents
            this.notes = notes
        }
        
        graphRepository.addEntity(distributionActivity)
        
        // Create stock movement activities for each distribution
        val stockMovements = mutableListOf<StockMovementActivity>()
        
        for ((entityId, consumptionAmount) in distribution.distributions) {
            val inventoryItem = graphRepository.getEntity(entityId) as? InventoryItem
                ?: continue
            
            // Record consumption as negative stock movement
            val result = inventoryService.recordStockMovement(
                inventoryItem = inventoryItem,
                movementType = StockMovementType.CONSUMPTION,
                quantityChange = -consumptionAmount,
                reason = "Composite consumption distribution",
                weightChange = -consumptionAmount // Assuming 1:1 weight to quantity for simplicity
            )
            
            if (result.success) {
                // Link distribution activity to inventory item
                val edge = Edge(
                    fromEntityId = distributionActivity.id,
                    toEntityId = inventoryItem.id,
                    relationshipType = InventoryRelationshipTypes.DISTRIBUTED_TO
                )
                graphRepository.addEdge(edge)
            }
        }
        
        // Update composite entity's last weight
        val compositeEntity = graphRepository.getEntity(calculation.compositeEntityId)
        compositeEntity?.setProperty("lastCompositeWeight", calculation.measuredWeight)
        
        // Link composite entity to distribution activity
        val compositeEdge = Edge(
            fromEntityId = calculation.compositeEntityId,
            toEntityId = distributionActivity.id,
            relationshipType = InventoryRelationshipTypes.MEASURED_AS_COMPOSITE
        )
        graphRepository.addEdge(compositeEdge)
        
        RecordingResult(
            success = true,
            distributionActivityId = distributionActivity.id,
            affectedEntityIds = distribution.distributions.keys.toList()
        )
    }
    
    /**
     * Infer unknown component weights from composite measurement
     * Perfect for scenarios like: total weight = cardboard core + filament + UNKNOWN spool weight
     */
    suspend fun inferUnknownComponentWeights(
        compositeEntityId: String,
        measuredTotalWeight: Float,
        knownComponents: Map<String, Float> = emptyMap()  // entityId -> known weight
    ): ComponentInferenceResult = withContext(Dispatchers.IO) {
        
        val compositeEntity = graphRepository.getEntity(compositeEntityId)
            ?: return@withContext ComponentInferenceResult(
                success = false,
                error = "Composite entity not found"
            )
        
        val components = getCompositeComponents(compositeEntityId)
        
        // Separate known and unknown weight components
        val knownWeightTotal = components.sumOf { component ->
            // Priority: user-provided known weights > stored fixedMass > stored currentWeight
            when {
                knownComponents.containsKey(component.id) -> knownComponents[component.id]!!.toDouble()
                component.fixedMass != null -> component.fixedMass!!.toDouble()
                component.currentWeight != null -> component.currentWeight!!.toDouble()
                else -> 0.0
            }
        }.toFloat()
        
        val unknownComponents = components.filter { component ->
            !knownComponents.containsKey(component.id) && 
            component.fixedMass == null &&
            component.currentWeight == null
        }
        
        if (unknownComponents.isEmpty()) {
            return@withContext ComponentInferenceResult(
                success = false,
                error = "No unknown components to infer weights for"
            )
        }
        
        val totalUnknownWeight = measuredTotalWeight - knownWeightTotal
        
        if (totalUnknownWeight < 0) {
            return@withContext ComponentInferenceResult(
                success = false,
                error = "Known component weights exceed total measured weight"
            )
        }
        
        // Strategy for distributing unknown weight among unknown components
        val inferredWeights = when (unknownComponents.size) {
            1 -> {
                // Single unknown component gets all remaining weight
                mapOf(unknownComponents.first().id to totalUnknownWeight)
            }
            else -> {
                // Multiple unknowns: distribute based on component type priority
                distributeUnknownWeight(unknownComponents, totalUnknownWeight)
            }
        }
        
        ComponentInferenceResult(
            success = true,
            measuredTotalWeight = measuredTotalWeight,
            knownComponentWeights = components.associate { component ->
                component.id to (knownComponents[component.id] 
                    ?: component.fixedMass 
                    ?: component.currentWeight 
                    ?: 0f)
            }.filterValues { it > 0 },
            inferredWeights = inferredWeights,
            totalUnknownWeight = totalUnknownWeight,
            confidence = calculateInferenceConfidence(unknownComponents, totalUnknownWeight)
        )
    }
    
    /**
     * Apply inferred weights to components and create calibration records
     */
    suspend fun applyInferredWeights(
        inferenceResult: ComponentInferenceResult,
        updateFixedMass: Boolean = true,
        notes: String = "Weight inferred from composite measurement"
    ): ApplyInferenceResult = withContext(Dispatchers.IO) {
        
        val updatedComponents = mutableListOf<String>()
        val calibrationActivities = mutableListOf<String>()
        
        for ((entityId, inferredWeight) in inferenceResult.inferredWeights) {
            val component = graphRepository.getEntity(entityId) as? InventoryItem
                ?: continue
            
            if (updateFixedMass) {
                // Set as fixed mass for non-consumable components (like spools)
                component.fixedMass = inferredWeight
                component.isConsumable = false
            } else {
                // Set as current weight for consumable components
                component.currentWeight = inferredWeight
                component.currentQuantity = inferredWeight
            }
            
            updatedComponents.add(entityId)
            
            // Create a calibration activity to record this inference
            val calibrationActivity = CalibrationActivity(
                label = "Inferred weight for ${component.label}"
            ).apply {
                totalWeight = inferenceResult.measuredTotalWeight
                knownQuantity = inferredWeight
                calculatedUnitWeight = 1.0f  // 1g per 1g for weight-based items
                calculatedNetWeight = inferredWeight
                calibrationAccuracy = inferenceResult.confidence
                result = "Weight inferred from composite measurement"
            }
            
            graphRepository.addEntity(calibrationActivity)
            calibrationActivities.add(calibrationActivity.id)
            
            // Link calibration to component
            val edge = Edge(
                fromEntityId = component.id,
                toEntityId = calibrationActivity.id,
                relationshipType = InventoryRelationshipTypes.CALIBRATED_BY
            )
            graphRepository.addEdge(edge)
        }
        
        ApplyInferenceResult(
            success = true,
            updatedComponentIds = updatedComponents,
            calibrationActivityIds = calibrationActivities
        )
    }
    
    /**
     * Distribute unknown weight among components based on type and characteristics
     */
    private fun distributeUnknownWeight(
        unknownComponents: List<InventoryItem>,
        totalUnknownWeight: Float
    ): Map<String, Float> {
        // Priority-based distribution:
        // 1. Single largest unknown component (likely the main container/spool)
        // 2. Equal distribution among remaining unknowns
        
        // Simple heuristic: component with longest label name likely the main item
        val mainComponent = unknownComponents.maxByOrNull { it.label.length }
        
        return if (unknownComponents.size == 1) {
            mapOf(unknownComponents.first().id to totalUnknownWeight)
        } else if (mainComponent != null) {
            // Give 80% to main component, distribute rest equally
            val mainWeight = totalUnknownWeight * 0.8f
            val otherWeight = (totalUnknownWeight * 0.2f) / (unknownComponents.size - 1)
            
            unknownComponents.associate { component ->
                component.id to if (component.id == mainComponent.id) mainWeight else otherWeight
            }
        } else {
            // Equal distribution fallback
            val equalWeight = totalUnknownWeight / unknownComponents.size
            unknownComponents.associate { it.id to equalWeight }
        }
    }
    
    /**
     * Calculate confidence level for weight inference
     */
    private fun calculateInferenceConfidence(
        unknownComponents: List<InventoryItem>,
        totalUnknownWeight: Float
    ): Float {
        return when {
            unknownComponents.size == 1 && totalUnknownWeight > 10f -> 0.95f  // Single unknown, reasonable weight
            unknownComponents.size == 1 && totalUnknownWeight > 1f -> 0.85f   // Single unknown, small weight
            unknownComponents.size <= 2 && totalUnknownWeight > 50f -> 0.80f  // Two unknowns, substantial weight
            unknownComponents.size <= 3 -> 0.70f                              // Multiple unknowns
            else -> 0.60f                                                     // Many unknowns, lower confidence
        }
    }
    
    /**
     * Get all components that make up a composite entity
     */
    private suspend fun getCompositeComponents(compositeEntityId: String): List<InventoryItem> {
        val componentEntities = graphRepository.getConnectedEntities(
            compositeEntityId, 
            InventoryRelationshipTypes.HAS_COMPONENT
        )
        
        return componentEntities.filterIsInstance<InventoryItem>()
    }
    
    /**
     * Calculate proportional distribution based on current quantities
     */
    private fun calculateProportionalDistribution(
        entities: List<InventoryItem>,
        totalConsumption: Float
    ): Map<String, Float> {
        val totalQuantity = entities.sumOf { it.currentQuantity.toDouble() }.toFloat()
        
        if (totalQuantity <= 0f) {
            // Equal split if no quantities available
            return calculateEqualSplitDistribution(entities, totalConsumption)
        }
        
        return entities.associate { entity ->
            val proportion = entity.currentQuantity / totalQuantity
            entity.id to (totalConsumption * proportion)
        }
    }
    
    /**
     * Calculate equal split distribution
     */
    private fun calculateEqualSplitDistribution(
        entities: List<InventoryItem>,
        totalConsumption: Float
    ): Map<String, Float> {
        val splitAmount = totalConsumption / entities.size
        return entities.associate { it.id to splitAmount }
    }
    
    /**
     * Calculate weighted distribution based on usage patterns
     * This is a placeholder for future ML-based patterns
     */
    private suspend fun calculateWeightedDistribution(
        entities: List<InventoryItem>,
        totalConsumption: Float
    ): Map<String, Float> {
        // For now, use proportional as fallback
        // Future: implement ML-based weighting from usage history
        return calculateProportionalDistribution(entities, totalConsumption)
    }
    
    /**
     * Calculate AI-inferred distribution
     * This is a placeholder for future ML inference
     */
    private suspend fun calculateInferredDistribution(
        entities: List<InventoryItem>,
        totalConsumption: Float
    ): Map<String, Float> {
        // For now, use proportional as fallback
        // Future: implement ML inference based on patterns, time, etc.
        return calculateProportionalDistribution(entities, totalConsumption)
    }
    
    /**
     * Calculate confidence level for distribution
     */
    private fun calculateDistributionConfidence(
        distributions: Map<String, Float>,
        entities: List<InventoryItem>,
        method: DistributionMethod
    ): Float {
        return when (method) {
            DistributionMethod.USER_SPECIFIED -> 1.0f  // User choice is certain
            DistributionMethod.EQUAL_SPLIT -> 0.7f     // Simple but reasonable
            DistributionMethod.PROPORTIONAL -> {
                // Higher confidence when quantities are well-established
                val avgQuantity = entities.map { it.currentQuantity }.average().toFloat()
                if (avgQuantity > 100f) 0.9f else 0.8f
            }
            DistributionMethod.WEIGHTED -> 0.85f       // Future ML weighting
            DistributionMethod.INFERRED -> 0.75f       // Future ML inference
        }
    }
    
    // ========================================================================================
    // BULK ITEM SUPPORT - Enhanced for box of screws scenario
    // ========================================================================================
    
    /**
     * Calculate consumption for bulk items using weight-based inference
     * Perfect for "box of 100 screws" scenario where we know unit weights
     */
    suspend fun calculateBulkItemConsumption(
        inventoryItemId: String,
        measuredTotalWeight: Float,
        containerWeight: Float? = null
    ): BulkConsumptionResult = withContext(Dispatchers.IO) {
        
        val inventoryItem = graphRepository.getEntity(inventoryItemId) as? InventoryItem
            ?: return@withContext BulkConsumptionResult(
                success = false,
                error = "Inventory item not found"
            )
        
        // Check if item has unit weight calibration
        val unitWeight = inventoryItem.unitWeight
            ?: return@withContext BulkConsumptionResult(
                success = false,
                error = "Item not calibrated - unit weight unknown"
            )
        
        // Calculate net weight (subtract container if provided)
        val effectiveContainerWeight = containerWeight ?: inventoryItem.containerWeight ?: 0f
        val netWeight = measuredTotalWeight - effectiveContainerWeight
        
        if (netWeight <= 0f) {
            return@withContext BulkConsumptionResult(
                success = false,
                error = "Invalid measurement - net weight is zero or negative"
            )
        }
        
        // Infer quantity from weight
        val inferredQuantity = (netWeight / unitWeight).toInt()
        val previousQuantity = inventoryItem.currentQuantity.toInt()
        val quantityConsumed = previousQuantity - inferredQuantity
        
        // Calculate confidence based on measurement precision
        val confidence = calculateBulkInferenceConfidence(
            measuredWeight = measuredTotalWeight,
            containerWeight = effectiveContainerWeight,
            unitWeight = unitWeight,
            inferredQuantity = inferredQuantity
        )
        
        BulkConsumptionResult(
            success = true,
            inventoryItemId = inventoryItemId,
            measuredTotalWeight = measuredTotalWeight,
            containerWeight = effectiveContainerWeight,
            netWeight = netWeight,
            unitWeight = unitWeight,
            previousQuantity = previousQuantity,
            inferredQuantity = inferredQuantity,
            quantityConsumed = quantityConsumed,
            confidence = confidence,
            inferenceMethod = "weight_to_quantity"
        )
    }
    
    /**
     * Calculate consumption for bulk items using count-based inference
     * When user counts items and we infer total weight
     */
    suspend fun calculateBulkItemConsumptionFromCount(
        inventoryItemId: String,
        countedQuantity: Int
    ): BulkConsumptionResult = withContext(Dispatchers.IO) {
        
        val inventoryItem = graphRepository.getEntity(inventoryItemId) as? InventoryItem
            ?: return@withContext BulkConsumptionResult(
                success = false,
                error = "Inventory item not found"
            )
        
        val unitWeight = inventoryItem.unitWeight
            ?: return@withContext BulkConsumptionResult(
                success = false,
                error = "Item not calibrated - unit weight unknown"
            )
        
        // Calculate weights from count
        val netWeight = countedQuantity * unitWeight
        val containerWeight = inventoryItem.containerWeight ?: 0f
        val totalWeight = netWeight + containerWeight
        
        val previousQuantity = inventoryItem.currentQuantity.toInt()
        val quantityConsumed = previousQuantity - countedQuantity
        
        BulkConsumptionResult(
            success = true,
            inventoryItemId = inventoryItemId,
            measuredTotalWeight = totalWeight,
            containerWeight = containerWeight,
            netWeight = netWeight,
            unitWeight = unitWeight,
            previousQuantity = previousQuantity,
            inferredQuantity = countedQuantity,
            quantityConsumed = quantityConsumed,
            confidence = 0.95f, // High confidence when user counted
            inferenceMethod = "count_to_weight"
        )
    }
    
    /**
     * Learn container weight from empty measurement
     * Used after initial calibration to improve accuracy
     */
    suspend fun learnContainerWeightFromBulkItem(
        inventoryItemId: String,
        emptyContainerWeight: Float
    ): ContainerLearningResult = withContext(Dispatchers.IO) {
        
        val inventoryItem = graphRepository.getEntity(inventoryItemId) as? InventoryItem
            ?: return@withContext ContainerLearningResult(
                success = false,
                error = "Inventory item not found"
            )
        
        val previousContainerWeight = inventoryItem.containerWeight
        inventoryItem.containerWeight = emptyContainerWeight
        inventoryItem.lastCalibratedAt = LocalDateTime.now().toString()
        
        // Update calibration confidence since we now know container weight precisely
        val currentConfidence = inventoryItem.calibrationConfidence ?: 0f
        inventoryItem.calibrationConfidence = (currentConfidence + 10f).coerceAtMost(95f)
        
        // Save updated inventory item
        graphRepository.addEntity(inventoryItem)
        
        // Create learning activity for audit trail
        val learningActivity = CalibrationActivity(
            label = "Container Weight Learning - ${inventoryItem.label}"
        ).apply {
            tareWeight = emptyContainerWeight
            calculatedUnitWeight = inventoryItem.unitWeight
            calibrationAccuracy = inventoryItem.calibrationConfidence
            setProperty("previousContainerWeight", previousContainerWeight ?: 0f)
            setProperty("notes", "Learned container weight to improve bulk inference accuracy")
        }
        
        graphRepository.addEntity(learningActivity)
        
        // Link to inventory item
        val edge = Edge(
            fromEntityId = inventoryItem.id,
            toEntityId = learningActivity.id,
            relationshipType = InventoryRelationshipTypes.CALIBRATED_BY
        )
        graphRepository.addEdge(edge)
        
        ContainerLearningResult(
            success = true,
            inventoryItemId = inventoryItemId,
            previousContainerWeight = previousContainerWeight,
            newContainerWeight = emptyContainerWeight,
            improvedConfidence = inventoryItem.calibrationConfidence ?: 0f,
            learningActivityId = learningActivity.id
        )
    }
    
    /**
     * Calculate confidence for bulk item inference
     */
    private fun calculateBulkInferenceConfidence(
        measuredWeight: Float,
        containerWeight: Float,
        unitWeight: Float,
        inferredQuantity: Int
    ): Float {
        // Base confidence on measurement precision and setup
        var confidence = 0.85f
        
        // Adjust for container weight knowledge
        if (containerWeight > 0f) {
            confidence += 0.10f // Knowing container weight improves confidence
        }
        
        // Adjust for unit weight precision
        when {
            unitWeight >= 1.0f -> confidence += 0.05f  // Heavier units easier to measure
            unitWeight >= 0.1f -> confidence += 0.02f  // Moderate precision
            unitWeight < 0.1f -> confidence -= 0.05f   // Very light units harder to measure
        }
        
        // Adjust for quantity size
        when {
            inferredQuantity >= 50 -> confidence += 0.03f  // Larger samples more accurate
            inferredQuantity <= 10 -> confidence -= 0.08f  // Small samples less reliable
        }
        
        // Measurement precision factors
        val netWeight = measuredWeight - containerWeight
        val expectedWeight = inferredQuantity * unitWeight
        val weightError = abs(netWeight - expectedWeight) / expectedWeight
        
        when {
            weightError < 0.02f -> confidence += 0.05f  // Very accurate measurement
            weightError < 0.05f -> confidence += 0.02f  // Good measurement
            weightError > 0.10f -> confidence -= 0.08f  // Less accurate measurement
        }
        
        return confidence.coerceIn(0.60f, 0.98f)
    }
}

/**
 * Result of bulk item consumption calculation
 */
data class BulkConsumptionResult(
    val success: Boolean,
    val inventoryItemId: String? = null,
    val measuredTotalWeight: Float = 0f,
    val containerWeight: Float = 0f,
    val netWeight: Float = 0f,
    val unitWeight: Float = 0f,
    val previousQuantity: Int = 0,
    val inferredQuantity: Int = 0,
    val quantityConsumed: Int = 0,
    val confidence: Float = 0f,
    val inferenceMethod: String = "",
    val error: String? = null
)

/**
 * Result of container weight learning
 */
data class ContainerLearningResult(
    val success: Boolean,
    val inventoryItemId: String? = null,
    val previousContainerWeight: Float? = null,
    val newContainerWeight: Float = 0f,
    val improvedConfidence: Float = 0f,
    val learningActivityId: String? = null,
    val error: String? = null
)

/**
 * Result of consumption calculation from composite measurement
 */
data class ConsumptionCalculation(
    val success: Boolean,
    val compositeEntityId: String = "",
    val measuredWeight: Float = 0f,
    val previousCompositeWeight: Float? = null,
    val totalFixedMass: Float = 0f,
    val consumableMass: Float = 0f,
    val totalConsumption: Float = 0f,
    val fixedComponents: Map<String, Float> = emptyMap(),
    val consumableComponents: Map<String, Float> = emptyMap(),
    val error: String? = null
)

/**
 * Result of consumption distribution calculation
 */
data class DistributionResult(
    val success: Boolean,
    val distributions: Map<String, Float> = emptyMap(),
    val confidence: Float = 0f,
    val method: DistributionMethod = DistributionMethod.PROPORTIONAL,
    val totalDistributed: Float = 0f,
    val error: String? = null
)

/**
 * Result of recording consumption distribution
 */
data class RecordingResult(
    val success: Boolean,
    val distributionActivityId: String = "",
    val affectedEntityIds: List<String> = emptyList(),
    val error: String? = null
)

/**
 * Result of component weight inference
 */
data class ComponentInferenceResult(
    val success: Boolean,
    val measuredTotalWeight: Float = 0f,
    val knownComponentWeights: Map<String, Float> = emptyMap(),
    val inferredWeights: Map<String, Float> = emptyMap(),
    val totalUnknownWeight: Float = 0f,
    val confidence: Float = 0f,
    val error: String? = null
)

/**
 * Result of applying inferred weights to components
 */
data class ApplyInferenceResult(
    val success: Boolean,
    val updatedComponentIds: List<String> = emptyList(),
    val calibrationActivityIds: List<String> = emptyList(),
    val error: String? = null
)