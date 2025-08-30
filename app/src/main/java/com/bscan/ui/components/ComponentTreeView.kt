package com.bscan.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.logic.WeightUnit
import com.bscan.model.Component
import com.bscan.model.ComponentIdentifier
import com.bscan.model.IdentifierType
import com.bscan.model.IdentifierPurpose
import com.bscan.ui.components.tree.*
import java.time.LocalDateTime


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
            TreeToolbar(
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
            TreeNode(
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

@Preview(showBackground = true)
@Composable
private fun ComponentTreeViewPreview() {
    // Create mock component hierarchy
    val mockComponents = remember {
        createMockComponentHierarchy()
    }
    
    val mockTreeState = ComponentTreeState(
        expandedNodes = setOf("tray-001"),
        selectedNode = null,
        searchQuery = "",
        showOnlyMatching = false
    )
    
    val mockTreeActions = object : ComponentTreeActions {
        override fun expandNode(nodeId: String) {}
        override fun collapseNode(nodeId: String) {}
        override fun toggleNode(nodeId: String) {}
        override fun selectNode(nodeId: String?) {}
        override fun updateSearch(query: String) {}
        override fun toggleSearchFilter(showOnlyMatching: Boolean) {}
        override fun expandAll(rootComponents: List<Component>, getChildren: (String) -> List<Component>) {}
        override fun collapseAll() {}
        override fun startDrag(nodeId: String) {}
        override fun updateDropTarget(targetId: String?) {}
        override fun completeDrop(draggedId: String, targetId: String?) {}
        override fun setLoading(nodeId: String, loading: Boolean) {}
    }
    
    MaterialTheme {
        ComponentTreeView(
            rootComponents = mockComponents.values.filter { it.parentComponentId == null },
            getChildren = { parentId -> mockComponents.values.filter { it.parentComponentId == parentId } },
            treeState = mockTreeState,
            treeActions = mockTreeActions,
            preferredWeightUnit = WeightUnit.GRAMS,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentTreeViewEmptyPreview() {
    val mockTreeState = ComponentTreeState()
    val mockTreeActions = object : ComponentTreeActions {
        override fun expandNode(nodeId: String) {}
        override fun collapseNode(nodeId: String) {}
        override fun toggleNode(nodeId: String) {}
        override fun selectNode(nodeId: String?) {}
        override fun updateSearch(query: String) {}
        override fun toggleSearchFilter(showOnlyMatching: Boolean) {}
        override fun expandAll(rootComponents: List<Component>, getChildren: (String) -> List<Component>) {}
        override fun collapseAll() {}
        override fun startDrag(nodeId: String) {}
        override fun updateDropTarget(targetId: String?) {}
        override fun completeDrop(draggedId: String, targetId: String?) {}
        override fun setLoading(nodeId: String, loading: Boolean) {}
    }
    
    MaterialTheme {
        ComponentTreeView(
            rootComponents = emptyList(),
            getChildren = { emptyList() },
            treeState = mockTreeState,
            treeActions = mockTreeActions,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun createMockComponentHierarchy(): Map<String, Component> {
    val components = mutableMapOf<String, Component>()
    
    // Root tray component
    val trayComponent = Component(
        id = "tray-001",
        identifiers = listOf(
            ComponentIdentifier(
                type = IdentifierType.CONSUMABLE_UNIT,
                value = "01008023ABC123", 
                purpose = IdentifierPurpose.TRACKING
            )
        ),
        name = "Bambu PLA Tray",
        category = "tray",
        childComponents = listOf("rfid-tag-001", "filament-001", "core-001", "spool-001"),
        massGrams = 1245.0f,
        fullMassGrams = 1245.0f,
        manufacturer = "Bambu Lab",
        description = "Complete filament spool assembly",
        lastUpdated = LocalDateTime.now()
    )
    components[trayComponent.id] = trayComponent
    
    // RFID tag child
    val rfidComponent = Component(
        id = "rfid-tag-001",
        identifiers = listOf(
            ComponentIdentifier(
                type = IdentifierType.RFID_HARDWARE,
                value = "A1B2C3D4",
                purpose = IdentifierPurpose.AUTHENTICATION
            )
        ),
        name = "RFID Tag",
        category = "rfid-tag",
        parentComponentId = "tray-001",
        massGrams = 0.5f,
        manufacturer = "Bambu Lab",
        description = "Mifare Classic 1K authentication tag"
    )
    components[rfidComponent.id] = rfidComponent
    
    // Filament child with variable mass
    val filamentComponent = Component(
        id = "filament-001",
        name = "PLA Basic Red",
        category = "filament", 
        parentComponentId = "tray-001",
        massGrams = 800.0f,
        fullMassGrams = 1000.0f,
        variableMass = true,
        manufacturer = "Bambu Lab",
        description = "High quality PLA filament",
        tags = listOf("PLA", "Basic", "Red")
    )
    components[filamentComponent.id] = filamentComponent
    
    // Core child
    val coreComponent = Component(
        id = "core-001",
        name = "Cardboard Core",
        category = "core",
        parentComponentId = "tray-001", 
        massGrams = 33.0f,
        manufacturer = "Bambu Lab",
        description = "Recyclable cardboard spool core"
    )
    components[coreComponent.id] = coreComponent
    
    // Spool child
    val spoolComponent = Component(
        id = "spool-001",
        name = "Plastic Spool",
        category = "spool",
        parentComponentId = "tray-001",
        massGrams = 212.0f,
        manufacturer = "Bambu Lab", 
        description = "Reusable plastic spool shell"
    )
    components[spoolComponent.id] = spoolComponent
    
    // Add a second root component (tool)
    val toolComponent = Component(
        id = "tool-001",
        identifiers = listOf(
            ComponentIdentifier(
                type = IdentifierType.SERIAL_NUMBER,
                value = "DRILL_SN12345",
                purpose = IdentifierPurpose.TRACKING
            )
        ),
        name = "Workshop Drill",
        category = "tool",
        massGrams = 2500.0f,
        manufacturer = "Acme Tools",
        description = "Professional workshop drill with various attachments",
        childComponents = listOf("drill-bit-001", "battery-001")
    )
    components[toolComponent.id] = toolComponent
    
    // Drill bit child
    val drillBitComponent = Component(
        id = "drill-bit-001",
        name = "10mm Drill Bit",
        category = "accessory",
        parentComponentId = "tool-001",
        massGrams = 50.0f,
        manufacturer = "Acme Tools",
        description = "High-speed steel drill bit"
    )
    components[drillBitComponent.id] = drillBitComponent
    
    // Battery child
    val batteryComponent = Component(
        id = "battery-001",
        name = "Li-ion Battery Pack",
        category = "battery",
        parentComponentId = "tool-001",
        massGrams = 450.0f,
        variableMass = false,
        manufacturer = "Acme Tools",
        description = "18V 2.0Ah lithium-ion battery"
    )
    components[batteryComponent.id] = batteryComponent
    
    return components
}

