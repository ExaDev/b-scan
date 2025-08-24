package com.bscan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.repository.ScanHistoryRepository
import com.bscan.ui.components.*
import com.bscan.ui.components.filament.*
import com.bscan.ui.components.history.*
import com.bscan.ui.components.spool.*
import com.bscan.ui.screens.home.SkuCard
import com.bscan.ui.screens.home.SpoolCard
import com.bscan.ui.screens.home.TagCard
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val viewModel = remember { DetailViewModel(repository) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(detailType, identifier) {
        viewModel.loadDetails(detailType, identifier)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (detailType) {
                            DetailType.SCAN -> "Scan Details"
                            DetailType.TAG -> "Tag Details" 
                            DetailType.SPOOL -> "Spool Details"
                            DetailType.SKU -> "SKU Details"
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
                                PrimaryScanSection(scan = scan)
                            }
                        }
                    }
                    DetailType.TAG -> {
                        uiState.primaryTag?.let { tag ->
                            item {
                                PrimaryTagSection(tag = tag)
                            }
                        }
                    }
                    DetailType.SPOOL -> {
                        uiState.primarySpool?.let { spool ->
                            item {
                                PrimarySpoolSection(spool = spool, onPurgeCache = onPurgeCache)
                            }
                        }
                    }
                    DetailType.SKU -> {
                        uiState.primarySku?.let { sku ->
                            item {
                                PrimarySkuSection(sku = sku)
                            }
                        }
                    }
                }
                
                // Related entities sections
                if (detailType != DetailType.SPOOL && uiState.relatedSpools.isNotEmpty()) {
                    item {
                        RelatedSpoolsSection(
                            spools = uiState.relatedSpools,
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

@Composable
fun PrimaryScanSection(scan: com.bscan.repository.InterpretedScan) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Scan Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        var expanded by remember { mutableStateOf(true) }
        ScanHistoryCard(
            scan = scan,
            isExpanded = expanded,
            onToggleExpanded = { expanded = !expanded },
            onScanClick = null
        )
        
        scan.filamentInfo?.let { filamentInfo ->
            ColorPreviewCard(
                colorHex = filamentInfo.colorHex,
                colorName = filamentInfo.colorName,
                filamentType = filamentInfo.filamentType
            )
            
            InfoCard(
                title = "Filament Type",
                value = filamentInfo.detailedFilamentType.ifEmpty { filamentInfo.filamentType }
            )
            
            SpecificationCard(filamentInfo = filamentInfo)
            TemperatureCard(filamentInfo = filamentInfo)
            ProductionInfoCard(filamentInfo = filamentInfo)
        }
    }
}

@Composable
fun PrimaryTagSection(tag: com.bscan.repository.InterpretedScan) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Tag Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        InfoCard(
            title = "Tag UID",
            value = tag.uid
        )
        
        InfoCard(
            title = "Last Scanned",
            value = tag.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        )
        
        tag.filamentInfo?.let { filamentInfo ->
            ColorPreviewCard(
                colorHex = filamentInfo.colorHex,
                colorName = filamentInfo.colorName,
                filamentType = filamentInfo.filamentType
            )
            
            InfoCard(
                title = "Filament Type",
                value = filamentInfo.detailedFilamentType.ifEmpty { filamentInfo.filamentType }
            )
            
            SpecificationCard(filamentInfo = filamentInfo)
            TemperatureCard(filamentInfo = filamentInfo)
            ProductionInfoCard(filamentInfo = filamentInfo)
        }
    }
}

