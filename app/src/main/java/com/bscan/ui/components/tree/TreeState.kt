package com.bscan.ui.components.tree

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.Component

/**
 * Tree view state for tracking expansion and selection
 */
@Stable
data class ComponentTreeState(
    val expandedNodes: Set<String> = emptySet(),
    val selectedNode: String? = null,
    val searchQuery: String = "",
    val showOnlyMatching: Boolean = false,
    val draggedNode: String? = null,
    val dropTarget: String? = null,
    val isLoading: Set<String> = emptySet()
) {
    fun isExpanded(nodeId: String): Boolean = nodeId in expandedNodes
    fun isSelected(nodeId: String): Boolean = selectedNode == nodeId
    fun isLoading(nodeId: String): Boolean = nodeId in isLoading
}

/**
 * Tree view actions for state management
 */
interface ComponentTreeActions {
    fun expandNode(nodeId: String)
    fun collapseNode(nodeId: String) 
    fun toggleNode(nodeId: String)
    fun selectNode(nodeId: String?)
    fun updateSearch(query: String)
    fun toggleSearchFilter(showOnlyMatching: Boolean)
    fun expandAll(rootComponents: List<Component>, getChildren: (String) -> List<Component>)
    fun collapseAll()
    fun startDrag(nodeId: String)
    fun updateDropTarget(targetId: String?)
    fun completeDrop(draggedId: String, targetId: String?)
    fun setLoading(nodeId: String, loading: Boolean)
}

/**
 * Remember tree state with actions
 */
@Composable
fun rememberComponentTreeState(
    initialExpanded: Set<String> = emptySet(),
    initialSelected: String? = null
): Pair<ComponentTreeState, ComponentTreeActions> {
    var state by remember {
        mutableStateOf(
            ComponentTreeState(
                expandedNodes = initialExpanded,
                selectedNode = initialSelected
            )
        )
    }

    val actions = remember {
        object : ComponentTreeActions {
            override fun expandNode(nodeId: String) {
                state = state.copy(expandedNodes = state.expandedNodes + nodeId)
            }

            override fun collapseNode(nodeId: String) {
                state = state.copy(expandedNodes = state.expandedNodes - nodeId)
            }

            override fun toggleNode(nodeId: String) {
                if (state.isExpanded(nodeId)) {
                    collapseNode(nodeId)
                } else {
                    expandNode(nodeId)
                }
            }

            override fun selectNode(nodeId: String?) {
                state = state.copy(selectedNode = nodeId)
            }

            override fun updateSearch(query: String) {
                state = state.copy(searchQuery = query)
            }

            override fun toggleSearchFilter(showOnlyMatching: Boolean) {
                state = state.copy(showOnlyMatching = showOnlyMatching)
            }

            override fun expandAll(
                rootComponents: List<Component>,
                getChildren: (String) -> List<Component>
            ) {
                val allNodeIds = mutableSetOf<String>()
                
                fun collectNodeIds(components: List<Component>) {
                    components.forEach { component ->
                        allNodeIds.add(component.id)
                        val children = getChildren(component.id)
                        if (children.isNotEmpty()) {
                            collectNodeIds(children)
                        }
                    }
                }
                
                collectNodeIds(rootComponents)
                state = state.copy(expandedNodes = allNodeIds)
            }

            override fun collapseAll() {
                state = state.copy(expandedNodes = emptySet())
            }

            override fun startDrag(nodeId: String) {
                state = state.copy(draggedNode = nodeId)
            }

            override fun updateDropTarget(targetId: String?) {
                state = state.copy(dropTarget = targetId)
            }

            override fun completeDrop(draggedId: String, targetId: String?) {
                state = state.copy(draggedNode = null, dropTarget = null)
            }

            override fun setLoading(nodeId: String, loading: Boolean) {
                state = if (loading) {
                    state.copy(isLoading = state.isLoading + nodeId)
                } else {
                    state.copy(isLoading = state.isLoading - nodeId)
                }
            }
        }
    }

    return Pair(state, actions)
}

/**
 * Data class for flattened tree items with depth and hierarchy info
 */
@Stable
data class ComponentTreeItem(
    val component: Component,
    val depth: Int,
    val isLast: Boolean,
    val parentIds: List<String>,
    val childCount: Int,
    val isVisible: Boolean = true
)

/**
 * Flatten hierarchical components into a list for LazyColumn
 */
fun flattenTree(
    rootComponents: List<Component>,
    getChildren: (String) -> List<Component>,
    expandedNodes: Set<String>,
    searchQuery: String,
    showOnlyMatching: Boolean,
    maxDepth: Int
): List<ComponentTreeItem> {
    val result = mutableListOf<ComponentTreeItem>()
    
    fun addComponentAndChildren(
        component: Component,
        depth: Int,
        isLast: Boolean,
        parentIds: List<String>
    ) {
        if (depth > maxDepth) return
        
        val children = getChildren(component.id)
        val matchesSearch = searchQuery.isEmpty() || 
                           component.name.contains(searchQuery, ignoreCase = true) ||
                           component.category.contains(searchQuery, ignoreCase = true) ||
                           component.tags.any { it.contains(searchQuery, ignoreCase = true) }
        
        val shouldShow = if (showOnlyMatching) matchesSearch else true
        
        if (shouldShow) {
            result.add(
                ComponentTreeItem(
                    component = component,
                    depth = depth,
                    isLast = isLast,
                    parentIds = parentIds,
                    childCount = children.size,
                    isVisible = true
                )
            )
        }
        
        if (expandedNodes.contains(component.id) && children.isNotEmpty()) {
            children.forEachIndexed { index, child ->
                addComponentAndChildren(
                    component = child,
                    depth = depth + 1,
                    isLast = index == children.lastIndex,
                    parentIds = parentIds + component.id
                )
            }
        }
    }
    
    rootComponents.forEachIndexed { index, component ->
        addComponentAndChildren(
            component = component,
            depth = 0,
            isLast = index == rootComponents.lastIndex,
            parentIds = emptyList()
        )
    }
    
    return result
}

@Preview(showBackground = true)
@Composable
private fun ComponentTreeStatePreview() {
    MaterialTheme {
        // Preview showing the state management system works
        val (state, actions) = rememberComponentTreeState(
            initialExpanded = setOf("component1"),
            initialSelected = "component2"
        )
        
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tree State Demo",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Expanded nodes: ${state.expandedNodes.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Selected: ${state.selectedNode ?: "None"}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Button(
                onClick = { actions.toggleNode("test-node") }
            ) {
                Text("Toggle Test Node")
            }
        }
    }
}