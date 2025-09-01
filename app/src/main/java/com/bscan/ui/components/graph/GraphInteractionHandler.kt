package com.bscan.ui.components.graph

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Handles all touch interactions for graph visualization including:
 * - Pan (drag to move viewport)
 * - Zoom (pinch to scale, double-tap to zoom)
 * - Node selection and dragging
 * - Multi-touch gesture recognition
 */
class GraphInteractionHandler(
    private val interactionState: GraphInteractionState,
    private val onNodeTapped: (GraphNode) -> Unit = {},
    private val onNodeDragged: (GraphNode, Offset) -> Unit = { _, _ -> },
    private val onNodeSelected: (GraphNode?) -> Unit = {},
    private val onCanvasTapped: (Offset) -> Unit = {},
    private val onZoomChanged: (Float) -> Unit = {},
    private val onPanChanged: (Offset) -> Unit = {}
) {
    companion object {
        private const val MIN_SCALE = 0.1f
        private const val MAX_SCALE = 5f
        private const val ZOOM_SENSITIVITY = 1.2f
        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val LONG_PRESS_TIMEOUT = 500L
        private const val DRAG_THRESHOLD = 10f
    }
    
    // State for gesture detection
    private var lastTapTime = 0L
    private var longPressJob: kotlinx.coroutines.Job? = null
    private var isDraggingNode = false
    private var dragStartPosition = Offset.Zero
    private var initialNodePosition = Offset.Zero
    
    /**
     * Create gesture modifier for the canvas
     */
    fun createGestureModifier(layout: GraphLayout): Modifier {
        return Modifier
            .pointerInput(layout) {
                detectTapGestures(
                    onTap = { offset ->
                        val transformedOffset = transformScreenToGraph(offset)
                        val tappedNode = findNodeAtPosition(layout.nodes, transformedOffset)
                        
                        if (tappedNode != null) {
                            // Handle node selection through callbacks
                            onNodeTapped(tappedNode)
                            onNodeSelected(tappedNode)
                        } else {
                            // Handle canvas tap
                            onNodeSelected(null)
                            onCanvasTapped(transformedOffset)
                        }
                    },
                    onDoubleTap = { offset ->
                        handleDoubleTap(offset)
                    },
                    onLongPress = { offset ->
                        val transformedOffset = transformScreenToGraph(offset)
                        val longPressedNode = findNodeAtPosition(layout.nodes, transformedOffset)
                        if (longPressedNode != null) {
                            handleLongPress(longPressedNode, transformedOffset)
                        }
                    }
                )
            }
            .pointerInput(layout) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val transformedOffset = transformScreenToGraph(offset)
                        val draggedNode = findNodeAtPosition(layout.nodes, transformedOffset)
                        isDraggingNode = draggedNode != null
                        if (draggedNode != null) {
                            dragStartPosition = offset
                            initialNodePosition = draggedNode.position
                        }
                    },
                    onDragEnd = {
                        isDraggingNode = false
                    },
                    onDrag = { _, dragAmount ->
                        if (isDraggingNode) {
                            // Node dragging - handled via callback
                            val transformedOffset = transformScreenToGraph(dragStartPosition)
                            val draggedNode = findNodeAtPosition(layout.nodes, transformedOffset)
                            if (draggedNode != null) {
                                onNodeDragged(draggedNode, dragAmount)
                            }
                        } else {
                            // Canvas panning
                            onPanChanged(dragAmount)
                        }
                    }
                )
            }
    }
    
    /**
     * Handle double tap events
     */
    private fun handleDoubleTap(position: Offset) {
        // Zoom in/out on double tap
        val targetScale = if (interactionState.scale < 1f) {
            1f
        } else if (interactionState.scale < 2f) {
            2f
        } else {
            0.5f
        }
        
        applyZoom(targetScale, position)
    }
    
    /**
     * Handle long press events
     */
    private fun handleLongPress(node: GraphNode, position: Offset) {
        // Long press selects node and could trigger context menu
        onNodeSelected(node)
        
        // Could trigger haptic feedback here
        // HapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    /**
     * Apply zoom with center point compensation
     */
    private fun applyZoom(targetScale: Float, center: Offset) {
        val clampedScale = targetScale.coerceIn(MIN_SCALE, MAX_SCALE)
        
        onZoomChanged(clampedScale)
        onPanChanged(Offset.Zero) // Simplified for now
    }
    
    /**
     * Transform screen coordinates to graph coordinates
     */
    private fun transformScreenToGraph(screenPos: Offset): Offset {
        return (screenPos - interactionState.offset) / interactionState.scale
    }
    
    /**
     * Transform graph coordinates to screen coordinates
     */
    private fun transformGraphToScreen(graphPos: Offset): Offset {
        return graphPos * interactionState.scale + interactionState.offset
    }
    
    /**
     * Find node at the given graph position
     */
    private fun findNodeAtPosition(nodes: List<GraphNode>, position: Offset): GraphNode? {
        return nodes.find { node ->
            val distance = (node.position - position).getDistance()
            val radius = getNodeRadius(node)
            distance <= radius
        }
    }
    
    /**
     * Get node radius (same as in visualization canvas)
     */
    private fun getNodeRadius(node: GraphNode): Float {
        val baseRadius = when (node.entity) {
            is com.bscan.model.graph.entities.PhysicalComponent -> 25f
            is com.bscan.model.graph.entities.InventoryItem -> 30f
            is com.bscan.model.graph.entities.Activity -> 20f
            is com.bscan.model.graph.entities.Information -> 18f
            is com.bscan.model.graph.entities.Identifier -> 15f
            is com.bscan.model.graph.entities.Location -> 28f
            is com.bscan.model.graph.entities.Person -> 22f
            is com.bscan.model.graph.entities.Virtual -> 16f
            else -> 20f
        }
        
        val importance = node.entity.getProperty<Float>("importance") ?: 1f
        return baseRadius * importance.coerceIn(0.5f, 2f)
    }
    
    /**
     * Get distance between two offsets
     */
    private fun Offset.getDistance(): Float {
        return sqrt(x * x + y * y)
    }
    
    /**
     * Programmatically zoom to fit all nodes in view
     */
    fun zoomToFit(
        layout: GraphLayout,
        canvasSize: androidx.compose.ui.geometry.Size,
        padding: Float = 50f
    ) {
        if (layout.nodes.isEmpty()) return
        
        // Calculate bounding box of all nodes
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        
        layout.nodes.forEach { node ->
            val radius = getNodeRadius(node)
            minX = minOf(minX, node.position.x - radius)
            minY = minOf(minY, node.position.y - radius)
            maxX = maxOf(maxX, node.position.x + radius)
            maxY = maxOf(maxY, node.position.y + radius)
        }
        
        val contentWidth = maxX - minX
        val contentHeight = maxY - minY
        
        if (contentWidth <= 0 || contentHeight <= 0) return
        
        // Calculate scale to fit content with padding
        val availableWidth = canvasSize.width - padding * 2
        val availableHeight = canvasSize.height - padding * 2
        val scaleX = availableWidth / contentWidth
        val scaleY = availableHeight / contentHeight
        val fitScale = minOf(scaleX, scaleY).coerceIn(MIN_SCALE, MAX_SCALE)
        
        // Apply transformations via callbacks
        onZoomChanged(fitScale)
        onPanChanged(Offset.Zero) // Simplified for now
    }
    
    /**
     * Programmatically zoom to a specific node
     */
    fun zoomToNode(
        node: GraphNode,
        canvasSize: androidx.compose.ui.geometry.Size,
        targetScale: Float = 2f
    ) {
        val clampedScale = targetScale.coerceIn(MIN_SCALE, MAX_SCALE)
        
        // Select the node and apply zoom
        onNodeSelected(node)
        onZoomChanged(clampedScale)
        onPanChanged(Offset.Zero) // Simplified for now
    }
    
    /**
     * Reset zoom and pan to default state
     */
    fun resetView() {
        onZoomChanged(1f)
        onPanChanged(Offset.Zero)
        onNodeSelected(null)
    }
}