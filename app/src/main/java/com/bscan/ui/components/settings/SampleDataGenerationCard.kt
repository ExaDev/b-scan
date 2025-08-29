package com.bscan.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.repository.ScanHistoryRepository
import com.bscan.ui.screens.settings.SampleDataGenerator
import com.bscan.ui.screens.settings.DataGenerationMode
import com.bscan.data.bambu.BambuVariantSkuMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Card component for generating sample data
 */
@Composable
fun SampleDataGenerationCard(
    repository: ScanHistoryRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    var isGenerating by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(DataGenerationMode.COMPLETE_COVERAGE) }
    var sampleCount by remember { mutableStateOf(25) }
    var generateFromDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var generateToDate by remember { mutableStateOf(LocalDate.now()) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sample Data Generator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Generate realistic scan data for testing and demonstration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Generation mode selection
            GenerationModeSelection(
                selectedMode = selectedMode,
                onModeChanged = { selectedMode = it }
            )
            
            // Advanced options toggle
            AdvancedOptionsToggle(
                showAdvancedOptions = showAdvancedOptions,
                onToggle = { showAdvancedOptions = !showAdvancedOptions }
            )
            
            if (showAdvancedOptions) {
                AdvancedOptionsSection(
                    selectedMode = selectedMode,
                    sampleCount = sampleCount,
                    onSampleCountChanged = { sampleCount = it },
                    generateFromDate = generateFromDate,
                    generateToDate = generateToDate,
                    dateFormatter = dateFormatter
                )
            }
            
            // Generation info
            GenerationInfoSection(
                selectedMode = selectedMode,
                sampleCount = sampleCount
            )
            
            // Generate button
            GenerateButton(
                isGenerating = isGenerating,
                selectedMode = selectedMode,
                sampleCount = sampleCount,
                onGenerate = {
                    scope.launch {
                        isGenerating = true
                        try {
                            val generator = SampleDataGenerator()
                            when (selectedMode) {
                                DataGenerationMode.COMPLETE_COVERAGE -> {
                                    generator.generateWithCompleteSkuCoverage(
                                        repository = repository,
                                        additionalRandomSpools = 50
                                    )
                                }
                                DataGenerationMode.RANDOM_SAMPLE -> {
                                    generator.generateRandomSample(
                                        repository = repository,
                                        spoolCount = sampleCount
                                    )
                                }
                                DataGenerationMode.MINIMAL_COVERAGE -> {
                                    generator.generateMinimalCoverage(
                                        repository = repository
                                    )
                                }
                            }
                        } finally {
                            isGenerating = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun GenerationModeSelection(
    selectedMode: DataGenerationMode,
    onModeChanged: (DataGenerationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Generation Mode",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        DataGenerationMode.entries.forEach { mode ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = { onModeChanged(mode) }
                )
                
                Column(
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodyLarge
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

@Composable
private fun AdvancedOptionsToggle(
    showAdvancedOptions: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Text(
            if (showAdvancedOptions) "Hide Advanced Options" else "Show Advanced Options"
        )
        Icon(
            if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null
        )
    }
}

@Composable
private fun AdvancedOptionsSection(
    selectedMode: DataGenerationMode,
    sampleCount: Int,
    onSampleCountChanged: (Int) -> Unit,
    generateFromDate: LocalDate,
    generateToDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sample count (only for non-complete coverage modes)
        if (selectedMode != DataGenerationMode.COMPLETE_COVERAGE) {
            OutlinedTextField(
                value = sampleCount.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { count ->
                        onSampleCountChanged(count.coerceIn(1, 500))
                    }
                },
                label = { Text("Sample Count") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Date range
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = generateFromDate.format(dateFormatter),
                onValueChange = { /* Read-only for now */ },
                label = { Text("From Date") },
                readOnly = true,
                modifier = Modifier.weight(1f)
            )
            
            OutlinedTextField(
                value = generateToDate.format(dateFormatter),
                onValueChange = { /* Read-only for now */ },
                label = { Text("To Date") },
                readOnly = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GenerationInfoSection(
    selectedMode: DataGenerationMode,
    sampleCount: Int,
    modifier: Modifier = Modifier
) {
    when (selectedMode) {
        DataGenerationMode.COMPLETE_COVERAGE -> {
            val totalSkus = BambuVariantSkuMapper.getAllKnownRfidKeys().size
            Text(
                text = "Will generate samples for all $totalSkus known Bambu Lab products",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
        DataGenerationMode.RANDOM_SAMPLE -> {
            Text(
                text = "Will generate $sampleCount random samples from available products",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
        DataGenerationMode.MINIMAL_COVERAGE -> {
            Text(
                text = "Will generate $sampleCount samples focusing on most common materials",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun GenerateButton(
    isGenerating: Boolean,
    selectedMode: DataGenerationMode,
    sampleCount: Int,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onGenerate,
        enabled = !isGenerating,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generating...")
        } else {
            Icon(Icons.Default.DataArray, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Sample Data")
        }
    }
}

