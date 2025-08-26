package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.utils.UrlUtils

@Composable
fun QuickStoreLink(
    storeUrl: String,
    productName: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    if (!UrlUtils.isValidUrl(storeUrl)) {
        return // Don't show if URL is invalid
    }
    
    OutlinedButton(
        onClick = { UrlUtils.openUrl(context, storeUrl) },
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Store,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "View in Bambu Store",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                productName?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open in browser",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun QuickStoreLinkCompact(
    storeUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    if (!UrlUtils.isValidUrl(storeUrl)) {
        return // Don't show if URL is invalid
    }
    
    TextButton(
        onClick = { UrlUtils.openUrl(context, storeUrl) },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Store",
                style = MaterialTheme.typography.bodySmall
            )
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open in browser",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}