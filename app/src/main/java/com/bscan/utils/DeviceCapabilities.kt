package com.bscan.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Utility class to detect device performance capabilities and determine
 * if resource-intensive features like accelerometer effects should be enabled
 */
class DeviceCapabilities(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    /**
     * Determines if this device should have accelerometer effects disabled by default
     * due to performance constraints
     */
    fun shouldDisableAccelerometerEffects(): Boolean {
        val reasons = mutableListOf<String>()
        var shouldDisable = false
        
        // Check 1: Low RAM device flag
        if (isLowRamDevice()) {
            reasons.add("low RAM device flag")
            shouldDisable = true
        }
        
        // Check 2: Insufficient heap memory
        if (hasInsufficientHeapMemory()) {
            reasons.add("heap memory < 128MB")
            shouldDisable = true
        }
        
        // Check 3: Low CPU core count
        if (hasLowCpuCores()) {
            reasons.add("< 4 CPU cores")
            shouldDisable = true
        }
        
        // Check 4: Emulator detection (often have sensor issues)
        if (isEmulator()) {
            reasons.add("emulator detected")
            shouldDisable = true
        }
        
        if (shouldDisable) {
            Log.i(TAG, "Auto-disabling accelerometer effects due to: ${reasons.joinToString(", ")}")
            Log.i(TAG, "Device info: RAM=${getMemoryClassMB()}MB, Cores=${getCpuCores()}, API=${Build.VERSION.SDK_INT}")
        } else {
            Log.i(TAG, "Device capable of accelerometer effects: RAM=${getMemoryClassMB()}MB, Cores=${getCpuCores()}, API=${Build.VERSION.SDK_INT}")
        }
        
        return shouldDisable
    }
    
    /**
     * Get human-readable reason for why accelerometer effects are disabled
     */
    fun getDisableReason(): String {
        val reasons = mutableListOf<String>()
        
        if (isLowRamDevice()) reasons.add("Low RAM device")
        if (hasInsufficientHeapMemory()) reasons.add("Limited memory (${getMemoryClassMB()}MB)")
        if (hasLowCpuCores()) reasons.add("Limited CPU (${getCpuCores()} cores)")
        if (isEmulator()) reasons.add("Emulator environment")
        
        return if (reasons.isEmpty()) {
            "Device supports motion effects"
        } else {
            "Disabled for performance: ${reasons.joinToString(", ")}"
        }
    }
    
    /**
     * Check if device is marked as low RAM by the system
     */
    private fun isLowRamDevice(): Boolean {
        return activityManager.isLowRamDevice
    }
    
    /**
     * Check if device has insufficient heap memory for smooth animations
     */
    private fun hasInsufficientHeapMemory(): Boolean {
        val heapSizeMB = activityManager.memoryClass
        return heapSizeMB < MIN_HEAP_SIZE_MB
    }
    
    /**
     * Check if device has too few CPU cores for smooth animations
     */
    private fun hasLowCpuCores(): Boolean {
        val cores = Runtime.getRuntime().availableProcessors()
        return cores < MIN_CPU_CORES
    }
    
    /**
     * Detect if running on an emulator (emulators often have sensor issues)
     */
    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("google_sdk") ||
                Build.PRODUCT.contains("sdk_gphone") ||
                Build.PRODUCT.contains("vbox86p") ||
                Build.BOARD == "QC_Reference_Phone"
    }
    
    /**
     * Get device memory class in MB
     */
    fun getMemoryClassMB(): Int {
        return activityManager.memoryClass
    }
    
    /**
     * Get number of CPU cores
     */
    fun getCpuCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }
    
    /**
     * Get device performance summary for debugging
     */
    fun getDeviceInfo(): String {
        return buildString {
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Memory: ${getMemoryClassMB()}MB heap")
            appendLine("CPU cores: ${getCpuCores()}")
            appendLine("Low RAM device: ${isLowRamDevice()}")
            appendLine("Emulator: ${isEmulator()}")
        }
    }
    
    companion object {
        private const val TAG = "DeviceCapabilities"
        
        // Performance thresholds
        private const val MIN_HEAP_SIZE_MB = 128  // Minimum heap size for smooth animations
        private const val MIN_CPU_CORES = 4       // Minimum CPU cores for good performance
    }
}