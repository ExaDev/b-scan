package com.bscan.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import com.bscan.model.BleScalesDevice
import com.bscan.model.SpoolWeight
import com.bscan.model.WeightMeasurementType
import com.bscan.model.BleScalesConfig
import com.bscan.model.BleScalesConfigs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BleScalesManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BleScalesManager"
        
        // Common BLE scales service UUIDs (these may need adjustment based on actual scales)
        private val WEIGHT_SERVICE_UUID = UUID.fromString("0000181d-0000-1000-8000-00805f9b34fb") // Weight Scale Service
        private val WEIGHT_MEASUREMENT_UUID = UUID.fromString("00002a9d-0000-1000-8000-00805f9b34fb") // Weight Measurement
        private val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb") // Battery Service
        private val BATTERY_LEVEL_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb") // Battery Level
        
        // Client Characteristic Configuration Descriptor
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        
        private const val SCAN_TIMEOUT_MS = 10000L // Reduced from 30s to 10s since we're using service filters
        private const val CONNECTION_TIMEOUT_MS = 10000L
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Discovered devices
    private val discoveredDevices = ConcurrentHashMap<String, BleScalesDevice>()
    
    // Connected devices and their GATT connections
    private val connectedGatts = ConcurrentHashMap<String, BluetoothGatt>()
    
    // Device configurations - maps device ID to scale config
    private val deviceConfigs = ConcurrentHashMap<String, BleScalesConfig>()
    
    // StateFlows for reactive updates
    private val _discoveredDevicesFlow = MutableStateFlow<List<BleScalesDevice>>(emptyList())
    val discoveredDevicesFlow: StateFlow<List<BleScalesDevice>> = _discoveredDevicesFlow.asStateFlow()
    
    private val _connectionStateFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val connectionStateFlow: StateFlow<Map<String, Boolean>> = _connectionStateFlow.asStateFlow()
    
    private val _weightMeasurementFlow = MutableStateFlow<Pair<String, Float>?>(null)
    val weightMeasurementFlow: StateFlow<Pair<String, Float>?> = _weightMeasurementFlow.asStateFlow()
    
    private val _batteryLevelFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    val batteryLevelFlow: StateFlow<Map<String, Int>> = _batteryLevelFlow.asStateFlow()
    
    private var isScanning = false
    
    init {
        // Check if BLE is supported
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "BLE is not supported on this device")
        }
    }
    
    /**
     * Starts scanning for BLE scales devices
     */
    fun startScan(useServiceFilters: Boolean = true) {
        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled")
            return
        }
        
        if (!hasPermissions()) {
            Log.w(TAG, "Missing required BLE permissions")
            return
        }
        
        if (isScanning) {
            Log.d(TAG, "Already scanning")
            return
        }
        
        Log.d(TAG, "Starting BLE scan for scales devices (filtered: $useServiceFilters)")
        
        // Create scan filters for known scale service UUIDs (or empty for unfiltered scan)
        val scanFilters = if (useServiceFilters) {
            BleScalesConfigs.ALL_CONFIGS.map { config ->
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(UUID.fromString(config.serviceUuid)))
                    .build()
            }
        } else {
            emptyList()
        }
        
        if (useServiceFilters) {
            Log.d(TAG, "Scanning for ${scanFilters.size} known scale service UUIDs: ${BleScalesConfigs.ALL_CONFIGS.joinToString(", ") { it.serviceUuid }}")
        } else {
            Log.d(TAG, "Scanning for all BLE devices (unfiltered mode for debugging)")
        }
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
        
        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            isScanning = true
            
            // Auto-stop scan after timeout
            scope.launch {
                delay(SCAN_TIMEOUT_MS)
                stopScan()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during scan: ${e.message}")
        }
    }
    
    /**
     * Stops BLE scanning
     */
    fun stopScan() {
        if (!isScanning) return
        
        Log.d(TAG, "Stopping BLE scan")
        
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping scan: ${e.message}")
        }
        
        isScanning = false
    }
    
    /**
     * Connects to a BLE scales device
     */
    fun connectToDevice(deviceId: String) {
        val device = bluetoothAdapter?.getRemoteDevice(deviceId)
        if (device == null) {
            Log.e(TAG, "Device not found: $deviceId")
            return
        }
        
        if (connectedGatts.containsKey(deviceId)) {
            Log.d(TAG, "Already connected to device: $deviceId")
            return
        }
        
        Log.d(TAG, "Connecting to device: $deviceId")
        
        try {
            val gatt = device.connectGatt(context, false, gattCallback)
            connectedGatts[deviceId] = gatt
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception connecting to device: ${e.message}")
        }
    }
    
    /**
     * Disconnects from a BLE scales device
     */
    fun disconnectFromDevice(deviceId: String) {
        Log.d(TAG, "Disconnecting from device: $deviceId")
        
        connectedGatts[deviceId]?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception disconnecting: ${e.message}")
            }
        }
        
        connectedGatts.remove(deviceId)
        updateConnectionState(deviceId, false)
    }
    
    /**
     * Gets the current list of discovered devices
     */
    fun getDiscoveredDevices(): List<BleScalesDevice> {
        return discoveredDevices.values.toList().sortedBy { it.name }
    }
    
    /**
     * Checks if a device is currently connected
     */
    fun isDeviceConnected(deviceId: String): Boolean {
        return connectedGatts.containsKey(deviceId)
    }
    
    /**
     * Cleanup - disconnects all devices and stops scanning
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up BLE connections")
        
        stopScan()
        
        connectedGatts.values.forEach { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup: ${e.message}")
            }
        }
        
        connectedGatts.clear()
        discoveredDevices.clear()
        _discoveredDevicesFlow.value = emptyList()
        _connectionStateFlow.value = emptyMap()
    }
    
    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    private fun hasPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceId = device.address
            
            // Get device name with enhanced logging
            val deviceName = try {
                device.name ?: "Unknown Device"
            } catch (e: SecurityException) {
                "Unknown Device"
            }
            
            // Log all discovered devices for debugging
            val serviceUuids = result.scanRecord?.serviceUuids?.joinToString(", ") { it.toString() } ?: "None"
            Log.d(TAG, "BLE Device Found: $deviceName ($deviceId) RSSI: ${result.rssi}dBm, Services: $serviceUuids")
            
            // Check if this device matches any known scale configurations
            val matchingConfig = result.scanRecord?.serviceUuids?.firstNotNullOfOrNull { serviceUuid ->
                BleScalesConfigs.getConfigByServiceUuid(serviceUuid.toString())
            }
            
            if (matchingConfig != null) {
                Log.d(TAG, "Device $deviceName matches scale config: ${matchingConfig.name} (${matchingConfig.manufacturer} ${matchingConfig.model})")
                deviceConfigs[deviceId] = matchingConfig
            }
            
            // Skip very weak signals but be more permissive for debugging
            if (result.rssi < -90) {
                Log.d(TAG, "Skipping device $deviceName due to weak signal: ${result.rssi}dBm")
                return
            }
            
            val bleDevice = BleScalesDevice(
                deviceId = deviceId,
                name = deviceName,
                isConnected = connectedGatts.containsKey(deviceId),
                lastSeen = LocalDateTime.now(),
                signalStrength = result.rssi
            )
            
            discoveredDevices[deviceId] = bleDevice
            _discoveredDevicesFlow.value = discoveredDevices.values.toList()
            
            Log.d(TAG, "Added to discovered devices: $deviceName ($deviceId)")
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with error code: $errorCode")
            isScanning = false
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceId = gatt.device.address
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to device: $deviceId")
                    updateConnectionState(deviceId, true)
                    
                    // Discover services
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Security exception discovering services: ${e.message}")
                    }
                }
                
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from device: $deviceId")
                    updateConnectionState(deviceId, false)
                    connectedGatts.remove(deviceId)
                    gatt.close()
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val deviceId = gatt.device.address
                Log.d(TAG, "Services discovered for device: $deviceId")
                
                // Log all available services and characteristics for debugging
                gatt.services.forEach { service ->
                    Log.d(TAG, "Service UUID: ${service.uuid}")
                    service.characteristics.forEach { characteristic ->
                        val properties = mutableListOf<String>()
                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) properties.add("READ")
                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) properties.add("WRITE")
                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) properties.add("NOTIFY")
                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) properties.add("INDICATE")
                        
                        Log.d(TAG, "  Characteristic UUID: ${characteristic.uuid}, Properties: ${properties.joinToString(", ")}")
                    }
                }
                
                // Subscribe to weight measurements
                subscribeToWeightMeasurements(gatt)
                
                // Read battery level if available
                readBatteryLevel(gatt)
            } else {
                Log.e(TAG, "Service discovery failed for device: ${gatt.device.address}, status: $status")
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val deviceId = gatt.device.address
            val config = deviceConfigs[deviceId]
            
            Log.d(TAG, "Characteristic changed for device $deviceId: ${characteristic.uuid}")
            
            // Check if this is a weight measurement characteristic
            val isWeightCharacteristic = if (config != null) {
                characteristic.uuid.toString().equals(config.weightCharacteristicUuid, ignoreCase = true)
            } else {
                characteristic.uuid == WEIGHT_MEASUREMENT_UUID
            }
            
            if (isWeightCharacteristic) {
                val weight = parseWeightMeasurement(deviceId, characteristic.value)
                if (weight > 0) {
                    Log.d(TAG, "Weight measurement from $deviceId: ${weight}g")
                    _weightMeasurementFlow.value = Pair(deviceId, weight)
                } else {
                    Log.d(TAG, "Invalid or zero weight measurement from $deviceId")
                }
            } else {
                Log.d(TAG, "Ignoring non-weight characteristic: ${characteristic.uuid}")
            }
        }
        
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val deviceId = gatt.device.address
                
                when (characteristic.uuid) {
                    BATTERY_LEVEL_UUID -> {
                        val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) ?: 0
                        Log.d(TAG, "Battery level from $deviceId: $batteryLevel%")
                        
                        val currentLevels = _batteryLevelFlow.value.toMutableMap()
                        currentLevels[deviceId] = batteryLevel
                        _batteryLevelFlow.value = currentLevels
                    }
                }
            }
        }
    }
    
    private fun subscribeToWeightMeasurements(gatt: BluetoothGatt) {
        val deviceId = gatt.device.address
        val config = deviceConfigs[deviceId]
        
        if (config != null) {
            Log.d(TAG, "Using scale config for $deviceId: ${config.name}")
            
            // Try to find the configured service and characteristic
            val serviceUuid = UUID.fromString(config.serviceUuid)
            val characteristicUuid = UUID.fromString(config.weightCharacteristicUuid)
            
            val weightService = gatt.getService(serviceUuid)
            val weightCharacteristic = weightService?.getCharacteristic(characteristicUuid)
            
            if (weightCharacteristic != null) {
                try {
                    gatt.setCharacteristicNotification(weightCharacteristic, true)
                    
                    val descriptor = weightCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    
                    Log.d(TAG, "Subscribed to weight measurements for ${config.name}")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception subscribing to notifications: ${e.message}")
                }
            } else {
                Log.w(TAG, "Weight characteristic not found for ${config.name}: $characteristicUuid")
            }
        } else {
            // Fallback to standard weight scale service
            Log.d(TAG, "No specific config found for $deviceId, trying standard weight scale service")
            
            val weightService = gatt.getService(WEIGHT_SERVICE_UUID)
            val weightCharacteristic = weightService?.getCharacteristic(WEIGHT_MEASUREMENT_UUID)
            
            if (weightCharacteristic != null) {
                try {
                    gatt.setCharacteristicNotification(weightCharacteristic, true)
                    
                    val descriptor = weightCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    
                    Log.d(TAG, "Subscribed to standard weight measurements")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception subscribing to notifications: ${e.message}")
                }
            }
        }
    }
    
    private fun readBatteryLevel(gatt: BluetoothGatt) {
        val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
        val batteryCharacteristic = batteryService?.getCharacteristic(BATTERY_LEVEL_UUID)
        
        if (batteryCharacteristic != null) {
            try {
                gatt.readCharacteristic(batteryCharacteristic)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception reading battery level: ${e.message}")
            }
        }
    }
    
    private fun parseWeightMeasurement(deviceId: String, data: ByteArray): Float {
        if (data.isEmpty()) return 0f
        
        val config = deviceConfigs[deviceId]
        val parser = config?.weightParser ?: com.bscan.model.WeightParser.STANDARD_16BIT_GRAMS
        
        Log.d(TAG, "Parsing weight for device $deviceId using parser: $parser")
        
        return WeightDataParser.parseWeight(data, parser)
    }
    
    private fun updateConnectionState(deviceId: String, isConnected: Boolean) {
        val currentStates = _connectionStateFlow.value.toMutableMap()
        currentStates[deviceId] = isConnected
        _connectionStateFlow.value = currentStates
        
        // Update discovered devices list
        discoveredDevices[deviceId]?.let { device ->
            discoveredDevices[deviceId] = device.copy(isConnected = isConnected)
            _discoveredDevicesFlow.value = discoveredDevices.values.toList()
        }
    }
}