@Composable
fun PrimarySpoolSection(
    spool: com.bscan.repository.SpoolDetails,
    onPurgeCache: ((String) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Spool Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Filament Color Preview
        ColorPreviewCard(
            colorHex = spool.filamentInfo.colorHex,
            colorName = spool.filamentInfo.colorName,
            filamentType = spool.filamentInfo.filamentType
        )
        
        // Filament Type Info
        InfoCard(
            title = "Filament Type",
            value = spool.filamentInfo.detailedFilamentType.ifEmpty { 
                spool.filamentInfo.filamentType 
            }
        )
        
        // Specifications
        SpecificationCard(filamentInfo = spool.filamentInfo)
        
        // Temperature Settings
        TemperatureCard(filamentInfo = spool.filamentInfo)
        
        // Production Information
        ProductionInfoCard(filamentInfo = spool.filamentInfo)
        
        // Spool Overview
        SpoolOverviewCard(spoolDetails = spool)
        
        // Associated Tags
        AssociatedTagsCard(spoolDetails = spool)
        
        // Cache Management
        onPurgeCache?.let { purgeCallback ->
            spool.tagUids.forEach { tagUid ->
                CacheManagementCard(
                    uid = tagUid,
                    onPurgeCache = purgeCallback
                )
            }
        }
    }
}

@Composable
fun PrimarySkuSection(sku: com.bscan.ui.screens.home.SkuInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "SKU Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        ColorPreviewCard(
            colorHex = sku.filamentInfo.colorHex,
            colorName = sku.filamentInfo.colorName,
            filamentType = sku.filamentInfo.filamentType
        )
        
        InfoCard(
            title = "Filament Type",
            value = sku.filamentInfo.detailedFilamentType.ifEmpty { sku.filamentInfo.filamentType }
        )
        
        InfoCard(
            title = "Unique Spools",
            value = "${sku.spoolCount} spools"
        )
        
        InfoCard(
            title = "Total Scans",
            value = "${sku.totalScans} scans"
        )
        
        InfoCard(
            title = "Success Rate",
            value = "${(sku.successRate * 100).toInt()}%"
        )
        
        InfoCard(
            title = "Last Scanned",
            value = sku.lastScanned.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        )
        
        SpecificationCard(filamentInfo = sku.filamentInfo)
        TemperatureCard(filamentInfo = sku.filamentInfo)
        ProductionInfoCard(filamentInfo = sku.filamentInfo)
    }
}

@Composable
fun RelatedSpoolsSection(
    spools: List<com.bscan.repository.UniqueSpool>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Related Spools (${spools.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        spools.forEach { spool ->
            SpoolCard(
                spool = spool,
                modifier = Modifier.clickable {
                    onNavigateToDetails?.invoke(DetailType.SPOOL, spool.uid)
                }
            )
        }
    }
}

@Composable
fun RelatedTagsSection(
    tagUids: List<String>,
    allScans: List<com.bscan.repository.InterpretedScan>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Related Tags (${tagUids.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        tagUids.forEach { tagUid ->
            val mostRecentScan = allScans.filter { it.uid == tagUid }.maxByOrNull { it.timestamp }
            if (mostRecentScan != null) {
                TagCard(
                    uid = tagUid,
                    mostRecentScan = mostRecentScan,
                    filamentInfo = mostRecentScan.filamentInfo,
                    allScans = listOf(mostRecentScan),
                    modifier = Modifier.clickable {
                        onNavigateToDetails?.invoke(DetailType.TAG, tagUid)
                    }
                )
            }
        }
    }
}

@Composable
fun AssociatedSkuSection(
    skus: List<com.bscan.ui.screens.home.SkuInfo>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Associated SKU",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        skus.forEach { sku ->
            SkuCard(
                sku = sku,
                modifier = Modifier.clickable {
                    onNavigateToDetails?.invoke(DetailType.SKU, sku.skuKey)
                }
            )
        }
    }
}

@Composable
fun RelatedScansSection(
    scans: List<com.bscan.repository.InterpretedScan>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Related Scans (${scans.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        scans.forEach { scan ->
            var expanded by remember { mutableStateOf(false) }
            ScanHistoryCard(
                scan = scan,
                isExpanded = expanded,
                onToggleExpanded = { expanded = !expanded },
                onScanClick = { detailType, scanId ->
                    onNavigateToDetails?.invoke(detailType, scanId)
                }
            )
        }
    }
}