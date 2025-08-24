package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.SpoolDetails
import com.bscan.ui.components.*
import com.bscan.ui.components.filament.*
import com.bscan.ui.components.history.*
import com.bscan.ui.components.spool.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolDetailsScreen(
    trayUid: String,
    onNavigateBack: () -> Unit,
    onPurgeCache: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    var spoolDetails by remember { mutableStateOf<SpoolDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(trayUid) {
        try {
            spoolDetails = repository.getSpoolDetails(trayUid)
        } catch (e: Exception) {
            spoolDetails = null
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spool Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            spoolDetails?.let { details ->
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filament Color Preview
                    item {
                        ColorPreviewCard(
                            colorHex = details.filamentInfo.colorHex,
                            colorName = details.filamentInfo.colorName,
                            filamentType = details.filamentInfo.filamentType
                        )
                    }
                    
                    // Filament Type Info
                    item {
                        InfoCard(
                            title = "Filament Type",
                            value = details.filamentInfo.detailedFilamentType.ifEmpty { 
                                details.filamentInfo.filamentType 
                            }
                        )
                    }
                    
                    // Specifications
                    item {
                        SpecificationCard(filamentInfo = details.filamentInfo)
                    }
                    
                    // Temperature Settings
                    item {
                        TemperatureCard(filamentInfo = details.filamentInfo)
                    }
                    
                    // Production Information
                    item {
                        ProductionInfoCard(filamentInfo = details.filamentInfo)
                    }
                    
                    // Spool Overview
                    item {
                        SpoolOverviewCard(spoolDetails = details)
                    }
                    
                    // Associated Tags
                    item {
                        AssociatedTagsCard(spoolDetails = details)
                    }
                    
                    // Cache Management
                    onPurgeCache?.let { purgeCallback ->
                        details.tagUids.forEach { tagUid ->
                            item {
                                CacheManagementCard(
                                    uid = tagUid,
                                    onPurgeCache = purgeCallback
                                )
                            }
                        }
                    }
                    
                    // Scan History Header
                    item {
                        Text(
                            text = "Scan History (${details.totalScans} scans)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // Scan History
                    items(details.allScans) { scan ->
                        var expanded by remember { mutableStateOf(false) }
                        ScanHistoryCard(
                            scan = scan,
                            isExpanded = expanded,
                            onToggleExpanded = { expanded = !expanded },
                            onScanClick = null // Don't navigate since we're already on the details page
                        )
                    }
                    
                    // Empty state for scans
                    if (details.allScans.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No scan history available",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                // Error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(
                            text = "Spool not found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Could not find spool with tray UID: $trayUid",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}