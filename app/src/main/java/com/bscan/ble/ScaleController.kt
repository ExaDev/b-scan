package com.bscan.ble

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for controlling BLE scales
 * Provides weight reading, tare functionality, and scale management
 */
interface ScaleController {
    
    // State flows for reactive UI
    val currentReading: StateFlow<ScaleReading?>
    val connectionState: StateFlow<ScaleConnectionState>
    val isReading: StateFlow<Boolean>
    
    /**
     * Connect to a specific BLE scale device
     */
    suspend fun connect(device: DiscoveredBleDevice): ScaleCommandResult
    
    /**
     * Disconnect from current scale
     */
    suspend fun disconnect(): ScaleCommandResult
    
    /**
     * Start continuous weight reading from scale
     */
    suspend fun startContinuousReading(): ScaleCommandResult
    
    /**
     * Stop continuous weight reading
     */
    suspend fun stopContinuousReading(): ScaleCommandResult
    
    /**
     * Send tare (zero) command to scale
     */
    suspend fun tareScale(): ScaleCommandResult
    
    /**
     * Change weight unit if supported by scale
     */
    suspend fun setUnit(unit: WeightUnit): ScaleCommandResult
    
    /**
     * Get single weight reading (non-continuous)
     */
    suspend fun getSingleReading(): ScaleReading?
    
    /**
     * Check if scale supports specific functionality
     */
    fun supportsCommand(command: ScaleCommand): Boolean
    
    /**
     * Get scale device information
     */
    fun getDeviceInfo(): DiscoveredBleDevice?
    
    /**
     * Cleanup resources
     */
    fun cleanup()
}

/**
 * Factory for creating scale controllers based on device configuration
 */
object ScaleControllerFactory {
    
    fun createController(device: DiscoveredBleDevice): ScaleController {
        return when (device.scaleConfig?.id) {
            "generic_ffe0" -> FFE0ScaleController(device)
            "standard_weight_scale" -> StandardWeightScaleController(device)
            else -> FFE0ScaleController(device) // Default to FFE0 for unknown devices
        }
    }
}