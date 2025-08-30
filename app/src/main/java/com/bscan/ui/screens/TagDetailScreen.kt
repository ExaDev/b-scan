package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.bscan.repository.ScanHistoryRepository
import com.bscan.ui.components.scans.EncodedDataView
import com.bscan.ui.components.scans.DecodedDataView
import com.bscan.ui.components.scans.DecryptedEncodedDataView
import com.bscan.ui.components.scans.DecryptedDecodedDataView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
    tagUid: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    
    // Load the most recent scan data for this tag UID
    var decryptedScan by remember { mutableStateOf<DecryptedScanData?>(null) }
    var encryptedScan by remember { mutableStateOf<EncryptedScanData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(tagUid) {
        try {
            // Get all scans for this tag and find the most recent one
            val groupedScans = repository.getScansGroupedByTagUid()
            val scansForTag = groupedScans[tagUid]
            
            if (scansForTag != null && scansForTag.isNotEmpty()) {
                // Get the most recent scan
                val latestScan = scansForTag.maxByOrNull { it.timestamp }
                if (latestScan != null) {
                    decryptedScan = latestScan
                    encryptedScan = repository.getEncryptedScanForDecrypted(latestScan)
                    isLoading = false
                } else {
                    error = "No scans found for this tag"
                    isLoading = false
                }
            } else {
                error = "Tag not found"
                isLoading = false
            }
        } catch (e: Exception) {
            error = "Failed to load tag data: ${e.message}"
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
                    Column {
                        Text("Tag Details")
                        Text(
                            text = tagUid,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                // Tag summary header
                decryptedScan?.let { scan ->
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
                                text = "Tag UID: ${scan.tagUid}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Latest Scan: ${scan.timestamp} • ${scan.scanResult.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Format: ${scan.tagFormat.name}",
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
                            EncryptedTabContent(
                                encryptedScan = encryptedScan,
                                decryptedScan = decryptedScan
                            )
                        }
                        1 -> {
                            // Decrypted tab with encoded/decoded subtabs  
                            DecryptedTabContent(
                                decryptedScan = decryptedScan
                            )
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