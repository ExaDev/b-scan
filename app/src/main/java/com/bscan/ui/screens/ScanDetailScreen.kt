package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.bscan.model.graph.entities.Activity
import com.bscan.model.graph.entities.ActivityTypes
import com.bscan.model.graph.entities.EntityTypes
import com.bscan.model.graph.Entity
import com.bscan.repository.GraphRepository
import com.bscan.repository.ScanHistoryRepository
import com.bscan.ui.components.scans.EncodedDataView
import com.bscan.ui.components.scans.DecodedDataView
import com.bscan.ui.components.scans.DecryptedDataView
import com.bscan.ui.components.scans.DecryptedEncodedDataView
import com.bscan.ui.components.scans.DecryptedDecodedDataView
import com.bscan.ui.components.scans.DecodedDecryptedDataView
import com.bscan.ui.screens.DetailType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    scanId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val graphRepository = remember { GraphRepository(context) }
    val scanHistoryRepository = remember { ScanHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    
    // Load scan data based on scanId (format: "timestamp-tagUID")
    var scanActivity by remember { mutableStateOf<Activity?>(null) }
    var relatedEntities by remember { mutableStateOf<List<Entity>>(emptyList()) }
    var decryptedScan by remember { mutableStateOf<DecryptedScanData?>(null) }
    var encryptedScan by remember { mutableStateOf<EncryptedScanData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(scanId) {
        try {
            // Find scan Activity entity from GraphRepository
            val allScanActivities = graphRepository.getEntitiesByType(EntityTypes.ACTIVITY)
                .filterIsInstance<Activity>()
                .filter { it.getProperty<String>("activityType") == ActivityTypes.SCAN }
            
            val foundScanActivity = allScanActivities.find { activity ->
                val timestamp = activity.getProperty<String>("timestamp") ?: ""
                val tagUid = activity.getProperty<String>("tagUid") ?: ""
                "$timestamp-$tagUid" == scanId
            }
            
            if (foundScanActivity != null) {
                scanActivity = foundScanActivity
                
                // Get related entities from the scan
                try {
                    relatedEntities = graphRepository.getConnectedEntities(foundScanActivity.id)
                } catch (e: Exception) {
                    relatedEntities = emptyList()
                }
                
                // Try to get corresponding legacy scan data for detailed views
                try {
                    val timestamp = foundScanActivity.getProperty<String>("timestamp") ?: ""
                    val tagUid = foundScanActivity.getProperty<String>("tagUid") ?: ""
                    
                    val allDecryptedScans = scanHistoryRepository.getAllDecryptedScans()
                    val foundDecryptedScan = allDecryptedScans.find { scan ->
                        "${scan.timestamp}-${scan.tagUid}" == scanId
                    }
                    
                    if (foundDecryptedScan != null) {
                        decryptedScan = foundDecryptedScan
                        encryptedScan = scanHistoryRepository.getEncryptedScanForDecrypted(foundDecryptedScan)
                    }
                } catch (e: Exception) {
                    // Legacy data may not be available, but we still have the Activity
                }
                
                isLoading = false
            } else {
                error = "Scan not found"
                isLoading = false
            }
        } catch (e: Exception) {
            error = "Failed to load scan: ${e.message}"
            isLoading = false
        }
    }
    
    // Pager state for the 2 main tabs
    val pagerState = rememberPagerState(pageCount = { 2 })
    val tabTitles = listOf("Encrypted", "Decrypted")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Scan Details")
                },
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
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
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
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Scan summary header
                scanActivity?.let { activity ->
                    val tagUid = activity.getProperty<String>("tagUid") ?: "Unknown"
                    val timestamp = activity.getProperty<String>("timestamp") ?: ""
                    val success = activity.getProperty<Boolean>("success") ?: false
                    val technology = activity.getProperty<String>("technology") ?: "NFC"
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tagUid,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$timestamp • ${if (success) "SUCCESS" else "FAILED"} • $technology",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Tab row
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }
                
                // Pager content with nested tabs
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            // Encrypted tab with encoded/decoded subtabs
                            if (encryptedScan != null || decryptedScan != null) {
                                EncryptedTabContent(
                                    encryptedScan = encryptedScan,
                                    decryptedScan = decryptedScan
                                )
                            } else {
                                // Show related entities when legacy data not available
                                RelatedEntitiesView(
                                    entities = relatedEntities,
                                    onNavigateToDetails = onNavigateToDetails
                                )
                            }
                        }
                        1 -> {
                            // Decrypted tab with encoded/decoded subtabs  
                            if (decryptedScan != null) {
                                DecryptedTabContent(
                                    decryptedScan = decryptedScan
                                )
                            } else {
                                // Show related entities when legacy data not available
                                RelatedEntitiesView(
                                    entities = relatedEntities,
                                    onNavigateToDetails = onNavigateToDetails
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncryptedTabContent(
    encryptedScan: EncryptedScanData?,
    decryptedScan: DecryptedScanData?,
    modifier: Modifier = Modifier
) {
    var selectedSubTabIndex by remember { mutableIntStateOf(0) }
    val subTabTitles = listOf("Encoded", "Decoded")
    
    Column(modifier = modifier.fillMaxSize()) {
        // Sub-tab row
        TabRow(
            selectedTabIndex = selectedSubTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            subTabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTabIndex == index,
                    onClick = { selectedSubTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Sub-tab content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTabIndex) {
                0 -> {
                    // Encoded (Raw encrypted data)
                    if (encryptedScan != null) {
                        EncodedDataView(encryptedScanData = encryptedScan)
                    } else {
                        DataNotAvailableMessage("Encoded data not available")
                    }
                }
                1 -> {
                    // Decoded (Non-encrypted readable metadata)
                    if (encryptedScan != null && decryptedScan != null) {
                        DecodedDataView(
                            encryptedScanData = encryptedScan,
                            decryptedScanData = decryptedScan
                        )
                    } else {
                        DataNotAvailableMessage("Decoded data not available")
                    }
                }
            }
        }
    }
}

@Composable
private fun DecryptedTabContent(
    decryptedScan: DecryptedScanData?,
    modifier: Modifier = Modifier
) {
    var selectedSubTabIndex by remember { mutableIntStateOf(0) }
    val subTabTitles = listOf("Encoded", "Decoded")
    
    Column(modifier = modifier.fillMaxSize()) {
        // Sub-tab row
        TabRow(
            selectedTabIndex = selectedSubTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            subTabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTabIndex == index,
                    onClick = { selectedSubTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Sub-tab content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTabIndex) {
                0 -> {
                    // Encoded (Raw decrypted blocks as hex)
                    if (decryptedScan != null) {
                        DecryptedEncodedDataView(decryptedScanData = decryptedScan)
                    } else {
                        DataNotAvailableMessage("Decrypted data not available")
                    }
                }
                1 -> {
                    // Decoded (Interpreted decrypted content as meaningful filament data)
                    if (decryptedScan != null) {
                        DecryptedDecodedDataView(decryptedScanData = decryptedScan)
                    } else {
                        DataNotAvailableMessage("Interpreted data not available")
                    }
                }
            }
        }
    }
}

@Composable
private fun DataNotAvailableMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelatedEntitiesView(
    entities: List<Entity>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Related Entities",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (entities.isEmpty()) {
            Text(
                text = "No related entities found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            entities.forEach { entity ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Navigate to entity details
                        // Navigate to entity details using entity navigation
                        onNavigateToDetails?.invoke(DetailType.COMPONENT, "entity/${entity.id}")
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entity.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = entity.entityType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}