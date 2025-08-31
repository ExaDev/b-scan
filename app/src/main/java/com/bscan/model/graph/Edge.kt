package com.bscan.model.graph

import java.time.LocalDateTime
import java.util.UUID

/**
 * Base edge class for the graph data model.
 * Represents relationships between entities with optional properties and directionality.
 */
data class Edge(
    val id: String = generateId(),
    val fromEntityId: String,
    val toEntityId: String,
    val relationshipType: String,
    val properties: MutableMap<String, PropertyValue> = mutableMapOf(),
    val metadata: EdgeMetadata = EdgeMetadata(),
    val directional: Boolean = false  // If true, relationship is fromEntity -> toEntity only
) {
    
    companion object {
        fun generateId(): String = UUID.randomUUID().toString()
    }
    
    /**
     * Get a property value with type safety
     */
    inline fun <reified T> getProperty(key: String): T? {
        return properties[key]?.getValue<T>()
    }
    
    /**
     * Set a property value
     */
    fun <T> setProperty(key: String, value: T) {
        properties[key] = PropertyValue.create(value)
        metadata.lastModified = LocalDateTime.now()
    }
    
    /**
     * Check if edge has a property
     */
    fun hasProperty(key: String): Boolean = properties.containsKey(key)
    
    /**
     * Remove a property
     */
    fun removeProperty(key: String) {
        properties.remove(key)
        metadata.lastModified = LocalDateTime.now()
    }
    
    /**
     * Get all property keys
     */
    fun getPropertyKeys(): Set<String> = properties.keys
    
    /**
     * Check if this edge connects two specific entities (ignoring direction if non-directional)
     */
    fun connects(entityId1: String, entityId2: String): Boolean {
        return if (directional) {
            fromEntityId == entityId1 && toEntityId == entityId2
        } else {
            (fromEntityId == entityId1 && toEntityId == entityId2) ||
            (fromEntityId == entityId2 && toEntityId == entityId1)
        }
    }
    
    /**
     * Check if this edge involves a specific entity
     */
    fun involves(entityId: String): Boolean {
        return fromEntityId == entityId || toEntityId == entityId
    }
    
    /**
     * Get the other entity ID in this relationship
     */
    fun getOtherEntity(entityId: String): String? {
        return when (entityId) {
            fromEntityId -> toEntityId
            toEntityId -> if (directional) null else fromEntityId  // Respect directionality
            else -> null
        }
    }
    
    /**
     * Create reverse edge for bidirectional relationships
     */
    fun reverse(): Edge {
        return copy(
            id = generateId(),
            fromEntityId = toEntityId,
            toEntityId = fromEntityId
        )
    }
    
    /**
     * Check if this edge is equivalent to another (same entities, type, and properties)
     */
    fun isEquivalent(other: Edge): Boolean {
        if (relationshipType != other.relationshipType) return false
        if (directional != other.directional) return false
        if (properties != other.properties) return false
        
        return if (directional) {
            fromEntityId == other.fromEntityId && toEntityId == other.toEntityId
        } else {
            connects(other.fromEntityId, other.toEntityId)
        }
    }
    
    /**
     * Validate this edge's properties and constraints
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (fromEntityId.isBlank()) errors.add("From entity ID cannot be blank")
        if (toEntityId.isBlank()) errors.add("To entity ID cannot be blank")
        if (relationshipType.isBlank()) errors.add("Relationship type cannot be blank")
        if (fromEntityId == toEntityId) errors.add("Self-referencing edges not allowed")
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(*errors.toTypedArray())
        }
    }
    
    override fun toString(): String = 
        if (directional) "$fromEntityId -[$relationshipType]-> $toEntityId" 
        else "$fromEntityId <-[$relationshipType]-> $toEntityId"
}

/**
 * Metadata about edge lifecycle and management
 */
data class EdgeMetadata(
    val created: LocalDateTime = LocalDateTime.now(),
    var lastModified: LocalDateTime = LocalDateTime.now(),
    val version: Int = 1,
    val weight: Double = 1.0,  // Relationship strength/importance
    val confidence: Float = 1.0f,  // Confidence level for inferred relationships
    val source: String? = null  // Where this relationship came from
)

/**
 * Common relationship types (extendable)
 */
object RelationshipTypes {
    // Physical relationships
    const val CONTAINS = "contains"
    const val PART_OF = "partOf"
    const val ATTACHED_TO = "attachedTo"
    const val LOCATED_AT = "locatedAt"
    
    // Logical relationships  
    const val INSTANCE_OF = "instanceOf"
    const val DERIVED_FROM = "derivedFrom"
    const val RELATED_TO = "relatedTo"
    const val SAME_AS = "sameAs"
    
    // Temporal relationships
    const val CREATED_FROM = "createdFrom"
    const val REPLACED_BY = "replacedBy"
    const val PRECEDED_BY = "precededBy"
    const val CAUSED_BY = "causedBy"
    
    // Identification relationships
    const val IDENTIFIED_BY = "identifiedBy"
    const val AUTHENTICATES = "authenticates"
    const val TRACKS = "tracks"
    
    // Inventory relationships
    const val CONSUMED_BY = "consumedBy"
    const val PRODUCED_BY = "producedBy"
    const val SUPPLIED_BY = "suppliedBy"
    const val COMPATIBLE_WITH = "compatibleWith"
    
    // Custom/Domain-specific (examples)
    const val SCANNED_WITH = "scannedWith"  // RFID tag scanned with reader
    const val MANUFACTURED_BY = "manufacturedBy"
    const val OWNS = "owns"  // User owns item
    const val MAINTAINS = "maintains"  // User maintains equipment
}