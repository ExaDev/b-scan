package com.bscan.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.bscan.repository.ExportScope
import com.bscan.repository.ImportMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportCard(
    scanCount: Int,
    spoolCount: Int,
    isExporting: Boolean,
    isImporting: Boolean,
    exportScope: ExportScope,
    importMode: ImportMode,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    previewData: ExportPreviewData?,
    showPreview: Boolean,
    onExportScopeChange: (ExportScope) -> Unit,
    onImportModeChange: (ImportMode) -> Unit,
    onFromDateChange: (LocalDate?) -> Unit,
    onToDateChange: (LocalDate?) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Data Export/Import",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Export scan data for backup or sharing, or import previously exported data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (scanCount > 0) {
                Text(
                    text = "Currently $scanCount scans from $spoolCount spools in database",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Export Section
            Text(
                text = "Export Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            ExportScopeSelector(
                selectedScope = exportScope,
                onScopeChange = onExportScopeChange,
                fromDate = fromDate,
                toDate = toDate,
                onFromDateChange = onFromDateChange,
                onToDateChange = onToDateChange
            )
            
            Button(
                onClick = onExportClick,
                enabled = !isExporting && !isImporting && scanCount > 0,
                modifier = Modifier.align(Alignment.Start)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isExporting) "Exporting..." else "Export Data")
            }
            
            HorizontalDivider()
            
            // Import Section
            Text(
                text = "Import Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            ImportModeSelector(
                selectedMode = importMode,
                onModeChange = onImportModeChange
            )
            
            if (showPreview && previewData != null) {
                ImportPreview(
                    previewData = previewData,
                    onConfirmImport = onConfirmImport,
                    onCancelImport = onCancelImport,
                    isImporting = isImporting
                )
            } else {
                Button(
                    onClick = onImportClick,
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Import File")
                }
            }
        }
    }
}

@Composable
private fun ExportScopeSelector(
    selectedScope: ExportScope,
    onScopeChange: (ExportScope) -> Unit,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    onFromDateChange: (LocalDate?) -> Unit,
    onToDateChange: (LocalDate?) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExportScope.values().forEach { scope ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedScope == scope),
                        onClick = { onScopeChange(scope) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedScope == scope),
                    onClick = null // null recommended for accessibility with screenreaders
                )
                Text(
                    text = when (scope) {
                        ExportScope.ALL_DATA -> "All data"
                        ExportScope.DATE_RANGE -> "Date range"
                        ExportScope.SELECTED_SPOOLS -> "Selected spools"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        // Date picker for date range scope
        if (selectedScope == ExportScope.DATE_RANGE) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select Date Range",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DatePickerButton(
                            label = "From",
                            date = fromDate,
                            onDateChange = onFromDateChange,
                            modifier = Modifier.weight(1f)
                        )
                        
                        DatePickerButton(
                            label = "To",
                            date = toDate,
                            onDateChange = onToDateChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Note for selected spools (not implemented in this version)
        if (selectedScope == ExportScope.SELECTED_SPOOLS) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "Spool selection will be added in a future version. For now, use 'All data' or 'Date range'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ImportModeSelector(
    selectedMode: ImportMode,
    onModeChange: (ImportMode) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ImportMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedMode == mode),
                        onClick = { onModeChange(mode) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedMode == mode),
                    onClick = null
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = when (mode) {
                            ImportMode.MERGE_WITH_EXISTING -> "Merge with existing data"
                            ImportMode.REPLACE_ALL -> "Replace all data"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = when (mode) {
                            ImportMode.MERGE_WITH_EXISTING -> "Add imported data to current database (duplicates skipped)"
                            ImportMode.REPLACE_ALL -> "⚠️ Delete all current data and replace with imported data"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mode == ImportMode.REPLACE_ALL) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerButton(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = modifier
    ) {
        Text(
            text = if (date != null) {
                "$label: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}"
            } else {
                "$label date"
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { millis ->
                millis?.let { ms ->
                    val localDate = java.time.Instant.ofEpochMilli(ms)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    onDateChange(localDate)
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

data class ExportPreviewData(
    val filename: String,
    val totalScans: Int,
    val encryptedScans: Int,
    val decryptedScans: Int,
    val uniqueSpools: Int,
    val dateRange: String,
    val fileVersion: Int,
    val exportDate: String
)

@Composable
private fun ImportPreview(
    previewData: ExportPreviewData,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    isImporting: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Import Preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "File: ${previewData.filename}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Total scans: ${previewData.totalScans} (${previewData.encryptedScans} encrypted, ${previewData.decryptedScans} decrypted)",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Unique spools: ${previewData.uniqueSpools}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Date range: ${previewData.dateRange}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Exported: ${previewData.exportDate}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onCancelImport,
                    enabled = !isImporting
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = onConfirmImport,
                    enabled = !isImporting
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isImporting) "Importing..." else "Confirm Import")
                }
            }
        }
    }
}