package com.bscan.model.graph.entities

import com.bscan.model.graph.*
import java.time.LocalDateTime

/**
 * Physical component entity (RFID tags, filament, tools, etc.)
 */
class PhysicalComponent(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "physical_component",
    label = label,
    properties = properties
) {
    
    // Convenience properties
    var manufacturer: String?
        get() = getProperty("manufacturer")
        set(value) { value?.let { setProperty("manufacturer", it) } }
    
    var model: String?
        get() = getProperty("model")
        set(value) { value?.let { setProperty("model", it) } }
    
    var serialNumber: String?
        get() = getProperty("serialNumber")
        set(value) { value?.let { setProperty("serialNumber", it) } }
    
    var massGrams: Float?
        get() = getProperty("massGrams")
        set(value) { value?.let { setProperty("massGrams", it) } }
    
    var category: String?
        get() = getProperty("category")
        set(value) { value?.let { setProperty("category", it) } }
    
    override fun copy(newId: String): PhysicalComponent {
        return PhysicalComponent(
            id = newId,
            label = label,
            properties = properties.toMutableMap()
        )
    }
    
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (label.isBlank()) errors.add("Physical component must have a label")
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(*errors.toTypedArray())
        }
    }
}

/**
 * Identifier entity (RFID UIDs, barcodes, QR codes, etc.)
 */
class Identifier(
    id: String = generateId(),
    val identifierType: String,
    val value: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "identifier",
    label = "$identifierType: $value",
    properties = properties
) {
    
    init {
        setProperty("identifierType", identifierType)
        setProperty("value", value)
    }
    
    var format: String?
        get() = getProperty("format")
        set(value) { value?.let { setProperty("format", it) } }
    
    var purpose: String?
        get() = getProperty("purpose")
        set(value) { value?.let { setProperty("purpose", it) } }
    
    var isUnique: Boolean
        get() = getProperty("isUnique") ?: false
        set(value) { setProperty("isUnique", value) }
    
    override fun copy(newId: String): Identifier {
        return Identifier(
            id = newId,
            identifierType = identifierType,
            value = value,
            properties = properties.toMutableMap()
        )
    }
    
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (identifierType.isBlank()) errors.add("Identifier must have a type")
        if (value.isBlank()) errors.add("Identifier must have a value")
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(*errors.toTypedArray())
        }
    }
}

/**
 * Location entity (storage locations, workstations, etc.)
 */
class Location(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "location",
    label = label,
    properties = properties
) {
    
    var locationType: String?
        get() = getProperty("locationType")
        set(value) { value?.let { setProperty("locationType", it) } }
    
    var address: String?
        get() = getProperty("address")
        set(value) { value?.let { setProperty("address", it) } }
    
    var coordinates: String?
        get() = getProperty("coordinates")
        set(value) { value?.let { setProperty("coordinates", it) } }
    
    override fun copy(newId: String): Location {
        return Location(
            id = newId,
            label = label,
            properties = properties.toMutableMap()
        )
    }
}

/**
 * Person entity (users, manufacturers, suppliers, etc.)
 */
class Person(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "person",
    label = label,
    properties = properties
) {
    
    var role: String?
        get() = getProperty("role")
        set(value) { value?.let { setProperty("role", it) } }
    
    var email: String?
        get() = getProperty("email")
        set(value) { value?.let { setProperty("email", it) } }
    
    var organization: String?
        get() = getProperty("organization")
        set(value) { value?.let { setProperty("organization", it) } }
    
    override fun copy(newId: String): Person {
        return Person(
            id = newId,
            label = label,
            properties = properties.toMutableMap()
        )
    }
    
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (label.isBlank()) errors.add("Person must have a name")
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(*errors.toTypedArray())
        }
    }
}

/**
 * Activity/Event entity (scans, maintenance, usage, etc.)
 */
open class Activity(
    id: String = generateId(),
    val activityType: String,
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "activity",
    label = label,
    properties = properties
) {
    
    init {
        setProperty("activityType", activityType)
        setProperty("timestamp", LocalDateTime.now())
    }
    
    var timestamp: LocalDateTime
        get() = getProperty("timestamp") ?: LocalDateTime.now()
        set(value) { setProperty("timestamp", value) }
    
    var duration: Long?
        get() = getProperty<Long>("duration")
        set(value) { value?.let { setProperty("duration", it) } }
    
    var status: String?
        get() = getProperty("status")
        set(value) { value?.let { setProperty("status", it) } }
    
    var result: String?
        get() = getProperty("result")
        set(value) { value?.let { setProperty("result", it) } }
    
    override fun copy(newId: String): Activity {
        return Activity(
            id = newId,
            activityType = activityType,
            label = label,
            properties = properties.toMutableMap()
        )
    }
    
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (activityType.isBlank()) errors.add("Activity must have a type")
        if (label.isBlank()) errors.add("Activity must have a label")
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(*errors.toTypedArray())
        }
    }
}

