package com.bscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.UniqueSpool
import com.bscan.ui.components.ScanStateIndicator

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    var recentSpools by remember { mutableStateOf(listOf<UniqueSpool>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            // Get the 5 most recently scanned spools
            recentSpools = repository.getUniqueSpools().take(5)
        } catch (e: Exception) {
            recentSpools = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    if (recentSpools.isEmpty()) {
        // Show full scan prompt for first-time users
        ScanPromptScreen(modifier = modifier)
    } else {
        // Show combined view with compact scan prompt + recent spools
        CombinedHomeScreen(
            recentSpools = recentSpools,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombinedHomeScreen(
    recentSpools: List<UniqueSpool>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Compact scan prompt at the top
            CompactScanPrompt()
        }
        
        item {
            // Section header
            Text(
                text = "Recently Scanned",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        
        items(recentSpools) { spool ->
            RecentSpoolCard(spool = spool)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactScanPrompt(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScanStateIndicator(
                isIdle = true,
                modifier = Modifier.size(60.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Scan a Spool",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Tap your device against a Bambu Lab filament spool to read its information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentSpoolCard(
    spool: UniqueSpool,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = parseColor(spool.filamentInfo.colorHex),
                        shape = CircleShape
                    )
            )
            
            // Filament info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = spool.filamentInfo.colorName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = spool.filamentInfo.filamentType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "Scanned ${spool.scanCount} time${if (spool.scanCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Success rate indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                when {
                    spool.successRate >= 0.9f -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "High success rate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    spool.successRate >= 0.7f -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Good success rate", 
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Low success rate",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseColor(colorHex: String): Color {
    return try {
        val hex = if (colorHex.startsWith("#")) colorHex.substring(1) else colorHex
        when (hex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$hex"))
            8 -> Color(android.graphics.Color.parseColor("#$hex"))
            else -> Color.Gray
        }
    } catch (e: IllegalArgumentException) {
        Color.Gray
    }
}