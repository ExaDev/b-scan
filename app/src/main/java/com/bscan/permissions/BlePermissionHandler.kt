package com.bscan.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlePermissionHandler(private val activity: ComponentActivity) {
    
    private val _permissionState = MutableStateFlow(BlePermissionState.CHECKING)
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
    
    /**
     * Check current permission status
     */
    fun checkPermissions() {
        val hasAllPermissions = requiredPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
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
     * Check if permissions are granted
     */
    fun hasPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get list of missing permissions for display
     */
    fun getMissingPermissions(): List<String> {
        return requiredPermissions.filter { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get human-readable permission names
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

enum class BlePermissionState {
    CHECKING,
    GRANTED,
    DENIED,
    REQUESTING
}

/**
 * Composable hook for handling BLE permissions
 */
@Composable
fun rememberBlePermissionHandler(activity: ComponentActivity): BlePermissionHandler {
    val handler = remember { BlePermissionHandler(activity) }
    
    LaunchedEffect(Unit) {
        handler.checkPermissions()
    }
    
    return handler
}