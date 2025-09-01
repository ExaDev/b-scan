package com.bscan.ui.components.graph

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Force-directed layout engine for graph visualization.
 * 
 * Implements a physics-based algorithm that positions nodes using:
 * - Repulsive forces between all nodes (Coulomb's law)
 * - Attractive forces between connected nodes (spring force)
 * - Clustering forces for grouped nodes
 * - Damping to stabilize the simulation
 */
class GraphLayoutEngine {
    
    companion object {
        // Physics parameters
        private const val REPULSION_STRENGTH = 1000f
        private const val ATTRACTION_STRENGTH = 0.1f
        private const val CLUSTER_ATTRACTION = 0.05f
        private const val DAMPING_FACTOR = 0.9f
        private const val MIN_DISTANCE = 10f
        private const val MAX_FORCE = 100f
        private const val TIME_STEP = 1f
        
        // Layout parameters
        private const val INITIAL_SPREAD = 300f
        private const val CLUSTER_SPACING = 200f
        
        // Simulation control
        private const val MAX_ITERATIONS = 1000
        private const val CONVERGENCE_THRESHOLD = 1f
        private const val UPDATE_INTERVAL = 16L // ~60fps
    }
    
    private var simulationJob: Job? = null
    private var isRunning = false
    
    /**
     * Initialize node positions randomly or based on clusters
     */
    fun initializeLayout(layout: GraphLayout) {
        val clusterCenters = mutableMapOf<String, Offset>()
        
        // Calculate cluster centers if clusters exist
        layout.clusters.forEachIndexed { index, cluster ->
            val angle = (2 * PI * index / layout.clusters.size).toFloat()
            val x = cos(angle) * CLUSTER_SPACING
            val y = sin(angle) * CLUSTER_SPACING
            clusterCenters[cluster.id] = Offset(x, y)
        }
        
        // Position nodes
        layout.nodes.forEach { node ->
            node.position = if (node.clusterId != null && clusterCenters.containsKey(node.clusterId)) {
                // Position around cluster center
                val clusterCenter = clusterCenters[node.clusterId]!!
                val randomOffset = Offset(
                    Random.nextFloat() * 100 - 50,
                    Random.nextFloat() * 100 - 50
                )
                clusterCenter + randomOffset
            } else {
                // Random position
                Offset(
                    Random.nextFloat() * INITIAL_SPREAD - INITIAL_SPREAD / 2,
                    Random.nextFloat() * INITIAL_SPREAD - INITIAL_SPREAD / 2
                )
            }
            
            node.velocity = Offset.Zero
        }
    }
    
    /**
     * Start the physics simulation
     */
    fun startSimulation(
        layout: GraphLayout,
        onUpdate: (GraphLayout) -> Unit,
        scope: CoroutineScope
    ) {
        stopSimulation()
        
        simulationJob = scope.launch(Dispatchers.Default) {
            isRunning = true
            var iteration = 0
            
            while (isRunning && iteration < MAX_ITERATIONS) {
                val totalMovement = simulateStep(layout)
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    onUpdate(layout)
                }
                
                // Check for convergence
                if (totalMovement < CONVERGENCE_THRESHOLD) {
                    break
                }
                
                iteration++
                delay(UPDATE_INTERVAL)
            }
            
            isRunning = false
        }
    }
    
    /**
     * Stop the physics simulation
     */
    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        isRunning = false
    }
    
    /**
     * Perform one simulation step and return total node movement
     */
    private fun simulateStep(layout: GraphLayout): Float {
        val forces = mutableMapOf<String, Offset>()
        
        // Initialize forces
        layout.nodes.forEach { node ->
            forces[node.id] = Offset.Zero
        }
        
        // Calculate repulsive forces between all node pairs
        for (i in layout.nodes.indices) {
            for (j in i + 1 until layout.nodes.size) {
                val node1 = layout.nodes[i]
                val node2 = layout.nodes[j]
                
                val repulsiveForce = calculateRepulsiveForce(node1.position, node2.position)
                
                forces[node1.id] = forces[node1.id]!! + repulsiveForce
                forces[node2.id] = forces[node2.id]!! - repulsiveForce
            }
        }
        
        // Calculate attractive forces between connected nodes
        layout.edges.forEach { edge ->
            val fromNode = layout.nodes.find { it.id == edge.fromNodeId }
            val toNode = layout.nodes.find { it.id == edge.toNodeId }
            
            if (fromNode != null && toNode != null) {
                // Use default ideal length based on relationship type
                val idealLength = when (edge.relationshipType) {
                    "CONTAINS" -> 60f
                    "IDENTIFIED_BY" -> 80f
                    "TRACKS" -> 100f
                    else -> 120f
                }
                
                val attractiveForce = calculateAttractiveForce(
                    fromNode.position, 
                    toNode.position,
                    idealLength
                )
                
                forces[fromNode.id] = forces[fromNode.id]!! + attractiveForce
                forces[toNode.id] = forces[toNode.id]!! - attractiveForce
            }
        }
        
        // Calculate cluster cohesion forces
        layout.clusters.forEach { cluster ->
            val clusterNodes = layout.nodes.filter { it.clusterId == cluster.id }
            if (clusterNodes.size > 1) {
                val clusterCenter = calculateClusterCenter(clusterNodes)
                
                clusterNodes.forEach { node ->
                    val clusterForce = calculateClusterForce(node.position, clusterCenter)
                    forces[node.id] = forces[node.id]!! + clusterForce
                }
            }
        }
        
        // Apply forces and update positions
        var totalMovement = 0f
        
        layout.nodes.forEach { node ->
            val force = forces[node.id] ?: Offset.Zero
            val clampedForce = clampForce(force)
            
            // Update velocity (with damping)
            node.velocity = (node.velocity + clampedForce * TIME_STEP) * DAMPING_FACTOR
            
            // Update position
            val oldPosition = node.position
            node.position = node.position + node.velocity * TIME_STEP
            
            // Calculate movement for convergence check
            val movement = (node.position - oldPosition).getDistance()
            totalMovement += movement
        }
        
        return totalMovement
    }
    
    /**
     * Calculate repulsive force between two nodes (Coulomb's law)
     */
    private fun calculateRepulsiveForce(pos1: Offset, pos2: Offset): Offset {
        val delta = pos1 - pos2
        val distance = maxOf(delta.getDistance(), MIN_DISTANCE)
        val force = REPULSION_STRENGTH / (distance * distance)
        
        return if (distance > 0) {
            delta / distance * force
        } else {
            // Random direction if nodes are at same position
            val randomAngle = Random.nextFloat() * 2 * PI.toFloat()
            Offset(cos(randomAngle) * force, sin(randomAngle) * force)
        }
    }
    
    /**
     * Calculate attractive force between connected nodes (spring force)
     */
    private fun calculateAttractiveForce(pos1: Offset, pos2: Offset, idealLength: Float): Offset {
        val delta = pos2 - pos1
        val distance = delta.getDistance()
        val displacement = distance - idealLength
        val force = ATTRACTION_STRENGTH * displacement
        
        return if (distance > 0) {
            delta / distance * force
        } else {
            Offset.Zero
        }
    }
    
    /**
     * Calculate cluster cohesion force
     */
    private fun calculateClusterForce(nodePosition: Offset, clusterCenter: Offset): Offset {
        val delta = clusterCenter - nodePosition
        val distance = delta.getDistance()
        
        return if (distance > 0) {
            delta / distance * CLUSTER_ATTRACTION * distance
        } else {
            Offset.Zero
        }
    }
    
    /**
     * Calculate the center point of a cluster
     */
    private fun calculateClusterCenter(nodes: List<GraphNode>): Offset {
        if (nodes.isEmpty()) return Offset.Zero
        
        val sumX = nodes.sumOf { it.position.x.toDouble() }.toFloat()
        val sumY = nodes.sumOf { it.position.y.toDouble() }.toFloat()
        
        return Offset(sumX / nodes.size, sumY / nodes.size)
    }
    
    /**
     * Clamp force magnitude to prevent instability
     */
    private fun clampForce(force: Offset): Offset {
        val magnitude = force.getDistance()
        return if (magnitude > MAX_FORCE) {
            force / magnitude * MAX_FORCE
        } else {
            force
        }
    }
    
    /**
     * Get the distance between two points
     */
    private fun Offset.getDistance(): Float {
        return sqrt(x * x + y * y)
    }
    
    /**
     * Check if simulation is currently running
     */
    fun isSimulationRunning(): Boolean = isRunning
    
    /**
     * Get current simulation statistics
     */
    fun getSimulationStats(layout: GraphLayout): SimulationStats {
        val totalEnergy = layout.nodes.sumOf { 
            val velocity = it.velocity.getDistance()
            (velocity * velocity / 2).toDouble()
        }.toFloat()
        
        val boundingBox = calculateBoundingBox(layout.nodes)
        
        return SimulationStats(
            totalEnergy = totalEnergy,
            nodeCount = layout.nodes.size,
            edgeCount = layout.edges.size,
            boundingBox = boundingBox,
            isRunning = isRunning
        )
    }
    
    /**
     * Calculate the bounding box of all nodes
     */
    private fun calculateBoundingBox(nodes: List<GraphNode>): BoundingBox {
        if (nodes.isEmpty()) {
            return BoundingBox(0f, 0f, 0f, 0f)
        }
        
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        
        nodes.forEach { node ->
            minX = minOf(minX, node.position.x)
            minY = minOf(minY, node.position.y)
            maxX = maxOf(maxX, node.position.x)
            maxY = maxOf(maxY, node.position.y)
        }
        
        return BoundingBox(minX, minY, maxX, maxY)
    }
}

/**
 * Statistics about the current simulation state
 */
data class SimulationStats(
    val totalEnergy: Float,
    val nodeCount: Int,
    val edgeCount: Int,
    val boundingBox: BoundingBox,
    val isRunning: Boolean
)

/**
 * Bounding box for layout calculations
 */
data class BoundingBox(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val center: Offset get() = Offset((minX + maxX) / 2, (minY + maxY) / 2)
}