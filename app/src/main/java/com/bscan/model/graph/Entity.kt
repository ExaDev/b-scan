package com.bscan.model.graph

import java.time.LocalDateTime
import java.util.UUID

/**
 * Base entity class for the graph data model.
 * All trackable items, components, locations, people, etc. extend from this.
 */
abstract class Entity(
    open val id: String = generateId(),
    open val entityType: String,
    open val label: String,
    open val properties: MutableMap<String, PropertyValue> = mutableMapOf(),
    open val metadata: EntityMetadata = EntityMetadata()
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
     * Check if entity has a property
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
     * Create a copy of this entity with new ID (for versioning/branching)
     */
    abstract fun copy(newId: String = generateId()): Entity
    
    /**
     * Validate this entity's properties and constraints
     */
    open fun validate(): ValidationResult = ValidationResult.valid()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity) return false
        return id == other.id
    }
    
    override fun hashCode(): Int = id.hashCode()
    
    override fun toString(): String = "$entityType($id): $label"
}

/**
 * Metadata about entity lifecycle and management
 */
data class EntityMetadata(
    val created: LocalDateTime = LocalDateTime.now(),
    var lastModified: LocalDateTime = LocalDateTime.now(),
    val version: Int = 1,
    val tags: MutableSet<String> = mutableSetOf(),
    val source: String? = null,  // Where this entity came from (scan, import, user, etc.)
    val confidence: Float = 1.0f  // Confidence level for inferred data
)

/**
 * Result of entity validation
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
    
    companion object {
        fun valid() = Valid
        fun invalid(vararg errors: String) = Invalid(errors.toList())
    }
    
    val isValid: Boolean get() = this is Valid
}