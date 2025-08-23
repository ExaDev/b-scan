package com.bscan.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        
        action?.let {
            Spacer(modifier = Modifier.height(16.dp))
            it()
        }
    }
}

@Composable
fun FilteredEmptyStateView(
    hasData: Boolean,
    emptyIcon: ImageVector = Icons.Default.Storage,
    emptyTitle: String = "No data available",
    emptySubtitle: String = "Start by adding some data",
    filteredIcon: ImageVector = Icons.Default.FilterList,
    filteredTitle: String = "No results match your filters",
    filteredSubtitle: String = "Try adjusting your filters",
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    if (hasData) {
        EmptyStateView(
            icon = filteredIcon,
            title = filteredTitle,
            subtitle = filteredSubtitle,
            modifier = modifier,
            action = action
        )
    } else {
        EmptyStateView(
            icon = emptyIcon,
            title = emptyTitle,
            subtitle = emptySubtitle,
            modifier = modifier,
            action = action
        )
    }
}

@Composable
fun SpoolEmptyStateView(
    hasSpools: Boolean,
    currentFilter: String,
    modifier: Modifier = Modifier
) {
    val (icon, title, subtitle) = if (hasSpools) {
        val (filterTitle, filterSubtitle) = when (currentFilter) {
            "Successful Only" -> "No successfully scanned spools" to "Try adjusting your filters"
            "High Success Rate" -> "No high-success spools found" to "Spools with 80%+ success rate will appear here"
            else -> "No spools match your filters" to "Try adjusting your filters"
        }
        Triple(Icons.Default.FilterList, filterTitle, filterSubtitle)
    } else {
        Triple(
            Icons.Default.Nfc,
            "No spools in your collection yet",
            "Scan NFC tags to build your spool collection"
        )
    }
    
    EmptyStateView(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier
    )
}