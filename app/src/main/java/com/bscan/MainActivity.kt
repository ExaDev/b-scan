package com.bscan

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.nfc.NfcManager
import com.bscan.ui.screens.FilamentDetailsScreen
import com.bscan.ui.screens.ScanPromptScreen
import com.bscan.ui.theme.BScanTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var nfcManager: NfcManager
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        nfcManager = NfcManager(this)
        
        if (!nfcManager.isNfcAvailable()) {
            Toast.makeText(this, "NFC is not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        setContent {
            BScanTheme {
                MainScreen(
                    viewModel = viewModel,
                    onResetScan = { viewModel.resetScan() }
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
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED || 
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            
            val tagData = nfcManager.handleIntent(intent)
            if (tagData != null) {
                viewModel.processTag(tagData)
            } else {
                viewModel.setNfcError("Error reading tag")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onResetScan: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(text = "B-Scan")
                },
                actions = {
                    if (uiState.filamentInfo != null) {
                        IconButton(onClick = onResetScan) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan new tag"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isProcessing -> {
                ProcessingScreen(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = { viewModel.clearError() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.filamentInfo != null -> {
                val filamentInfo = uiState.filamentInfo
                FilamentDetailsScreen(
                    filamentInfo = filamentInfo!!,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                ScanPromptScreen(modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@Composable
private fun ProcessingScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            Text(
                text = "Processing tag...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}