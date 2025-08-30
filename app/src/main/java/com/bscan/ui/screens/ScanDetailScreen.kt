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
import com.bscan.ui.components.scans.DecryptedDataView
import com.bscan.ui.components.scans.DecodedDecryptedDataView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    scanId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    
    // Load scan data based on scanId (format: "timestamp-tagUID")
    var decryptedScan by remember { mutableStateOf<DecryptedScanData?>(null) }
    var encryptedScan by remember { mutableStateOf<EncryptedScanData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(scanId) {
        try {
            // Parse scanId to find matching scan
            val allDecryptedScans = repository.getAllDecryptedScans()
            val foundDecryptedScan = allDecryptedScans.find { scan ->
                "${scan.timestamp}-${scan.tagUid}" == scanId
            }
            
            if (foundDecryptedScan != null) {
                decryptedScan = foundDecryptedScan
                encryptedScan = repository.getEncryptedScanForDecrypted(foundDecryptedScan)
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
    
    // Pager state for the 4 tabs
    val pagerState = rememberPagerState(pageCount = { 4 })
    val tabTitles = listOf("Encoded", "Decoded", "Decrypted", "Decoded+Decrypted")
    
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
                                text = scan.tagUid,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${scan.timestamp} • ${scan.scanResult.name}",
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
                
                // Pager content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            // Encoded (Raw) Data
                            if (encryptedScan != null) {
                                EncodedDataView(encryptedScanData = encryptedScan!!)
                            } else {
                                DataNotAvailableMessage("Encoded data not available")
                            }
                        }
                        1 -> {
                            // Decoded (Non-encrypted readable data)
                            if (encryptedScan != null && decryptedScan != null) {
                                DecodedDataView(
                                    encryptedScanData = encryptedScan!!,
                                    decryptedScanData = decryptedScan!!
                                )
                            } else {
                                DataNotAvailableMessage("Decoded data not available")
                            }
                        }
                        2 -> {
                            // Decrypted (Decrypted blocks)
                            if (decryptedScan != null) {
                                DecryptedDataView(decryptedScanData = decryptedScan!!)
                            } else {
                                DataNotAvailableMessage("Decrypted data not available")
                            }
                        }
                        3 -> {
                            // Decoded + Decrypted (Combined view)
                            if (encryptedScan != null && decryptedScan != null) {
                                DecodedDecryptedDataView(
                                    encryptedScanData = encryptedScan!!,
                                    decryptedScanData = decryptedScan!!
                                )
                            } else {
                                DataNotAvailableMessage("Combined data not available")
                            }
                        }
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