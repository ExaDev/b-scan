package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.ble.BleScalesManager
import com.bscan.model.SpoolWeight
import com.bscan.model.WeightMeasurementType
import com.bscan.repository.SpoolWeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Service that coordinates BLE scales and weight data storage
 * This bridges the BLE hardware layer with the data persistence layer
 */
class WeightTrackingService(context: Context) {
    
    companion object {
        private const val TAG = "WeightTrackingService"
    }
    
    private val bleScalesManager = BleScalesManager(context)
    private val spoolWeightRepository = SpoolWeightRepository(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Current active spool for weight tracking
    private val _activeSpoolFlow = MutableStateFlow<ActiveSpool?>(null)
    val activeSpoolFlow: StateFlow<ActiveSpool?> = _activeSpoolFlow.asStateFlow()
    
    // Service state
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    
    init {
        // Listen for weight measurements from BLE scales
        scope.launch {
            bleScalesManager.weightMeasurementFlow.collect { measurement ->
                measurement?.let { (deviceId, weight) ->
                    handleWeightMeasurement(deviceId, weight)
                }
            }
        }
    }
    
    /**
     * Represents the currently active spool for weight tracking
     */
    data class ActiveSpool(
        val spoolId: String,
        val trayUid: String,
        val tagUid: String,
        val filamentType: String,
        val colorName: String,
        val activatedAt: LocalDateTime = LocalDateTime.now()
    )
    
    /**
     * Starts the weight tracking service
     */
    fun start() {
        if (_isServiceRunning.value) {
            Log.d(TAG, "Service already running")
            return
        }
        
        Log.d(TAG, "Starting weight tracking service")
        _isServiceRunning.value = true
        
        // Start BLE scanning for scales
        bleScalesManager.startScan()
    }
    
    /**
     * Stops the weight tracking service
     */
    fun stop() {
        if (!_isServiceRunning.value) return
        
        Log.d(TAG, "Stopping weight tracking service")
        _isServiceRunning.value = false
        
        // Stop BLE scanning and disconnect devices
        bleScalesManager.stopScan()
        
        // Clear active spool
        _activeSpoolFlow.value = null
    }
    
    /**
     * Sets the active spool for weight tracking (called after NFC scan)
     */
    fun setActiveSpool(
        trayUid: String,
        tagUid: String,
        filamentType: String,
        colorName: String
    ) {
        val spoolId = spoolWeightRepository.createSpoolId(trayUid, tagUid)
        
        val activeSpool = ActiveSpool(
            spoolId = spoolId,
            trayUid = trayUid,
            tagUid = tagUid,
            filamentType = filamentType,
            colorName = colorName
        )
        
        _activeSpoolFlow.value = activeSpool
        Log.d(TAG, "Set active spool: $spoolId ($filamentType - $colorName)")
        
        // Auto-start service if not running
        if (!_isServiceRunning.value) {
            start()
        }
    }
    
    /**
     * Clears the active spool (stops tracking current spool)
     */
    fun clearActiveSpool() {
        _activeSpoolFlow.value = null
        Log.d(TAG, "Cleared active spool")
    }
    
    /**
     * Manually records a weight measurement for the active spool
     */
    fun recordManualWeight(deviceId: String, weight: Float) {
        val activeSpool = _activeSpoolFlow.value
        if (activeSpool == null) {
            Log.w(TAG, "No active spool for manual weight recording")
            return
        }
        
        recordWeight(
            spoolId = activeSpool.spoolId,
            trayUid = activeSpool.trayUid,
            tagUid = activeSpool.tagUid,
            weight = weight,
            deviceId = deviceId,
            measurementType = WeightMeasurementType.MANUAL
        )
    }
    
    /**
     * Gets discovered BLE scales devices
     */
    fun getDiscoveredDevices() = bleScalesManager.discoveredDevicesFlow
    
    /**
     * Gets connection states for BLE devices
     */
    fun getConnectionStates() = bleScalesManager.connectionStateFlow
    
    /**
     * Gets battery levels for connected devices
     */
    fun getBatteryLevels() = bleScalesManager.batteryLevelFlow
    
    /**
     * Connects to a BLE scales device
     */
    fun connectToDevice(deviceId: String) {
        Log.d(TAG, "Connecting to scales device: $deviceId")
        bleScalesManager.connectToDevice(deviceId)
    }
    
    /**
     * Disconnects from a BLE scales device
     */
    fun disconnectFromDevice(deviceId: String) {
        Log.d(TAG, "Disconnecting from scales device: $deviceId")
        bleScalesManager.disconnectFromDevice(deviceId)
    }
    
    /**
     * Starts scanning for BLE scales devices
     */
    fun scanForDevices() {
        bleScalesManager.startScan()
    }
    
    /**
     * Gets weight history for a specific spool
     */
    fun getSpoolWeightHistory(spoolId: String) = spoolWeightRepository.getWeightsForSpool(spoolId)
    
    /**
     * Gets weight statistics for a specific spool
     */
    fun getSpoolStats(spoolId: String) = spoolWeightRepository.getSpoolStats(spoolId)
    
    /**
     * Gets the latest weight for a specific spool
     */
    fun getLatestWeight(spoolId: String) = spoolWeightRepository.getLatestWeight(spoolId)
    
    /**
     * Creates a combined flow of weight updates and active spool changes
     */
    fun getWeightUpdatesForActiveSpool(): Flow<SpoolWeight?> {
        return combine(
            _activeSpoolFlow,
            spoolWeightRepository.weightUpdatesFlow
        ) { activeSpool, weightUpdate ->
            if (activeSpool != null && weightUpdate?.spoolId == activeSpool.spoolId) {
                weightUpdate
            } else {
                null
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up weight tracking service")
        stop()
        bleScalesManager.cleanup()
    }
    
    private fun handleWeightMeasurement(deviceId: String, weight: Float) {
        val activeSpool = _activeSpoolFlow.value
        if (activeSpool == null) {
            Log.d(TAG, "Received weight measurement but no active spool: ${weight}g from $deviceId")
            return
        }
        
        Log.d(TAG, "Recording weight for active spool ${activeSpool.spoolId}: ${weight}g from $deviceId")
        
        recordWeight(
            spoolId = activeSpool.spoolId,
            trayUid = activeSpool.trayUid,
            tagUid = activeSpool.tagUid,
            weight = weight,
            deviceId = deviceId,
            measurementType = WeightMeasurementType.AUTOMATIC
        )
    }
    
    private fun recordWeight(
        spoolId: String,
        trayUid: String,
        tagUid: String,
        weight: Float,
        deviceId: String,
        measurementType: WeightMeasurementType
    ) {
        // Get device name for better tracking
        val deviceName = bleScalesManager.discoveredDevicesFlow.value
            .find { it.deviceId == deviceId }?.name ?: "Unknown Scales"
        
        // Get battery level if available
        val batteryLevel = bleScalesManager.batteryLevelFlow.value[deviceId]
        
        val spoolWeight = SpoolWeight(
            spoolId = spoolId,
            trayUid = trayUid,
            tagUid = tagUid,
            weightGrams = weight,
            timestamp = LocalDateTime.now(),
            deviceId = deviceId,
            deviceName = deviceName,
            batteryLevel = batteryLevel,
            measurementType = measurementType
        )
        
        spoolWeightRepository.recordWeight(spoolWeight)
        
        Log.i(TAG, "Recorded weight: ${weight}g for spool $spoolId using $deviceName")
    }
}