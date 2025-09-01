package com.bscan.ui.components.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bscan.model.graph.entities.*
import kotlin.math.*

/**
 * Canvas component for drawing entity graph visualization with nodes, edges, and clusters
 */
@Composable
fun GraphVisualizationCanvas(
    layout: GraphLayout,
    config: GraphVisualizationConfig,
    interactionState: GraphInteractionState,
    onNodeTapped: (GraphNode) -> Unit = {},
    onNodeDragged: (GraphNode, Offset) -> Unit = { _, _ -> },
    onCanvasTapped: (Offset) -> Unit = {},
    onPanChanged: (Offset) -> Unit = {},
    onZoomChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    // Color scheme from Material Theme
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(layout) {
                detectTapGestures(
                    onTap = { offset ->
                        val transformedOffset = transformScreenToGraph(offset, interactionState)
                        val tappedNode = findNodeAtPosition(layout.nodes, transformedOffset)
                        
                        if (tappedNode != null) {
                            onNodeTapped(tappedNode)
                        } else {
                            onCanvasTapped(transformedOffset)
                        }
                    },
                    onDoubleTap = { offset ->
                        // Double-tap to zoom in/out
                        val targetScale = when {
                            interactionState.scale < 1f -> 1f
                            interactionState.scale < 2f -> 2f
                            else -> 0.5f
                        }
                        onZoomChanged(targetScale)
                        
                        // Zoom towards the tap point
                        val tapInGraph = transformScreenToGraph(offset, interactionState)
                        val newOffset = offset - (tapInGraph * targetScale)
                        onPanChanged(newOffset - interactionState.offset)
                    }
                )
            }
            .pointerInput(layout) {
                var draggedNode: GraphNode? = null
                detectDragGestures(
                    onDragStart = { offset ->
                        val transformedOffset = transformScreenToGraph(offset, interactionState)
                        draggedNode = findNodeAtPosition(layout.nodes, transformedOffset)
                    },
                    onDragEnd = {
                        draggedNode = null
                    },
                    onDrag = { _, dragAmount ->
                        if (draggedNode != null) {
                            // Node dragging
                            val scaledDrag = dragAmount / interactionState.scale
                            onNodeDragged(draggedNode!!, scaledDrag)
                        } else {
                            // Canvas panning
                            onPanChanged(dragAmount)
                        }
                    }
                )
            }
            .pointerInput(layout) {
                detectTransformGestures(
                    panZoomLock = false
                ) { centroid, pan, zoom, _ ->
                    if (zoom != 1f) {
                        // Handle pinch-to-zoom
                        val newScale = (interactionState.scale * zoom).coerceIn(0.1f, 5f)
                        onZoomChanged(newScale)
                        
                        // Adjust pan to zoom around the centroid
                        val zoomFactor = newScale / interactionState.scale
                        val centroidInGraph = transformScreenToGraph(centroid, interactionState)
                        val newOffset = centroid - (centroidInGraph * newScale)
                        onPanChanged(newOffset - interactionState.offset)
                    } else if (pan != Offset.Zero) {
                        // Handle pan from transform gesture (when not zooming)
                        onPanChanged(pan)
                    }
                }
            }
    ) {
        // Clear background
        drawRect(
            color = colorScheme.surface,
            size = size
        )
        
        // Transform nodes for display (apply pan and zoom)
        val transformedNodes = layout.nodes.map { node ->
            node.copy().apply {
                position = transformGraphToScreen(node.position, interactionState)
            }
        }
        
        val transformedLayout = layout.copy(nodes = transformedNodes)
        
        // Draw clusters (behind other elements)
        if (config.showClusters) {
            drawClusters(
                clusters = transformedLayout.clusters,
                nodes = transformedNodes,
                config = config,
                colorScheme = colorScheme
            )
        }
        
        // Draw edges
        drawEdges(
            edges = transformedLayout.edges,
            nodes = transformedNodes,
            config = config,
            colorScheme = colorScheme
        )
        
        // Draw nodes
        drawNodes(
            nodes = transformedNodes,
            config = config,
            colorScheme = colorScheme,
            textMeasurer = textMeasurer,
            interactionState = interactionState
        )
        
        // Draw node labels
        if (config.showNodeLabels) {
            drawNodeLabels(
                nodes = transformedNodes,
                config = config,
                colorScheme = colorScheme,
                textMeasurer = textMeasurer,
                typography = typography
            )
        }
        
        // Draw UI overlay (not affected by pan/zoom)
        // Note: Legend is always shown in this version
        drawLegend(
            colorScheme = colorScheme,
            textMeasurer = textMeasurer,
            typography = typography
        )
    }
}

