package com.bscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bscan.ui.theme.BScanTheme
import com.bscan.viewmodel.UpdateViewModel

/**
 * Test activity for instrumented tests that avoids NFC requirements
 * This allows testing UI components and navigation without hardware dependencies
 */
class TestActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            BScanTheme {
                TestScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen() {
    var currentScreen by remember { mutableStateOf("home") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App bar simulation
        TopAppBar(
            title = { Text("B-Scan") },
            actions = {
                IconButton(
                    onClick = { currentScreen = "history" }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "View scan history"
                    )
                }
            }
        )
        
        when (currentScreen) {
            "home" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ready to scan NFC tags")
                }
            }
            "history" -> {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        IconButton(
                            onClick = { currentScreen = "home" }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back"
                            )
                        }
                        Text(
                            text = "Scan History",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No scans yet")
                    }
                }
            }
        }
    }
}