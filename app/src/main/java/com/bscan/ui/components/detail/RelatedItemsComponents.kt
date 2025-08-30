package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.Component
import java.time.LocalDateTime

/**
 * Component for displaying related items (components with same parent or children).
 * This replaces the old SKU-based related items system.
 */
@Composable
fun RelatedItemsCard(
    relatedComponents: List<Component>,
    modifier: Modifier = Modifier,
    onItemClick: ((Component) -> Unit)? = null
) {
    if (relatedComponents.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Related Components",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp) // Limit height
            ) {
                items(relatedComponents) { component ->
                    RelatedComponentItem(
                        component = component,
                        onClick = onItemClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelatedComponentItem(
    component: Component,
    onClick: ((Component) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { onClick?.invoke(component) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${component.category} • ${component.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            component.massGrams?.let { mass ->
                Text(
                    text = "${mass}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RelatedItemsCardPreview() {
    MaterialTheme {
        RelatedItemsCard(
            relatedComponents = listOf(
                Component(
                    id = "RFID_001",
                    name = "NFC Tag 001",
                    category = "rfid-tag",
                    parentComponentId = "TRAY_001",
                    massGrams = null,
                    createdAt = System.currentTimeMillis(),
                    lastUpdated = LocalDateTime.now()
                ),
                Component(
                    id = "FILAMENT_001",
                    name = "PLA Filament", 
                    category = "filament",
                    parentComponentId = "TRAY_001",
                    massGrams = 1000f,
                    createdAt = System.currentTimeMillis(),
                    lastUpdated = LocalDateTime.now()
                )
            )
        )
    }
}