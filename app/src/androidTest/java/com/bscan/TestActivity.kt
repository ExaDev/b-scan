package com.bscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bscan.ble.BlePermissionHandler
import com.bscan.cache.CachedBambuKeyDerivation
import com.bscan.model.AppTheme
import com.bscan.navigation.AppNavigation
import com.bscan.nfc.NfcManager
import com.bscan.nfc.handlers.HapticFeedbackProvider
import com.bscan.nfc.handlers.NfcIntentProcessor
import com.bscan.ui.theme.BScanTheme
import com.bscan.viewmodel.UpdateViewModel

/**
 * Test activity for instrumented tests that provides full app functionality without NFC hardware requirements
 * This allows testing UI components and navigation using the real app structure
 */
class TestActivity : ComponentActivity() {
    
    private lateinit var nfcManager: NfcManager
    private lateinit var blePermissionHandler: BlePermissionHandler
    
    private val viewModel: MainViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize the key cache system
        CachedBambuKeyDerivation.initialize(this)
        
        // Use real NFC manager but we'll handle NFC unavailability gracefully
        nfcManager = NfcManager(this)
        
        // Initialize BLE permission handler for testing
        blePermissionHandler = BlePermissionHandler(this)
        
        setContent {
            BScanTheme(theme = AppTheme.AUTO) {
                AppNavigation(
                    viewModel = viewModel,
                    updateViewModel = updateViewModel,
                    nfcManager = nfcManager,
                    blePermissionHandler = blePermissionHandler
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            nfcManager.cleanup()
        } catch (e: Exception) {
            // Ignore cleanup errors in tests
        }
    }
}