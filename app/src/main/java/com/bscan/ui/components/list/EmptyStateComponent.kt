package com.bscan.ui.components.list

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.ui.screens.ScanPromptScreen

/**
 * Reusable empty state component for lists with no data or no filtered results.
 * Provides contextual messaging and actions based on the empty state type.
 */
@Composable
fun EmptyStateComponent(
    emptyType: EmptyStateType,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null
) {
    when (emptyType) {
        EmptyStateType.NO_INVENTORY_DATA -> {
            // Show full scan prompt when no data exists
            ScanPromptScreen()
        }
        EmptyStateType.NO_FILTERED_RESULTS -> {
            GenericEmptyState(
                icon = Icons.Default.Search,
                title = "No matching items found",
                message = "Try adjusting your filters or search criteria",
                actionText = "Clear Filters",
                onAction = onAction,
                modifier = modifier
            )
        }
        EmptyStateType.NO_COMPONENTS -> {
            GenericEmptyState(
                icon = Icons.Default.Inventory,
                title = "No components found",
                message = "Scan your first component to get started",
                actionText = "Start Scanning",
                onAction = onAction,
                modifier = modifier
            )
        }
    }
}

/**
 * Generic empty state UI with customizable icon, text, and action.
 */
@Composable
private fun GenericEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

/**
 * Types of empty states for different list contexts.
 */
enum class EmptyStateType {
    /** No inventory data exists - show full scan prompt */
    NO_INVENTORY_DATA,
    /** No items match current filters */
    NO_FILTERED_RESULTS,
    /** No components/items exist */
    NO_COMPONENTS
}

