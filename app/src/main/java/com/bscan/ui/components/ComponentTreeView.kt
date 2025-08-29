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
import androidx.compose.ui.unit.dp
import com.bscan.logic.WeightUnit
import com.bscan.model.Component
import com.bscan.ui.components.tree.*


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


