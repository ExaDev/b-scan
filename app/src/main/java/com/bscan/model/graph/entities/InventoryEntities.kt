package com.bscan.model.graph.entities

import com.bscan.model.graph.Entity
import com.bscan.model.graph.PropertyValue
import com.bscan.model.graph.ValidationResult
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
class Activity(
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
class Information(
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
}