package com.bscan.ui.components.tree

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.ComponentIdentifier
import com.bscan.model.IdentifierPurpose
import com.bscan.model.IdentifierType

/**
 * Identifier display chip showing type and truncated value
 */
@Composable
fun IdentifierChip(
    identifier: ComponentIdentifier,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (identifier.type) {
        IdentifierType.RFID_HARDWARE -> "RFID" to MaterialTheme.colorScheme.primary
        IdentifierType.CONSUMABLE_UNIT -> "Tray" to MaterialTheme.colorScheme.secondary
        IdentifierType.SERIAL_NUMBER -> "S/N" to MaterialTheme.colorScheme.tertiary
        IdentifierType.SKU -> "SKU" to MaterialTheme.colorScheme.primary
        IdentifierType.QR -> "QR" to MaterialTheme.colorScheme.secondary
        IdentifierType.BARCODE -> "Barcode" to MaterialTheme.colorScheme.tertiary
        else -> identifier.type.name to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AssistChip(
        onClick = { },
        label = { 
            Text(
                "$label: ${identifier.value.take(8)}${if (identifier.value.length > 8) "..." else ""}",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            ) 
        },
        modifier = modifier.height(20.dp),
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun IdentifierChipPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdentifierChip(
                identifier = ComponentIdentifier(
                    type = IdentifierType.RFID_HARDWARE,
                    value = "A1B2C3D4E5F6G7H8",
                    purpose = IdentifierPurpose.AUTHENTICATION
                )
            )
            IdentifierChip(
                identifier = ComponentIdentifier(
                    type = IdentifierType.SKU,
                    value = "GFL00A00K0",
                    purpose = IdentifierPurpose.LOOKUP
                )
            )
            IdentifierChip(
                identifier = ComponentIdentifier(
                    type = IdentifierType.SERIAL_NUMBER,
                    value = "SN123456789",
                    purpose = IdentifierPurpose.TRACKING
                )
            )
        }
    }
}

