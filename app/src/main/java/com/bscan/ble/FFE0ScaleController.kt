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
        private const val FFE1_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
        private const val FFE2_CHARACTERISTIC_UUID = "0000ffe2-0000-1000-8000-00805f9b34fb"
        private const val FFE3_CHARACTERISTIC_UUID = "0000ffe3-0000-1000-8000-00805f9b34fb"
        private const val FFE4_CHARACTERISTIC_UUID = "0000ffe4-0000-1000-8000-00805f9b34fb"
        private const val FFE5_CHARACTERISTIC_UUID = "0000ffe5-0000-1000-8000-00805f9b34fb"
        private const val BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb"
        private const val BATTERY_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb"
        
        // Discovered tare command from TomBastable's Salter scales research
        private val SALTER_TARE_COMMAND = byteArrayOf(9.toByte(), 3.toByte(), 5.toByte()) // [9,3,5] to FFE3 characteristic
        
        // Tare command for commodity scales (common pattern) 
        private val TARE_COMMAND = byteArrayOf(0x54.toByte(), 0x41.toByte(), 0x52.toByte(), 0x45.toByte()) // "TARE" in ASCII
        // Alternative tare commands to try (expanded based on common BLE scale protocols)
        private val TARE_COMMANDS = listOf(
            // Common single-byte commands
            byteArrayOf(0x54.toByte()), // 'T' for Tare
            byteArrayOf(0x5A.toByte()), // 'Z' for Zero
            byteArrayOf(0x30.toByte()), // '0' for Zero
            byteArrayOf(0x00.toByte()), // Null byte
            
            // Protocol-specific commands (08 07 03 format)
            byteArrayOf(0x08.toByte(), 0x07.toByte(), 0x03.toByte(), 0x02.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()), // Tare command in same format
            byteArrayOf(0x08.toByte(), 0x05.toByte(), 0x03.toByte(), 0x02.toByte()), // Shorter tare command
            
            // Original ASCII commands  
            byteArrayOf(0x54.toByte(), 0x41.toByte(), 0x52.toByte(), 0x45.toByte()), // "TARE" in ASCII
            byteArrayOf(0x5A.toByte(), 0x45.toByte(), 0x52.toByte(), 0x4F.toByte()), // "ZERO" in ASCII
            
            // Common hex patterns
            byteArrayOf(0x02.toByte()), // STX (Start of Text) - common command prefix
            byteArrayOf(0x0A.toByte()), // Line Feed
            byteArrayOf(0x0D.toByte()), // Carriage Return
            byteArrayOf(0xFF.toByte()), // Max byte
            
            // Multi-byte patterns
            byteArrayOf(0x00.toByte(), 0x00.toByte()), // Double zero
            byteArrayOf(0xFF.toByte(), 0xFF.toByte()), // Double max
            byteArrayOf(0x30.toByte(), 0x30.toByte(), 0x30.toByte(), 0x30.toByte())  // "0000"
        )
    }
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var weightCharacteristic: BluetoothGattCharacteristic? = null
    private var tareCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryCharacteristic: BluetoothGattCharacteristic? = null
    
    // All FFE characteristics for comprehensive monitoring
    private val allFfeCharacteristics = mutableMapOf<String, BluetoothGattCharacteristic>()
    private val ffeCharacteristicUUIDs = listOf(
        FFE1_CHARACTERISTIC_UUID,
        FFE2_CHARACTERISTIC_UUID, 
        FFE3_CHARACTERISTIC_UUID,
        FFE4_CHARACTERISTIC_UUID,
        FFE5_CHARACTERISTIC_UUID
    )
    
    // Unit detection state
    private var currentDetectedUnit: WeightUnit = WeightUnit.UNKNOWN
    private var currentUnitValid: Boolean = false
    
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
            tareCharacteristic = null
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
            Log.i(TAG, "Starting comprehensive characteristic monitoring...")
            
            var subscriptionCount = 0
            
            // Subscribe to notifications from ALL available FFE characteristics
            for ((name, char) in allFfeCharacteristics) {
                try {
                    val success = bluetoothGatt?.setCharacteristicNotification(char, true) ?: false
                    if (success) {
                        // Enable notifications on the characteristic
                        val descriptor = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (descriptor != null) {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            val writeResult = bluetoothGatt?.writeDescriptor(descriptor) ?: false
                            if (writeResult) {
                                subscriptionCount++
                                Log.d(TAG, "Subscribed to $name notifications")
                            } else {
                                Log.w(TAG, "Failed to write descriptor for $name")
                            }
                        } else {
                            Log.w(TAG, "$name has no notification descriptor")
                        }
                    } else {
                        Log.w(TAG, "Failed to subscribe to $name notifications")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error subscribing to $name: ${e.message}")
                }
            }
            
            Log.i(TAG, "Subscribed to $subscriptionCount/${allFfeCharacteristics.size} characteristics")
            
            if (subscriptionCount > 0) {
                _isReading.value = true
                _connectionState.value = ScaleConnectionState.READING
                ScaleCommandResult.Success
            } else {
                ScaleCommandResult.Error("Failed to subscribe to any characteristics")
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
        Log.i(TAG, "Attempting to tare scale")
        
        // First try the discovered Salter approach: [9,3,5] to FFE3 characteristic
        tareCharacteristic?.let { ffe3Char ->
            try {
                Log.d(TAG, "Trying Salter tare command [9,3,5] to FFE3 characteristic")
                @Suppress("DEPRECATION")
                ffe3Char.value = SALTER_TARE_COMMAND
                val success = bluetoothGatt?.writeCharacteristic(ffe3Char) ?: false
                
                if (success) {
                    Log.i(TAG, "Salter tare command sent successfully")
                    delay(1000)
                    return ScaleCommandResult.Success
                } else {
                    Log.w(TAG, "Failed to write Salter tare command to FFE3")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Salter tare command failed: ${e.message}")
            }
        } ?: Log.w(TAG, "FFE3 tare characteristic not available")
        
        // Fallback: try traditional tare commands to FFE1 (weight) characteristic
        val weightChar = weightCharacteristic ?: return ScaleCommandResult.Error("Not connected")
        
        Log.d(TAG, "Fallback: trying traditional tare commands to FFE1 characteristic")
        for ((index, command) in TARE_COMMANDS.withIndex()) {
            try {
                Log.d(TAG, "Trying tare command ${index + 1}: ${command.joinToString(" ") { "%02X".format(it) }}")
                @Suppress("DEPRECATION")
                weightChar.value = command
                val success = bluetoothGatt?.writeCharacteristic(weightChar) ?: false
                
                if (success) {
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
    
    /**
     * Enable comprehensive monitoring without triggering legacy pairing
     * Only subscribes to notifications - no direct reads to avoid pairing attempts
     */
    suspend fun enableUnitDetectionMonitoring(): ScaleCommandResult {
        return try {
            Log.i(TAG, "🔍 UNIT_DETECTION: Enabling passive monitoring (notifications only)...")
            
            var subscriptionCount = 0
            
            // Only subscribe to notifications - avoid read operations that trigger pairing
            for ((name, char) in allFfeCharacteristics) {
                try {
                    // Check if characteristic supports notifications
                    val properties = char.properties
                    val supportsNotify = (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    val supportsIndicate = (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    
                    if (supportsNotify || supportsIndicate) {
                        val success = bluetoothGatt?.setCharacteristicNotification(char, true) ?: false
                        if (success) {
                            val descriptor = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            if (descriptor != null) {
                                val descriptorValue = if (supportsNotify) {
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                } else {
                                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                                }
                                @Suppress("DEPRECATION")
                                descriptor.value = descriptorValue
                                val writeResult = bluetoothGatt?.writeDescriptor(descriptor) ?: false
                                if (writeResult) {
                                    subscriptionCount++
                                    Log.d(TAG, "🔍 Subscribed to $name (${if (supportsNotify) "notify" else "indicate"})")
                                } else {
                                    Log.w(TAG, "Failed to write descriptor for $name")
                                }
                            } else {
                                Log.w(TAG, "$name has no notification descriptor")
                            }
                        } else {
                            Log.w(TAG, "Failed to subscribe to $name")
                        }
                    } else {
                        Log.d(TAG, "$name doesn't support notifications (properties: $properties)")
                    }
                    
                    delay(50) // Small delay between subscriptions
                } catch (e: Exception) {
                    Log.e(TAG, "Error subscribing to $name: ${e.message}")
                }
            }
            
            Log.i(TAG, "🔍 UNIT_DETECTION: Subscribed to $subscriptionCount/${allFfeCharacteristics.size} characteristics for passive monitoring")
            
            if (subscriptionCount > 0) {
                ScaleCommandResult.Success
            } else {
                ScaleCommandResult.Error("No characteristics support notifications")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling unit detection monitoring", e)
            ScaleCommandResult.Error("Unit detection setup failed: ${e.message}", e)
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
                
                // Find FFE0 service and all characteristics
                val ffe0Service = gatt.getService(UUID.fromString(FFE0_SERVICE_UUID))
                if (ffe0Service != null) {
                    Log.i(TAG, "FFE0 Service found - discovering all characteristics...")
                    
                    // Discover all FFE characteristics for comprehensive monitoring
                    allFfeCharacteristics.clear()
                    for (uuid in ffeCharacteristicUUIDs) {
                        val characteristic = ffe0Service.getCharacteristic(UUID.fromString(uuid))
                        if (characteristic != null) {
                            val shortName = uuid.substring(4, 8).uppercase() // Extract "FFE1", "FFE2", etc.
                            allFfeCharacteristics[shortName] = characteristic
                            Log.d(TAG, "Found characteristic: $shortName")
                            
                            // Set up specific references
                            when (shortName) {
                                "FFE1" -> weightCharacteristic = characteristic
                                "FFE3" -> tareCharacteristic = characteristic
                            }
                        } else {
                            val shortName = uuid.substring(4, 8).uppercase()
                            Log.w(TAG, "Characteristic not found: $shortName")
                        }
                    }
                    
                    Log.i(TAG, "Total FFE characteristics found: ${allFfeCharacteristics.size}")
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
        
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val characteristicUuid = characteristic.uuid.toString().lowercase()
            val data = characteristic.value
            
            if (data != null && data.isNotEmpty()) {
                val hexData = data.joinToString(" ") { "%02X".format(it) }
                val timestamp = System.currentTimeMillis()
                
                // Identify which characteristic this is
                val charName = when (characteristicUuid) {
                    FFE1_CHARACTERISTIC_UUID.lowercase() -> "FFE1"
                    FFE2_CHARACTERISTIC_UUID.lowercase() -> "FFE2"
                    FFE3_CHARACTERISTIC_UUID.lowercase() -> "FFE3"
                    FFE4_CHARACTERISTIC_UUID.lowercase() -> "FFE4"
                    FFE5_CHARACTERISTIC_UUID.lowercase() -> "FFE5"
                    BATTERY_CHARACTERISTIC_UUID.lowercase() -> "BATTERY"
                    else -> "UNKNOWN"
                }
                
                // LOG ALL CHARACTERISTIC DATA for unit detection analysis
                Log.i(TAG, "🔍 CHARACTERISTIC_DATA: $charName | $hexData | Time: $timestamp | Length: ${data.size}")
                
                // For non-FFE1 characteristics, log more detailed analysis
                if (charName != "FFE1" && charName != "BATTERY") {
                    Log.w(TAG, "🔍 NON_WEIGHT_DATA: $charName sent data - could be unit info!")
                    for (i in data.indices) {
                        val byteVal = data[i].toInt() and 0xFF
                        Log.d(TAG, "🔍 $charName Byte[$i]: 0x${"%02X".format(byteVal)} (${byteVal})")
                    }
                }
                
                // Handle specific characteristics
                when (characteristicUuid) {
                    FFE0_CHARACTERISTIC_UUID.lowercase(), FFE1_CHARACTERISTIC_UUID.lowercase() -> {
                        // Check for power-off message
                        if (data.size >= 4 && 
                            data[0] == 0x08.toByte() && 
                            data[1] == 0x04.toByte() && 
                            data[2] == 0xAF.toByte() && 
                            data[3] == 0x01.toByte()) {
                            Log.i(TAG, "Scale power-off message detected: $hexData")
                            return
                        }
                        
                        // Parse weight data from FFE1
                        parseWeightData(data)
                    }
                    BATTERY_CHARACTERISTIC_UUID.lowercase() -> {
                        val batteryLevel = if (data.isNotEmpty()) {
                            data[0].toInt() and 0xFF
                        } else null
                        
                        _currentReading.value?.let { reading ->
                            _currentReading.value = reading.copy(batteryLevel = batteryLevel)
                        }
                    }
                    // For FFE2, FFE3, FFE4, FFE5 - just log for now (unit detection research)
                    else -> {
                        Log.d(TAG, "Data from $charName: $hexData (length: ${data.size})")
                    }
                }
            }
        }
        
        @Suppress("DEPRECATION") 
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onCharacteristicChanged(gatt, characteristic)
            }
        }
    }
    
    private fun parseWeightData(data: ByteArray) {
        try {
            // Check if this matches the new 08 07 03 protocol
            if (data.size >= 7 && 
                data[0] == 0x08.toByte() && 
                data[1] == 0x07.toByte() && 
                data[2] == 0x03.toByte()) {
                
                // Parse using new 08 07 03 protocol
                val weight = parseFFE0Protocol_08_07_03(data)
                val isStable = data[3] == 0x01.toByte() // Byte 3: 01 = stable, 00 = unstable
                
                val reading = ScaleReading(
                    weight = weight,
                    isStable = isStable,
                    unit = currentDetectedUnit,
                    isUnitValid = currentUnitValid,
                    batteryLevel = _currentReading.value?.batteryLevel,
                    signalStrength = device.rssi,
                    rawData = data.clone(),
                    parsingMethod = "FFE0_PROTOCOL_08_07_03"
                )
                
                _currentReading.value = reading
                
                Log.d(TAG, "Weight reading: ${reading.getDisplayWeight()} ${if (isStable) "[STABLE]" else "[UNSTABLE]"} | Raw: ${reading.getRawDataHex()}")
                
            } else {
                // Fallback to old 24-bit little-endian parsing for compatibility
                val weight = parseFFE0Bytes4_5_6LittleEndian(data)
                
                // Determine stability - assume stable if weight hasn't changed much
                val lastWeight = _currentReading.value?.weight ?: 0f
                val isStable = kotlin.math.abs(weight - lastWeight) < 1.0f // Within 1g = stable
                
                val reading = ScaleReading(
                    weight = weight,
                    isStable = isStable,
                    unit = WeightUnit.GRAMS, // Fallback assumes grams
                    isUnitValid = true, // Assume valid for fallback
                    batteryLevel = _currentReading.value?.batteryLevel,
                    signalStrength = device.rssi,
                    rawData = data.clone(),
                    parsingMethod = "FFE0_BYTES_4_5_6_LITTLE_ENDIAN"
                )
                
                _currentReading.value = reading
                
                Log.d(TAG, "Weight reading: ${reading.getDisplayWeight()} ${if (isStable) "[STABLE]" else "[UNSTABLE]"} | Raw: ${reading.getRawDataHex()}")
            }
            
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
    
    /**
     * Parse weight from new 08 07 03 protocol with comprehensive unit analysis
     * Format: 08 07 03 [stability] [sign_byte] [magnitude_byte] [unit_info?]
     */
    private fun parseFFE0Protocol_08_07_03(data: ByteArray): Float {
        if (data.size < 6) {
            Log.w(TAG, "Insufficient data for 08 07 03 protocol: ${data.size} bytes")
            return 0f
        }
        
        // COMPREHENSIVE ANALYSIS: Log every single byte for unit detection
        val fullHex = data.joinToString(" ") { "%02X".format(it) }
        Log.i(TAG, "🔍 WEIGHT_DATA_ANALYSIS: Full packet: $fullHex")
        
        // Analyze each byte position for potential unit information
        for (i in data.indices) {
            val byteValue = data[i].toInt() and 0xFF
            Log.d(TAG, "🔍 Byte[$i]: 0x${"%02X".format(byteValue)} (${byteValue}) ${if (byteValue in 32..126) "ASCII:'${byteValue.toChar()}'" else ""}")
        }
        
        val signByte = data[4].toInt() and 0xFF
        val magnitudeByte = data[5].toInt() and 0xFF
        val byte6 = if (data.size > 6) data[6].toInt() and 0xFF else 0
        
        // Check if sign bit is set (0x80 = negative)
        val isNegative = (signByte and 0x80) != 0
        
        // Calculate weight magnitude
        val overflowBits = signByte and 0x7F // Remove sign bit
        val totalMagnitude = if (overflowBits > 0) {
            // Weight > 255g: combine overflow + magnitude
            (overflowBits * 256) + magnitudeByte
        } else {
            // Weight <= 255g: just the magnitude
            magnitudeByte
        }
        
        // UNIT DETECTION: Parse byte 6 for unit information first
        val detectedUnit = WeightUnit.fromScaleByte(byte6)
        val isUnitValid = WeightUnit.isExpectedUnit(detectedUnit)
        
        // Apply unit-specific scaling to raw weight
        val scaledWeight = when (detectedUnit) {
            WeightUnit.OUNCES, WeightUnit.FLUID_OUNCES -> totalMagnitude.toFloat() / 10f // Scale sends oz * 10
            else -> totalMagnitude.toFloat() // Grams and other units are 1:1
        }
        
        val rawWeight = if (isNegative) -scaledWeight else scaledWeight
        
        // Log unit detection results
        Log.i(TAG, "🎯 UNIT_DETECTED: raw=${rawWeight} | unit=${detectedUnit.displayName} (byte6=0x${"%02X".format(byte6)}) | valid=$isUnitValid | sign=0x${"%02X".format(data[4])} | mag=0x${"%02X".format(data[5])} | stability=0x${"%02X".format(data[3])}")
        
        if (!isUnitValid) {
            Log.w(TAG, "⚠ UNIT_WARNING: Scale is in ${detectedUnit.displayName}, expected ${WeightUnit.GRAMS.displayName}")
        }
        
        // Store unit detection info for ScaleReading creation
        currentDetectedUnit = detectedUnit
        currentUnitValid = isUnitValid
        
        return if (rawWeight >= -5000f && rawWeight <= 10000f) rawWeight else 0f
    }
    
}