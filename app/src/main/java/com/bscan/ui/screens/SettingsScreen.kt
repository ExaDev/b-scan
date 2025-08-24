package com.bscan.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import android.util.Log
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.DataExportManager
import com.bscan.repository.ExportScope
import com.bscan.repository.ImportMode
import com.bscan.ui.screens.settings.SampleDataGenerator
import com.bscan.ui.screens.settings.ExportImportCard
import com.bscan.ui.screens.settings.ExportPreviewData
import com.bscan.ui.screens.settings.DataGenerationMode
import com.bscan.data.BambuProductDatabase
import com.bscan.repository.UserPreferencesRepository
import com.bscan.ui.components.MaterialDisplayMode
import com.bscan.ui.components.FilamentColorBox
import com.bscan.ble.BlePermissionHandler
import com.bscan.ble.BleScalesManager
import com.bscan.ble.ScaleConnectionState
import com.bscan.ble.ScaleCommandResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    blePermissionHandler: BlePermissionHandler,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val exportManager = remember { DataExportManager(context) }
    val userPrefsRepository = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    
    // UI State
    var isPopulating by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // UI Preferences State
    var materialDisplayMode by remember { mutableStateOf(userPrefsRepository.getMaterialDisplayMode()) }
    
    // Configuration State
    var generationMode by remember { mutableStateOf(DataGenerationMode.COMPLETE_COVERAGE) }
    var spoolCount by remember { mutableStateOf("10") }
    var additionalSpools by remember { mutableStateOf("50") }
    var minScans by remember { mutableStateOf("1") }
    var maxScans by remember { mutableStateOf("5") }
    var generatedCount by remember { mutableStateOf(0) }
    
    // Export/Import State
    var exportScope by remember { mutableStateOf(ExportScope.ALL_DATA) }
    var importMode by remember { mutableStateOf(ImportMode.MERGE_WITH_EXISTING) }
    var fromDate by remember { mutableStateOf<LocalDate?>(null) }
    var toDate by remember { mutableStateOf<LocalDate?>(null) }
    var showImportPreview by remember { mutableStateOf(false) }
    var importPreviewData by remember { mutableStateOf<ExportPreviewData?>(null) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    // Load scan statistics
    var totalScans by remember { mutableStateOf(0) }
    var totalSpools by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        generatedCount = repository.getHistoryCount()
        totalScans = repository.getAllEncryptedScans().size + repository.getAllDecryptedScans().size
        // Calculate unique spools by tray UID from successful decrypted scans
        val uniqueTrayUids = repository.getSuccessfulDecryptedScans()
            .mapNotNull { scan -> 
                scan.decryptedBlocks[9]?.substring(0, 32) // First 16 bytes as hex
            }
            .distinct()
        totalSpools = uniqueTrayUids.size
    }
    
    // Export file picker
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportUri ->
            scope.launch {
                isExporting = true
                try {
                    val fromDateTime = fromDate?.atStartOfDay()
                    val toDateTime = toDate?.atTime(23, 59, 59)
                    
                    val result = exportManager.exportData(
                        repository = repository,
                        uri = exportUri,
                        scope = exportScope,
                        fromDate = fromDateTime,
                        toDate = toDateTime,
                        selectedTrayUids = emptyList() // Not implemented yet
                    )
                    
                    result.fold(
                        onSuccess = { message ->
                            successMessage = message
                            showSuccessMessage = true
                        },
                        onFailure = { error ->
                            successMessage = "Export failed: ${error.message}"
                            showSuccessMessage = true
                        }
                    )
                } finally {
                    isExporting = false
                }
            }
        }
    }
    
    // Import file picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importUri ->
            scope.launch {
                // Validate and preview the file first
                val result = exportManager.validateImportFile(importUri)
                result.fold(
                    onSuccess = { exportData ->
                        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
                        importPreviewData = ExportPreviewData(
                            filename = exportData.deviceInfo.exportedBy + "_" + exportData.exportDate.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
                            totalScans = exportData.encryptedScans.size + exportData.decryptedScans.size,
                            encryptedScans = exportData.encryptedScans.size,
                            decryptedScans = exportData.decryptedScans.size,
                            uniqueSpools = exportData.statistics.uniqueSpools,
                            dateRange = "${exportData.statistics.dateRange.from?.format(dateFormatter) ?: "Unknown"} to ${exportData.statistics.dateRange.to?.format(dateFormatter) ?: "Unknown"}",
                            fileVersion = exportData.version,
                            exportDate = exportData.exportDate.format(dateFormatter)
                        )
                        pendingImportUri = importUri
                        showImportPreview = true
                    },
                    onFailure = { error ->
                        successMessage = "File validation failed: ${error.message}"
                        showSuccessMessage = true
                    }
                )
            }
        }
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
                    text = "Display Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                MaterialDisplayPreferenceCard(
                    currentMode = materialDisplayMode,
                    onModeChange = { mode ->
                        materialDisplayMode = mode
                        userPrefsRepository.setMaterialDisplayMode(mode)
                    }
                )
            }
            
            item {
                Text(
                    text = "BLE Scales",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                BleScalesPreferenceCard(
                    userPrefsRepository = userPrefsRepository,
                    blePermissionHandler = blePermissionHandler
                )
            }
            
            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                ExportImportCard(
                    scanCount = totalScans,
                    spoolCount = totalSpools,
                    isExporting = isExporting,
                    isImporting = isImporting,
                    exportScope = exportScope,
                    importMode = importMode,
                    fromDate = fromDate,
                    toDate = toDate,
                    previewData = importPreviewData,
                    showPreview = showImportPreview,
                    onExportScopeChange = { exportScope = it },
                    onImportModeChange = { importMode = it },
                    onFromDateChange = { fromDate = it },
                    onToDateChange = { toDate = it },
                    onExportClick = {
                        if (exportScope != ExportScope.SELECTED_SPOOLS) {
                            val filename = exportManager.generateExportFilename()
                            exportLauncher.launch(filename)
                        } else {
                            successMessage = "Spool selection not yet implemented. Please use 'All data' or 'Date range'."
                            showSuccessMessage = true
                        }
                    },
                    onImportClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    onConfirmImport = {
                        pendingImportUri?.let { uri ->
                            scope.launch {
                                isImporting = true
                                try {
                                    val result = exportManager.importData(repository, uri, importMode)
                                    result.fold(
                                        onSuccess = { importResult ->
                                            if (importResult.success) {
                                                successMessage = importResult.message
                                                // Refresh scan counts
                                                totalScans = repository.getAllEncryptedScans().size + repository.getAllDecryptedScans().size
                                                val uniqueTrayUids = repository.getSuccessfulDecryptedScans()
                                                    .mapNotNull { scan -> 
                                                        scan.decryptedBlocks[9]?.substring(0, 32)
                                                    }
                                                    .distinct()
                                                totalSpools = uniqueTrayUids.size
                                            } else {
                                                successMessage = importResult.message
                                            }
                                            showSuccessMessage = true
                                        },
                                        onFailure = { error ->
                                            successMessage = "Import failed: ${error.message}"
                                            showSuccessMessage = true
                                        }
                                    )
                                } finally {
                                    isImporting = false
                                    showImportPreview = false
                                    importPreviewData = null
                                    pendingImportUri = null
                                }
                            }
                        }
                    },
                    onCancelImport = {
                        showImportPreview = false
                        importPreviewData = null
                        pendingImportUri = null
                    }
                )
            }
            
            item {
                Text(
                    text = "Development Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                SampleDataCard(
                    generationMode = generationMode,
                    spoolCount = spoolCount,
                    additionalSpools = additionalSpools,
                    minScans = minScans,
                    maxScans = maxScans,
                    isPopulating = isPopulating,
                    generatedCount = generatedCount,
                    onGenerationModeChange = { generationMode = it },
                    onSpoolCountChange = { spoolCount = it },
                    onAdditionalSpoolsChange = { additionalSpools = it },
                    onMinScansChange = { minScans = it },
                    onMaxScansChange = { maxScans = it },
                    onPopulateClick = {
                        scope.launch {
                            isPopulating = true
                            try {
                                val generator = SampleDataGenerator()
                                val stats = when (generationMode) {
                                    DataGenerationMode.COMPLETE_COVERAGE -> {
                                        val additional = additionalSpools.toIntOrNull() ?: 50
                                        val min = minScans.toIntOrNull() ?: 1
                                        val max = maxScans.toIntOrNull() ?: 5
                                        generator.generateWithCompleteSkuCoverage(repository, additional, min, max)
                                    }
                                    DataGenerationMode.RANDOM_SAMPLE -> {
                                        val count = spoolCount.toIntOrNull() ?: 10
                                        val min = minScans.toIntOrNull() ?: 1
                                        val max = maxScans.toIntOrNull() ?: 10
                                        generator.generateRandomSample(repository, count, min, max)
                                    }
                                    DataGenerationMode.MINIMAL_COVERAGE -> {
                                        generator.generateMinimalCoverage(repository)
                                    }
                                }
                                
                                generatedCount = repository.getHistoryCount()
                                
                                successMessage = when (generationMode) {
                                    DataGenerationMode.COMPLETE_COVERAGE -> 
                                        "Generated ${stats.totalScans} scans covering all ${stats.skusCovered} SKUs across ${stats.totalSpools} spools! (${(stats.successRate * 100).toInt()}% success rate)"
                                    DataGenerationMode.RANDOM_SAMPLE -> 
                                        "Generated ${stats.totalScans} scans across ${stats.totalSpools} random spools covering ${stats.skusCovered} SKUs! (${(stats.successRate * 100).toInt()}% success rate)"
                                    DataGenerationMode.MINIMAL_COVERAGE -> 
                                        "Generated minimal dataset: ${stats.totalScans} scans covering all ${stats.skusCovered} SKUs! (${(stats.successRate * 100).toInt()}% success rate)"
                                }
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
                                repository.clearHistory()
                                generatedCount = 0
                                // Refresh scan counts
                                totalScans = 0
                                totalSpools = 0
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
    generationMode: DataGenerationMode,
    spoolCount: String,
    additionalSpools: String,
    minScans: String,
    maxScans: String,
    isPopulating: Boolean,
    generatedCount: Int,
    onGenerationModeChange: (DataGenerationMode) -> Unit,
    onSpoolCountChange: (String) -> Unit,
    onAdditionalSpoolsChange: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sample Data Generation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Mode Selection
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Generation Mode",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                DataGenerationMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = generationMode == mode,
                            onClick = { onGenerationModeChange(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Coverage Information
            when (generationMode) {
                DataGenerationMode.COMPLETE_COVERAGE -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "✓ All ${BambuProductDatabase.getProductCount()} SKUs will have at least one tag scanned",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "✓ Each spool has 2 tags, we might have scanned both (~30% chance)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "✓ Additional random spools provide realistic variety",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                DataGenerationMode.RANDOM_SAMPLE -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "⚠ May not cover all SKUs. Consider Complete Coverage for comprehensive testing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                DataGenerationMode.MINIMAL_COVERAGE -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = "Minimal dataset: One tag scan per SKU (spools have 2 tags each)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            
            // Configuration Fields
            when (generationMode) {
                DataGenerationMode.COMPLETE_COVERAGE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = additionalSpools,
                            onValueChange = onAdditionalSpoolsChange,
                            label = { Text("Additional Random Spools") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minScans,
                            onValueChange = onMinScansChange,
                            label = { Text("Min Scans per Tag") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxScans,
                            onValueChange = onMaxScansChange,
                            label = { Text("Max Scans per Tag") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
                DataGenerationMode.RANDOM_SAMPLE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = spoolCount,
                            onValueChange = onSpoolCountChange,
                            label = { Text("Number of Random Spools") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minScans,
                            onValueChange = onMinScansChange,
                            label = { Text("Min Scans per Tag") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxScans,
                            onValueChange = onMaxScansChange,
                            label = { Text("Max Scans per Tag") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
                DataGenerationMode.MINIMAL_COVERAGE -> {
                    // No configuration needed for minimal coverage
                }
            }
            
            if (generatedCount > 0) {
                Text(
                    text = "Currently $generatedCount generated scan entries in database",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
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

@Composable
private fun MaterialDisplayPreferenceCard(
    currentMode: MaterialDisplayMode,
    onModeChange: (MaterialDisplayMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Material Display Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Choose how material types are displayed in filament color boxes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Preview samples
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Shapes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilamentColorBox(
                            colorHex = "#FF4444",
                            filamentType = "PLA Basic",
                            size = 32.dp,
                            displayMode = MaterialDisplayMode.SHAPES
                        )
                        FilamentColorBox(
                            colorHex = "#44FF44",
                            filamentType = "PETG Basic",
                            size = 32.dp,
                            displayMode = MaterialDisplayMode.SHAPES
                        )
                        FilamentColorBox(
                            colorHex = "#4444FF",
                            filamentType = "ABS",
                            size = 32.dp,
                            displayMode = MaterialDisplayMode.SHAPES
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Text Labels",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilamentColorBox(
                            colorHex = "#FF4444",
                            filamentType = "PLA Basic",
                            size = 32.dp,
                            displayMode = MaterialDisplayMode.TEXT_LABELS
                        )
                        FilamentColorBox(
                            colorHex = "#44FF44",
                            filamentType = "PETG Basic",
                            size = 32.dp,
                            displayMode = MaterialDisplayMode.TEXT_LABELS
                        )
                        FilamentColorBox(
                            colorHex = "#4444FF",
                            filamentType = "ABS",
                            size = 32.dp,
                            displayMode = MaterialDisplayMode.TEXT_LABELS
                        )
                    }
                }
            }
            
            // Mode Selection
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaterialDisplayMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeChange(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BleScalesPreferenceCard(
    userPrefsRepository: UserPreferencesRepository,
    blePermissionHandler: BlePermissionHandler
) {
    val context = LocalContext.current
    
    var isScalesEnabled by remember { mutableStateOf(userPrefsRepository.isBleScalesEnabled()) }
    var preferredScaleName by remember { mutableStateOf(userPrefsRepository.getPreferredScaleName()) }
    var autoConnectEnabled by remember { mutableStateOf(userPrefsRepository.isBleScalesAutoConnectEnabled()) }
    
    // BLE components
    val bleScalesManager = remember { BleScalesManager(context) }
    
    // BLE state
    val isScanning by bleScalesManager.isScanning.collectAsStateWithLifecycle()
    val discoveredDevices by bleScalesManager.discoveredDevices.collectAsStateWithLifecycle()
    val permissionState by blePermissionHandler.permissionState.collectAsStateWithLifecycle()
    val currentReading by bleScalesManager.currentReading.collectAsStateWithLifecycle()
    val connectionState by bleScalesManager.connectionState.collectAsStateWithLifecycle()
    val isReading by bleScalesManager.isReading.collectAsStateWithLifecycle()
    
    var showDeviceSelection by remember { mutableStateOf(false) }
    var isTaring by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Auto-connect to stored scale on first load
    LaunchedEffect(bleScalesManager, preferredScaleName) {
        // Always attempt to reconnect if we have stored scale info and aren't connected
        val storedAddress = userPrefsRepository.getPreferredScaleAddress()
        val storedName = userPrefsRepository.getPreferredScaleName()
        
        if (storedAddress != null && storedName != null && !bleScalesManager.isConnectedToScale()) {
            Log.i("BleScalesSettings", "Attempting auto-reconnect to stored scale: $storedName ($storedAddress)")
            
            val result = bleScalesManager.reconnectToStoredScale(
                storedAddress, 
                storedName, 
                blePermissionHandler
            )
            
            when (result) {
                is ScaleCommandResult.Success -> {
                    Log.i("BleScalesSettings", "Auto-reconnect successful")
                }
                is ScaleCommandResult.Error -> {
                    Log.w("BleScalesSettings", "Auto-reconnect failed: ${result.message}")
                }
                else -> {
                    Log.w("BleScalesSettings", "Auto-reconnect failed: $result")
                }
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Weight Tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Connect a Bluetooth scale to automatically track filament weight after NFC scans",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Current scale status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val isActuallyConnected = bleScalesManager.isConnectedToScale()
                    val hasStoredScale = preferredScaleName != null
                    
                    Text(
                        text = when {
                            isActuallyConnected -> "Connected Scale"
                            hasStoredScale -> "Configured Scale (Not Connected)"
                            else -> "No Scale Connected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (hasStoredScale) {
                        Text(
                            text = preferredScaleName!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActuallyConnected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        if (!isActuallyConnected) {
                            Text(
                                text = "Tap Connect to establish BLE connection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Text(
                            text = "Tap to connect a BLE scale",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                val isActuallyConnected = bleScalesManager.isConnectedToScale()
                val hasStoredScale = preferredScaleName != null
                
                if (hasStoredScale) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isActuallyConnected) {
                            // Connect button (reconnect to stored scale)
                            Button(
                                onClick = {
                                    scope.launch {
                                        val storedAddress = userPrefsRepository.getPreferredScaleAddress()
                                        if (storedAddress != null) {
                                            bleScalesManager.reconnectToStoredScale(
                                                storedAddress, 
                                                preferredScaleName!!, 
                                                blePermissionHandler
                                            )
                                        }
                                    }
                                },
                                enabled = connectionState != ScaleConnectionState.CONNECTING
                            ) {
                                Text(if (connectionState == ScaleConnectionState.CONNECTING) "Connecting..." else "Connect")
                            }
                        }
                        
                        // Disconnect/Remove button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    // Actual BLE disconnection if connected
                                    if (isActuallyConnected) {
                                        bleScalesManager.disconnectFromScale()
                                    }
                                    
                                    // Clear preferences
                                    userPrefsRepository.clearBleScalesConfiguration()
                                    isScalesEnabled = false
                                    preferredScaleName = null
                                }
                            }
                        ) {
                            Text(if (isActuallyConnected) "Disconnect" else "Remove")
                        }
                    }
                } else {
                    // Connect button
                    Button(
                        onClick = {
                            if (blePermissionHandler.hasAllPermissions()) {
                                showDeviceSelection = true
                                bleScalesManager.startScanning(blePermissionHandler)
                            } else {
                                blePermissionHandler.requestPermissions()
                            }
                        },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            Text("Scanning...")
                        } else {
                            Text("Connect Scale")
                        }
                    }
                }
            }
            
            // Auto-connect setting (only shown when scale is configured)
            if (preferredScaleName != null) {
                HorizontalDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-connect",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Automatically connect to scale when app starts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoConnectEnabled,
                        onCheckedChange = { enabled ->
                            autoConnectEnabled = enabled
                            userPrefsRepository.setBleScalesAutoConnectEnabled(enabled)
                        }
                    )
                }
                
                // Debug information panel (when connected)
                if (bleScalesManager.isConnectedToScale()) {
                    HorizontalDivider()
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Scale Debug Information",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Connection status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Connection Status:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            val statusColor = when (connectionState) {
                                ScaleConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                ScaleConnectionState.READING -> MaterialTheme.colorScheme.tertiary
                                ScaleConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary
                                ScaleConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            
                            Text(
                                text = connectionState.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = statusColor
                            )
                        }
                        
                        // Current weight reading
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Weight:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            currentReading?.let { reading ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = reading.getDisplayWeight(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = reading.getStabilityIcon(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } ?: Text(
                                text = "No reading",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Reading status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reading Status:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isReading) "Active" else "Inactive",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isReading) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Raw data display
                        currentReading?.let { reading ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Raw Data:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = reading.getRawDataHex(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Method: ${reading.parsingMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Age: ${reading.getAgeString()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Control buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start/Stop reading button
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (isReading) {
                                            bleScalesManager.stopWeightReading()
                                        } else {
                                            bleScalesManager.startWeightReading()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = connectionState == ScaleConnectionState.CONNECTED || connectionState == ScaleConnectionState.READING
                            ) {
                                Text(if (isReading) "Stop Reading" else "Start Reading")
                            }
                            
                            // Tare button
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isTaring = true
                                        val result = bleScalesManager.tareScale()
                                        isTaring = false
                                        
                                        // Could add toast/snackbar feedback here
                                    }
                                },
                                enabled = !isTaring && connectionState in listOf(ScaleConnectionState.CONNECTED, ScaleConnectionState.READING)
                            ) {
                                if (isTaring) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Tare")
                                }
                            }
                        }
                    }
                }
            }
            
            // Device discovery and selection
            if (isScanning || discoveredDevices.isNotEmpty()) {
                HorizontalDivider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Scanning indicator
                    if (isScanning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Scanning for BLE scales...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Discovered devices
                    if (discoveredDevices.isNotEmpty()) {
                        Text(
                            text = "Found ${discoveredDevices.size} device${if (discoveredDevices.size == 1) "" else "s"}:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        discoveredDevices.forEach { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (device.isKnownScale) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${device.address} • ${device.rssi} dBm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (device.isKnownScale) {
                                            Text(
                                                text = "✓ Compatible scale",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                // Stop scanning first
                                                bleScalesManager.stopScanning()
                                                
                                                // Attempt actual BLE connection
                                                val result = bleScalesManager.connectToScale(device)
                                                
                                                if (result is ScaleCommandResult.Success) {
                                                    // Save preferences only on successful connection
                                                    userPrefsRepository.setPreferredScaleAddress(device.address)
                                                    userPrefsRepository.setPreferredScaleName(device.displayName)
                                                    userPrefsRepository.setBleScalesEnabled(true)
                                                    
                                                    // Update local state
                                                    isScalesEnabled = true
                                                    preferredScaleName = device.displayName
                                                    showDeviceSelection = false
                                                } else {
                                                    // Handle connection failure - could show toast/snackbar here
                                                    // For now, just continue showing device selection
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Connect")
                                    }
                                }
                            }
                        }
                        
                        // Cancel scanning button
                        OutlinedButton(
                            onClick = {
                                bleScalesManager.stopScanning()
                                showDeviceSelection = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                    
                    // Permission state feedback
                    when (permissionState) {
                        com.bscan.ble.BlePermissionState.DENIED -> {
                            Text(
                                text = "⚠ BLE permissions required to scan for scales",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        com.bscan.ble.BlePermissionState.REQUESTING -> {
                            Text(
                                text = "Requesting BLE permissions...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> { /* No feedback needed */ }
                    }
                }
            }
        }
    }
}

