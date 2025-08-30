package com.bscan.ui.components.spool

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.Component
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolOverviewCard(
    component: Component,
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
                text = "Component Overview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            val primaryId = component.getPrimaryTrackingIdentifier()
            
            StatisticGrid(
                statistics = buildList {
                    add("Name" to component.name)
                    add("Category" to component.category)
                    add("Manufacturer" to component.manufacturer)
                    if (primaryId != null) {
                        add("Primary ID" to primaryId.value)
                    }
                    add("Identifiers" to "${component.identifiers.size} IDs")
                    if (component.massGrams != null) {
                        add("Mass" to "${component.massGrams}g")
                    }
                    add("Created" to component.lastUpdated.format(formatter))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociatedIdentifiersCard(
    component: Component,
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
                text = "Component Identifiers",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (component.identifiers.isNotEmpty()) {
                component.identifiers.forEach { identifier ->
                    IdentifierInfoRow(
                        identifier = identifier
                    )
                    
                    if (identifier != component.identifiers.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                Text(
                    text = "No identifiers assigned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IdentifierInfoRow(
    identifier: com.bscan.model.ComponentIdentifier,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Nfc,
            contentDescription = "Identifier",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = identifier.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${identifier.type.name} • ${identifier.purpose.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpoolOverviewCardPreview() {
    MaterialTheme {
        val mockComponent = Component(
            id = "tray-001",
            identifiers = listOf(
                com.bscan.model.ComponentIdentifier(
                    type = com.bscan.model.IdentifierType.CONSUMABLE_UNIT,
                    value = "01008023456789ABCDEF",
                    purpose = com.bscan.model.IdentifierPurpose.TRACKING
                ),
                com.bscan.model.ComponentIdentifier(
                    type = com.bscan.model.IdentifierType.RFID_HARDWARE,
                    value = "A1B2C3D4",
                    purpose = com.bscan.model.IdentifierPurpose.AUTHENTICATION
                )
            ),
            name = "Bambu PLA Basic - Red",
            category = "filament",
            massGrams = 1000.0f,
            fullMassGrams = 1000.0f,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "PLA Basic filament in red color"
        )
        
        SpoolOverviewCard(
            component = mockComponent
        )
    }
}

@Preview(showBackground = true)  
@Composable
fun AssociatedIdentifiersCardPreview() {
    MaterialTheme {
        val mockComponent = Component(
            id = "tray-002",
            identifiers = listOf(
                com.bscan.model.ComponentIdentifier(
                    type = com.bscan.model.IdentifierType.CONSUMABLE_UNIT,
                    value = "01008023456789ABCDEF",
                    purpose = com.bscan.model.IdentifierPurpose.TRACKING
                ),
                com.bscan.model.ComponentIdentifier(
                    type = com.bscan.model.IdentifierType.RFID_HARDWARE,
                    value = "A1B2C3D4",
                    purpose = com.bscan.model.IdentifierPurpose.AUTHENTICATION
                ),
                com.bscan.model.ComponentIdentifier(
                    type = com.bscan.model.IdentifierType.SERIAL_NUMBER,
                    value = "SN-ABC123",
                    purpose = com.bscan.model.IdentifierPurpose.DISPLAY
                )
            ),
            name = "Bambu PLA Basic - Green",
            category = "filament",
            massGrams = 850.0f,
            fullMassGrams = 1000.0f,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "PLA Basic filament in green color"
        )
        
        AssociatedIdentifiersCard(
            component = mockComponent
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IdentifierInfoRowPreview() {
    MaterialTheme {
        IdentifierInfoRow(
            identifier = com.bscan.model.ComponentIdentifier(
                type = com.bscan.model.IdentifierType.RFID_HARDWARE,
                value = "A1B2C3D4E5F6",
                purpose = com.bscan.model.IdentifierPurpose.AUTHENTICATION
            )
        )
    }
}