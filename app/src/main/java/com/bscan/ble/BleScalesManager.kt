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
    
    // Discovered devices
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()
    
    // Scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    // Connection state
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
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
     * Cleanup resources
     */
    fun cleanup() {
        stopScanning()
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

/**
 * BLE connection states
 */
enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}