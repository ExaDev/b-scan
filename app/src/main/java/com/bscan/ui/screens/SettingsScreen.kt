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
        // PLA Basic Series
        Triple("Bambu White", "#FFFFFF", "PLA" to "PLA Basic"),
        Triple("Bambu Black", "#2C2C2C", "PLA" to "PLA Basic"),
        Triple("Bambu Red", "#DC3545", "PLA" to "PLA Basic"),
        Triple("Bambu Blue", "#2196F3", "PLA" to "PLA Basic"),
        Triple("Bambu Green", "#28A745", "PLA" to "PLA Basic"),
        Triple("Bambu Yellow", "#FFC107", "PLA" to "PLA Basic"),
        Triple("Bambu Orange", "#FF6B35", "PLA" to "PLA Basic"),
        Triple("Bambu Purple", "#6F42C1", "PLA" to "PLA Basic"),
        
        // PLA Matte Series
        Triple("Matte Ivory White", "#F8F8FF", "PLA" to "PLA Matte"),
        Triple("Matte Charcoal", "#36454F", "PLA" to "PLA Matte"),
        Triple("Matte Misty Blue", "#6F8FAF", "PLA" to "PLA Matte"),
        Triple("Matte Sakura Pink", "#FFB7C5", "PLA" to "PLA Matte"),
        
        // PLA Silk+ Series
        Triple("Silk Gold", "#FFD700", "PLA" to "PLA Silk+"),
        Triple("Silk Silver", "#C0C0C0", "PLA" to "PLA Silk+"),
        Triple("Silk Copper", "#B87333", "PLA" to "PLA Silk+"),
        Triple("Silk Pearl White", "#F8F6FF", "PLA" to "PLA Silk+"),
        
        // PLA Translucent Series
        Triple("Translucent Teal", "#008080", "PLA" to "PLA Translucent"),
        Triple("Translucent Purple", "#8B008B", "PLA" to "PLA Translucent"),
        Triple("Translucent Blue", "#4169E1", "PLA" to "PLA Translucent"),
        Triple("Translucent Green", "#32CD32", "PLA" to "PLA Translucent"),
        
        // PLA Special Series
        Triple("Galaxy Purple", "#4B0082", "PLA" to "PLA Galaxy"),
        Triple("Sparkle Silver", "#E5E5E5", "PLA" to "PLA Sparkle"),
        Triple("Marble Gray", "#808080", "PLA" to "PLA Marble"),
        Triple("Wood Brown", "#8B4513", "PLA" to "PLA Wood"),
        Triple("Glow Green", "#ADFF2F", "PLA" to "PLA Glow"),
        
        // PLA Gradient Series
        Triple("Dawn Radiance", "#FF69B4", "PLA" to "PLA Silk Multi-Color"),
        Triple("Blue Bubble", "#87CEEB", "PLA" to "PLA Basic Gradient"),
        
        // PLA Carbon Fiber
        Triple("Carbon Black", "#1C1C1C", "PLA" to "PLA-CF"),
        
        // PLA Tough+
        Triple("Tough White", "#FFFFFE", "PLA" to "PLA Tough+"),
        Triple("Tough Black", "#2B2B2B", "PLA" to "PLA Tough+"),
        
        // PETG Series
        Triple("PETG Natural", "#F5F5DC", "PETG" to "PETG Basic"),
        Triple("PETG Clear", "#E0E0E0", "PETG" to "PETG Translucent"),
        Triple("PETG White", "#FFFFFF", "PETG" to "PETG Basic"),
        Triple("PETG Black", "#2C2C2C", "PETG" to "PETG Basic"),
        Triple("PETG Red", "#DC143C", "PETG" to "PETG Basic"),
        Triple("PETG Blue", "#4682B4", "PETG" to "PETG Basic"),
        
        // PETG Carbon Fiber
        Triple("PETG Carbon Black", "#1A1A1A", "PETG" to "PETG-CF"),
        
        // ABS Series
        Triple("ABS White", "#FFFFFF", "ABS" to "ABS"),
        Triple("ABS Black", "#2C2C2C", "ABS" to "ABS"),
        Triple("ABS Red", "#B22222", "ABS" to "ABS"),
        Triple("ABS Blue", "#191970", "ABS" to "ABS"),
        
        // ASA Series
        Triple("ASA White", "#FFFFFF", "ASA" to "ASA"),
        Triple("ASA Black", "#2C2C2C", "ASA" to "ASA"),
        
        // TPU Series
        Triple("TPU Clear", "#E8E8E8", "TPU" to "TPU 95A HF"),
        Triple("TPU Black", "#2F2F2F", "TPU" to "TPU 95A"),
        Triple("TPU White", "#FFFFFF", "TPU" to "TPU 85A"),
        Triple("TPU Frozen", "#B0E0E6", "TPU" to "TPU 90A"),
        Triple("TPU Blaze", "#FF4500", "TPU" to "TPU 90A"),
        
        // Engineering Materials
        Triple("PC Natural", "#F0F8FF", "PC" to "PC"),
        Triple("PC Black", "#2F2F2F", "PC" to "PC"),
        Triple("PA Natural", "#F5F5DC", "PA" to "PA"),
        Triple("PAHT-CF Black", "#1A1A1A", "PA" to "PAHT-CF"),
        
        // Support Materials
        Triple("PVA Natural", "#FFFEF7", "PVA" to "PVA Support"),
        Triple("PLA Support White", "#FFFFFF", "PLA" to "PLA Support")
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