package com.bscan.service

import android.util.Log
import com.bscan.model.Component
import com.bscan.model.ComponentIdentifier
import com.bscan.model.UserComponentOverlay
import java.time.LocalDateTime

/**
 * Service that merges generated components with user overlays to create the final view.
 * Generated components provide the base data, user overlays provide customisations.
 */
class ComponentMergerService {
    
    companion object {
        private const val TAG = "ComponentMergerService"
    }
    
    /**
     * Merge generated components with user overlays to produce the final component list.
     * 
     * @param generatedComponents Components created from scan data (base layer)
     * @param userOverlays User modifications and customisations (overlay layer)
     * @return Merged components with user modifications applied
     */
    fun mergeComponents(
        generatedComponents: List<Component>,
        userOverlays: List<UserComponentOverlay>
    ): List<Component> {
        Log.d(TAG, "Merging ${generatedComponents.size} generated components with ${userOverlays.size} user overlays")
        
        val activeOverlays = userOverlays.filter { !it.isDeleted }
        val overlayMap = activeOverlays.associateBy { it.componentId }
        
        val mergedComponents = mutableListOf<Component>()
        
        // Process generated components and apply overlays
        generatedComponents.forEach { generatedComponent ->
            val overlay = overlayMap[generatedComponent.id]
            
            if (overlay?.isHidden == true) {
                // Skip hidden components
                Log.d(TAG, "Hiding component: ${generatedComponent.id}")
                return@forEach
            }
            
            val mergedComponent = if (overlay != null) {
                // Apply user modifications
                applyOverlay(generatedComponent, overlay)
            } else {
                // No modifications, use as-is
                generatedComponent
            }
            
            mergedComponents.add(mergedComponent)
        }
        
        // Add user-created components (not based on generated components)
        val userCreatedOverlays = activeOverlays.filter { it.isUserCreated }
        userCreatedOverlays.forEach { overlay ->
            if (!overlay.isHidden) {
                val userCreatedComponent = createComponentFromOverlay(overlay)
                mergedComponents.add(userCreatedComponent)
                Log.d(TAG, "Added user-created component: ${overlay.componentId}")
            }
        }
        
        // Sort components with user preferences
        val sortedComponents = sortComponentsWithUserPreferences(mergedComponents, activeOverlays)
        
        Log.d(TAG, "Merged result: ${sortedComponents.size} components")
        return sortedComponents
    }
    
    /**
     * Apply a user overlay to a generated component
     */
    private fun applyOverlay(baseComponent: Component, overlay: UserComponentOverlay): Component {
        return baseComponent.copy(
            // Basic properties
            name = overlay.nameOverride ?: baseComponent.name,
            category = overlay.categoryOverride ?: baseComponent.category,
            description = overlay.descriptionOverride ?: baseComponent.description,
            manufacturer = overlay.manufacturerOverride ?: baseComponent.manufacturer,
            
            // Tags - handle both override and additions
            tags = when {
                overlay.tagsOverride != null -> overlay.tagsOverride!! + overlay.tagsAdditions
                overlay.tagsAdditions.isNotEmpty() -> baseComponent.tags + overlay.tagsAdditions
                else -> baseComponent.tags
            },
            
            // Mass overrides
            massGrams = overlay.massOverride ?: baseComponent.massGrams,
            fullMassGrams = overlay.fullMassOverride ?: baseComponent.fullMassGrams,
            
            // Identifiers - add user identifiers to generated ones
            identifiers = mergeIdentifiers(baseComponent.identifiers, overlay.additionalIdentifiers),
            
            // Hierarchy modifications
            parentComponentId = overlay.customParentId ?: baseComponent.parentComponentId,
            childComponents = mergeChildComponents(baseComponent.childComponents, overlay.additionalChildIds, overlay.hiddenChildIds),
            
            // Metadata - apply overrides and additions
            metadata = mergeMetadata(baseComponent.metadata, overlay.metadataOverrides, overlay.metadataAdditions),
            
            // Update timestamp to reflect user modification
            lastUpdated = overlay.modifiedAt
        )
    }
    
    /**
     * Create a component entirely from a user overlay (user-created components)
     */
    private fun createComponentFromOverlay(overlay: UserComponentOverlay): Component {
        return Component(
            id = overlay.componentId,
            name = overlay.nameOverride ?: "User Component",
            category = overlay.categoryOverride ?: "custom",
            description = overlay.descriptionOverride ?: "",
            manufacturer = overlay.manufacturerOverride ?: "User",
            tags = (overlay.tagsOverride ?: emptyList()) + overlay.tagsAdditions,
            massGrams = overlay.massOverride,
            fullMassGrams = overlay.fullMassOverride,
            identifiers = overlay.additionalIdentifiers,
            parentComponentId = overlay.customParentId,
            childComponents = overlay.additionalChildIds,
            metadata = overlay.metadataOverrides + overlay.metadataAdditions,
            createdAt = overlay.createdAt.toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            lastUpdated = overlay.modifiedAt
        )
    }
    
