package com.bscan.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun StatisticDisplay(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )
    }
}

@Composable
fun StatisticGrid(
    statistics: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    columns: Int = 2
) {
    Column(modifier = modifier) {
        statistics.chunked(columns).forEach { rowStats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowStats.forEach { (label, value) ->
                    StatisticDisplay(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Add empty spaces for incomplete rows
                repeat(columns - rowStats.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (statistics.indexOf(rowStats.first()) < statistics.size - columns) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CollectionStatsCard(
    title: String,
    statistics: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatisticGrid(statistics = statistics)
            
            additionalContent()
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun StatisticDisplayPreview() {
    MaterialTheme {
        StatisticDisplay(
            label = "Total Weight",
            value = "2.5kg"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticGridPreview() {
    MaterialTheme {
        StatisticGrid(
            statistics = listOf(
                "Total Spools" to "12",
                "Weight" to "2.5kg",
                "Materials" to "5",
                "Colors" to "18"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CollectionStatsCardPreview() {
    MaterialTheme {
        CollectionStatsCard(
            title = "Inventory Overview",
            statistics = listOf(
                "Total Spools" to "12",
                "Weight" to "2.5kg",
                "Materials" to "5",
                "Colors" to "18"
            )
        )
    }
}