/**
 * Draw cluster background regions
 */
private fun DrawScope.drawClusters(
    clusters: List<GraphCluster>,
    nodes: List<GraphNode>,
    config: GraphVisualizationConfig,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    clusters.forEach { cluster ->
        val clusterNodes = nodes.filter { it.clusterId == cluster.id }
        if (clusterNodes.isEmpty()) return@forEach
        
        // Calculate cluster bounds with padding
        val bounds = calculateClusterBounds(clusterNodes, config.clusterPadding)
        
        // Draw cluster background
        drawOval(
            color = cluster.color.copy(alpha = 0.1f),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )
        
        // Draw cluster border
        drawOval(
            color = cluster.color.copy(alpha = 0.3f),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
            )
        )
    }
}

/**
 * Calculate bounding rectangle for cluster nodes
 */
private fun calculateClusterBounds(nodes: List<GraphNode>, padding: Float): androidx.compose.ui.geometry.Rect {
    if (nodes.isEmpty()) return androidx.compose.ui.geometry.Rect.Zero
    
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    
    nodes.forEach { node ->
        val radius = getNodeRadius(node)
        minX = minOf(minX, node.position.x - radius)
        minY = minOf(minY, node.position.y - radius)
        maxX = maxOf(maxX, node.position.x + radius)
        maxY = maxOf(maxY, node.position.y + radius)
    }
    
    return androidx.compose.ui.geometry.Rect(
        left = minX - padding,
        top = minY - padding,
        right = maxX + padding,
        bottom = maxY + padding
    )
}

/**
 * Draw graph edges
 */
private fun DrawScope.drawEdges(
    edges: List<GraphEdge>,
    nodes: List<GraphNode>,
    config: GraphVisualizationConfig,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    val nodePositions = nodes.associateBy { it.id }
    
    edges.forEach { edge ->
        val fromNode = nodePositions[edge.fromNodeId]
        val toNode = nodePositions[edge.toNodeId]
        
        if (fromNode != null && toNode != null) {
            drawEdge(
                from = fromNode.position,
                to = toNode.position,
                edge = edge,
                config = config,
                colorScheme = colorScheme
            )
        }
    }
}

/**
 * Draw a single edge
 */