/**
 * Information entity (documents, specifications, manuals, etc.)
 */
open class Information(
    id: String = generateId(),
    val informationType: String,
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "information",
    label = label,
    properties = properties
) {
    
    init {
        setProperty("informationType", informationType)
    }
    
    var content: String?
        get() = getProperty("content")
        set(value) { value?.let { setProperty("content", it) } }
    
    var url: String?
        get() = getProperty("url")
        set(value) { value?.let { setProperty("url", it) } }
    
    var mediaType: String?
        get() = getProperty("mediaType")
        set(value) { value?.let { setProperty("mediaType", it) } }
    
    override fun copy(newId: String): Information {
        return Information(
            id = newId,
            informationType = informationType,
            label = label,
            properties = properties.toMutableMap()
        )
    }
}

/**
 * Virtual entity (concepts, categories, templates, etc.)
 */
class Virtual(
    id: String = generateId(),
    val virtualType: String,
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "virtual",
    label = label,
    properties = properties
) {
    
    init {
        setProperty("virtualType", virtualType)
    }
    
    var template: Boolean
        get() = getProperty("template") ?: false
        set(value) { setProperty("template", value) }
    
    var abstract: Boolean
        get() = getProperty("abstract") ?: false
        set(value) { setProperty("abstract", value) }
    
    override fun copy(newId: String): Virtual {
        return Virtual(
            id = newId,
            virtualType = virtualType,
            label = label,
            properties = properties.toMutableMap()
        )
    }
}

/**
 * Stock definition entity for describing types of items that can be stocked
 * Generic specifications of materials, components, tools, etc. that can exist in inventory
 * Actual inventory items reference these stock definitions via TRACKS relationships
 */
