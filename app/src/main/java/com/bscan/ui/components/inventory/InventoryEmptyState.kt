package com.bscan.ui.components.inventory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import com.bscan.ui.components.common.EmptyStateView

/**
 * Empty state display for inventory screen
 */
@Composable
fun InventoryEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.Inventory,
        title = "No Inventory Items",
        subtitle = "Scan components using RFID, QR codes, barcodes, or add them manually to start building your inventory",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun InventoryEmptyStatePreview() {
    MaterialTheme {
        InventoryEmptyState()
    }
}

