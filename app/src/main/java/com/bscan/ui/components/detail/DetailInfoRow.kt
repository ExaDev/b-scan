package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A reusable component for displaying key-value pairs in detail views.
 * Used consistently across different detail screen components.
 */
@Composable
fun DetailInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailInfoRowPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailInfoRow(
                label = "Material Type",
                value = "PLA Basic"
            )
            DetailInfoRow(
                label = "Temperature Range",
                value = "210-230°C"
            )
            DetailInfoRow(
                label = "SKU",
                value = "GFL00A00K0"
            )
            DetailInfoRow(
                label = "Weight",
                value = "250.5g"
            )
        }
    }
}