class StockDefinition(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "stock_definition",
    label = label,
    properties = properties
) {
    
    // Core catalog properties
    var sku: String?
        get() = getProperty("sku")
        set(value) { value?.let { setProperty("sku", it) } }
    
    var manufacturer: String?
        get() = getProperty("manufacturer")
        set(value) { value?.let { setProperty("manufacturer", it) } }
    
    var displayName: String?
        get() = getProperty("displayName")
        set(value) { value?.let { setProperty("displayName", it) } }
    
    var description: String?
        get() = getProperty("description")
        set(value) { value?.let { setProperty("description", it) } }
    
    var category: String?
        get() = getProperty("category")
        set(value) { value?.let { setProperty("category", it) } }
    
    var productUrl: String?
        get() = getProperty("productUrl")
        set(value) { value?.let { setProperty("productUrl", it) } }
    
    // Physical properties using Quantity types
    var weight: Quantity?
        get() = getProperty("weight")
        set(value) { value?.let { setProperty("weight", it) } }
    
    var dimensions: String?
        get() = getProperty("dimensions")
        set(value) { value?.let { setProperty("dimensions", it) } }
    
    // Usage characteristics
    var consumable: Boolean
        get() = getProperty("consumable") ?: true  // Default to consumable for materials
        set(value) { setProperty("consumable", value) }
    
    var reusable: Boolean
        get() = getProperty("reusable") ?: false  // Default to not reusable
        set(value) { setProperty("reusable", value) }
    
    var recyclable: Boolean
        get() = getProperty("recyclable") ?: false
        set(value) { setProperty("recyclable", value) }
    
    // Material-specific properties (only set if applicable)
    var materialType: String?
        get() = getProperty("materialType")
        set(value) { value?.let { setProperty("materialType", it) } }
    
    var colorName: String?
        get() = getProperty("colorName")
        set(value) { value?.let { setProperty("colorName", it) } }
    
    var colorHex: String?
        get() = getProperty("colorHex")
        set(value) { value?.let { setProperty("colorHex", it) } }
    
    var colorCode: String?
        get() = getProperty("colorCode")
        set(value) { value?.let { setProperty("colorCode", it) } }
    
    // Temperature properties (only set if data exists - no defaults)
    var minNozzleTemp: Int?
        get() = getProperty("minNozzleTemp")
        set(value) { value?.let { setProperty("minNozzleTemp", it) } }
    
    var maxNozzleTemp: Int?
        get() = getProperty("maxNozzleTemp")
        set(value) { value?.let { setProperty("maxNozzleTemp", it) } }
    
    var bedTemp: Int?
        get() = getProperty("bedTemp")
        set(value) { value?.let { setProperty("bedTemp", it) } }
    
    var enclosureTemp: Int?
        get() = getProperty("enclosureTemp")
        set(value) { value?.let { setProperty("enclosureTemp", it) } }
    
    // Availability and pricing
    var available: Boolean
        get() = getProperty("available") ?: true
        set(value) { setProperty("available", value) }
    
    var price: Double?
        get() = getProperty("price")
        set(value) { value?.let { setProperty("price", it) } }
    
    var currency: String?
        get() = getProperty("currency")
        set(value) { value?.let { setProperty("currency", it) } }
    
    // Alternative identifiers
    var alternativeIds: Set<String>
        get() = getProperty<List<String>>("alternativeIds")?.toSet() ?: emptySet()
        set(value) { setProperty("alternativeIds", value.toList()) }
    
    /**
     * Check if this item has temperature properties defined
     */
    fun hasTemperatureProperties(): Boolean {
        return minNozzleTemp != null || maxNozzleTemp != null || bedTemp != null || enclosureTemp != null
    }
    
    /**
     * Check if this item is a material (vs packaging/component)
     */
    fun isMaterial(): Boolean {
        return materialType != null
    }
    
    /**
     * Check if this item is packaging/component (vs material)
     */
    fun isPackaging(): Boolean {
        return !isMaterial() && (category?.contains("spool", ignoreCase = true) == true ||
                                 category?.contains("core", ignoreCase = true) == true ||
                                 category?.contains("packaging", ignoreCase = true) == true)
    }
    
    override fun copy(newId: String): StockDefinition {
        return StockDefinition(
            id = newId,
            label = label,
            properties = properties.toMutableMap()
        )
    }
    
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (label.isBlank()) errors.add("Catalog item must have a label")
        if (sku.isNullOrBlank()) errors.add("Catalog item must have a SKU")
        if (manufacturer.isNullOrBlank()) errors.add("Catalog item must have a manufacturer")
        
        // Validate temperature ranges if provided
        val minTemp = minNozzleTemp
        val maxTemp = maxNozzleTemp
        if (minTemp != null && maxTemp != null && minTemp > maxTemp) {
            errors.add("Minimum nozzle temperature cannot be greater than maximum")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(*errors.toTypedArray())
        }
    }
}

/**
 * Common entity types
 */
object EntityTypes {
    const val PHYSICAL_COMPONENT = "physical_component"
    const val IDENTIFIER = "identifier"
    const val LOCATION = "location"
    const val PERSON = "person"
    const val ACTIVITY = "activity"
    const val INFORMATION = "information"
    const val VIRTUAL = "virtual"
    const val STOCK_DEFINITION = "stock_definition"
}

/**
 * Common identifier types
 */
object IdentifierTypes {
    const val RFID_HARDWARE = "rfid_hardware"
    const val CONSUMABLE_UNIT = "consumable_unit"
    const val QR_CODE = "qr_code"
    const val BARCODE = "barcode"
    const val SERIAL_NUMBER = "serial_number"
    const val SKU = "sku"
    const val BATCH_NUMBER = "batch_number"
    const val MODEL_NUMBER = "model_number"
    const val CUSTOM = "custom"
}

/**
 * Inventory item entity for tracking quantities over time
 * Links to either unique physical items or fungible product types
 */