private fun DrawScope.drawEdge(
    from: Offset,
    to: Offset,
    edge: GraphEdge,
    config: GraphVisualizationConfig,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    // Calculate edge color based on relationship type
    val edgeColor = when (edge.relationshipType) {
        "IDENTIFIED_BY" -> Color(0xFF4CAF50) // Green
        "CONTAINS" -> Color(0xFF2196F3) // Blue
        "TRACKS" -> Color(0xFFFF9800) // Orange
        "HAD_MOVEMENT" -> Color(0xFF9C27B0) // Purple
        "STORED_AT" -> Color(0xFF795548) // Brown
        else -> colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    // Use edge's built-in stroke width
    val strokeWidth = edge.strokeWidth
    
    // Check if edge should be dashed based on relationship type
    val isDashed = when (edge.relationshipType) {
        "HAD_MOVEMENT", "INFERRED_FROM" -> true
        else -> false
    }
    
    // Draw main edge line
    drawLine(
        color = edgeColor,
        start = from,
        end = to,
        strokeWidth = strokeWidth,
        pathEffect = if (isDashed) {
            PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        } else null
    )
    
    // Draw arrowhead for directed edges (always show for this version)
    if (edge.directional) {
        drawArrowhead(from, to, edgeColor, strokeWidth)
    }
}

/**
 * Draw arrowhead at the end of directed edges
 */
private fun DrawScope.drawArrowhead(
    from: Offset,
    to: Offset,
    color: Color,
    strokeWidth: Float
) {
    val direction = (to - from).let { 
        if (it.getDistance() > 0) it / it.getDistance() else Offset(1f, 0f)
    }
    val arrowSize = strokeWidth * 3
    val angle = 25 * PI / 180 // 25 degrees
    
    // Calculate arrowhead points
    val arrowOffset = direction * arrowSize
    val perpendicular = Offset(-direction.y, direction.x)
    
    val tip = to - direction * (arrowSize * 0.5f) // Slightly back from edge end
    val left = tip - arrowOffset + perpendicular * (arrowSize * 0.5f)
    val right = tip - arrowOffset - perpendicular * (arrowSize * 0.5f)
    
    // Draw arrowhead triangle
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    
    drawPath(
        path = path,
        color = color
    )
}

/**
 * Draw graph nodes
 */
private fun DrawScope.drawNodes(
    nodes: List<GraphNode>,
    config: GraphVisualizationConfig,
    colorScheme: androidx.compose.material3.ColorScheme,
    textMeasurer: TextMeasurer,
    interactionState: GraphInteractionState
) {
    nodes.forEach { node ->
        val radius = getNodeRadius(node)
        val isSelected = node.id == interactionState.selectedNodeId
        val isDragged = false // Simplified for now
        
        // Calculate node color based on entity type
        val nodeColor = getNodeColor(node.entity, colorScheme)
        val strokeColor = if (isSelected) {
            colorScheme.primary
        } else if (isDragged) {
            colorScheme.secondary
        } else {
            nodeColor.copy(alpha = 0.8f)
        }
        
        // Draw node shadow for depth
        drawCircle(
            color = Color.Black.copy(alpha = 0.1f),
            radius = radius,
            center = node.position + Offset(2f, 2f)
        )
        
        // Draw main node circle
        drawCircle(
            color = nodeColor,
            radius = radius,
            center = node.position
        )
        
        // Draw node border
        drawCircle(
            color = strokeColor,
            radius = radius,
            center = node.position,
            style = Stroke(
                width = if (isSelected) 4.dp.toPx() else 2.dp.toPx()
            )
        )
        
        // Draw entity type icon
        drawNodeIcon(node, radius * 0.6f, colorScheme)
    }
}

/**
 * Get node radius based on entity type and properties
 */
private fun getNodeRadius(node: GraphNode): Float {
    val baseRadius = when (node.entity) {
        is PhysicalComponent -> 25f
        is InventoryItem -> 30f
        is Activity -> 20f
        is Information -> 18f
        is Identifier -> 15f
        is Location -> 28f
        is Person -> 22f
        is Virtual -> 16f
        else -> 20f
    }
    
    // Scale radius based on importance/connections
    val importance = node.entity.getProperty<Float>("importance") ?: 1f
    return baseRadius * importance.coerceIn(0.5f, 2f)
}

/**
 * Get node color based on entity type
 */
private fun getNodeColor(
    entity: com.bscan.model.graph.Entity,
    colorScheme: androidx.compose.material3.ColorScheme
): Color {
    return when (entity) {
        is PhysicalComponent -> {
            // Use color property if available
            entity.getProperty<String>("color")?.let { colorHex ->
                try {
                    Color(android.graphics.Color.parseColor(colorHex))
                } catch (e: Exception) {
                    Color(0xFF4CAF50) // Default green
                }
            } ?: Color(0xFF4CAF50)
        }
        is InventoryItem -> Color(0xFF2196F3) // Blue
        is Activity -> Color(0xFFFF9800) // Orange
        is Information -> Color(0xFF9C27B0) // Purple
        is Identifier -> Color(0xFFF44336) // Red
        is Location -> Color(0xFF795548) // Brown
        is Person -> Color(0xFFE91E63) // Pink
        is Virtual -> Color(0xFF607D8B) // Blue Grey
        else -> colorScheme.primary
    }
}

/**
 * Draw entity type icon inside node
 */
private fun DrawScope.drawNodeIcon(
    node: GraphNode,
    iconSize: Float,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    val iconColor = Color.White
    val center = node.position
    
    when (node.entity) {
        is PhysicalComponent -> {
            // Draw cube icon
            drawRect(
                color = iconColor,
                topLeft = center - Offset(iconSize/2, iconSize/2),
                size = Size(iconSize, iconSize),
                style = Stroke(width = 2f)
            )
        }
        is InventoryItem -> {
            // Draw stack of rectangles
            repeat(3) { i ->
                val offset = i * 3f
                drawRect(
                    color = iconColor,
                    topLeft = center - Offset(iconSize/2 - offset, iconSize/2 - offset),
                    size = Size(iconSize - offset * 2, iconSize - offset * 2),
                    style = Stroke(width = 1.5f)
                )
            }
        }
        is Activity -> {
            // Draw clock icon
            drawCircle(
                color = iconColor,
                radius = iconSize / 2,
                center = center,
                style = Stroke(width = 2f)
            )
            drawLine(
                color = iconColor,
                start = center,
                end = center + Offset(0f, -iconSize/3),
                strokeWidth = 2f
            )
        }
        is Information -> {
            // Draw document icon
            drawRect(
                color = iconColor,
                topLeft = center - Offset(iconSize/3, iconSize/2),
                size = Size(iconSize/1.5f, iconSize),
                style = Stroke(width = 2f)
            )
        }
        is Identifier -> {
            // Draw tag icon
            val path = Path().apply {
                moveTo(center.x - iconSize/2, center.y)
                lineTo(center.x + iconSize/2, center.y - iconSize/3)
                lineTo(center.x + iconSize/2, center.y + iconSize/3)
                close()
            }
            drawPath(path, iconColor, style = Stroke(width = 2f))
        }
        is Location -> {
            // Draw location pin icon
            drawCircle(
                color = iconColor,
                radius = iconSize / 3,
                center = center - Offset(0f, iconSize/6),
                style = Stroke(width = 2f)
            )
            drawLine(
                color = iconColor,
                start = center + Offset(0f, iconSize/6),
                end = center + Offset(0f, iconSize/2),
                strokeWidth = 2f
            )
        }
    }
}

/**
 * Draw node labels
 */
private fun DrawScope.drawNodeLabels(
    nodes: List<GraphNode>,
    config: GraphVisualizationConfig,
    colorScheme: androidx.compose.material3.ColorScheme,
    textMeasurer: TextMeasurer,
    typography: androidx.compose.material3.Typography
) {
    nodes.forEach { node ->
        val label = getNodeLabel(node.entity)
        if (label.isNotEmpty()) {
            val radius = getNodeRadius(node)
            val labelPosition = node.position + Offset(0f, radius + 15f)
            
            val textStyle = TextStyle(
                color = colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(label),
                style = textStyle
            )
            
            // Draw text background
            drawRect(
                color = colorScheme.surface.copy(alpha = 0.8f),
                topLeft = labelPosition - Offset(textLayoutResult.size.width / 2f, 0f),
                size = Size(textLayoutResult.size.width.toFloat(), textLayoutResult.size.height.toFloat())
            )
            
            // Draw text
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = labelPosition - Offset(textLayoutResult.size.width / 2f, 0f)
            )
        }
    }
}

