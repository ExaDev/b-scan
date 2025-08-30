package com.bscan.ui.components.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Expandable floating action button with quick inventory actions
 */
@Composable
fun InventoryQuickActionsFAB(
    onScanComponent: () -> Unit,
    onAddComponent: () -> Unit,
    onExportInventory: () -> Unit,
    onRefreshInventory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Expanded actions
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick action buttons
                QuickActionItem(
                    icon = Icons.Default.Nfc,
                    label = "Scan Component",
                    onClick = {
                        onScanComponent()
                        isExpanded = false
                    }
                )
                
                QuickActionItem(
                    icon = Icons.Default.Add,
                    label = "Add Component",
                    onClick = {
                        onAddComponent()
                        isExpanded = false
                    }
                )
                
                QuickActionItem(
                    icon = Icons.Default.FileDownload,
                    label = "Export Inventory",
                    onClick = {
                        onExportInventory()
                        isExpanded = false
                    }
                )
                
                QuickActionItem(
                    icon = Icons.Default.Refresh,
                    label = "Refresh Data",
                    onClick = {
                        onRefreshInventory()
                        isExpanded = false
                    }
                )
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.MoreVert,
                contentDescription = if (isExpanded) "Close actions" else "Quick actions",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Individual quick action item with icon and label
 */
@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Action label
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Action button
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryQuickActionsFABPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            InventoryQuickActionsFAB(
                onScanComponent = { },
                onAddComponent = { },
                onExportInventory = { },
                onRefreshInventory = { }
            )
        }
    }
}