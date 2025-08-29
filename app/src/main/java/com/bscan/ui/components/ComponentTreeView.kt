package com.bscan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.bscan.logic.WeightUnit
import com.bscan.model.Component
import com.bscan.model.ComponentIdentifier
import com.bscan.model.IdentifierType

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
 * Main component tree view displaying hierarchical component structures
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComponentTreeView(
    rootComponents: List<Component>,
    getChildren: (String) -> List<Component>,
    treeState: ComponentTreeState,
    treeActions: ComponentTreeActions,
    preferredWeightUnit: WeightUnit = WeightUnit.GRAMS,
    onComponentClick: (Component) -> Unit = {},
    onComponentLongClick: (Component) -> Unit = {},
    onDragDrop: (draggedId: String, targetId: String) -> Unit = { _, _ -> },
    onAddChild: (parentId: String) -> Unit = {},
    onRemoveComponent: (componentId: String) -> Unit = {},
    onEditMass: (componentId: String) -> Unit = {},
    showSearchBar: Boolean = true,
    showToolbar: Boolean = true,
    maxDepth: Int = 10,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Search bar and toolbar
        if (showSearchBar || showToolbar) {
            ComponentTreeToolbar(
                searchQuery = treeState.searchQuery,
                onSearchChange = treeActions::updateSearch,
                showOnlyMatching = treeState.showOnlyMatching,
                onToggleFilter = treeActions::toggleSearchFilter,
                onExpandAll = { 
                    treeActions.expandAll(rootComponents, getChildren)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onCollapseAll = {
                    treeActions.collapseAll()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                showSearchBar = showSearchBar,
                showToolbar = showToolbar,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Tree content
        Box(modifier = Modifier.fillMaxWidth()) {
            if (rootComponents.isEmpty()) {
                EmptyTreeView(modifier = Modifier.padding(24.dp))
            } else {
                ComponentTreeContent(
                    rootComponents = rootComponents,
                    getChildren = getChildren,
                    treeState = treeState,
                    treeActions = treeActions,
                    preferredWeightUnit = preferredWeightUnit,
                    onComponentClick = onComponentClick,
                    onComponentLongClick = onComponentLongClick,
                    onDragDrop = onDragDrop,
                    onAddChild = onAddChild,
                    onRemoveComponent = onRemoveComponent,
                    onEditMass = onEditMass,
                    maxDepth = maxDepth,
                    listState = listState,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Loading overlay for async operations
            if (treeState.isLoading.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

/**
 * Toolbar with search and tree operations
 */
@Composable
private fun ComponentTreeToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showOnlyMatching: Boolean,
    onToggleFilter: (Boolean) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    showSearchBar: Boolean,
    showToolbar: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search bar
        if (showSearchBar) {
            ComponentTreeSearchBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                showOnlyMatching = showOnlyMatching,
                onToggleFilter = onToggleFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Toolbar with actions
        if (showToolbar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/Collapse buttons
                OutlinedButton(
                    onClick = onExpandAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.UnfoldMore,
                        contentDescription = "Expand All",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expand All", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onCollapseAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.UnfoldLess,
                        contentDescription = "Collapse All",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Collapse All", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Search bar for tree filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComponentTreeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    showOnlyMatching: Boolean,
    onToggleFilter: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { 
                Text(
                    "Search components...",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier.weight(1f)
        )

        // Filter toggle
        FilterChip(
            onClick = { onToggleFilter(!showOnlyMatching) },
            label = { 
                Text(
                    "Filter",
                    style = MaterialTheme.typography.labelSmall
                ) 
            },
            selected = showOnlyMatching,
            leadingIcon = if (showOnlyMatching) {
                {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else null
        )
    }
}

/**
 * Tree content with lazy scrolling
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComponentTreeContent(
    rootComponents: List<Component>,
    getChildren: (String) -> List<Component>,
    treeState: ComponentTreeState,
    treeActions: ComponentTreeActions,
    preferredWeightUnit: WeightUnit,
    onComponentClick: (Component) -> Unit,
    onComponentLongClick: (Component) -> Unit,
    onDragDrop: (draggedId: String, targetId: String) -> Unit,
    onAddChild: (parentId: String) -> Unit,
    onRemoveComponent: (componentId: String) -> Unit,
    onEditMass: (componentId: String) -> Unit,
    maxDepth: Int,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val flattenedItems = remember(rootComponents, treeState.expandedNodes, treeState.searchQuery, treeState.showOnlyMatching) {
        flattenTree(
            rootComponents = rootComponents,
            getChildren = getChildren,
            expandedNodes = treeState.expandedNodes,
            searchQuery = treeState.searchQuery,
            showOnlyMatching = treeState.showOnlyMatching,
            maxDepth = maxDepth
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = flattenedItems,
            key = { it.component.id }
        ) { treeItem ->
            ComponentNode(
                treeItem = treeItem,
                treeState = treeState,
                treeActions = treeActions,
                preferredWeightUnit = preferredWeightUnit,
                onComponentClick = onComponentClick,
                onComponentLongClick = onComponentLongClick,
                onDragDrop = onDragDrop,
                onAddChild = onAddChild,
                onRemoveComponent = onRemoveComponent,
                onEditMass = onEditMass,
                getChildren = getChildren,
                modifier = Modifier
                    // Note: animateItemPlacement() is not available in current Compose version
                    .fillMaxWidth()
            )
        }
    }
}

/**
 * Empty state display when no components exist
 */
@Composable
private fun EmptyTreeView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Outlined.AccountTree,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Text(
            text = "No Components",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Scan an RFID tag or add components manually to see them in a hierarchical view",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
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
private fun flattenTree(
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

/**
 * Individual component node in the tree
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ComponentNode(
    treeItem: ComponentTreeItem,
    treeState: ComponentTreeState,
    treeActions: ComponentTreeActions,
    preferredWeightUnit: WeightUnit,
    onComponentClick: (Component) -> Unit,
    onComponentLongClick: (Component) -> Unit,
    onDragDrop: (draggedId: String, targetId: String) -> Unit,
    onAddChild: (parentId: String) -> Unit,
    onRemoveComponent: (componentId: String) -> Unit,
    onEditMass: (componentId: String) -> Unit,
    getChildren: (String) -> List<Component>,
    modifier: Modifier = Modifier
) {
    val component = treeItem.component
    val isExpanded = treeState.isExpanded(component.id)
    val isSelected = treeState.isSelected(component.id)
    val isLoading = treeState.isLoading(component.id)
    val isDragTarget = treeState.dropTarget == component.id
    val isDragged = treeState.draggedNode == component.id
    val haptic = LocalHapticFeedback.current

    // Animation states
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(200),
        label = "expand_rotation"
    )
    
    val cardElevation by animateFloatAsState(
        targetValue = when {
            isDragged -> 8f
            isDragTarget -> 4f
            isSelected -> 2f
            else -> 1f
        },
        animationSpec = tween(200),
        label = "card_elevation"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isDragged) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .scale(cardScale)
            .alpha(if (isDragged) 0.8f else 1f)
            .pointerInput(component.id) {
                detectDragGestures(
                    onDragStart = {
                        treeActions.startDrag(component.id)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        val draggedId = treeState.draggedNode
                        val targetId = treeState.dropTarget
                        if (draggedId != null && targetId != null && draggedId != targetId) {
                            onDragDrop(draggedId, targetId)
                        }
                        treeActions.completeDrop(draggedId ?: "", targetId)
                    },
                    onDrag = { _, _ ->
                        // Update drop target based on position
                        // This would require more complex hit testing implementation
                    }
                )
            }
            .semantics {
                contentDescription = "Component: ${component.name}, Category: ${component.category}"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isDragTarget -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isDragTarget) {
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    )
                ),
                width = 2.dp
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onComponentClick(component)
                    treeActions.selectNode(component.id)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hierarchy indicators
            HierarchyIndicator(
                depth = treeItem.depth,
                isLast = treeItem.isLast,
                parentIds = treeItem.parentIds,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Expand/collapse button
            if (treeItem.childCount > 0) {
                IconButton(
                    onClick = {
                        treeActions.toggleNode(component.id)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(expandRotation)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            // Component icon
            ComponentIcon(
                category = component.category,
                tags = component.tags,
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 12.dp)
            )

            // Component info
            Column(modifier = Modifier.weight(1f)) {
                // Name and identifier
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Primary identifier chip
                    component.getPrimaryTrackingIdentifier()?.let { identifier ->
                        IdentifierChip(
                            identifier = identifier,
                            modifier = Modifier
                        )
                    }
                }

                // Category and tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                component.category,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        modifier = Modifier.height(24.dp)
                    )

                    // Key tags
                    component.tags.take(2).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (component.tags.size > 2) {
                        Text(
                            "+${component.tags.size - 2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mass information
                MassDisplayChip(
                    massGrams = component.massGrams,
                    fullMassGrams = component.fullMassGrams,
                    variableMass = component.variableMass,
                    inferredMass = component.inferredMass,
                    preferredUnit = preferredWeightUnit,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Child count indicator
                if (treeItem.childCount > 0) {
                    Text(
                        text = "${treeItem.childCount} component${if (treeItem.childCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Add child button (for composite components)
                if (component.isComposite || treeItem.childCount == 0) {
                    IconButton(
                        onClick = { onAddChild(component.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add child component",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Edit mass button (for variable mass components)
                if (component.variableMass) {
                    IconButton(
                        onClick = { onEditMass(component.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit mass",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // More actions menu
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Remove Component") },
                            onClick = {
                                onRemoveComponent(component.id)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Component Details") },
                            onClick = {
                                onComponentLongClick(component)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual hierarchy indicators (lines and connections)
 */
@Composable
private fun HierarchyIndicator(
    depth: Int,
    isLast: Boolean,
    parentIds: List<String>,
    modifier: Modifier = Modifier
) {
    if (depth == 0) return

    val strokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val density = LocalDensity.current

    Canvas(
        modifier = modifier.size(width = (depth * 16).dp, height = 24.dp)
    ) {
        val strokeWidth = with(density) { 1.dp.toPx() }
        val segmentWidth = with(density) { 16.dp.toPx() }
        val centerY = size.height / 2f

        // Draw vertical lines for each parent level
        for (level in 0 until depth) {
            val x = level * segmentWidth + segmentWidth / 2f
            
            if (level < depth - 1) {
                // Vertical line for intermediate levels
                drawLine(
                    color = strokeColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth
                )
            } else {
                // L-shaped connector for current level
                if (!isLast) {
                    // Vertical line continues down
                    drawLine(
                        color = strokeColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                } else {
                    // Vertical line stops at center
                    drawLine(
                        color = strokeColor,
                        start = Offset(x, 0f),
                        end = Offset(x, centerY),
                        strokeWidth = strokeWidth
                    )
                }
                
                // Horizontal line to component
                drawLine(
                    color = strokeColor,
                    start = Offset(x, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

/**
 * Component category-specific icons
 */
@Composable
private fun ComponentIcon(
    category: String,
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (category.lowercase()) {
        "filament" -> Icons.Default.Polymer to MaterialTheme.colorScheme.primary
        "spool" -> Icons.Outlined.Circle to MaterialTheme.colorScheme.secondary
        "core" -> Icons.Default.FiberManualRecord to MaterialTheme.colorScheme.tertiary
        "adapter" -> Icons.Default.Transform to MaterialTheme.colorScheme.primary
        "packaging" -> Icons.Default.LocalShipping to MaterialTheme.colorScheme.secondary
        "rfid-tag" -> Icons.Default.Sensors to MaterialTheme.colorScheme.tertiary
        "filament-tray" -> Icons.Default.Inventory to MaterialTheme.colorScheme.primary
        "nozzle" -> Icons.Default.Circle to MaterialTheme.colorScheme.secondary
        "hotend" -> Icons.Default.Thermostat to MaterialTheme.colorScheme.error
        "tool" -> Icons.Default.Build to MaterialTheme.colorScheme.primary
        else -> Icons.Default.Category to MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Special handling for tagged components
    val finalIcon = when {
        "consumable" in tags -> Icons.AutoMirrored.Filled.TrendingDown
        "hardware" in tags -> Icons.Default.Build  
        "electronics" in tags -> Icons.Default.Memory
        "bambu" in tags -> Icons.Default.Build // PrecisionManufacturing not available
        else -> icon
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            finalIcon,
            contentDescription = "Component category: $category",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Identifier display chip
 */
@Composable
private fun IdentifierChip(
    identifier: ComponentIdentifier,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (identifier.type) {
        IdentifierType.RFID_HARDWARE -> "RFID" to MaterialTheme.colorScheme.primary
        IdentifierType.CONSUMABLE_UNIT -> "Tray" to MaterialTheme.colorScheme.secondary
        IdentifierType.SERIAL_NUMBER -> "S/N" to MaterialTheme.colorScheme.tertiary
        IdentifierType.SKU -> "SKU" to MaterialTheme.colorScheme.primary
        IdentifierType.QR -> "QR" to MaterialTheme.colorScheme.secondary
        IdentifierType.BARCODE -> "Barcode" to MaterialTheme.colorScheme.tertiary
        else -> identifier.type.name to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AssistChip(
        onClick = { },
        label = { 
            Text(
                "$label: ${identifier.value.take(8)}${if (identifier.value.length > 8) "..." else ""}",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            ) 
        },
        modifier = modifier.height(20.dp),
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color
        )
    )
}

/**
 * Mass information display with status indicators
 */
@Composable
private fun MassDisplayChip(
    massGrams: Float?,
    fullMassGrams: Float?,
    variableMass: Boolean,
    inferredMass: Boolean,
    preferredUnit: WeightUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mass value
        Text(
            text = formatMass(massGrams, preferredUnit),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (inferredMass) FontWeight.Normal else FontWeight.Medium,
            color = if (inferredMass) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        // Status indicators
        if (inferredMass) {
            Icon(
                Icons.Default.Calculate,
                contentDescription = "Inferred mass",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        if (variableMass) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = "Variable mass",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        // Percentage for variable mass components
        if (variableMass && fullMassGrams != null && fullMassGrams > 0f && massGrams != null) {
            val percentage = ((massGrams / fullMassGrams) * 100).toInt()
            Text(
                text = "($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    percentage < 10 -> MaterialTheme.colorScheme.error
                    percentage < 25 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Helper function to format mass values
 */
private fun formatMass(massGrams: Float?, preferredUnit: WeightUnit): String {
    if (massGrams == null) return "Unknown"
    
    return when (preferredUnit) {
        WeightUnit.GRAMS -> "${String.format("%.1f", massGrams)}g"
        WeightUnit.KILOGRAMS -> "${String.format("%.3f", massGrams / 1000f)}kg"  
        WeightUnit.OUNCES -> "${String.format("%.2f", massGrams * 0.035274f)}oz"
        WeightUnit.POUNDS -> "${String.format("%.3f", massGrams * 0.00220462f)}lbs"
    }
}