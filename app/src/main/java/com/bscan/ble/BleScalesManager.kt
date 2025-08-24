package com.bscan.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.bscan.model.BleScalesConfig
import com.bscan.model.BleScalesConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Manages BLE scales discovery, connection, and data parsing
 * Following the app's established architecture patterns
 */
class BleScalesManager(private val context: Context) {
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private val handler = Handler(Looper.getMainLooper())
    private val scanTimeoutMs = 30000L // 30 seconds
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Discovered devices
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()
    
    // Scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    // Current connected scale controller
    private var currentScaleController: ScaleController? = null
    
    // Current weight reading
    private val _currentReading = MutableStateFlow<ScaleReading?>(null)
    val currentReading: StateFlow<ScaleReading?> = _currentReading.asStateFlow()
    
    // Connection state - maintain our own state flow for consistency
    private val _connectionState = MutableStateFlow(ScaleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ScaleConnectionState> = _connectionState.asStateFlow()
    
    // Reading state - maintain our own state flow for consistency  
    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()
    
    private var scanTimeoutRunnable: Runnable? = null
    
    /**
     * Start scanning for BLE scales
     */
    fun startScanning(permissionHandler: BlePermissionHandler) {
        if (!permissionHandler.hasAllPermissions()) {
            Log.w(TAG, "Missing BLE permissions, cannot start scanning")
            permissionHandler.requestPermissions()
            return
        }
        
        if (bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG, "Bluetooth is not enabled")
            return
        }
        
        if (_isScanning.value) {
            Log.d(TAG, "Already scanning")
            return
        }
        
        Log.i(TAG, "Starting BLE scales scan")
        _isScanning.value = true
        _discoveredDevices.value = emptyList()
        
        // Create scan filters for known scale service UUIDs
        val scanFilters = createScanFilters()
        
        // Scan settings for balanced power and latency
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
        
        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            
            // Set timeout
            scanTimeoutRunnable = Runnable {
                stopScanning()
                Log.i(TAG, "BLE scan timeout reached")
            }
            handler.postDelayed(scanTimeoutRunnable!!, scanTimeoutMs)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during BLE scan: ${e.message}")
            _isScanning.value = false
        }
    }
    
    /**
     * Stop scanning for BLE devices
     */
    fun stopScanning() {
        if (!_isScanning.value) return
        
        Log.i(TAG, "Stopping BLE scales scan")
        _isScanning.value = false
        
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping BLE scan: ${e.message}")
        }
        
        scanTimeoutRunnable?.let { handler.removeCallbacks(it) }
        scanTimeoutRunnable = null
    }
    
    /**
     * Create scan filters for known BLE scales services
     */
    private fun createScanFilters(): List<ScanFilter> {
        val filters = mutableListOf<ScanFilter>()
        
        // Add filters for each known scale configuration
        BleScalesConfigs.ALL_CONFIGS.forEach { config ->
            try {
                val serviceUuid = ParcelUuid.fromString(config.serviceUuid)
                val filter = ScanFilter.Builder()
                    .setServiceUuid(serviceUuid)
                    .build()
                filters.add(filter)
                Log.d(TAG, "Added scan filter for service: ${config.serviceUuid}")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid service UUID in config ${config.id}: ${config.serviceUuid}")
            }
        }
        
        // If no specific filters, scan for all devices (less efficient but more comprehensive)
        if (filters.isEmpty()) {
            Log.d(TAG, "No service filters, scanning all BLE devices")
            // Return empty list to scan all devices
        }
        
        return filters
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            val advertisedServices = result.scanRecord?.serviceUuids?.map { it.toString() } ?: emptyList()
            
            try {
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address
                
                // Check if we've already discovered this device
                val currentDevices = _discoveredDevices.value.toMutableList()
                val existingIndex = currentDevices.indexOfFirst { it.address == deviceAddress }
                
                // Determine scale configuration if available
                val scaleConfig = findMatchingScaleConfig(advertisedServices)
                
                val discoveredDevice = DiscoveredBleDevice(
                    name = deviceName,
                    address = deviceAddress,
                    rssi = rssi,
                    advertisedServices = advertisedServices,
                    scaleConfig = scaleConfig,
                    device = device
                )
                
                if (existingIndex >= 0) {
                    // Update existing device
                    currentDevices[existingIndex] = discoveredDevice
                } else {
                    // Add new device
                    currentDevices.add(discoveredDevice)
                }
                
                _discoveredDevices.value = currentDevices.sortedByDescending { it.rssi }
                
                Log.d(TAG, "Discovered BLE device: $deviceName ($deviceAddress) RSSI: ${rssi}dBm Services: $advertisedServices")
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception accessing device info: ${e.message}")
            }
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with error code: $errorCode")
            _isScanning.value = false
        }
    }
    
    /**
     * Find matching scale configuration for advertised services
     */
    private fun findMatchingScaleConfig(advertisedServices: List<String>): BleScalesConfig? {
        return BleScalesConfigs.ALL_CONFIGS.find { config ->
            advertisedServices.any { service -> 
                service.equals(config.serviceUuid, ignoreCase = true) 
            }
        }
    }
    
    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Connect to a discovered BLE scale device
     */
    suspend fun connectToScale(device: DiscoveredBleDevice): ScaleCommandResult {
        if (currentScaleController != null) {
            Log.w(TAG, "Already connected to a scale, disconnecting first")
            disconnectFromScale()
        }
        
        Log.i(TAG, "Connecting to scale: ${device.displayName} (${device.address})")
        
        val controller = ScaleControllerFactory.createController(device)
        currentScaleController = controller
        
        val result = controller.connect(device)
        
        if (result is ScaleCommandResult.Success) {
            // Start monitoring weight readings
            scope.launch {
                controller.currentReading.collect { reading ->
                    _currentReading.value = reading
                }
            }
            
            // Start monitoring connection state
            scope.launch {
                controller.connectionState.collect { state ->
                    _connectionState.value = state
                }
            }
            
            // Start monitoring reading state
            scope.launch {
                controller.isReading.collect { reading ->
                    _isReading.value = reading
                }
            }
            
            Log.i(TAG, "Successfully connected to scale: ${device.displayName}")
        } else {
            Log.w(TAG, "Failed to connect to scale: $result")
            currentScaleController = null
            _connectionState.value = ScaleConnectionState.DISCONNECTED
            _isReading.value = false
        }
        
        return result
    }
    
    /**
     * Disconnect from current scale
     */
    suspend fun disconnectFromScale(): ScaleCommandResult {
        val controller = currentScaleController ?: return ScaleCommandResult.Success
        
        Log.i(TAG, "Disconnecting from scale")
        val result = controller.disconnect()
        
        currentScaleController = null
        _currentReading.value = null
        _connectionState.value = ScaleConnectionState.DISCONNECTED
        _isReading.value = false
        
        return result
    }
    
    /**
     * Start continuous weight reading from connected scale
     */
    suspend fun startWeightReading(): ScaleCommandResult {
        val controller = currentScaleController ?: return ScaleCommandResult.Error("No scale connected")
        return controller.startContinuousReading()
    }
    
    /**
     * Stop continuous weight reading
     */
    suspend fun stopWeightReading(): ScaleCommandResult {
        val controller = currentScaleController ?: return ScaleCommandResult.Success
        return controller.stopContinuousReading()
    }
    
    /**
     * Send tare (zero) command to connected scale
     */
    suspend fun tareScale(): ScaleCommandResult {
        val controller = currentScaleController ?: return ScaleCommandResult.Error("No scale connected")
        
        if (!controller.supportsCommand(ScaleCommand.Tare)) {
            return ScaleCommandResult.NotSupported
        }
        
        Log.i(TAG, "Sending tare command to scale")
        return controller.tareScale()
    }
    
    /**
     * Get single weight reading from connected scale
     */
    suspend fun getSingleReading(): ScaleReading? {
        val controller = currentScaleController ?: return null
        return controller.getSingleReading()
    }
    
    /**
     * Enable passive monitoring for unit detection (notifications only - no reads)
     */
    suspend fun enableUnitDetectionMonitoring(): ScaleCommandResult {
        val controller = currentScaleController as? FFE0ScaleController
            ?: return ScaleCommandResult.Error("Not connected to FFE0 scale")
        
        return controller.enableUnitDetectionMonitoring()
    }
    
    /**
     * Check if currently connected to a scale
     */
    fun isConnectedToScale(): Boolean {
        return currentScaleController != null && 
               _connectionState.value in listOf(ScaleConnectionState.CONNECTED, ScaleConnectionState.READING)
    }
    
    /**
     * Get currently connected scale device info
     */
    fun getConnectedScaleInfo(): DiscoveredBleDevice? {
        return currentScaleController?.getDeviceInfo()
    }
    
    /**
     * Attempt to reconnect to previously configured scale
     */
    suspend fun reconnectToStoredScale(
        storedAddress: String,
        storedName: String,
        permissionHandler: BlePermissionHandler
    ): ScaleCommandResult {
        if (!permissionHandler.hasAllPermissions()) {
            return ScaleCommandResult.Error("Missing BLE permissions")
        }
        
        if (bluetoothAdapter?.isEnabled != true) {
            return ScaleCommandResult.Error("Bluetooth is not enabled")
        }
        
        try {
            // Try to get the device directly by address (works for previously discovered devices)
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(storedAddress)
                ?: return ScaleCommandResult.Error("Cannot create device from stored address")
            
            // Create a DiscoveredBleDevice from the stored info
            val discoveredDevice = DiscoveredBleDevice(
                name = storedName,
                address = storedAddress,
                rssi = -50, // Placeholder RSSI for stored device
                advertisedServices = emptyList(), // Will be discovered during connection
                scaleConfig = BleScalesConfigs.ALL_CONFIGS.firstOrNull { it.id == "generic_ffe0" }, // Default to FFE0
                device = bluetoothDevice
            )
            
            Log.i(TAG, "Attempting to reconnect to stored scale: $storedName ($storedAddress)")
            return connectToScale(discoveredDevice)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during reconnection: ${e.message}")
            return ScaleCommandResult.Error("Permission denied during reconnection")
        } catch (e: Exception) {
            Log.e(TAG, "Error during reconnection: ${e.message}", e)
            return ScaleCommandResult.Error("Reconnection failed: ${e.message}")
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopScanning()
        scope.launch {
            disconnectFromScale()
        }
        scope.cancel()
    }
    
    companion object {
        private const val TAG = "BleScalesManager"
    }
}

/**
 * Discovered BLE device with scale-specific information
 */
data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val advertisedServices: List<String>,
    val scaleConfig: BleScalesConfig?,
    val device: BluetoothDevice
) {
    val isKnownScale: Boolean
        get() = scaleConfig != null
        
    val displayName: String
        get() = if (name.isBlank() || name == "Unknown Device") {
            scaleConfig?.name ?: "BLE Device"
        } else {
            name
        }
}

