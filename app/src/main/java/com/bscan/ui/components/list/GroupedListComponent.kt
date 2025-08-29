package com.bscan.ui.components.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bscan.ui.screens.home.GroupByOption

/**
 * Generic reusable component for displaying grouped lists with consistent structure.
 * Handles group headers, empty states, and provides consistent spacing.
 */
@Composable
fun <T> GroupedListComponent(
    items: List<Pair<String, List<T>>>,
    lazyListState: LazyListState,
    groupByOption: GroupByOption,
    emptyStateType: EmptyStateType,
    keySelector: (T) -> Any,
    onEmptyAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty() || items.all { it.second.isEmpty() }) {
        EmptyStateComponent(
            emptyType = emptyStateType,
            onAction = onEmptyAction,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (groupKey, groupItems) ->
            // Show group header if grouping is enabled
            if (groupByOption != GroupByOption.NONE) {
                item(key = "header_$groupKey") {
                    GroupHeader(title = groupKey, itemCount = groupItems.size)
                }
            }
            
            // Show items in the group
            items(groupItems, key = keySelector) { item ->
                itemContent(item)
            }
        }
    }
}

/**
 * Wrapper that combines overscroll functionality with grouped list display.
 * This is the complete reusable list structure used across the app.
 */
@Composable
fun <T> OverscrollGroupedListComponent(
    items: List<Pair<String, List<T>>>,
    lazyListState: LazyListState,
    groupByOption: GroupByOption,
    emptyStateType: EmptyStateType,
    keySelector: (T) -> Any,
    scanState: com.bscan.ScanState = com.bscan.ScanState.IDLE,
    scanProgress: com.bscan.model.ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    onEmptyAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    // Handle no data case with full scan prompt
    if (items.isEmpty() || items.all { it.second.isEmpty() }) {
        when (emptyStateType) {
            EmptyStateType.NO_INVENTORY_DATA -> {
                // Show full scan prompt directly
                EmptyStateComponent(
                    emptyType = emptyStateType,
                    onAction = onEmptyAction,
                    modifier = modifier
                )
                return
            }
            else -> {
                // For other empty states, show within overscroll wrapper
            }
        }
    }

    OverscrollListWrapper(
        lazyListState = lazyListState,
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan,
        modifier = modifier
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (items.isEmpty() || items.all { it.second.isEmpty() }) {
                item {
                    EmptyStateComponent(
                        emptyType = emptyStateType,
                        onAction = onEmptyAction
                    )
                }
            } else {
                items.forEach { (groupKey, groupItems) ->
                    // Show group header if grouping is enabled
                    if (groupByOption != GroupByOption.NONE) {
                        item(key = "header_$groupKey") {
                            GroupHeader(title = groupKey, itemCount = groupItems.size)
                        }
                    }
                    
                    // Show items in the group
                    items(groupItems, key = keySelector) { item ->
                        itemContent(item)
                    }
                }
            }
        }
    }
}