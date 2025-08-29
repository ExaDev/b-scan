package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

/**
 * Primary card for displaying filament reel inventory information.
 * Shows basic reel identification and filament properties.
 */
@Composable
fun PrimaryFilamentReelCard(
    filamentReel: com.bscan.repository.FilamentReelDetails,
    onPurgeCache: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Simplified version to test layout constraints
    Card(
        modifier = modifier.fillMaxWidth(),
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
            
            DetailInfoRow(
                label = "Material",
                value = filamentReel.filamentInfo.filamentType
            )
            
            DetailInfoRow(
                label = "Color",
                value = filamentReel.filamentInfo.colorName
            )
            
            DetailInfoRow(
                label = "Detailed Type",
                value = filamentReel.filamentInfo.detailedFilamentType.ifEmpty { "Standard" }
            )
            
            DetailInfoRow(
                label = "Spool Weight",
                value = "${filamentReel.filamentInfo.spoolWeight}g"
            )
            
            DetailInfoRow(
                label = "Filament Length",
                value = "${filamentReel.filamentInfo.filamentLength}m"
            )
            
            // TODO: Add more detailed reel information and actions
            // This component can be expanded based on FilamentReelDetails structure
        }
    }
}


// Mock data for preview
private fun createMockFilamentReelDetails(): com.bscan.repository.FilamentReelDetails {
    return com.bscan.repository.FilamentReelDetails(
        trayUid = "01008023456789AB",
        filamentInfo = com.bscan.model.FilamentInfo(
            tagUid = "A1B2C3D4E5F6G7H8",
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
        ),
        tagUids = listOf("A1B2C3D4E5F6G7H8"),
        allScans = emptyList(),
        scansByTag = emptyMap(),
        totalScans = 12,
        successfulScans = 10,
        lastScanned = LocalDateTime.now().minusHours(3)
    )
}