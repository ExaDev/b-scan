package com.bscan.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles BLE permissions for scales connectivity
 * Manages Android version differences in BLE permission requirements
 */
class BlePermissionHandler(private val activity: ComponentActivity) {
    
    private val _permissionState = MutableStateFlow(BlePermissionState.UNKNOWN)
    val permissionState: StateFlow<BlePermissionState> = _permissionState.asStateFlow()
    
    // Required permissions based on Android version
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    // Permission launcher
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        _permissionState.value = if (allGranted) {
            BlePermissionState.GRANTED
        } else {
            BlePermissionState.DENIED
        }
    }
    
    init {
        checkPermissions()
    }
    
    /**
     * Check current permission status
     */
    fun checkPermissions() {
        val hasAllPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        _permissionState.value = if (hasAllPermissions) {
            BlePermissionState.GRANTED
        } else {
            BlePermissionState.DENIED
        }
    }
    
    /**
     * Request BLE permissions
     */
    fun requestPermissions() {
        _permissionState.value = BlePermissionState.REQUESTING
        permissionLauncher.launch(requiredPermissions)
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(): List<String> {
        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get human-readable permission names for display
     */
    fun getPermissionDisplayNames(permissions: List<String>): List<String> {
        return permissions.map { permission ->
            when (permission) {
                Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth Scan"
                Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth Connect"
                Manifest.permission.BLUETOOTH -> "Bluetooth"
                Manifest.permission.BLUETOOTH_ADMIN -> "Bluetooth Admin"
                Manifest.permission.ACCESS_FINE_LOCATION -> "Location (for BLE device discovery)"
                else -> permission.substringAfterLast(".")
            }
        }
    }
}

/**
 * State of BLE permissions
 */
enum class BlePermissionState {
    UNKNOWN,
    GRANTED,
    DENIED,
    REQUESTING
}