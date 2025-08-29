package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.FilamentInfo
import com.bscan.model.TagFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificationCard(
    filamentInfo: FilamentInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Specifications",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SpecificationRow(
                label = "Diameter",
                value = "${filamentInfo.filamentDiameter} mm"
            )
            
            SpecificationRow(
                label = "Weight",
                value = "${filamentInfo.spoolWeight} g"
            )
            
            SpecificationRow(
                label = "Length",
                value = "${filamentInfo.filamentLength} mm"
            )
        }
    }
}

@Composable
private fun SpecificationRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpecificationCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SpecificationCard(
                filamentInfo = FilamentInfo(
                    tagUid = "A1B2C3D4",
                    trayUid = "01008023",
                    tagFormat = TagFormat.BAMBU_LAB,
                    manufacturerName = "Bambu Lab",
                    filamentType = "PLA",
                    detailedFilamentType = "PLA Basic",
                    colorHex = "#000000",
                    colorName = "Black",
                    spoolWeight = 1000,
                    filamentDiameter = 1.75f,
                    filamentLength = 330000,
                    productionDate = "2024-03",
                    minTemperature = 210,
                    maxTemperature = 230,
                    bedTemperature = 60,
                    dryingTemperature = 40,
                    dryingTime = 8
                )
            )
            SpecificationCard(
                filamentInfo = FilamentInfo(
                    tagUid = "E5F6A7B8",
                    trayUid = "01008024",
                    tagFormat = TagFormat.BAMBU_LAB,
                    manufacturerName = "Bambu Lab",
                    filamentType = "PETG",
                    detailedFilamentType = "PETG Basic",
                    colorHex = "#4ECDC4",
                    colorName = "Turquoise",
                    spoolWeight = 750,
                    filamentDiameter = 1.75f,
                    filamentLength = 250000,
                    productionDate = "2024-02",
                    minTemperature = 240,
                    maxTemperature = 260,
                    bedTemperature = 80,
                    dryingTemperature = 65,
                    dryingTime = 6
                )
            )
        }
    }
}