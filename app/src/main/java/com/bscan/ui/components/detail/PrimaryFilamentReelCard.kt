package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.Component
import com.bscan.model.ComponentIdentifier
import com.bscan.model.IdentifierType
import com.bscan.model.IdentifierPurpose
import java.time.LocalDateTime

/**
 * Primary card for displaying filament reel inventory information.
 * Shows basic reel identification and filament properties.
 */
@Composable
fun PrimaryFilamentReelCard(
    component: Component,
    onAction: ((String) -> Unit)? = null,
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
                text = "Component: ${component.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            DetailInfoRow(
                label = "Category",
                value = component.category
            )
            
            DetailInfoRow(
                label = "Manufacturer",
                value = component.manufacturer
            )
            
            if (component.massGrams != null) {
                DetailInfoRow(
                    label = "Mass",
                    value = "${component.massGrams}g"
                )
            }
            
            if (component.identifiers.isNotEmpty()) {
                val primaryId = component.getPrimaryTrackingIdentifier()
                if (primaryId != null) {
                    DetailInfoRow(
                        label = "ID",
                        value = primaryId.value
                    )
                }
            }
            
            // TODO: Add more component-specific information and actions
            // This component can be expanded based on component category and metadata
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryFilamentReelCardPreview() {
    MaterialTheme {
        PrimaryFilamentReelCard(
            component = createMockComponent()
        )
    }
}

// Mock data for preview
private fun createMockComponent(): Component {
    return Component(
        id = "tray-001",
        identifiers = listOf(
            ComponentIdentifier(
                type = IdentifierType.CONSUMABLE_UNIT,
                value = "01008023456789AB",
                purpose = IdentifierPurpose.TRACKING
            ),
            ComponentIdentifier(
                type = IdentifierType.RFID_HARDWARE,
                value = "A1B2C3D4E5F6G7H8",
                purpose = IdentifierPurpose.AUTHENTICATION
            )
        ),
        name = "Bambu PLA Tray - Marble Orange",
        category = "filament",
        massGrams = 1245.0f,
        fullMassGrams = 1245.0f,
        variableMass = true,
        manufacturer = "Bambu Lab",
        description = "PLA Basic filament in Marble Orange"
    )
}