    /**
     * Merge identifiers from base component and user additions
     */
    private fun mergeIdentifiers(
        baseIdentifiers: List<ComponentIdentifier>,
        additionalIdentifiers: List<ComponentIdentifier>
    ): List<ComponentIdentifier> {
        val merged = baseIdentifiers.toMutableList()
        
        // Add user identifiers, avoiding duplicates
        additionalIdentifiers.forEach { userIdentifier ->
            val duplicate = merged.any { 
                it.type == userIdentifier.type && it.value == userIdentifier.value 
            }
            if (!duplicate) {
                merged.add(userIdentifier)
            }
        }
        
        return merged
    }
    
    /**
     * Merge child component lists with user additions and exclusions
     */
    private fun mergeChildComponents(
        baseChildren: List<String>,
        additionalChildren: List<String>,
        hiddenChildren: List<String>
    ): List<String> {
        val merged = baseChildren.toMutableList()
        
        // Add additional children
        additionalChildren.forEach { childId ->
            if (!merged.contains(childId)) {
                merged.add(childId)
            }
        }
        
        // Remove hidden children
        merged.removeAll(hiddenChildren)
        
        return merged
    }
    
    /**
     * Merge metadata with overrides and additions
     */
    private fun mergeMetadata(
        baseMetadata: Map<String, String>,
        overrides: Map<String, String>,
        additions: Map<String, String>
    ): Map<String, String> {
        val merged = baseMetadata.toMutableMap()
        
        // Apply overrides (replaces existing values)
        merged.putAll(overrides)
        
        // Add new metadata (only if key doesn't exist)
        additions.forEach { (key, value) ->
            if (!merged.containsKey(key)) {
                merged[key] = value
            }
        }
        
        return merged
    }
    
    /**
     * Sort components considering user preferences (pinned items first, etc.)
     */
    private fun sortComponentsWithUserPreferences(
        components: List<Component>,
        overlays: List<UserComponentOverlay>
    ): List<Component> {
        val overlayMap = overlays.associateBy { it.componentId }
        
        return components.sortedWith(compareBy<Component> { component ->
            // Pinned components go first (0 for pinned, 1 for not pinned)
            if (overlayMap[component.id]?.isPinned == true) 0 else 1
        }.thenBy { component ->
            // Then by category
            component.category
        }.thenBy { component ->
            // Finally by name
            component.name.lowercase()
        })
    }
    
    /**
     * Get the effective component name (considering user overrides)
     */
    fun getEffectiveName(component: Component, overlay: UserComponentOverlay?): String {
        return overlay?.nameOverride ?: component.name
    }
    
    /**
     * Check if a component has user modifications
     */
    fun hasUserModifications(componentId: String, overlays: List<UserComponentOverlay>): Boolean {
        val overlay = overlays.find { it.componentId == componentId && !it.isDeleted }
        return overlay != null && overlay.hasModifications()
    }
    
    /**
     * Get summary of applied modifications for a component
     */
    fun getModificationSummary(componentId: String, overlays: List<UserComponentOverlay>): List<String> {
        val overlay = overlays.find { it.componentId == componentId && !it.isDeleted }
            ?: return emptyList()
        
        val modifications = mutableListOf<String>()
        
        if (overlay.nameOverride != null) modifications.add("Name modified")
        if (overlay.categoryOverride != null) modifications.add("Category changed")
        if (overlay.tagsOverride != null || overlay.tagsAdditions.isNotEmpty()) modifications.add("Tags updated")
        if (overlay.descriptionOverride != null) modifications.add("Description updated")
        if (overlay.massOverride != null) modifications.add("Mass override")
        if (overlay.manufacturerOverride != null) modifications.add("Manufacturer changed")
        if (overlay.metadataOverrides.isNotEmpty() || overlay.metadataAdditions.isNotEmpty()) modifications.add("Metadata updated")
        if (overlay.additionalIdentifiers.isNotEmpty()) modifications.add("Identifiers added")
        if (overlay.customParentId != null) modifications.add("Parent changed")
        if (overlay.additionalChildIds.isNotEmpty()) modifications.add("Children added")
        if (overlay.hiddenChildIds.isNotEmpty()) modifications.add("Children hidden")
        if (overlay.userNotes.isNotBlank()) modifications.add("Notes added")
        if (overlay.isPinned) modifications.add("Pinned")
        if (overlay.customColor != null) modifications.add("Custom colour")
        if (overlay.customIcon != null) modifications.add("Custom icon")
        
        return modifications
    }
}