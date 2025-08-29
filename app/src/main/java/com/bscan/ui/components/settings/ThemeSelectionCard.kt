package com.bscan.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bscan.model.AppTheme

/**
 * Card component for theme selection settings
 */
@Composable
fun ThemeSelectionCard(
    currentTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Choose the app appearance theme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Theme options
            AppTheme.entries.forEach { theme ->
                ThemeOption(
                    theme = theme,
                    isSelected = currentTheme == theme,
                    onSelected = { onThemeChanged(theme) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelected() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = when (theme) {
                    AppTheme.AUTO -> "Auto (System)"
                    AppTheme.LIGHT -> "Light"
                    AppTheme.DARK -> "Dark"
                    AppTheme.WHITE -> "White"
                    AppTheme.BLACK -> "Black"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            
            Text(
                text = when (theme) {
                    AppTheme.AUTO -> "Follow system dark/light mode setting"
                    AppTheme.LIGHT -> "Light theme with standard colours"
                    AppTheme.DARK -> "Dark theme for low-light environments"
                    AppTheme.WHITE -> "High contrast white theme"
                    AppTheme.BLACK -> "Pure black theme for OLED displays"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Visual indicator for theme
        ThemeVisualIndicator(theme = theme)
    }
}

@Composable
private fun ThemeVisualIndicator(
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                when (theme) {
                    AppTheme.AUTO -> Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.onSurface
                        )
                    )
                    AppTheme.LIGHT -> SolidColor(Color(0xFFFFFFFF))
                    AppTheme.DARK -> SolidColor(Color(0xFF121212))
                    AppTheme.WHITE -> SolidColor(Color(0xFFFFFFFF))
                    AppTheme.BLACK -> SolidColor(Color(0xFF000000))
                }
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = CircleShape
            )
    ) {
        if (theme == AppTheme.AUTO) {
            // Show a small icon for Auto theme
            Icon(
                imageVector = Icons.Default.Brightness4,
                contentDescription = "Auto theme",
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}


