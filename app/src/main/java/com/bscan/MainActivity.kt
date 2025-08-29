package com.bscan

import android.content.Intent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.cache.CachedBambuKeyDerivation
import com.bscan.model.*
import com.bscan.navigation.AppNavigation
import com.bscan.nfc.NfcManager
import com.bscan.nfc.handlers.HapticFeedbackProvider
import com.bscan.nfc.handlers.NfcIntentProcessor
import com.bscan.ui.theme.BScanTheme
import com.bscan.viewmodel.UpdateViewModel
import com.bscan.ble.BlePermissionHandler

class MainActivity : ComponentActivity() {
    
    private var nfcManager: NfcManager? = null
    private var nfcIntentProcessor: NfcIntentProcessor? = null
    private lateinit var hapticFeedbackProvider: HapticFeedbackProvider
    private var blePermissionHandler: BlePermissionHandler? = null
    
    private val viewModel: MainViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize the key cache system
        CachedBambuKeyDerivation.initialize(this)
        
        
        // Check NFC availability before initialising NFC-dependent components
        val isNfcSupported = NfcAdapter.getDefaultAdapter(this) != null
        
        // Check BLE availability before initialising BLE-dependent components
        val isBleSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter != null
        
        hapticFeedbackProvider = HapticFeedbackProvider(this)
        
        if (isNfcSupported) {
            val nfcManagerInstance = NfcManager(this)
            nfcManager = nfcManagerInstance
            nfcIntentProcessor = NfcIntentProcessor(
                nfcManager = nfcManagerInstance,
                viewModel = viewModel,
                lifecycleScope = lifecycleScope,
                hapticFeedbackProvider = hapticFeedbackProvider
            )
        } else {
            // Show informational message but continue app functionality
            Toast.makeText(this, "NFC not available - app will work with limited functionality", Toast.LENGTH_LONG).show()
            viewModel.setNfcError("NFC hardware not available on this device")
        }
        
        if (isBleSupported) {
            blePermissionHandler = BlePermissionHandler(this)
        } else {
            // Show informational message but continue app functionality
            Toast.makeText(this, "Bluetooth Low Energy not available - scales functionality disabled", Toast.LENGTH_LONG).show()
            // Note: BLE error state could be added to viewModel if needed
        }
        
        setContent {
            // Get user theme preference, fallback to AUTO while loading
            val userData = viewModel.getUserDataRepository().getUserData()
            val theme = userData?.preferences?.theme ?: AppTheme.AUTO
            
            BScanTheme(theme = theme) {
                AppNavigation(
                    viewModel = viewModel,
                    updateViewModel = updateViewModel,
                    nfcManager = nfcManager,
                    blePermissionHandler = blePermissionHandler
                )
            }
        }
        
        // Process intent if launched via NFC
        handleIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        nfcManager?.let { manager ->
            if (!manager.isNfcEnabled()) {
                viewModel.setNfcError("NFC is disabled")
                return
            }
            manager.enableForegroundDispatch()
        }
    }
    
    override fun onPause() {
        super.onPause()
        nfcManager?.disableForegroundDispatch()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        nfcManager?.cleanup()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        nfcIntentProcessor?.handleIntent(intent)
    }
}