package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.cache.CachedBambuKeyDerivation

/**
 * Card component displaying cache performance statistics.
 * Useful for debugging and monitoring cache effectiveness.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheStatsCard(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onExpandToggle: ((Boolean) -> Unit)? = null
) {
    val stats = CachedBambuKeyDerivation.getCacheStatistics()
    val sizes = CachedBambuKeyDerivation.getCacheSizes()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Cache Statistics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Key Cache Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (onExpandToggle != null) {
                    IconButton(
                        onClick = { onExpandToggle(!expanded) }
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.Memory else Icons.Default.Storage,
                            contentDescription = if (expanded) "Hide details" else "Show details"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Always show hit rate summary
            if (stats != null) {
                val hitRate = (stats.getHitRate() * 100).toInt()
                val totalRequests = stats.getTotalRequests()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hit Rate:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$hitRate% ($totalRequests requests)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            hitRate >= 80 -> MaterialTheme.colorScheme.primary
                            hitRate >= 50 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                if (totalRequests > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = stats.getHitRate(),
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            hitRate >= 80 -> MaterialTheme.colorScheme.primary
                            hitRate >= 50 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            } else {
                Text(
                    text = "Cache not initialized",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Show detailed stats when expanded
            if (expanded && stats != null && sizes != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Detailed Statistics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Cache hits breakdown
                CacheStatRow(
                    label = "Memory Hits",
                    value = "${stats.memoryHits}",
                    icon = Icons.Default.Memory
                )
                
                CacheStatRow(
                    label = "Storage Hits",
                    value = "${stats.persistentHits}",
                    icon = Icons.Default.Storage
                )
                
                CacheStatRow(
                    label = "Cache Misses",
                    value = "${stats.misses}",
                    icon = null
                )
                
                if (stats.errors > 0) {
                    CacheStatRow(
                        label = "Errors",
                        value = "${stats.errors}",
                        icon = null,
                        valueColor = MaterialTheme.colorScheme.error
                    )
                }
                
                if (stats.invalidations > 0) {
                    CacheStatRow(
                        label = "Invalidations",
                        value = "${stats.invalidations}",
                        icon = null
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Cache Sizes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                CacheStatRow(
                    label = "Memory Cache",
                    value = "${sizes.memorySize}/${sizes.memoryMaxSize}",
                    icon = Icons.Default.Memory
                )
                
                CacheStatRow(
                    label = "Storage Cache",
                    value = "${sizes.persistentSize}/${sizes.persistentMaxSize}",
                    icon = Icons.Default.Storage
                )
            }
        }
    }
}

@Composable
private fun CacheStatRow(
    label: String,
    value: String,
    icon: ImageVector?,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

/**
 * Utility function to determine cache performance level
 */
fun getCachePerformanceLevel(hitRate: Float): String {
    return when {
        hitRate >= 0.8f -> "Excellent"
        hitRate >= 0.6f -> "Good"
        hitRate >= 0.4f -> "Fair"
        hitRate >= 0.2f -> "Poor"
        else -> "Very Poor"
    }
}