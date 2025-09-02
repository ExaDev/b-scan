package com.bscan.service

import com.bscan.model.graph.Edge
import com.bscan.model.graph.entities.*
import com.bscan.model.graph.ValidationResult
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages dynamic relationships between composite entities and their components.
 * Handles scenarios where component sets change over time (box+bag+filament → spool+filament).
 */
class ComponentRelationshipManager(
    private val graphRepository: GraphRepository
) {
    
    /**
     * Get currently active components of a composite entity
     */
    suspend fun getActiveComponents(compositeEntityId: String): List<ComponentInfo> = withContext(Dispatchers.IO) {
        val fixedComponents = graphRepository.getConnectedEntities(
            compositeEntityId, 
            InventoryRelationshipTypes.FIXED_MASS_COMPONENT
        ).filterIsInstance<InventoryItem>()
        
        val consumableComponents = graphRepository.getConnectedEntities(
            compositeEntityId,
            InventoryRelationshipTypes.CONSUMABLE_COMPONENT
        ).filterIsInstance<InventoryItem>()
        
        val generalComponents = graphRepository.getConnectedEntities(
            compositeEntityId,
            InventoryRelationshipTypes.HAS_COMPONENT
        ).filterIsInstance<InventoryItem>()
        
        // Combine all components with their roles
        val allComponents = mutableListOf<ComponentInfo>()
        
        fixedComponents.forEach { component ->
            allComponents.add(ComponentInfo(
                entity = component,
                role = ComponentRole.FIXED_MASS,
                isActive = true
            ))
        }
        
        consumableComponents.forEach { component ->
            allComponents.add(ComponentInfo(
                entity = component,
                role = ComponentRole.CONSUMABLE,
                isActive = true
            ))
        }
        
        // General components that aren't specifically categorised
        generalComponents.forEach { component ->
            if (allComponents.none { it.entity.id == component.id }) {
                val role = if (component.isConsumable) ComponentRole.CONSUMABLE else ComponentRole.FIXED_MASS
                allComponents.add(ComponentInfo(
                    entity = component,
                    role = role,
                    isActive = true
                ))
            }
        }
        
        allComponents
    }
    
    /**
     * Update the component set for a composite entity
     * Perfect for transitions like box+bag+filament → spool+filament
     */
    suspend fun updateComponentSet(
        compositeEntityId: String,
        newComponents: List<ComponentUpdate>,
        transitionReason: String = "Component set updated"
    ): ComponentSetUpdateResult = withContext(Dispatchers.IO) {
        
        val compositeEntity = graphRepository.getEntity(compositeEntityId)
            ?: return@withContext ComponentSetUpdateResult(
                success = false,
                error = "Composite entity not found"
            )
        
        // Get current components
        val currentComponents = getActiveComponents(compositeEntityId)
        
        // Deactivate removed components
        val removedComponents = mutableListOf<String>()
        val keptComponents = mutableListOf<String>()
        val addedComponents = mutableListOf<String>()
        
        // Mark components as inactive if they're not in the new set
        for (currentComponent in currentComponents) {
            val stillPresent = newComponents.any { it.entityId == currentComponent.entity.id }
            if (!stillPresent) {
                // Remove relationship edges
                removeComponentRelationships(compositeEntityId, currentComponent.entity.id)
                removedComponents.add(currentComponent.entity.id)
            } else {
                keptComponents.add(currentComponent.entity.id)
            }
        }
        
        // Add new components
        for (componentUpdate in newComponents) {
            val isNew = currentComponents.none { it.entity.id == componentUpdate.entityId }
            
            if (isNew) {
                val result = addComponentToComposite(compositeEntityId, componentUpdate)
                if (result.success) {
                    addedComponents.add(componentUpdate.entityId)
                }
            } else {
                // Update existing component properties if needed
                updateComponentProperties(componentUpdate)
            }
        }
        
        // Create activity record for this transition
        val transitionActivity = Activity(
            activityType = "component_transition",
            label = "Component set transition: $compositeEntityId"
        ).apply {
            setProperty("compositeEntityId", compositeEntityId)
            setProperty("transitionReason", transitionReason)
            setProperty("removedComponents", removedComponents.joinToString(","))
            setProperty("addedComponents", addedComponents.joinToString(","))
            setProperty("keptComponents", keptComponents.joinToString(","))
        }
        
        graphRepository.addEntity(transitionActivity)
        
        // Link transition activity to composite
        val edge = Edge(
            fromEntityId = compositeEntityId,
            toEntityId = transitionActivity.id,
            relationshipType = "had_transition"
        )
        graphRepository.addEdge(edge)
        
        ComponentSetUpdateResult(
            success = true,
            transitionActivityId = transitionActivity.id,
            removedComponentIds = removedComponents,
            addedComponentIds = addedComponents,
            keptComponentIds = keptComponents
        )
    }
    
    /**
     * Calculate total fixed mass of non-consumable components
     */
    suspend fun calculateFixedMass(compositeEntityId: String): Float = withContext(Dispatchers.IO) {
        val components = getActiveComponents(compositeEntityId)
        components.filter { it.role == ComponentRole.FIXED_MASS }
            .mapNotNull { it.entity.fixedMass ?: it.entity.currentWeight }
            .sum()
    }
    
    /**
     * Identify all consumable components in a composite
     */
    suspend fun identifyConsumables(compositeEntityId: String): List<InventoryItem> = withContext(Dispatchers.IO) {
        val components = getActiveComponents(compositeEntityId)
        components.filter { it.role == ComponentRole.CONSUMABLE }
            .map { it.entity }
    }
    
    /**
     * Check if a composite entity is ready for consumption tracking
     * (has at least one consumable component and all fixed masses are known)
     */
    suspend fun validateCompositeForConsumption(compositeEntityId: String): ValidationResult = withContext(Dispatchers.IO) {
        val components = getActiveComponents(compositeEntityId)
        val errors = mutableListOf<String>()
        
        val consumables = components.filter { it.role == ComponentRole.CONSUMABLE }
        val fixedMassComponents = components.filter { it.role == ComponentRole.FIXED_MASS }
        
        if (consumables.isEmpty()) {
            errors.add("No consumable components found")
        }
        
        val unknownFixedMass = fixedMassComponents.filter { 
            it.entity.fixedMass == null && it.entity.currentWeight == null 
        }
        
        if (unknownFixedMass.isNotEmpty()) {
            errors.add("Unknown weights for fixed components: ${unknownFixedMass.joinToString { it.entity.label }}")
        }
        
        if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Get the component transition history for a composite entity
     */
    suspend fun getTransitionHistory(compositeEntityId: String): List<Activity> = withContext(Dispatchers.IO) {
        val transitions = graphRepository.getConnectedEntities(compositeEntityId, "had_transition")
        transitions.filterIsInstance<Activity>()
            .filter { it.activityType == "component_transition" }
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Suggest component transitions based on common patterns
     * This is a placeholder for future ML-based suggestions
     */
    suspend fun suggestTransitions(
        compositeEntityId: String,
        currentStage: String
    ): List<TransitionSuggestion> = withContext(Dispatchers.IO) {
        // Common filament spool transitions
        when (currentStage.lowercase()) {
            "unopened", "sealed" -> listOf(
                TransitionSuggestion(
                    description = "Remove packaging (box + bag)",
                    removeComponents = listOf("box", "bag"),
                    addComponents = listOf("reusable_spool", "filament"),
                    confidence = 0.9f
                )
            )
            "opened", "in_use" -> listOf(
                TransitionSuggestion(
                    description = "Switch to refillable spool",
                    removeComponents = listOf("cardboard_core"),
                    addComponents = listOf("refillable_spool"),
                    confidence = 0.7f
                )
            )
            else -> emptyList()
        }
    }
    
    /**
     * Add a component to a composite entity with proper relationships
     */
    private suspend fun addComponentToComposite(
        compositeEntityId: String,
        componentUpdate: ComponentUpdate
    ): ComponentAddResult {
        val component = graphRepository.getEntity(componentUpdate.entityId) as? InventoryItem
            ?: return ComponentAddResult(
                success = false,
                error = "Component entity not found: ${componentUpdate.entityId}"
            )
        
        // Update component properties
        componentUpdate.fixedMass?.let { component.fixedMass = it }
        componentUpdate.isConsumable?.let { component.isConsumable = it }
        componentUpdate.componentType?.let { component.componentType = it }
        
        // Create appropriate relationship
        val relationshipType = when {
            component.isConsumable -> InventoryRelationshipTypes.CONSUMABLE_COMPONENT
            component.fixedMass != null -> InventoryRelationshipTypes.FIXED_MASS_COMPONENT
            else -> InventoryRelationshipTypes.HAS_COMPONENT
        }
        
        val edge = Edge(
            fromEntityId = compositeEntityId,
            toEntityId = component.id,
            relationshipType = relationshipType
        )
        
        graphRepository.addEdge(edge)
        
        return ComponentAddResult(success = true)
    }
    
    /**
     * Remove component relationships
     */
    private suspend fun removeComponentRelationships(compositeEntityId: String, componentId: String) {
        // Find and remove all relationship edges between composite and component
        val compositeEdges = graphRepository.getAllEdgesForEntity(compositeEntityId)
        
        compositeEdges.filter { edge ->
            (edge.fromEntityId == compositeEntityId && edge.toEntityId == componentId) ||
            (edge.toEntityId == compositeEntityId && edge.fromEntityId == componentId)
        }.forEach { edge ->
            graphRepository.removeEdge(edge.id)
        }
    }
    
    /**
     * Update component properties
     */
    private suspend fun updateComponentProperties(componentUpdate: ComponentUpdate) {
        val component = graphRepository.getEntity(componentUpdate.entityId) as? InventoryItem
            ?: return
        
        componentUpdate.fixedMass?.let { component.fixedMass = it }
        componentUpdate.isConsumable?.let { component.isConsumable = it }
        componentUpdate.componentType?.let { component.componentType = it }
    }
}

/**
 * Information about a component in a composite entity
 */
data class ComponentInfo(
    val entity: InventoryItem,
    val role: ComponentRole,
    val isActive: Boolean = true,
    val addedTimestamp: java.time.LocalDateTime? = null,
    val removedTimestamp: java.time.LocalDateTime? = null
)

/**
 * Component roles in composite entities
 */
enum class ComponentRole {
    FIXED_MASS,    // Non-consumable components (spools, cores, packaging)
    CONSUMABLE,    // Consumable components (filament, materials)
    UNKNOWN        // Role not yet determined
}

/**
 * Update specification for a component
 */
data class ComponentUpdate(
    val entityId: String,
    val fixedMass: Float? = null,
    val isConsumable: Boolean? = null,
    val componentType: String? = null
)

/**
 * Result of component set update
 */
data class ComponentSetUpdateResult(
    val success: Boolean,
    val transitionActivityId: String = "",
    val removedComponentIds: List<String> = emptyList(),
    val addedComponentIds: List<String> = emptyList(),
    val keptComponentIds: List<String> = emptyList(),
    val error: String? = null
)

/**
 * Result of adding a component
 */
data class ComponentAddResult(
    val success: Boolean,
    val error: String? = null
)

/**
 * Transition suggestion for component sets
 */
data class TransitionSuggestion(
    val description: String,
    val removeComponents: List<String> = emptyList(),
    val addComponents: List<String> = emptyList(),
    val confidence: Float = 0.5f,
    val reason: String = ""
)