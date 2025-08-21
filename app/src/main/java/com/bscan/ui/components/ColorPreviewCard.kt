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
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(parseColor(colorHex))
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
                    text = "#${colorHex.take(6)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun parseColor(colorHex: String): Color {
    return try {
        val cleanHex = colorHex.removePrefix("#")
        
        // Handle different hex formats
        val colorLong = when (cleanHex.length) {
            6 -> {
                // RGB format - add full alpha
                ("FF" + cleanHex).toLong(16)
            }
            8 -> {
                // Check if this is RGBA or AARRGGBB format
                // If it looks like RGBA (common from tag data), convert to AARRGGBB
                val r = cleanHex.substring(0, 2)
                val g = cleanHex.substring(2, 4)
                val b = cleanHex.substring(4, 6)
                val a = cleanHex.substring(6, 8)
                // Convert RGBA to AARRGGBB for Android
                (a + r + g + b).toLong(16)
            }
            else -> {
                // Default to gray
                0xFFAAAAAA
            }
        }
        
        Color(colorLong)
    } catch (e: Exception) {
        Color.Gray
    }
}