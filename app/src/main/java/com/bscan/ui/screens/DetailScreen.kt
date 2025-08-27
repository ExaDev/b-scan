package com.bscan.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.InventoryRepository
import com.bscan.repository.PhysicalComponentRepository
import com.bscan.repository.UserPreferencesRepository
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import com.bscan.repository.UnifiedDataAccess
import com.bscan.logic.SkuWeightService
import com.bscan.ui.components.*
import com.bscan.ui.components.filament.*
import com.bscan.ui.components.history.*
import com.bscan.ui.components.spool.*
import com.bscan.ui.components.inventory.InventoryComponentsCard
import com.bscan.ui.components.inventory.MassEditDialog
import com.bscan.ui.components.inventory.AddComponentDialog
import com.bscan.ui.components.inventory.EditMassResult
import com.bscan.ui.screens.home.SkuCard
import com.bscan.ui.screens.home.FilamentReelCard
import com.bscan.ui.screens.home.TagCard
import com.bscan.model.InventoryItem
import com.bscan.model.PhysicalComponent
import com.bscan.model.sectorCount
import com.bscan.model.tagSizeBytes
import com.bscan.model.tagFormat
import com.bscan.model.manufacturerName
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
                                PrimaryFilamentReelContent(filamentReel = spool, onPurgeCache = onPurgeCache)
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
        
        // Basic Tag Information
        TagBasicInfoCard(tag = tag)
        
        // Scan Status and Technical Details
        TagTechnicalInfoCard(tag = tag)
        
        // Authentication Details
        TagAuthenticationCard(tag = tag)
        
        // Raw Data Section (expandable)
        TagRawDataCard(tag = tag)
        
        // Filament Information (if available)
        tag.filamentInfo?.let { filamentInfo ->
            InterpretedFilamentDataCard(filamentInfo = filamentInfo)
        }
    }
}

@Composable
fun TagBasicInfoCard(tag: com.bscan.repository.InterpretedScan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            InfoRow(label = "Tag UID", value = tag.uid)
            InfoRow(label = "Technology", value = tag.technology)
            InfoRow(label = "Manufacturer", value = tag.encryptedData.manufacturerName)
            InfoRow(label = "Last Scanned", value = tag.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
            InfoRow(label = "Scan Result", value = tag.scanResult.name.replace('_', ' '))
        }
    }
}

@Composable
fun TagTechnicalInfoCard(tag: com.bscan.repository.InterpretedScan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Technical Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            InfoRow(label = "Tag Format", value = tag.encryptedData.tagFormat.name)
            InfoRow(label = "Tag Size", value = "${tag.decryptedData.tagSizeBytes} bytes")
            InfoRow(label = "Sector Count", value = "${tag.decryptedData.sectorCount} sectors")
            InfoRow(label = "Scan Duration", value = "${tag.encryptedData.scanDurationMs}ms")
            InfoRow(label = "Key Derivation Time", value = "${tag.decryptedData.keyDerivationTimeMs}ms")
            InfoRow(label = "Authentication Time", value = "${tag.decryptedData.authenticationTimeMs}ms")
        }
    }
}

