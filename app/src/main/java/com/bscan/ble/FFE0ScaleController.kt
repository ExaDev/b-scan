package com.bscan.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.bscan.model.WeightParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Scale controller for FFE0 service scales (our discovered protocol)
 * Implements the 24-bit little-endian weight parsing and tare functionality
 */
class FFE0ScaleController(
    private val device: DiscoveredBleDevice
) : ScaleController {
    
    companion object {
        private const val TAG = "FFE0ScaleController"
        private const val FFE0_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
        private const val FFE0_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
        private const val BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb"
        private const val BATTERY_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb"
        
        // Tare command for commodity scales (common pattern)
        private val TARE_COMMAND = byteArrayOf(0x54.toByte(), 0x41.toByte(), 0x52.toByte(), 0x45.toByte()) // "TARE" in ASCII
        // Alternative tare commands to try
        private val TARE_COMMANDS = listOf(
            byteArrayOf(0x54.toByte(), 0x41.toByte(), 0x52.toByte(), 0x45.toByte()), // "TARE"
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()), // Zero bytes
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), // Max bytes
            byteArrayOf(0x30.toByte(), 0x30.toByte(), 0x30.toByte(), 0x30.toByte())  // "0000"
        )
    }
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var weightCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryCharacteristic: BluetoothGattCharacteristic? = null
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // State flows
    private val _currentReading = MutableStateFlow<ScaleReading?>(null)
    override val currentReading: StateFlow<ScaleReading?> = _currentReading.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ScaleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ScaleConnectionState> = _connectionState.asStateFlow()
    
    private val _isReading = MutableStateFlow(false)
    override val isReading: StateFlow<Boolean> = _isReading.asStateFlow()
    
    // Connection management
    private var connectionDeferred: CompletableDeferred<Boolean>? = null
    
    override suspend fun connect(device: DiscoveredBleDevice): ScaleCommandResult {
        if (_connectionState.value != ScaleConnectionState.DISCONNECTED) {
            return ScaleCommandResult.Error("Already connected or connecting")
        }
        
        return withContext(Dispatchers.Main) {
            try {
                _connectionState.value = ScaleConnectionState.CONNECTING
                connectionDeferred = CompletableDeferred()
                
                Log.i(TAG, "Connecting to FFE0 scale: ${device.address}")
                bluetoothGatt = device.device.connectGatt(
                    null, // Context not needed for direct connection
                    false,
                    gattCallback
                )
                
                // Wait for connection with timeout
                val connected = withTimeoutOrNull(10000) {
                    connectionDeferred?.await() ?: false
                }
                
                if (connected == true) {
                    _connectionState.value = ScaleConnectionState.CONNECTED
                    ScaleCommandResult.Success
                } else {
                    _connectionState.value = ScaleConnectionState.ERROR
                    ScaleCommandResult.Timeout
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to scale", e)
                _connectionState.value = ScaleConnectionState.ERROR
                ScaleCommandResult.Error("Connection failed: ${e.message}", e)
            }
        }
    }
    
    override suspend fun disconnect(): ScaleCommandResult {
        return try {
            stopContinuousReading()
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            weightCharacteristic = null
            batteryCharacteristic = null
            _connectionState.value = ScaleConnectionState.DISCONNECTED
            _currentReading.value = null
            ScaleCommandResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from scale", e)
            ScaleCommandResult.Error("Disconnect failed: ${e.message}", e)
        }
    }
    
    override suspend fun startContinuousReading(): ScaleCommandResult {
        val characteristic = weightCharacteristic ?: return ScaleCommandResult.Error("Not connected")
        
        return try {
            Log.i(TAG, "Starting continuous weight reading")
            val success = bluetoothGatt?.setCharacteristicNotification(characteristic, true) ?: false
            
            if (success) {
                // Enable notifications on the characteristic
                val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    bluetoothGatt?.writeDescriptor(descriptor)
                }
                
                _isReading.value = true
                _connectionState.value = ScaleConnectionState.READING
                ScaleCommandResult.Success
            } else {
                ScaleCommandResult.Error("Failed to enable notifications")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting continuous reading", e)
            ScaleCommandResult.Error("Start reading failed: ${e.message}", e)
        }
    }
    
    override suspend fun stopContinuousReading(): ScaleCommandResult {
        val characteristic = weightCharacteristic ?: return ScaleCommandResult.Success
        
        return try {
            Log.i(TAG, "Stopping continuous weight reading")
            bluetoothGatt?.setCharacteristicNotification(characteristic, false)
            _isReading.value = false
            if (_connectionState.value == ScaleConnectionState.READING) {
                _connectionState.value = ScaleConnectionState.CONNECTED
            }
            ScaleCommandResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping continuous reading", e)
            ScaleCommandResult.Error("Stop reading failed: ${e.message}", e)
        }
    }
    
    override suspend fun tareScale(): ScaleCommandResult {
        val characteristic = weightCharacteristic ?: return ScaleCommandResult.Error("Not connected")
        
        Log.i(TAG, "Attempting to tare scale")
        
        // Try different tare commands until one works
        for ((index, command) in TARE_COMMANDS.withIndex()) {
            try {
                Log.d(TAG, "Trying tare command ${index + 1}: ${command.joinToString(" ") { "%02X".format(it) }}")
                characteristic.value = command
                val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
                
                if (success) {
                    // Wait a moment for the tare to take effect
                    delay(1000)
                    return ScaleCommandResult.Success
                }
            } catch (e: Exception) {
                Log.w(TAG, "Tare command ${index + 1} failed: ${e.message}")
            }
        }
        
        return ScaleCommandResult.Error("All tare commands failed")
    }
    
    override suspend fun setUnit(unit: WeightUnit): ScaleCommandResult {
        // Most commodity scales don't support remote unit switching
        return ScaleCommandResult.NotSupported
    }
    
    override suspend fun getSingleReading(): ScaleReading? {
        val characteristic = weightCharacteristic ?: return null
        
        return try {
            // Request a single read
            bluetoothGatt?.readCharacteristic(characteristic)
            // The result will come through the gatt callback
            // For now, return the last reading
            _currentReading.value
        } catch (e: Exception) {
            Log.e(TAG, "Error getting single reading", e)
            null
        }
    }
    
    override fun supportsCommand(command: ScaleCommand): Boolean {
        return when (command) {
            is ScaleCommand.Tare -> true
            is ScaleCommand.SetUnit -> false // Most don't support this
            is ScaleCommand.EnterCalibration -> false
            is ScaleCommand.GetBatteryLevel -> batteryCharacteristic != null
        }
    }
    
    override fun getDeviceInfo(): DiscoveredBleDevice = device
    
    override fun cleanup() {
        scope.cancel()
        runBlocking { disconnect() }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server")
                    // Discover services
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server")
                    _connectionState.value = ScaleConnectionState.DISCONNECTED
                    _isReading.value = false
                    _currentReading.value = null
                    connectionDeferred?.complete(false)
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered")
                
                // Find FFE0 service and characteristic
                val ffe0Service = gatt.getService(UUID.fromString(FFE0_SERVICE_UUID))
                if (ffe0Service != null) {
                    weightCharacteristic = ffe0Service.getCharacteristic(UUID.fromString(FFE0_CHARACTERISTIC_UUID))
                    Log.d(TAG, "Found FFE0 weight characteristic")
                }
                
                // Find battery service if available
                val batteryService = gatt.getService(UUID.fromString(BATTERY_SERVICE_UUID))
                if (batteryService != null) {
                    batteryCharacteristic = batteryService.getCharacteristic(UUID.fromString(BATTERY_CHARACTERISTIC_UUID))
                    Log.d(TAG, "Found battery characteristic")
                }
                
                connectionDeferred?.complete(weightCharacteristic != null)
            } else {
                Log.w(TAG, "Service discovery failed: $status")
                connectionDeferred?.complete(false)
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid.toString().lowercase()) {
                FFE0_CHARACTERISTIC_UUID.lowercase() -> {
                    val data = characteristic.value
                    if (data != null) {
                        parseWeightData(data)
                    }
                }
                BATTERY_CHARACTERISTIC_UUID.lowercase() -> {
                    val batteryLevel = if (characteristic.value.isNotEmpty()) {
                        characteristic.value[0].toInt() and 0xFF
                    } else null
                    
                    // Update current reading with battery info
                    _currentReading.value?.let { reading ->
                        _currentReading.value = reading.copy(batteryLevel = batteryLevel)
                    }
                }
            }
        }
        
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onCharacteristicChanged(gatt, characteristic)
            }
        }
    }
    
    private fun parseWeightData(data: ByteArray) {
        try {
            // Use our discovered 24-bit little-endian parsing
            val weight = parseFFE0Bytes4_5_6LittleEndian(data)
            
            // Determine stability - for now, assume stable if weight hasn't changed much
            val lastWeight = _currentReading.value?.weight ?: 0f
            val isStable = kotlin.math.abs(weight - lastWeight) < 1.0f // Within 1g = stable
            
            val reading = ScaleReading(
                weight = weight,
                isStable = isStable,
                unit = WeightUnit.GRAMS,
                batteryLevel = _currentReading.value?.batteryLevel,
                signalStrength = device.rssi,
                rawData = data.clone(),
                parsingMethod = "FFE0_BYTES_4_5_6_LITTLE_ENDIAN"
            )
            
            _currentReading.value = reading
            
            Log.d(TAG, "Weight reading: ${reading.getDisplayWeight()} ${if (isStable) "[STABLE]" else "[UNSTABLE]"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing weight data", e)
        }
    }
    
    private fun parseFFE0Bytes4_5_6LittleEndian(data: ByteArray): Float {
        if (data.size < 7) {
            return 0f
        }
        
        val byte4 = data[4].toInt() and 0xFF
        val byte5 = data[5].toInt() and 0xFF  
        val byte6 = data[6].toInt() and 0xFF
        
        // 24-bit little-endian combination
        val weightRaw = byte4 or (byte5 shl 8) or (byte6 shl 16)
        
        return if (weightRaw in 0..10000) weightRaw.toFloat() else 0f
    }
}