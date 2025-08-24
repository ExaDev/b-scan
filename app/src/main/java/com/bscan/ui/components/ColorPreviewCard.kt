package com.bscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPreviewCard(
    colorHex: String,
    colorName: String,
    filamentType: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color preview box
            FilamentColorBox(
                colorHex = colorHex,
                filamentType = filamentType,
                size = 60.dp,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = colorName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "#${colorHex.removePrefix("#").take(6)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ColorPreviewDot(
    colorHex: String,
    filamentType: String = "",
    size: androidx.compose.ui.unit.Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    FilamentColorBox(
        colorHex = colorHex,
        filamentType = filamentType,
        size = size,
        shape = RoundedCornerShape(size / 2),
        modifier = modifier
    )
}

