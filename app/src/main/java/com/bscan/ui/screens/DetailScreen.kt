package com.bscan.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import com.bscan.repository.UnifiedDataAccess
import com.bscan.ui.components.detail.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    detailType: DetailType,
    identifier: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onPurgeCache: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Log.d("DetailScreen", "Starting DetailScreen with type: $detailType, identifier: $identifier")
    
    val context = LocalContext.current
    val catalogRepository = remember { CatalogRepository(context) }
    val userRepository = remember { UserDataRepository(context) }
    val scanHistoryRepository = remember { ScanHistoryRepository(context) }
    val unifiedDataAccess = remember { UnifiedDataAccess(catalogRepository, userRepository, scanHistoryRepository) }
    val viewModel = remember { DetailViewModel(unifiedDataAccess) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Add comprehensive error logging
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Log.e("DetailScreen", "UI State Error: $error for type: $detailType, identifier: $identifier")
        }
    }
    
    LaunchedEffect(detailType, identifier) {
        Log.d("DetailScreen", "Loading details for type: $detailType, identifier: $identifier")
        try {
            viewModel.loadDetails(detailType, identifier)
        } catch (e: Exception) {
            Log.e("DetailScreen", "Failed to load details", e)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (detailType) {
                            DetailType.SCAN -> "Scan Details"
                            DetailType.TAG -> "Tag Details" 
                            DetailType.INVENTORY_STOCK -> "Inventory Stock Details"
                            DetailType.SKU -> "SKU Details"
                            DetailType.COMPONENT -> "Component Details"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary entity section
                when (detailType) {
                    DetailType.SCAN -> {
                        uiState.primaryScan?.let { scan ->
                            item {
                                PrimaryScanCard(scan = scan)
                            }
                        }
                    }
                    DetailType.TAG -> {
                        uiState.primaryTag?.let { tag ->
                            item {
                                PrimaryTagCard(tag = tag)
                            }
                        }
                    }
                    DetailType.INVENTORY_STOCK -> {
                        uiState.primarySpool?.let { spool ->
                            // Add individual items instead of a single wrapped component
                            item {
                                Text(
                                    text = "Inventory Stock Information",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            item {
                                PrimaryFilamentReelCard(filamentReel = spool, onPurgeCache = onPurgeCache)
                            }
                        }
                    }
                    DetailType.SKU -> {
                        uiState.primarySku?.let { sku ->
                            item {
                                PrimarySkuCard(sku = sku)
                            }
                        }
                    }
                    DetailType.COMPONENT -> {
                        uiState.primaryComponent?.let { component ->
                            item {
                                Text(
                                    text = "Component Information",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Add component display logic here when needed
                        }
                    }
                }
                
                // Related entities sections
                if (detailType != DetailType.INVENTORY_STOCK && uiState.relatedFilamentReels.isNotEmpty()) {
                    item {
                        RelatedFilamentReelsSection(
                            filamentReels = uiState.relatedFilamentReels,
                            onNavigateToDetails = onNavigateToDetails
                        )
                    }
                }
                
                if (detailType != DetailType.TAG && uiState.relatedTags.isNotEmpty()) {
                    item {
                        RelatedTagsSection(
                            tagUids = uiState.relatedTags,
                            allScans = uiState.relatedScans,
                            onNavigateToDetails = onNavigateToDetails
                        )
                    }
                }
                
                if (detailType != DetailType.SKU && uiState.relatedSkus.isNotEmpty()) {
                    item {
                        AssociatedSkuSection(
                            skus = uiState.relatedSkus,
                            onNavigateToDetails = onNavigateToDetails
                        )
                    }
                }
                
                if (detailType != DetailType.SCAN && uiState.relatedScans.isNotEmpty()) {
                    item {
                        RelatedScansSection(
                            scans = uiState.relatedScans,
                            onNavigateToDetails = onNavigateToDetails
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenPreview() {
    MaterialTheme {
        DetailScreen(
            detailType = DetailType.TAG,
            identifier = "sample-tag-id",
            onNavigateBack = {},
            onNavigateToDetails = { _, _ -> },
            onPurgeCache = {}
        )
    }
}

