package com.bscan

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bscan.cache.CachedBambuKeyDerivation
import com.bscan.navigation.AppNavigation
import com.bscan.nfc.NfcManager
import com.bscan.nfc.handlers.HapticFeedbackProvider
import com.bscan.nfc.handlers.NfcIntentProcessor
import com.bscan.ui.theme.BScanTheme
import com.bscan.viewmodel.UpdateViewModel
import com.bscan.ble.BlePermissionHandler

class MainActivity : ComponentActivity() {
    
    private lateinit var nfcManager: NfcManager
    private lateinit var nfcIntentProcessor: NfcIntentProcessor
    private lateinit var hapticFeedbackProvider: HapticFeedbackProvider
    private lateinit var blePermissionHandler: BlePermissionHandler
    
    private val viewModel: MainViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize the key cache system
        CachedBambuKeyDerivation.initialize(this)
        
        nfcManager = NfcManager(this)
        hapticFeedbackProvider = HapticFeedbackProvider(this)
        blePermissionHandler = BlePermissionHandler(this)
        nfcIntentProcessor = NfcIntentProcessor(
            nfcManager = nfcManager,
            viewModel = viewModel,
            lifecycleScope = lifecycleScope,
            hapticFeedbackProvider = hapticFeedbackProvider
        )
        
        if (!nfcManager.isNfcAvailable()) {
            Toast.makeText(this, "NFC is not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        setContent {
            BScanTheme {
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
        
        if (!nfcManager.isNfcEnabled()) {
            viewModel.setNfcError("NFC is disabled")
            return
        }
        
        nfcManager.enableForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        nfcManager.cleanup()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        nfcIntentProcessor.handleIntent(intent)
    }
}