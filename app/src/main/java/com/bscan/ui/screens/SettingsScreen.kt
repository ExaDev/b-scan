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
import com.bscan.repository.ScanHistoryRepository
import com.bscan.ui.screens.settings.SampleDataGenerator
import kotlinx.coroutines.launch

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
        generatedCount = repository.getHistoryCount()
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

