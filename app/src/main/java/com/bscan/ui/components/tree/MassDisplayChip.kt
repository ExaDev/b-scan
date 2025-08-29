package com.bscan.ui.components.tree

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.logic.WeightUnit

/**
 * Mass information display with status indicators for variable mass and inferred values
 */
@Composable
fun MassDisplayChip(
    massGrams: Float?,
    fullMassGrams: Float? = null,
    variableMass: Boolean = false,
    inferredMass: Boolean = false,
    preferredUnit: WeightUnit = WeightUnit.GRAMS,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mass value
        Text(
            text = formatMass(massGrams, preferredUnit),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (inferredMass) FontWeight.Normal else FontWeight.Medium,
            color = if (inferredMass) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        // Status indicators
        if (inferredMass) {
            Icon(
                Icons.Default.Calculate,
                contentDescription = "Inferred mass",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        if (variableMass) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = "Variable mass",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        // Percentage for variable mass components
        if (variableMass && fullMassGrams != null && fullMassGrams > 0f && massGrams != null) {
            val percentage = ((massGrams / fullMassGrams) * 100).toInt()
            Text(
                text = "($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    percentage < 10 -> MaterialTheme.colorScheme.error
                    percentage < 25 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Helper function to format mass values according to preferred unit
 */
private fun formatMass(massGrams: Float?, preferredUnit: WeightUnit): String {
    if (massGrams == null) return "Unknown"
    
    return when (preferredUnit) {
        WeightUnit.GRAMS -> "${String.format("%.1f", massGrams)}g"
        WeightUnit.KILOGRAMS -> "${String.format("%.3f", massGrams / 1000f)}kg"  
        WeightUnit.OUNCES -> "${String.format("%.2f", massGrams * 0.035274f)}oz"
        WeightUnit.POUNDS -> "${String.format("%.3f", massGrams * 0.00220462f)}lbs"
    }
}