class InventoryItem(
    id: String = generateId(),
    label: String,
    val trackingMode: TrackingMode = TrackingMode.DISCRETE,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Entity(
    id = id,
    entityType = "inventory_item",
    label = label,
    properties = properties
) {
    
    init {
        setProperty("trackingMode", trackingMode.name)
    }
    
    // Current quantity/state
    var currentQuantity: Float
        get() = getProperty("currentQuantity") ?: 0f
        set(value) { setProperty("currentQuantity", value) }
    
    var currentWeight: Float?
        get() = getProperty("currentWeight")
        set(value) { value?.let { setProperty("currentWeight", it) } }
    
    // Reorder management
    var reorderLevel: Float?
        get() = getProperty("reorderLevel")
        set(value) { value?.let { setProperty("reorderLevel", it) } }
    
    var reorderQuantity: Float?
        get() = getProperty("reorderQuantity")
        set(value) { value?.let { setProperty("reorderQuantity", it) } }
    
    // Component weights for inference (e.g., spool tare weight)
    var tareWeight: Float?
        get() = getProperty("tareWeight")
        set(value) { value?.let { setProperty("tareWeight", it) } }
    
    var unitWeight: Float?
        get() = getProperty("unitWeight")
        set(value) { value?.let { setProperty("unitWeight", it) } }
    
    // Storage and location
    var location: String?
        get() = getProperty("location")
        set(value) { value?.let { setProperty("location", it) } }
    
    var notes: String?
        get() = getProperty("notes")
        set(value) { value?.let { setProperty("notes", it) } }
    
    // Composite consumption properties
    var isConsumable: Boolean
        get() = getProperty("isConsumable") ?: true  // Default to consumable
        set(value) { setProperty("isConsumable", value) }
    
    var fixedMass: Float?
        get() = getProperty("fixedMass")
        set(value) { value?.let { setProperty("fixedMass", it) } }
    
    var lastCompositeWeight: Float?
        get() = getProperty("lastCompositeWeight")
        set(value) { value?.let { setProperty("lastCompositeWeight", it) } }
    
    var componentType: String?
        get() = getProperty("componentType")
        set(value) { value?.let { setProperty("componentType", it) } }
    
    // Unit weight calibration properties
    var containerWeight: Float?
        get() = getProperty("containerWeight") ?: tareWeight  // Alias for tareWeight with clearer name
        set(value) { 
            value?.let { 
                setProperty("containerWeight", it)
                tareWeight = it  // Keep both in sync
            }
        }
    
    var lastCalibratedAt: String?
        get() = getProperty("lastCalibratedAt")
        set(value) { value?.let { setProperty("lastCalibratedAt", it) } }
    
    var calibrationMethod: String?
        get() = getProperty("calibrationMethod")
        set(value) { value?.let { setProperty("calibrationMethod", it) } }
    
    var calibrationConfidence: Float?
        get() = getProperty("calibrationConfidence")
        set(value) { value?.let { setProperty("calibrationConfidence", it) } }
    
    /**
     * Calibrate unit weight from known count and total weight
     * Perfect for: "I have 100 screws weighing 247g total, box weighs 47g"
     */
    fun calibrateUnitWeight(
        totalWeight: Float,
        knownQuantity: Float,
        containerWeight: Float? = null
    ): CalibrationResult {
        if (knownQuantity <= 0) {
            return CalibrationResult(
                success = false,
                error = "Known quantity must be greater than zero"
            )
        }
        
        val tare = containerWeight ?: this.containerWeight ?: 0f
        val netWeight = totalWeight - tare
        
        if (netWeight <= 0) {
            return CalibrationResult(
                success = false,
                error = "Net weight must be positive (check container weight)"
            )
        }
        
        val unitWt = netWeight / knownQuantity
        
        // Update stored values
        this.unitWeight = unitWt
        this.tareWeight = tare
        this.containerWeight = tare
        this.currentQuantity = knownQuantity
        this.currentWeight = totalWeight
        this.lastCalibratedAt = LocalDateTime.now().toString()
        this.calibrationMethod = "COUNTED_AND_WEIGHED"
        this.calibrationConfidence = 95f  // High confidence for direct measurement
        
        return CalibrationResult(
            success = true,
            unitWeight = unitWt,
            netWeight = netWeight,
            accuracy = 95f,
            method = "DIRECT_CALIBRATION"
        )
    }
    
    /**
     * Learn container weight from empty container measurement
     * Used when initially didn't know box weight
     */
    fun learnContainerWeight(emptyContainerWeight: Float): CalibrationResult {
        val oldTare = tareWeight ?: 0f
        val oldUnitWeight = unitWeight
        val lastWeight = currentWeight
        val lastQuantity = currentQuantity
        
        if (oldUnitWeight == null || lastWeight == null) {
            return CalibrationResult(
                success = false,
                error = "No previous calibration data available"
            )
        }
        
        // Recalculate unit weight with accurate container weight
        val correctedNetWeight = lastWeight - emptyContainerWeight
        val correctedUnitWeight = if (lastQuantity > 0) correctedNetWeight / lastQuantity else oldUnitWeight
        
        // Update values
        this.containerWeight = emptyContainerWeight
        this.tareWeight = emptyContainerWeight
        this.unitWeight = correctedUnitWeight
        this.calibrationMethod = "CONTAINER_WEIGHT_LEARNED"
        this.calibrationConfidence = 98f  // Even higher confidence with known container
        
        return CalibrationResult(
            success = true,
            unitWeight = correctedUnitWeight,
            netWeight = correctedNetWeight,
            accuracy = 98f,
            method = "CONTAINER_LEARNING"
        )
    }
    
    /**
     * Perform bidirectional inference between weight and quantity
     */
    fun inferFromWeight(totalWeight: Float): InferenceResult? {
        val tare = containerWeight ?: tareWeight ?: return null
        val unitWt = unitWeight ?: return null
        
        val netWeight = totalWeight - tare
        val inferredQuantity = when (trackingMode) {
            TrackingMode.DISCRETE -> (netWeight / unitWt).toInt().toFloat()
            TrackingMode.CONTINUOUS -> netWeight / unitWt
        }
        
        return InferenceResult(
            inferredQuantity = inferredQuantity,
            inferredWeight = totalWeight,
            confidence = calculateInferenceConfidence(netWeight, unitWt),
            method = "weight_inference"
        )
    }
    
    /**
     * Perform bidirectional inference from quantity to weight
     */
    fun inferFromQuantity(quantity: Float): InferenceResult? {
        val tare = containerWeight ?: tareWeight ?: return null
        val unitWt = unitWeight ?: return null
        
        val netWeight = when (trackingMode) {
            TrackingMode.DISCRETE -> quantity * unitWt
            TrackingMode.CONTINUOUS -> quantity * unitWt
        }
        val inferredWeight = netWeight + tare
        
        return InferenceResult(
            inferredQuantity = quantity,
            inferredWeight = inferredWeight,
            confidence = 100f, // Quantity-based inference is exact
            method = "quantity_inference"
        )
    }
    
    /**
     * Update quantity and weight from new total weight measurement
     * Perfect for: "Box now weighs 187g, how many screws are left?"
     */
    fun updateFromWeightMeasurement(newTotalWeight: Float): WeightUpdateResult {
        val inference = inferFromWeight(newTotalWeight)
            ?: return WeightUpdateResult(
                success = false,
                error = "Cannot infer quantity - missing calibration data"
            )
        
        val oldQuantity = currentQuantity
        val consumedQuantity = oldQuantity - inference.inferredQuantity
        
        // Update current values
        this.currentQuantity = inference.inferredQuantity
        this.currentWeight = newTotalWeight
        
        return WeightUpdateResult(
            success = true,
            newQuantity = inference.inferredQuantity,
            quantityConsumed = consumedQuantity,
            confidence = inference.confidence,
            inferenceMethod = inference.method
        )
    }
    
    private fun calculateInferenceConfidence(netWeight: Float, unitWeight: Float): Float {
        // Base confidence on calibration quality and measurement precision
        val baseConfidence = calibrationConfidence ?: 70f
        
        // Factor in measurement precision for discrete items
        return when (trackingMode) {
            TrackingMode.DISCRETE -> {
                val exactUnits = netWeight / unitWeight
                val roundedUnits = exactUnits.toInt()
                val error = kotlin.math.abs(exactUnits - roundedUnits)
                val precisionFactor = 1f - error
                (baseConfidence * precisionFactor).coerceIn(50f, 100f)
            }
            TrackingMode.CONTINUOUS -> baseConfidence.coerceIn(70f, 100f)
        }
    }
    
    /**
     * Check if this item is properly calibrated for weight-based inference
     */
    fun isProperlyCalibrated(): Boolean {
        return unitWeight != null && unitWeight!! > 0 && 
               (containerWeight != null || tareWeight != null)
    }
    
    /**
     * Get calibration status summary
     */
    fun getCalibrationStatus(): String {
        return when {
            !isProperlyCalibrated() -> "Not calibrated"
            calibrationMethod == "COUNTED_AND_WEIGHED" -> "Calibrated (counted & weighed)"
            calibrationMethod == "CONTAINER_WEIGHT_LEARNED" -> "Calibrated (container learned)"
            calibrationMethod == "ESTIMATED" -> "Estimated calibration"
            else -> "Calibrated (method unknown)"
        }
    }
    
    override fun copy(newId: String): InventoryItem {
        return InventoryItem(
            id = newId,
            label = label,
            trackingMode = trackingMode,
            properties = properties.toMutableMap()
        )
    }
    
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (label.isBlank()) errors.add("Inventory item must have a label")
        if (currentQuantity < 0) errors.add("Current quantity cannot be negative")
        
        // Validate calibration consistency
        val unitWt = unitWeight
        val tare = containerWeight ?: tareWeight
        if (unitWt != null && unitWt <= 0) {
            errors.add("Unit weight must be positive")
        }
        if (tare != null && tare < 0) {
            errors.add("Container/tare weight cannot be negative")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}

/**
 * Calibration activity - establishes weight/quantity relationships
 */
open class CalibrationActivity(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Activity(
    id = id,
    activityType = ActivityTypes.CALIBRATION,
    label = label,
    properties = properties
) {
    
    // Calibration inputs
    var totalWeight: Float
        get() = getProperty("totalWeight") ?: 0f
        set(value) { setProperty("totalWeight", value) }
    
    var tareWeight: Float?
        get() = getProperty("tareWeight")
        set(value) { value?.let { setProperty("tareWeight", it) } }
    
    var knownQuantity: Float
        get() = getProperty("knownQuantity") ?: 0f
        set(value) { setProperty("knownQuantity", value) }
    
    // Calibration results
    var calculatedUnitWeight: Float?
        get() = getProperty("calculatedUnitWeight")
        set(value) { value?.let { setProperty("calculatedUnitWeight", it) } }
    
    var calculatedNetWeight: Float?
        get() = getProperty("calculatedNetWeight")
        set(value) { value?.let { setProperty("calculatedNetWeight", it) } }
    
    var calibrationAccuracy: Float?
        get() = getProperty("calibrationAccuracy")
        set(value) { value?.let { setProperty("calibrationAccuracy", it) } }
    
    /**
     * Perform calibration calculation
     */
    fun performCalibration(): CalibrationResult {
        val tare = tareWeight ?: 0f
        val net = totalWeight - tare
        
        if (knownQuantity <= 0) {
            return CalibrationResult(
                success = false,
                error = "Known quantity must be greater than zero"
            )
        }
        
        val unitWt = net / knownQuantity
        
        calculatedNetWeight = net
        calculatedUnitWeight = unitWt
        calibrationAccuracy = if (unitWt > 0) 100f else 0f
        
        return CalibrationResult(
            success = true,
            unitWeight = unitWt,
            netWeight = net,
            accuracy = calibrationAccuracy ?: 0f
        )
    }
}

/**
 * Measurement activity with bidirectional inference
 */
open class MeasurementActivity(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Activity(
    id = id,
    activityType = ActivityTypes.MEASUREMENT,
    label = label,
    properties = properties
) {
    
    // User inputs (either weight OR quantity)
    var providedWeight: Float?
        get() = getProperty("providedWeight")
        set(value) { value?.let { setProperty("providedWeight", it) } }
    
    var providedQuantity: Float?
        get() = getProperty("providedQuantity")
        set(value) { value?.let { setProperty("providedQuantity", it) } }
    
    // Inferred values
    var inferredWeight: Float?
        get() = getProperty("inferredWeight")
        set(value) { value?.let { setProperty("inferredWeight", it) } }
    
    var inferredQuantity: Float?
        get() = getProperty("inferredQuantity")
        set(value) { value?.let { setProperty("inferredQuantity", it) } }
    
    var confidence: Float?
        get() = getProperty("confidence")
        set(value) { value?.let { setProperty("confidence", it) } }
    
    var inferenceMethod: String?
        get() = getProperty("inferenceMethod")
        set(value) { value?.let { setProperty("inferenceMethod", it) } }
    
    // Previous values for change calculation
    var previousWeight: Float?
        get() = getProperty("previousWeight")
        set(value) { value?.let { setProperty("previousWeight", it) } }
    
    var previousQuantity: Float?
        get() = getProperty("previousQuantity")
        set(value) { value?.let { setProperty("previousQuantity", it) } }
    
    var weightChange: Float?
        get() = getProperty("weightChange")
        set(value) { value?.let { setProperty("weightChange", it) } }
    
    var quantityChange: Float?
        get() = getProperty("quantityChange")
        set(value) { value?.let { setProperty("quantityChange", it) } }
}

/**
 * Stock movement activity - records inventory changes
 */
open class StockMovementActivity(
    id: String = generateId(),
    val movementType: StockMovementType,
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Activity(
    id = id,
    activityType = ActivityTypes.STOCK_MOVEMENT,
    label = label,
    properties = properties
) {
    
    init {
        setProperty("movementType", movementType.name)
    }
    
    var quantityChange: Float
        get() = getProperty("quantityChange") ?: 0f
        set(value) { setProperty("quantityChange", value) }
    
    var weightChange: Float?
        get() = getProperty("weightChange")
        set(value) { value?.let { setProperty("weightChange", it) } }
    
    var newQuantity: Float
        get() = getProperty("newQuantity") ?: 0f
        set(value) { setProperty("newQuantity", value) }
    
    var newWeight: Float?
        get() = getProperty("newWeight")
        set(value) { value?.let { setProperty("newWeight", it) } }
    
    var reason: String?
        get() = getProperty("reason")
        set(value) { value?.let { setProperty("reason", it) } }
    
    var batchNumber: String?
        get() = getProperty("batchNumber")
        set(value) { value?.let { setProperty("batchNumber", it) } }
    
    var cost: Float?
        get() = getProperty("cost")
        set(value) { value?.let { setProperty("cost", it) } }
    
    var supplier: String?
        get() = getProperty("supplier")
        set(value) { value?.let { setProperty("supplier", it) } }
}

/**
 * Consumption distribution activity - records distribution of composite measurements
 * across multiple consumable entities (like splitting a bill between participants)
 */
open class ConsumptionDistributionActivity(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Activity(
    id = id,
    activityType = ActivityTypes.CONSUMPTION_DISTRIBUTION,
    label = label,
    properties = properties
) {
    
    // Composite measurement inputs
    var compositeEntityId: String
        get() = getProperty("compositeEntityId") ?: ""
        set(value) { setProperty("compositeEntityId", value) }
    
    var measuredWeight: Float
        get() = getProperty("measuredWeight") ?: 0f
        set(value) { setProperty("measuredWeight", value) }
    
    var previousCompositeWeight: Float?
        get() = getProperty("previousCompositeWeight")
        set(value) { value?.let { setProperty("previousCompositeWeight", it) } }
    
    var totalConsumption: Float
        get() = getProperty("totalConsumption") ?: 0f
        set(value) { setProperty("totalConsumption", value) }
    
    // Distribution method and results
    var distributionMethod: DistributionMethod
        get() = getProperty<String>("distributionMethod")?.let { 
            DistributionMethod.valueOf(it) 
        } ?: DistributionMethod.PROPORTIONAL
        set(value) { setProperty("distributionMethod", value.name) }
    
    var distributionConfidence: Float
        get() = getProperty("distributionConfidence") ?: 0.95f
        set(value) { setProperty("distributionConfidence", value) }
    
    // Individual entity distributions (stored as JSON map string)
    var distributions: Map<String, Float>
        get() {
            val jsonString = getProperty<String>("distributions") ?: "{}"
            return try {
                // Simple JSON parsing for Float values
                jsonString.removeSurrounding("{", "}")
                    .split(",")
                    .associate { pair ->
                        val (key, value) = pair.split(":")
                        key.trim().removeSurrounding("\"") to value.trim().toFloat()
                    }
            } catch (e: Exception) {
                emptyMap()
            }
        }
        set(value) { 
            val jsonString = value.entries.joinToString(",", "{", "}") { (k, v) -> 
                "\"$k\":$v" 
            }
            setProperty("distributions", jsonString)
        }
    
    // Component information at time of measurement
    var fixedComponents: Map<String, Float>  // entityId -> fixed mass
        get() {
            val jsonString = getProperty<String>("fixedComponents") ?: "{}"
            return try {
                jsonString.removeSurrounding("{", "}")
                    .split(",")
                    .associate { pair ->
                        val (key, value) = pair.split(":")
                        key.trim().removeSurrounding("\"") to value.trim().toFloat()
                    }
            } catch (e: Exception) {
                emptyMap()
            }
        }
        set(value) {
            val jsonString = value.entries.joinToString(",", "{", "}") { (k, v) -> 
                "\"$k\":$v" 
            }
            setProperty("fixedComponents", jsonString)
        }
    
    var consumableComponents: Map<String, Float>  // entityId -> previous mass
        get() {
            val jsonString = getProperty<String>("consumableComponents") ?: "{}"
            return try {
                jsonString.removeSurrounding("{", "}")
                    .split(",")
                    .associate { pair ->
                        val (key, value) = pair.split(":")
                        key.trim().removeSurrounding("\"") to value.trim().toFloat()
                    }
            } catch (e: Exception) {
                emptyMap()
            }
        }
        set(value) {
            val jsonString = value.entries.joinToString(",", "{", "}") { (k, v) -> 
                "\"$k\":$v" 
            }
            setProperty("consumableComponents", jsonString)
        }
    
    var notes: String?
        get() = getProperty("notes")
        set(value) { value?.let { setProperty("notes", it) } }
}

/**
 * Distribution methods for composite consumption
 */
enum class DistributionMethod {
    PROPORTIONAL,       // Distribute based on current quantities
    USER_SPECIFIED,     // User manually specified distribution
    EQUAL_SPLIT,        // Split equally between all consumables
    WEIGHTED,           // Distribute based on usage patterns
    INFERRED            // AI/ML-based inference from patterns
}

/**
 * Tracking modes for inventory items
 */
enum class TrackingMode {
    DISCRETE,    // Count-based (pieces, units)
    CONTINUOUS   // Weight/volume-based (grams, milliliters)
}

/**
 * Stock movement types
 */
enum class StockMovementType {
    ADDITION,      // Adding stock (purchase, refill)
    CONSUMPTION,   // Using stock (manufacturing, projects)
    ADJUSTMENT,    // Corrections (counting errors)
    TRANSFER,      // Moving between locations
    WASTE,         // Loss/damage/expiry
    CALIBRATION    // Setting initial values
}

/**
 * Result of inference calculations
 */
data class InferenceResult(
    val inferredQuantity: Float,
    val inferredWeight: Float,
    val confidence: Float,
    val method: String,
    val uncertainty: Float? = null
)

/**
 * Result of calibration calculations
 */
data class CalibrationResult(
    val success: Boolean,
    val unitWeight: Float? = null,
    val netWeight: Float? = null,
    val accuracy: Float? = null,
    val method: String? = null,
    val error: String? = null
)

/**
 * Result of weight-based quantity update
 */
data class WeightUpdateResult(
    val success: Boolean,
    val newQuantity: Float = 0f,
    val quantityConsumed: Float = 0f,
    val confidence: Float = 0f,
    val inferenceMethod: String = "",
    val error: String? = null
)


/**
 * Common activity types
 */
object ActivityTypes {
    const val SCAN = "scan"
    const val MAINTENANCE = "maintenance"
    const val USAGE = "usage"
    const val PURCHASE = "purchase"
    const val DISPOSAL = "disposal"
    const val CALIBRATION = "calibration"
    const val INSPECTION = "inspection"
    const val MEASUREMENT = "measurement"
    const val STOCK_MOVEMENT = "stock_movement"
    const val CONSUMPTION_DISTRIBUTION = "consumption_distribution"
}

/**
 * Common relationship types for inventory
 */
object InventoryRelationshipTypes {
    const val TRACKS = "tracks"                    // InventoryItem -> PhysicalComponent/Virtual
    const val HAD_MOVEMENT = "had_movement"        // InventoryItem -> StockMovementActivity
    const val CALIBRATED_BY = "calibrated_by"     // InventoryItem -> CalibrationActivity
    const val MEASURED_BY = "measured_by"         // InventoryItem -> MeasurementActivity
    const val HAS_COMPONENT = "has_component"     // InventoryItem -> PhysicalComponent (tare weights)
    const val STORED_AT = "stored_at"             // InventoryItem -> Location
    const val SUPPLIED_BY = "supplied_by"         // InventoryItem -> Person (supplier)
    
    // Composite consumption relationships
    const val DISTRIBUTED_TO = "distributed_to"   // ConsumptionDistributionActivity -> InventoryItem
    const val MEASURED_AS_COMPOSITE = "measured_as_composite"  // Entity -> ConsumptionDistributionActivity
    const val COMPONENT_OF = "component_of"       // PhysicalComponent -> PhysicalComponent (composite)
    const val FIXED_MASS_COMPONENT = "fixed_mass_component"   // Composite -> Fixed mass component
    const val CONSUMABLE_COMPONENT = "consumable_component"   // Composite -> Consumable component
}