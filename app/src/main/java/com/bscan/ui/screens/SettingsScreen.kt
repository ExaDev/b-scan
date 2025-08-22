package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bscan.model.FilamentInfo
import com.bscan.model.ScanHistory
import com.bscan.model.ScanResult
import com.bscan.repository.ScanHistoryRepository
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    
    // UI State
    var isPopulating by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // Configuration State
    var spoolCount by remember { mutableStateOf("10") }
    var minScans by remember { mutableStateOf("1") }
    var maxScans by remember { mutableStateOf("10") }
    var generatedCount by remember { mutableStateOf(0) }
    
    // Load generated count on composition
    LaunchedEffect(Unit) {
        generatedCount = repository.getGeneratedDataCount()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Development Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                SampleDataCard(
                    spoolCount = spoolCount,
                    minScans = minScans,
                    maxScans = maxScans,
                    isPopulating = isPopulating,
                    generatedCount = generatedCount,
                    onSpoolCountChange = { spoolCount = it },
                    onMinScansChange = { minScans = it },
                    onMaxScansChange = { maxScans = it },
                    onPopulateClick = {
                        scope.launch {
                            isPopulating = true
                            try {
                                val count = spoolCount.toIntOrNull() ?: 10
                                val min = minScans.toIntOrNull() ?: 1
                                val max = maxScans.toIntOrNull() ?: 10
                                populateSampleData(repository, count, min, max)
                                generatedCount = repository.getGeneratedDataCount()
                                successMessage = "Generated $count spools with sample data!"
                                showSuccessMessage = true
                            } finally {
                                isPopulating = false
                            }
                        }
                    }
                )
            }
            
            item {
                ClearDataCard(
                    generatedCount = generatedCount,
                    isClearing = isClearing,
                    onClearClick = {
                        scope.launch {
                            isClearing = true
                            try {
                                repository.clearGeneratedData()
                                generatedCount = 0
                                successMessage = "Generated data cleared successfully!"
                                showSuccessMessage = true
                            } finally {
                                isClearing = false
                            }
                        }
                    }
                )
            }
            
            if (showSuccessMessage) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = successMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(3000)
                        showSuccessMessage = false
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleDataCard(
    spoolCount: String,
    minScans: String,
    maxScans: String,
    isPopulating: Boolean,
    generatedCount: Int,
    onSpoolCountChange: (String) -> Unit,
    onMinScansChange: (String) -> Unit,
    onMaxScansChange: (String) -> Unit,
    onPopulateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sample Data Generation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Generate sample spools with configurable scan history. Each spool can have 1 or both tags scanned.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (generatedCount > 0) {
                Text(
                    text = "Currently $generatedCount generated scan entries in database",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = spoolCount,
                    onValueChange = onSpoolCountChange,
                    label = { Text("Spools") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = minScans,
                    onValueChange = onMinScansChange,
                    label = { Text("Min Scans") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = maxScans,
                    onValueChange = onMaxScansChange,
                    label = { Text("Max Scans") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            
            Button(
                onClick = onPopulateClick,
                enabled = !isPopulating,
                modifier = Modifier.align(Alignment.Start)
            ) {
                if (isPopulating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isPopulating) "Generating..." else "Generate Sample Data")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearDataCard(
    generatedCount: Int,
    isClearing: Boolean,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Clear only generated sample data, preserving real scan history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (generatedCount > 0) {
                Text(
                    text = "$generatedCount generated entries will be removed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            OutlinedButton(
                onClick = onClearClick,
                enabled = !isClearing && generatedCount > 0,
                modifier = Modifier.align(Alignment.Start)
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isClearing) "Clearing..." else "Clear Generated Data")
            }
        }
    }
}

private suspend fun populateSampleData(
    repository: ScanHistoryRepository, 
    spoolCount: Int = 10, 
    minScans: Int = 1, 
    maxScans: Int = 10
) {
    val filamentSpecs = listOf(
        Triple("Bambu Red", "#FF4444", "PLA" to "PLA Basic"),
        Triple("Ocean Blue", "#2196F3", "PETG" to "PETG Tough"),
        Triple("Forest Green", "#4CAF50", "ABS" to "ABS Professional"),
        Triple("Sunset Orange", "#FF9800", "PLA" to "PLA Silk"),
        Triple("Royal Purple", "#9C27B0", "PETG" to "PETG Carbon Fiber"),
        Triple("Charcoal Black", "#2E2E2E", "ABS" to "ABS Standard"),
        Triple("Snow White", "#FFFFFF", "PLA" to "PLA Matte"),
        Triple("Mint Green", "#00E676", "PLA" to "PLA Basic"),
        Triple("Golden Yellow", "#FFD700", "PETG" to "PETG Basic"),
        Triple("Deep Crimson", "#DC143C", "ABS" to "ABS Fire Resistant"),
        Triple("Sky Blue", "#87CEEB", "PLA" to "PLA Translucent"),
        Triple("Emerald Green", "#50C878", "PETG" to "PETG Crystal"),
        Triple("Violet", "#8B00FF", "ABS" to "ABS Impact"),
        Triple("Silver", "#C0C0C0", "PLA" to "PLA Metallic"),
        Triple("Bronze", "#CD7F32", "PETG" to "PETG Metal Fill")
    )
    
    repeat(spoolCount) { spoolIndex ->
        val spec = filamentSpecs[spoolIndex % filamentSpecs.size]
        val trayUid = "TRAY${(spoolIndex + 1).toString().padStart(3, '0')}"
        val baseTagId = String.format("%08X", spoolIndex * 2 + 1000)
        
        // Randomly choose 1 or both tags for this spool
        val useBothTags = Random.nextBoolean()
        val tagsToGenerate = if (useBothTags) 2 else 1
        
        repeat(tagsToGenerate) { tagIndex ->
            val tagUid = String.format("%08X", baseTagId.toInt(16) + tagIndex)
            
            val filamentInfo = createSampleFilamentInfo(
                tagUid = tagUid,
                trayUid = trayUid,
                colorName = spec.first,
                colorHex = spec.second,
                filamentType = spec.third.first,
                detailedType = spec.third.second
            )
            
            // Generate scan history for this tag
            val scanCount = Random.nextInt(minScans, maxScans + 1)
            val successRate = Random.nextFloat() * 0.4f + 0.6f // 60-100% success rate
            val successCount = (scanCount * successRate).toInt().coerceAtLeast(1)
            
            repeat(scanCount) { scanIndex ->
                val isSuccess = scanIndex < successCount
                val scanTime = LocalDateTime.now().minusDays(Random.nextLong(0, 30))
                
                val scanHistory = ScanHistory(
                    uid = tagUid,
                    timestamp = scanTime,
                    technology = "MifareClassic",
                    scanResult = if (isSuccess) ScanResult.SUCCESS else ScanResult.AUTHENTICATION_FAILED,
                    filamentInfo = if (isSuccess) filamentInfo else null,
                    debugInfo = createSampleDebugInfo(tagUid, isSuccess)
                )
                
                repository.saveScan(scanHistory)
            }
        }
    }
}

private fun createSampleFilamentInfo(
    tagUid: String,
    trayUid: String,
    colorName: String,
    colorHex: String,
    filamentType: String,
    detailedType: String
): FilamentInfo {
    return FilamentInfo(
        tagUid = tagUid,
        trayUid = trayUid,
        filamentType = filamentType,
        detailedFilamentType = detailedType,
        colorHex = colorHex,
        colorName = colorName,
        spoolWeight = Random.nextInt(200, 1000),
        filamentDiameter = if (Random.nextBoolean()) 1.75f else 2.85f,
        filamentLength = Random.nextInt(100000, 500000),
        productionDate = "2024-${Random.nextInt(1, 13).toString().padStart(2, '0')}-${Random.nextInt(1, 29).toString().padStart(2, '0')}",
        minTemperature = Random.nextInt(180, 220),
        maxTemperature = Random.nextInt(220, 280),
        bedTemperature = Random.nextInt(50, 80),
        dryingTemperature = Random.nextInt(40, 70),
        dryingTime = Random.nextInt(4, 24)
    )
}

private fun createSampleDebugInfo(tagUid: String, isSuccess: Boolean): com.bscan.model.ScanDebugInfo {
    return com.bscan.model.ScanDebugInfo(
        uid = tagUid,
        tagSizeBytes = 1024,
        sectorCount = 16,
        authenticatedSectors = if (isSuccess) listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15) else listOf(0),
        failedSectors = if (isSuccess) emptyList() else listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
        usedKeyTypes = if (isSuccess) {
            mapOf(
                0 to "KeyA", 1 to "KeyB", 2 to "KeyA", 3 to "KeyB", 4 to "KeyA",
                5 to "KeyB", 6 to "KeyA", 7 to "KeyB", 8 to "KeyA", 9 to "KeyB",
                10 to "KeyA", 11 to "KeyB", 12 to "KeyA", 13 to "KeyB", 14 to "KeyA", 15 to "KeyB"
            )
        } else {
            mapOf(0 to "KeyA")
        },
        blockData = if (isSuccess) {
            mapOf(
                0 to tagUid,
                1 to "00112233445566778899AABBCCDDEEFF",
                2 to "FF00FF00FF00FF00FF00FF00FF00FF00"
            )
        } else {
            mapOf(0 to tagUid)
        },
        derivedKeys = listOf("A1B2C3D4E5F6", "123456789ABC"),
        rawColorBytes = "FF4444",
        errorMessages = if (isSuccess) emptyList() else listOf("Authentication failed for sectors 1-15"),
        parsingDetails = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "success" to isSuccess,
            "sampleData" to true
        )
    )
}