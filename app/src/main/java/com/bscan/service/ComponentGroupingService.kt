package com.bscan.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.bscan.model.Component
import com.bscan.repository.ComponentRepository
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service for managing complex component relationships including hierarchical
 * parent-child structures and sibling groupings. Handles mixed relationship
 * patterns where components can be both nested AND have sibling relationships.
 */
class ComponentGroupingService(
    private val componentRepository: ComponentRepository
) {
    
    companion object {
        private const val TAG = "ComponentGroupingService"
        private const val MAX_HIERARCHY_DEPTH = 10
        private const val GROUP_CATEGORY = "component-group"
    }
    
    // === Core Data Structures ===
    
    /**
     * Represents the complete hierarchical structure of a component tree
     */
    data class ComponentHierarchy(
        val root: Component,
        val children: Map<String, List<Component>>,
        val totalDepth: Int,
        val totalComponents: Int,
        val siblingGroups: Map<String, List<Component>> = emptyMap()
    )
    
    /**
     * Result of grouping operations with detailed feedback
     */
    data class GroupingResult(
        val success: Boolean,
        val groupId: String? = null,
        val message: String,
        val affectedComponents: List<String> = emptyList(),
        val previousState: GroupingSnapshot? = null
    )
    
    /**
     * Snapshot of component relationships for undo operations
     */
    data class GroupingSnapshot(
        val timestamp: LocalDateTime,
        val affectedComponents: Map<String, Component>,
        val operation: String,
        val description: String
    )
    
    /**
     * Configuration for relationship validation
     */
    data class RelationshipValidationConfig(
        val preventCircularReferences: Boolean = true,
        val maxHierarchyDepth: Int = MAX_HIERARCHY_DEPTH,
        val enforceCompatibleCategories: Boolean = true,
        val allowSelfReference: Boolean = false
    )
    
    // === Relationship Management ===
    
    /**
     * Create a hierarchical parent-child group
     */
    suspend fun createHierarchicalGroup(
        parentId: String,
        childIds: List<String>
    ): GroupingResult = withContext(Dispatchers.IO) {
        
        try {
            val parent = componentRepository.getComponent(parentId)
                ?: return@withContext GroupingResult(
                    success = false,
                    message = "Parent component not found: $parentId"
                )
            
            // Take snapshot for potential undo
            val snapshot = createSnapshot(
                "create_hierarchical_group",
                "Creating hierarchical group with parent $parentId and ${childIds.size} children",
                childIds + parentId
            )
            
            // Validate all child components exist
            val children = childIds.mapNotNull { componentRepository.getComponent(it) }
            if (children.size != childIds.size) {
                val missing = childIds.toSet() - children.map { it.id }.toSet()
                return@withContext GroupingResult(
                    success = false,
                    message = "Child components not found: ${missing.joinToString()}"
                )
            }
            
            // Validate relationships
            val validationResult = validateHierarchicalRelationship(parent, children)
            if (!validationResult.success) {
                return@withContext validationResult
            }
            
            // Update parent component
            var updatedParent = parent
            children.forEach { child ->
                updatedParent = updatedParent.withChildComponent(child.id)
            }
            componentRepository.saveComponent(updatedParent)
            
            // Update child components
            children.forEach { child ->
                val updatedChild = child.copy(
                    parentComponentId = parentId,
                    lastUpdated = LocalDateTime.now()
                )
                componentRepository.saveComponent(updatedChild)
            }
            
            Log.d(TAG, "Created hierarchical group with parent ${parent.name} and ${children.size} children")
            
            GroupingResult(
                success = true,
                groupId = parentId,
                message = "Successfully created hierarchical group with ${children.size} children",
                affectedComponents = childIds + parentId,
                previousState = snapshot
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating hierarchical group", e)
            GroupingResult(
                success = false,
                message = "Error creating hierarchical group: ${e.message}"
            )
        }
    }
    
    /**
     * Create a sibling group (components sharing same parent)
     */
    suspend fun createSiblingGroup(
        groupName: String,
        componentIds: List<String>
    ): GroupingResult = withContext(Dispatchers.IO) {
        
        try {
            if (componentIds.size < 2) {
                return@withContext GroupingResult(
                    success = false,
                    message = "Sibling groups require at least 2 components"
                )
            }
            
            val components = componentIds.mapNotNull { componentRepository.getComponent(it) }
            if (components.size != componentIds.size) {
                val missing = componentIds.toSet() - components.map { it.id }.toSet()
                return@withContext GroupingResult(
                    success = false,
                    message = "Components not found: ${missing.joinToString()}"
                )
            }
            
            // Take snapshot
            val snapshot = createSnapshot(
                "create_sibling_group",
                "Creating sibling group '$groupName' with ${componentIds.size} components",
                componentIds
            )
            
            // Validate sibling relationships
            val validationResult = validateSiblingRelationship(components)
            if (!validationResult.success) {
                return@withContext validationResult
            }
            
            // Create or find common parent if needed
            val commonParentId = findOrCreateCommonParent(components, groupName)
            
            // Update each component to reference siblings and set common parent
            components.forEach { component ->
                var updatedComponent = component.copy(parentComponentId = commonParentId)
                
                // Add sibling references (exclude self)
                components.filter { it.id != component.id }.forEach { sibling ->
                    updatedComponent = updatedComponent.addSiblingReference(sibling.id)
                }
                
                componentRepository.saveComponent(updatedComponent)
            }
            
            Log.d(TAG, "Created sibling group '$groupName' with ${components.size} components")
            
            GroupingResult(
                success = true,
                groupId = commonParentId,
                message = "Successfully created sibling group '$groupName' with ${components.size} components",
                affectedComponents = componentIds,
                previousState = snapshot
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sibling group", e)
            GroupingResult(
                success = false,
                message = "Error creating sibling group: ${e.message}"
            )
        }
    }
    
    /**
     * Create a mixed group with both hierarchical and sibling relationships
     */
    suspend fun createMixedGroup(
        parentId: String,
        hierarchicalChildren: List<String>,
        siblingComponents: List<String>
    ): GroupingResult = withContext(Dispatchers.IO) {
        
        try {
            // Take snapshot
            val allAffected = listOf(parentId) + hierarchicalChildren + siblingComponents
            val snapshot = createSnapshot(
                "create_mixed_group",
                "Creating mixed group with parent $parentId, ${hierarchicalChildren.size} hierarchical children, ${siblingComponents.size} siblings",
                allAffected
            )
            
            // First create hierarchical relationships
            if (hierarchicalChildren.isNotEmpty()) {
                val hierarchicalResult = createHierarchicalGroup(parentId, hierarchicalChildren)
                if (!hierarchicalResult.success) {
                    return@withContext hierarchicalResult
                }
            }
            
            // Then create sibling relationships among the siblings
            if (siblingComponents.size >= 2) {
                val siblingResult = createSiblingGroup("Mixed Group Siblings", siblingComponents)
                if (!siblingResult.success) {
                    // Rollback hierarchical changes if sibling creation failed
                    rollbackFromSnapshot(snapshot)
                    return@withContext GroupingResult(
                        success = false,
                        message = "Failed to create sibling relationships: ${siblingResult.message}"
                    )
                }
            }
            
            Log.d(TAG, "Created mixed group with parent $parentId")
            
            GroupingResult(
                success = true,
                groupId = parentId,
                message = "Successfully created mixed group with ${hierarchicalChildren.size} hierarchical children and ${siblingComponents.size} siblings",
                affectedComponents = allAffected,
                previousState = snapshot
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating mixed group", e)
            GroupingResult(
                success = false,
                message = "Error creating mixed group: ${e.message}"
            )
        }
    }
    
    // === Navigation Methods ===
    
    /**
     * Find the direct parent component
     */
    suspend fun findParentComponent(componentId: String): Component? = withContext(Dispatchers.IO) {
        val component = componentRepository.getComponent(componentId) ?: return@withContext null
        component.parentComponentId?.let { componentRepository.getComponent(it) }
    }
    
    /**
     * Find all direct child components
     */
    suspend fun findChildComponents(parentId: String): List<Component> = withContext(Dispatchers.IO) {
        componentRepository.getChildComponents(parentId)
    }
    
    /**
     * Find sibling components (components sharing the same parent)
     */
    suspend fun findSiblingComponents(componentId: String): List<Component> = withContext(Dispatchers.IO) {
        val component = componentRepository.getComponent(componentId) ?: return@withContext emptyList()
        
        // Get explicit sibling references
        val explicitSiblings = component.siblingReferences.mapNotNull { 
            componentRepository.getComponent(it) 
        }
        
        // Get implicit siblings (same parent)
        val implicitSiblings = component.parentComponentId?.let { parentId ->
            componentRepository.getChildComponents(parentId)
                .filter { it.id != componentId }
        } ?: emptyList()
        
        // Combine and deduplicate
        (explicitSiblings + implicitSiblings).distinctBy { it.id }
    }
    
    /**
     * Get complete component hierarchy starting from a root component
     */
    suspend fun getComponentHierarchy(rootId: String): ComponentHierarchy? = withContext(Dispatchers.IO) {
        val root = componentRepository.getComponent(rootId) ?: return@withContext null
        
        if (!root.isRootComponent) {
            return@withContext null // Only build hierarchy from root components
        }
        
        val allComponents = componentRepository.getComponents()
        val hierarchyMap = buildHierarchyMap(root, allComponents)
        val siblingGroups = buildSiblingGroupMap(allComponents)
        
        ComponentHierarchy(
            root = root,
            children = hierarchyMap,
            totalDepth = calculateMaxDepth(root, allComponents),
            totalComponents = hierarchyMap.values.flatten().size + 1, // +1 for root
            siblingGroups = siblingGroups
        )
    }
    
    /**
     * Find all root components (components with no parent)
     */
    suspend fun findRootComponents(): List<Component> = withContext(Dispatchers.IO) {
        componentRepository.getComponents().filter { it.isRootComponent }
    }
    
    // === Relationship Validation ===
    
    private suspend fun validateHierarchicalRelationship(
        parent: Component,
        children: List<Component>,
        config: RelationshipValidationConfig = RelationshipValidationConfig()
    ): GroupingResult = withContext(Dispatchers.IO) {
        
        // Check for circular references
        if (config.preventCircularReferences) {
            for (child in children) {
                if (wouldCreateCircularReference(parent.id, child.id)) {
                    return@withContext GroupingResult(
                        success = false,
                        message = "Would create circular reference: ${parent.name} -> ${child.name}"
                    )
                }
            }
        }
        
        // Check hierarchy depth
        if (config.maxHierarchyDepth > 0) {
            val currentDepth = calculateComponentDepth(parent.id)
            if (currentDepth >= config.maxHierarchyDepth) {
                return@withContext GroupingResult(
                    success = false,
                    message = "Maximum hierarchy depth ($config.maxHierarchyDepth) would be exceeded"
                )
            }
        }
        
        // Check category compatibility
        if (config.enforceCompatibleCategories) {
            val incompatibleChildren = children.filter { 
                !areCategoriesCompatible(parent.category, it.category) 
            }
            if (incompatibleChildren.isNotEmpty()) {
                return@withContext GroupingResult(
                    success = false,
                    message = "Incompatible categories: ${incompatibleChildren.map { it.category }.distinct().joinToString()}"
                )
            }
        }
        
        // Check for self-reference
        if (!config.allowSelfReference) {
            val selfReferences = children.filter { it.id == parent.id }
            if (selfReferences.isNotEmpty()) {
                return@withContext GroupingResult(
                    success = false,
                    message = "Component cannot be parent of itself"
                )
            }
        }
        
        GroupingResult(success = true, message = "Validation passed")
    }
    
    private suspend fun validateSiblingRelationship(
        components: List<Component>
    ): GroupingResult = withContext(Dispatchers.IO) {
        
        if (components.size < 2) {
            return@withContext GroupingResult(
                success = false,
                message = "Sibling groups require at least 2 components"
            )
        }
        
        // Check for duplicate IDs
        val uniqueIds = components.map { it.id }.toSet()
        if (uniqueIds.size != components.size) {
            return@withContext GroupingResult(
                success = false,
                message = "Duplicate components in sibling group"
            )
        }
        
        // Validate component compatibility for sibling relationships
        val categories = components.map { it.category }.distinct()
        val incompatibleCategories = categories.filter { category ->
            !canBeInSiblingGroup(category)
        }
        
        if (incompatibleCategories.isNotEmpty()) {
            return@withContext GroupingResult(
                success = false,
                message = "Categories cannot form sibling groups: ${incompatibleCategories.joinToString()}"
            )
        }
        
        GroupingResult(success = true, message = "Sibling validation passed")
    }
    
    // === Bulk Operations ===
    
    /**
     * Move components between groups
     */
    suspend fun moveComponentsBetweenGroups(
        componentIds: List<String>,
        newParentId: String?,
        oldParentId: String?
    ): GroupingResult = withContext(Dispatchers.IO) {
        
        try {
            val snapshot = createSnapshot(
                "move_components",
                "Moving ${componentIds.size} components from $oldParentId to $newParentId",
                componentIds + listOfNotNull(newParentId, oldParentId)
            )
            
            val components = componentIds.mapNotNull { componentRepository.getComponent(it) }
            if (components.size != componentIds.size) {
                return@withContext GroupingResult(
                    success = false,
                    message = "Some components not found"
                )
            }
            
            // Remove from old parent
            oldParentId?.let { oldId ->
                val oldParent = componentRepository.getComponent(oldId)
                oldParent?.let { parent ->
                    var updatedParent = parent
                    componentIds.forEach { childId ->
                        updatedParent = updatedParent.withoutChildComponent(childId)
                    }
                    componentRepository.saveComponent(updatedParent)
                }
            }
            
            // Add to new parent
            newParentId?.let { newId ->
                val newParent = componentRepository.getComponent(newId)
                    ?: return@withContext GroupingResult(
                        success = false,
                        message = "New parent component not found: $newId"
                    )
                
                var updatedParent = newParent
                componentIds.forEach { childId ->
                    updatedParent = updatedParent.withChildComponent(childId)
                }
                componentRepository.saveComponent(updatedParent)
            }
            
            // Update child components
            components.forEach { component ->
                val updatedComponent = component.copy(
                    parentComponentId = newParentId,
                    lastUpdated = LocalDateTime.now()
                )
                componentRepository.saveComponent(updatedComponent)
            }
            
            GroupingResult(
                success = true,
                message = "Successfully moved ${components.size} components",
                affectedComponents = componentIds,
                previousState = snapshot
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error moving components between groups", e)
            GroupingResult(
                success = false,
                message = "Error moving components: ${e.message}"
            )
        }
    }
    
    /**
     * Dissolve group relationships
     */
    suspend fun dissolveGroup(groupId: String): GroupingResult = withContext(Dispatchers.IO) {
        try {
            val group = componentRepository.getComponent(groupId) ?: return@withContext GroupingResult(
                success = false,
                message = "Group component not found: $groupId"
            )
            
            val children = componentRepository.getChildComponents(groupId)
            val snapshot = createSnapshot(
                "dissolve_group",
                "Dissolving group ${group.name} with ${children.size} children",
                children.map { it.id } + groupId
            )
            
            // Remove parent-child relationships
            children.forEach { child ->
                val updatedChild = child.copy(
                    parentComponentId = null,
                    siblingReferences = emptyList(), // Clear sibling references too
                    lastUpdated = LocalDateTime.now()
                )
                componentRepository.saveComponent(updatedChild)
            }
            
            // Clear the group's child references
            val updatedGroup = group.copy(
                childComponents = emptyList(),
                lastUpdated = LocalDateTime.now()
            )
            componentRepository.saveComponent(updatedGroup)
            
            GroupingResult(
                success = true,
                message = "Successfully dissolved group ${group.name}",
                affectedComponents = children.map { it.id } + groupId,
                previousState = snapshot
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error dissolving group", e)
            GroupingResult(
                success = false,
                message = "Error dissolving group: ${e.message}"
            )
        }
    }
    
    /**
     * Merge two component groups
     */
    suspend fun mergeGroups(
        sourceGroupId: String,
        targetGroupId: String
    ): GroupingResult = withContext(Dispatchers.IO) {
        
        try {
            val sourceGroup = componentRepository.getComponent(sourceGroupId)
                ?: return@withContext GroupingResult(
                    success = false,
                    message = "Source group not found: $sourceGroupId"
                )
            
            val targetGroup = componentRepository.getComponent(targetGroupId)
                ?: return@withContext GroupingResult(
                    success = false,
                    message = "Target group not found: $targetGroupId"
                )
            
            val sourceChildren = componentRepository.getChildComponents(sourceGroupId)
            val snapshot = createSnapshot(
                "merge_groups",
                "Merging group ${sourceGroup.name} (${sourceChildren.size} children) into ${targetGroup.name}",
                sourceChildren.map { it.id } + sourceGroupId + targetGroupId
            )
            
            // Move all children from source to target
            val moveResult = moveComponentsBetweenGroups(
                sourceChildren.map { it.id },
                targetGroupId,
                sourceGroupId
            )
            
            if (!moveResult.success) {
                return@withContext moveResult
            }
            
            // Optionally delete the now-empty source group
            // (Implementation depends on business requirements)
            
            GroupingResult(
                success = true,
                message = "Successfully merged ${sourceGroup.name} into ${targetGroup.name}",
                affectedComponents = sourceChildren.map { it.id } + sourceGroupId + targetGroupId,
                previousState = snapshot
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error merging groups", e)
            GroupingResult(
                success = false,
                message = "Error merging groups: ${e.message}"
            )
        }
    }
    
    /**
     * Split components from a group
     */
    suspend fun splitComponentsFromGroup(
        componentsToSplit: List<String>,
        originalGroupId: String
    ): GroupingResult = withContext(Dispatchers.IO) {
        
        return@withContext moveComponentsBetweenGroups(
            componentsToSplit,
            newParentId = null, // Make them root components
            oldParentId = originalGroupId
        )
    }
    
    // === Advanced Features ===
    
    /**
     * Suggest optimal grouping strategies based on component metadata
     */
    suspend fun suggestGroupingStrategies(componentIds: List<String>): List<GroupingSuggestion> = withContext(Dispatchers.IO) {
        val components = componentIds.mapNotNull { componentRepository.getComponent(it) }
        val suggestions = mutableListOf<GroupingSuggestion>()
        
        // Group by category
        val categoryGroups = components.groupBy { it.category }
        categoryGroups.filter { it.value.size > 1 }.forEach { (category, componentsInCategory) ->
            suggestions.add(
                GroupingSuggestion(
                    type = "category",
                    reason = "Components share category: $category",
                    componentIds = componentsInCategory.map { it.id },
                    confidence = 0.8f
                )
            )
        }
        
        // Group by common tags
        val tagGroups = components.flatMap { component ->
            component.tags.map { tag -> tag to component }
        }.groupBy({ it.first }, { it.second })
        
        tagGroups.filter { it.value.size > 1 }.forEach { (tag, componentsWithTag) ->
            suggestions.add(
                GroupingSuggestion(
                    type = "tag",
                    reason = "Components share tag: $tag",
                    componentIds = componentsWithTag.map { it.id },
                    confidence = 0.6f
                )
            )
        }
        
        // Group by manufacturer
        val manufacturerGroups = components.groupBy { it.manufacturer }
        manufacturerGroups.filter { it.value.size > 1 }.forEach { (manufacturer, componentsFromManufacturer) ->
            suggestions.add(
                GroupingSuggestion(
                    type = "manufacturer",
                    reason = "Components from same manufacturer: $manufacturer",
                    componentIds = componentsFromManufacturer.map { it.id },
                    confidence = 0.4f
                )
            )
        }
        
        suggestions.sortedByDescending { it.confidence }
    }
    
    /**
     * Auto-detect relationship opportunities
     */
    suspend fun detectRelationshipOpportunities(): List<RelationshipOpportunity> = withContext(Dispatchers.IO) {
        val allComponents = componentRepository.getComponents()
        val opportunities = mutableListOf<RelationshipOpportunity>()
        
        // Find components with similar names or identifiers that might be related
        val componentsByName = allComponents.groupBy { 
            it.name.lowercase().substringBefore(" ") // Group by first word
        }
        
        componentsByName.filter { it.value.size > 1 }.forEach { (namePrefix, similarComponents) ->
            opportunities.add(
                RelationshipOpportunity(
                    type = "similar_names",
                    description = "Components with similar names: $namePrefix*",
                    componentIds = similarComponents.map { it.id },
                    suggestedAction = "Consider creating a hierarchical or sibling group",
                    confidence = 0.5f
                )
            )
        }
        
        // Find orphaned components that might belong to existing groups
        val orphanedComponents = allComponents.filter { it.parentComponentId == null && !it.isInventoryItem }
        val existingGroups = allComponents.filter { it.isComposite }
        
        orphanedComponents.forEach { orphan ->
            val compatibleGroups = existingGroups.filter { group ->
                areCategoriesCompatible(group.category, orphan.category)
            }
            
            if (compatibleGroups.isNotEmpty()) {
                opportunities.add(
                    RelationshipOpportunity(
                        type = "orphaned_component",
                        description = "Orphaned component '${orphan.name}' could belong to existing groups",
                        componentIds = listOf(orphan.id),
                        suggestedAction = "Consider adding to compatible group",
                        confidence = 0.3f,
                        metadata = mapOf("compatibleGroups" to compatibleGroups.map { it.id }.joinToString())
                    )
                )
            }
        }
        
        opportunities.sortedByDescending { it.confidence }
    }
    
    // === Undo/Redo Support ===
    
    /**
     * Create a snapshot of current state for undo operations
     */
    private suspend fun createSnapshot(
        operation: String,
        description: String,
        componentIds: List<String>
    ): GroupingSnapshot = withContext(Dispatchers.IO) {
        val affectedComponents = componentIds.mapNotNull { id ->
            componentRepository.getComponent(id)?.let { id to it }
        }.toMap()
        
        GroupingSnapshot(
            timestamp = LocalDateTime.now(),
            affectedComponents = affectedComponents,
            operation = operation,
            description = description
        )
    }
    
    /**
     * Rollback to a previous snapshot
     */
    suspend fun rollbackFromSnapshot(snapshot: GroupingSnapshot): GroupingResult = withContext(Dispatchers.IO) {
        try {
            snapshot.affectedComponents.forEach { (_, component) ->
                componentRepository.saveComponent(component)
            }
            
            Log.d(TAG, "Rolled back operation: ${snapshot.operation}")
            
            GroupingResult(
                success = true,
                message = "Successfully rolled back: ${snapshot.description}",
                affectedComponents = snapshot.affectedComponents.keys.toList()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during rollback", e)
            GroupingResult(
                success = false,
                message = "Error during rollback: ${e.message}"
            )
        }
    }
    
    // === Helper Methods ===
    
    private suspend fun wouldCreateCircularReference(parentId: String, childId: String): Boolean = withContext(Dispatchers.IO) {
        val visited = mutableSetOf<String>()
        
        fun checkCircular(currentId: String): Boolean {
            if (currentId == parentId) return true
            if (currentId in visited) return false
            
            visited.add(currentId)
            
            val component = componentRepository.getComponent(currentId) ?: return false
            return component.childComponents.any { checkCircular(it) }
        }
        
        checkCircular(childId)
    }
    
    private suspend fun calculateComponentDepth(componentId: String): Int = withContext(Dispatchers.IO) {
        val component = componentRepository.getComponent(componentId) ?: return@withContext 0
        
        if (component.parentComponentId == null) {
            0
        } else {
            1 + calculateComponentDepth(component.parentComponentId!!)
        }
    }
    
    private suspend fun calculateMaxDepth(root: Component, allComponents: List<Component>): Int = withContext(Dispatchers.IO) {
        fun getMaxDepthRecursive(componentId: String, currentDepth: Int): Int {
            val children = allComponents.filter { it.parentComponentId == componentId }
            if (children.isEmpty()) return currentDepth
            
            return children.maxOfOrNull { child ->
                getMaxDepthRecursive(child.id, currentDepth + 1)
            } ?: currentDepth
        }
        
        getMaxDepthRecursive(root.id, 0)
    }
    
    private suspend fun buildHierarchyMap(
        root: Component,
        allComponents: List<Component>
    ): Map<String, List<Component>> = withContext(Dispatchers.IO) {
        val hierarchyMap = mutableMapOf<String, List<Component>>()
        
        fun buildMapRecursive(componentId: String) {
            val children = allComponents.filter { it.parentComponentId == componentId }
            hierarchyMap[componentId] = children
            
            children.forEach { child ->
                buildMapRecursive(child.id)
            }
        }
        
        buildMapRecursive(root.id)
        hierarchyMap
    }
    
    private suspend fun buildSiblingGroupMap(allComponents: List<Component>): Map<String, List<Component>> = withContext(Dispatchers.IO) {
        val siblingMap = mutableMapOf<String, List<Component>>()
        
        // Group components by parent to find sibling groups
        val componentsByParent = allComponents
            .filter { it.parentComponentId != null }
            .groupBy { it.parentComponentId!! }
        
        componentsByParent.forEach { (parentId, siblings) ->
            if (siblings.size > 1) {
                siblingMap[parentId] = siblings
            }
        }
        
        siblingMap
    }
    
    private suspend fun findOrCreateCommonParent(components: List<Component>, groupName: String): String = withContext(Dispatchers.IO) {
        // Check if all components already share a common parent
        val parentIds = components.mapNotNull { it.parentComponentId }.distinct()
        
        if (parentIds.size == 1) {
            return@withContext parentIds.first()
        }
        
        // Create new parent group component
        val groupComponent = Component(
            id = UUID.randomUUID().toString(),
            name = groupName,
            category = GROUP_CATEGORY,
            tags = listOf("component-group", "auto-generated"),
            childComponents = components.map { it.id },
            massGrams = 0f, // Group components have no inherent mass
            lastUpdated = LocalDateTime.now()
        )
        
        componentRepository.saveComponent(groupComponent)
        Log.d(TAG, "Created new parent group: $groupName")
        
        groupComponent.id
    }
    
    private fun areCategoriesCompatible(parentCategory: String, childCategory: String): Boolean {
        // Define compatibility rules between categories
        val compatibilityMap = mapOf(
            "filament-tray" to listOf("rfid-tag", "filament", "core", "spool"),
            "component-group" to listOf("general", "filament", "nozzle", "tool"),
            "general" to listOf("general", "component")
        )
        
        val compatibleCategories = compatibilityMap[parentCategory]
        return compatibleCategories?.contains(childCategory) ?: true // Default to compatible
    }
    
    private fun canBeInSiblingGroup(category: String): Boolean {
        // Define which categories can form sibling relationships
        val siblingCompatibleCategories = setOf(
            "filament", "nozzle", "tool", "rfid-tag", "general", "component"
        )
        
        return category in siblingCompatibleCategories
    }
    
    // === Data Classes for Advanced Features ===
    
    data class GroupingSuggestion(
        val type: String,
        val reason: String,
        val componentIds: List<String>,
        val confidence: Float,
        val metadata: Map<String, String> = emptyMap()
    )
    
    data class RelationshipOpportunity(
        val type: String,
        val description: String,
        val componentIds: List<String>,
        val suggestedAction: String,
        val confidence: Float,
        val metadata: Map<String, String> = emptyMap()
    )
}