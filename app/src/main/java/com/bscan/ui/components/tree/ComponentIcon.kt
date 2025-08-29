package com.bscan.ui.components.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Component category-specific icons with background colors
 */
@Composable
fun ComponentIcon(
    category: String,
    tags: List<String> = emptyList(),
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

@Preview(showBackground = true)
@Composable
private fun ComponentIconPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComponentIcon(
                category = "filament",
                modifier = Modifier.size(40.dp)
            )
            ComponentIcon(
                category = "spool",
                modifier = Modifier.size(40.dp)
            )
            ComponentIcon(
                category = "rfid-tag",
                tags = listOf("hardware", "bambu"),
                modifier = Modifier.size(40.dp)
            )
            ComponentIcon(
                category = "tool",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

