package com.bscan.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bscan.model.FilamentInfo
import com.bscan.model.ScanDebugInfo
import com.bscan.ui.components.*
import com.bscan.ui.components.filament.*

@Composable
fun FilamentDetailsScreen(
    filamentInfo: FilamentInfo,
    debugInfo: ScanDebugInfo? = null,
    onPurgeCache: ((String) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Handle back press to return to scan prompt instead of closing app
    BackHandler(enabled = onNavigateBack != null) {
        onNavigateBack?.invoke()
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ColorPreviewCard(
                colorHex = filamentInfo.colorHex,
                colorName = filamentInfo.colorName
            )
        }
        
        item {
            InfoCard(
                title = "Filament Type",
                value = filamentInfo.detailedFilamentType.ifEmpty { filamentInfo.filamentType }
            )
        }
        
        item {
            SpecificationCard(filamentInfo = filamentInfo)
        }
        
        item {
            TemperatureCard(filamentInfo = filamentInfo)
        }
        
        item {
            ProductionInfoCard(filamentInfo = filamentInfo)
        }
        
        // Show cache management if callback provided
        onPurgeCache?.let { purgeCallback ->
            item {
                CacheManagementCard(
                    uid = filamentInfo.tagUid,
                    onPurgeCache = purgeCallback
                )
            }
        }
        
        // Show debug info if available
        debugInfo?.let { debug ->
            item {
                DebugInfoCard(debugInfo = debug)
            }
        }
    }
}

