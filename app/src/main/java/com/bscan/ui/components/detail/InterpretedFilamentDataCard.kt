package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.ui.components.ColorPreviewCard
import com.bscan.ui.components.InfoCard
import com.bscan.ui.components.SpecificationCard
import com.bscan.ui.components.TemperatureCard
import com.bscan.ui.components.filament.*

/**
 * Card displaying interpreted filament data from RFID tag including specifications,
 * temperature settings, and production information.
 * Includes copy functionality for structured data export.
 */
@Composable
fun InterpretedFilamentDataCard(
    filamentInfo: com.bscan.model.FilamentInfo,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
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
                        val filamentDataText = buildFilamentDataString(filamentInfo)
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
                    val jsonData = buildFilamentJsonString(filamentInfo)
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

private fun buildFilamentDataString(filamentInfo: com.bscan.model.FilamentInfo): String {
    return buildString {
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
}

private fun buildFilamentJsonString(filamentInfo: com.bscan.model.FilamentInfo): String {
    return buildString {
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
}


// Mock filament info for preview
private fun createMockFilamentInfo(): com.bscan.model.FilamentInfo {
    return com.bscan.model.FilamentInfo(
        tagUid = "A1B2C3D4",
        trayUid = "01008023456789AB",
        filamentType = "PLA",
        detailedFilamentType = "PLA Basic",
        colorName = "Marble Orange",
        colorHex = "#FF8B42",
        spoolWeight = 245,
        filamentDiameter = 1.75f,
        filamentLength = 330,
        productionDate = "2024-01-15",
        minTemperature = 210,
        maxTemperature = 230,
        bedTemperature = 60,
        dryingTemperature = 45,
        dryingTime = 8
    )
}