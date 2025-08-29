package com.bscan.ui.components.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Visual hierarchy indicators (lines and connections) for tree structures
 */
@Composable
fun HierarchyIndicator(
    depth: Int,
    isLast: Boolean,
    parentIds: List<String>,
    modifier: Modifier = Modifier
) {
    if (depth == 0) return

    val strokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val density = LocalDensity.current

    Canvas(
        modifier = modifier.size(width = (depth * 16).dp, height = 24.dp)
    ) {
        val strokeWidth = with(density) { 1.dp.toPx() }
        val segmentWidth = with(density) { 16.dp.toPx() }
        val centerY = size.height / 2f

        // Draw vertical lines for each parent level
        for (level in 0 until depth) {
            val x = level * segmentWidth + segmentWidth / 2f
            
            if (level < depth - 1) {
                // Vertical line for intermediate levels
                drawLine(
                    color = strokeColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth
                )
            } else {
                // L-shaped connector for current level
                if (!isLast) {
                    // Vertical line continues down
                    drawLine(
                        color = strokeColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                } else {
                    // Vertical line stops at center
                    drawLine(
                        color = strokeColor,
                        start = Offset(x, 0f),
                        end = Offset(x, centerY),
                        strokeWidth = strokeWidth
                    )
                }
                
                // Horizontal line to component
                drawLine(
                    color = strokeColor,
                    start = Offset(x, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HierarchyIndicatorPreview() {
    MaterialTheme {
        Column {
            HierarchyIndicator(
                depth = 1,
                isLast = false,
                parentIds = listOf("parent1")
            )
            HierarchyIndicator(
                depth = 2,
                isLast = true,
                parentIds = listOf("parent1", "parent2")
            )
            HierarchyIndicator(
                depth = 3,
                isLast = false,
                parentIds = listOf("parent1", "parent2", "parent3")
            )
        }
    }
}

