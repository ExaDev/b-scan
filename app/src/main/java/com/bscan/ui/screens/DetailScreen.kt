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
                // Primary entity section - placeholder for redesign
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Detail View",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "This view needs to be redesigned for the new Component architecture",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Additional sections would go here
                
                // Related entities sections - placeholder
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = "Related items will be displayed here",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Additional related sections would go here
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