@Composable
fun TagAuthenticationCard(tag: com.bscan.repository.InterpretedScan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Authentication Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            InfoRow(
                label = "Authenticated Sectors", 
                value = "${tag.decryptedData.authenticatedSectors.size}/${tag.decryptedData.sectorCount}"
            )
            
            if (tag.decryptedData.authenticatedSectors.isNotEmpty()) {
                InfoRow(
                    label = "Success Sectors", 
                    value = tag.decryptedData.authenticatedSectors.sorted().joinToString(", ")
                )
            }
            
            if (tag.decryptedData.failedSectors.isNotEmpty()) {
                InfoRow(
                    label = "Failed Sectors", 
                    value = tag.decryptedData.failedSectors.sorted().joinToString(", ")
                )
            }
            
            InfoRow(
                label = "Derived Keys", 
                value = "${tag.decryptedData.derivedKeys.size} keys generated"
            )
            
            if (tag.decryptedData.errors.isNotEmpty()) {
                Text(
                    text = "Errors:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                tag.decryptedData.errors.take(3).forEach { error ->
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (tag.decryptedData.errors.size > 3) {
                    Text(
                        text = "... and ${tag.decryptedData.errors.size - 3} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun TagRawDataCard(tag: com.bscan.repository.InterpretedScan) {
    var expanded by remember { mutableStateOf(false) }
    var showAllBlocks by remember { mutableStateOf(false) }
    var showAllKeys by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Raw Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (expanded) "Hide" else "Show",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (expanded) {
                InfoRow(
                    label = "Encrypted Size", 
                    value = "${tag.encryptedData.encryptedData.size} bytes"
                )
                
                InfoRow(
                    label = "Decrypted Blocks", 
                    value = "${tag.decryptedData.decryptedBlocks.size} blocks"
                )
                
                // Show decrypted blocks
                if (tag.decryptedData.decryptedBlocks.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Decrypted Blocks:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        IconButton(
                            onClick = {
                                val allBlocksText = tag.decryptedData.decryptedBlocks.entries
                                    .sortedBy { it.key }
                                    .joinToString("\n") { (blockNum, data) ->
                                        "Block $blockNum: $data"
                                    }
                                clipboardManager.setText(AnnotatedString(allBlocksText))
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy all blocks",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    val blocksToShow = if (showAllBlocks) {
                        tag.decryptedData.decryptedBlocks.entries.sortedBy { it.key }
                    } else {
                        tag.decryptedData.decryptedBlocks.entries.sortedBy { it.key }.take(3)
                    }
                    
                    blocksToShow.forEach { (blockNum, data) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Block $blockNum: ${data.take(32)}${if (data.length > 32) "..." else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("Block $blockNum: $data"))
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy block $blockNum",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    if (tag.decryptedData.decryptedBlocks.size > 3) {
                        Text(
                            text = if (showAllBlocks) {
                                "Hide additional blocks"
                            } else {
                                "... and ${tag.decryptedData.decryptedBlocks.size - 3} more blocks"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                showAllBlocks = !showAllBlocks
                            }
                        )
                    }
                }
                
                // Show derived keys (truncated for security)
                if (tag.decryptedData.derivedKeys.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Derived Keys:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        IconButton(
                            onClick = {
                                val allKeysText = tag.decryptedData.derivedKeys.joinToString("\n") { key ->
                                    key
                                }
                                clipboardManager.setText(AnnotatedString(allKeysText))
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy all keys",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    val keysToShow = if (showAllKeys) {
                        tag.decryptedData.derivedKeys
                    } else {
                        tag.decryptedData.derivedKeys.take(2)
                    }
                    
                    keysToShow.forEach { key ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${key.take(8)}...${key.takeLast(8)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(key))
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy key",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    if (tag.decryptedData.derivedKeys.size > 2) {
                        Text(
                            text = if (showAllKeys) {
                                "Hide additional keys"
                            } else {
                                "... and ${tag.decryptedData.derivedKeys.size - 2} more keys"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                showAllKeys = !showAllKeys
                            }
                        )
                    }
                }
                
                // Copy all raw data button
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        val allRawData = buildString {
                            appendLine("=== TAG RAW DATA ===")
                            appendLine("Tag UID: ${tag.uid}")
                            appendLine("Technology: ${tag.technology}")
                            appendLine("Manufacturer: ${tag.encryptedData.manufacturerName}")
                            appendLine("Scan Result: ${tag.scanResult}")
                            appendLine("Tag Size: ${tag.decryptedData.tagSizeBytes} bytes")
                            appendLine("Encrypted Data Size: ${tag.encryptedData.encryptedData.size} bytes")
                            appendLine()
                            
                            appendLine("=== DECRYPTED BLOCKS ===")
                            tag.decryptedData.decryptedBlocks.entries.sortedBy { it.key }.forEach { (blockNum, data) ->
                                appendLine("Block $blockNum: $data")
                            }
                            appendLine()
                            
                            appendLine("=== AUTHENTICATION ===")
                            appendLine("Authenticated Sectors: ${tag.decryptedData.authenticatedSectors.sorted().joinToString(", ")}")
                            appendLine("Failed Sectors: ${tag.decryptedData.failedSectors.sorted().joinToString(", ")}")
                            appendLine()
                            
                            appendLine("=== DERIVED KEYS ===")
                            tag.decryptedData.derivedKeys.forEach { key ->
                                appendLine(key)
                            }
                            
                            if (tag.decryptedData.errors.isNotEmpty()) {
                                appendLine()
                                appendLine("=== ERRORS ===")
                                tag.decryptedData.errors.forEach { error ->
                                    appendLine("• $error")
                                }
                            }
                        }
                        clipboardManager.setText(AnnotatedString(allRawData))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy All Raw Data")
                }
            }
        }
    }
}

@Composable
fun InterpretedFilamentDataCard(filamentInfo: com.bscan.model.FilamentInfo) {
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interpreted Filament Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = {
                        val filamentDataText = buildString {
                            appendLine("=== INTERPRETED FILAMENT DATA ===")
                            appendLine("Tag UID: ${filamentInfo.tagUid}")
                            appendLine("Tray UID: ${filamentInfo.trayUid}")
                            appendLine("Filament Type: ${filamentInfo.filamentType}")
                            appendLine("Detailed Type: ${filamentInfo.detailedFilamentType}")
                            appendLine("Color Name: ${filamentInfo.colorName}")
                            appendLine("Color Hex: ${filamentInfo.colorHex}")
                            appendLine("Spool Weight: ${filamentInfo.spoolWeight}g")
                            appendLine("Filament Diameter: ${filamentInfo.filamentDiameter}mm")
                            appendLine("Filament Length: ${filamentInfo.filamentLength}m")
                            appendLine("Production Date: ${filamentInfo.productionDate}")
                            appendLine("Min Temperature: ${filamentInfo.minTemperature}°C")
                            appendLine("Max Temperature: ${filamentInfo.maxTemperature}°C")
                            appendLine("Bed Temperature: ${filamentInfo.bedTemperature}°C")
                            appendLine("Drying Temperature: ${filamentInfo.dryingTemperature}°C")
                            appendLine("Drying Time: ${filamentInfo.dryingTime}h")
                        }
                        clipboardManager.setText(AnnotatedString(filamentDataText))
                    }
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy all filament data",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
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
            
            // Copy structured JSON button
            OutlinedButton(
                onClick = {
                    val jsonData = buildString {
                        appendLine("{")
                        appendLine("  \"tagUid\": \"${filamentInfo.tagUid}\",")
                        appendLine("  \"trayUid\": \"${filamentInfo.trayUid}\",")
                        appendLine("  \"filamentType\": \"${filamentInfo.filamentType}\",")
                        appendLine("  \"detailedFilamentType\": \"${filamentInfo.detailedFilamentType}\",")
                        appendLine("  \"colorName\": \"${filamentInfo.colorName}\",")
                        appendLine("  \"colorHex\": \"${filamentInfo.colorHex}\",")
                        appendLine("  \"spoolWeight\": ${filamentInfo.spoolWeight},")
                        appendLine("  \"filamentDiameter\": ${filamentInfo.filamentDiameter},")
                        appendLine("  \"filamentLength\": ${filamentInfo.filamentLength},")
                        appendLine("  \"productionDate\": \"${filamentInfo.productionDate}\",")
                        appendLine("  \"minTemperature\": ${filamentInfo.minTemperature},")
                        appendLine("  \"maxTemperature\": ${filamentInfo.maxTemperature},")
                        appendLine("  \"bedTemperature\": ${filamentInfo.bedTemperature},")
                        appendLine("  \"dryingTemperature\": ${filamentInfo.dryingTemperature},")
                        appendLine("  \"dryingTime\": ${filamentInfo.dryingTime}")
                        appendLine("}")
                    }
                    clipboardManager.setText(AnnotatedString(jsonData))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy as JSON")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
fun PrimaryFilamentReelContent(
    filamentReel: com.bscan.repository.FilamentReelDetails,
    onPurgeCache: ((String) -> Unit)?
) {
    // Simplified version to test layout constraints
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Filament Reel: ${filamentReel.trayUid}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Material: ${filamentReel.filamentInfo.filamentType}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Color: ${filamentReel.filamentInfo.colorName}",
                style = MaterialTheme.typography.bodyMedium
            )
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
            title = "Unique Reels",
            value = "${sku.filamentReelCount} reels"
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
fun RelatedFilamentReelsSection(
    filamentReels: List<com.bscan.repository.UniqueFilamentReel>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Related Inventory Stock (${filamentReels.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        filamentReels.forEach { filamentReel ->
            FilamentReelCard(
                filamentReel = filamentReel,
                onClick = { trayUid ->
                    onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, trayUid)
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