package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bscan.cache.CachedBambuKeyDerivation
import com.bscan.ui.components.cache.*

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
            CachePerformanceHeader(
                expanded = expanded,
                onExpandToggle = onExpandToggle
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Always show hit rate summary
            if (stats != null) {
                CacheHitRateDisplay(stats = stats)
            } else {
                CacheNotInitializedDisplay()
            }
            
            // Show detailed stats when expanded
            if (expanded && stats != null && sizes != null) {
                CacheDetailedStats(
                    stats = stats,
                    sizes = sizes
                )
            }
        }
    }
}

