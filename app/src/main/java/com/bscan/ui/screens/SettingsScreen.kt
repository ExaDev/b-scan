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
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.DataExportManager
import com.bscan.repository.ExportScope
import com.bscan.repository.ImportMode
import com.bscan.ui.screens.settings.SampleDataGenerator
import com.bscan.ui.screens.settings.ExportImportCard
import com.bscan.ui.screens.settings.ExportPreviewData
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val exportManager = remember { DataExportManager(context) }
    val scope = rememberCoroutineScope()
    
    // UI State
    var isPopulating by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // Configuration State
    var spoolCount by remember { mutableStateOf("10") }
    var minScans by remember { mutableStateOf("1") }
    var maxScans by remember { mutableStateOf("10") }
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
                                val generator = SampleDataGenerator()
                                generator.generateSampleData(repository, count, min, max)
                                generatedCount = repository.getHistoryCount()
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