/**
 * Get display label for a node
 */
private fun getNodeLabel(entity: com.bscan.model.graph.Entity): String {
    return entity.getProperty<String>("name") 
        ?: entity.getProperty<String>("label")
        ?: entity.getProperty<String>("material")
        ?: entity::class.simpleName?.take(8) 
        ?: "Entity"
}

/**
 * Draw legend showing entity types and colors
 */
private fun DrawScope.drawLegend(
    colorScheme: androidx.compose.material3.ColorScheme,
    textMeasurer: TextMeasurer,
    typography: androidx.compose.material3.Typography
) {
    val legendItems = listOf(
        "Physical" to Color(0xFF4CAF50),
        "Inventory" to Color(0xFF2196F3),
        "Activity" to Color(0xFFFF9800),
        "Information" to Color(0xFF9C27B0),
        "Identifier" to Color(0xFFF44336),
        "Location" to Color(0xFF795548)
    )
    
    val padding = 16f
    val itemHeight = 24f
    val legendWidth = 120f
    val legendHeight = legendItems.size * itemHeight + padding * 2
    
    // Draw legend background
    drawRect(
        color = colorScheme.surface.copy(alpha = 0.9f),
        topLeft = Offset(size.width - legendWidth - padding, padding),
        size = Size(legendWidth, legendHeight)
    )
    
    // Draw legend border
    drawRect(
        color = colorScheme.outline,
        topLeft = Offset(size.width - legendWidth - padding, padding),
        size = Size(legendWidth, legendHeight),
        style = Stroke(width = 1f)
    )
    
    // Draw legend items
    legendItems.forEachIndexed { index, (label, color) ->
        val y = padding + padding + index * itemHeight
        val x = size.width - legendWidth
        
        // Draw color indicator
        drawCircle(
            color = color,
            radius = 6f,
            center = Offset(x, y + itemHeight / 2)
        )
        
        // Draw label
        val textStyle = TextStyle(
            color = colorScheme.onSurface,
            fontSize = 10.sp
        )
        
        val textLayout = textMeasurer.measure(
            text = AnnotatedString(label),
            style = textStyle
        )
        
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(x + 16f, y + (itemHeight - textLayout.size.height) / 2)
        )
    }
}

/**
 * Transform screen coordinates to graph coordinates
 */
private fun transformScreenToGraph(screenPos: Offset, interactionState: GraphInteractionState): Offset {
    return (screenPos - interactionState.offset) / interactionState.scale
}

/**
 * Transform graph coordinates to screen coordinates
 */
private fun transformGraphToScreen(graphPos: Offset, interactionState: GraphInteractionState): Offset {
    return graphPos * interactionState.scale + interactionState.offset
}

/**
 * Find node at the given position
 */
private fun findNodeAtPosition(nodes: List<GraphNode>, position: Offset): GraphNode? {
    return nodes.find { node ->
        val distance = (node.position - position).getDistance()
        distance <= getNodeRadius(node)
    }
}

/**
 * Get distance between two offsets
 */
private fun Offset.getDistance(): Float {
    return sqrt(x * x + y * y)
}