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
import androidx.compose.ui.tooling.preview.Preview
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

// Mock UrlUtils for preview purposes
@Composable
private fun MockQuickStoreLink(
    productName: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = { /* Mock onClick */ },
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
private fun MockQuickStoreLinkCompact(
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = { /* Mock onClick */ },
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

// Preview Functions
@Preview(showBackground = true)
@Composable
fun QuickStoreLinkPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MockQuickStoreLink()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuickStoreLinkWithProductNamePreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MockQuickStoreLink(
                productName = "PLA Basic - Blue"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuickStoreLinkWithLongProductNamePreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MockQuickStoreLink(
                productName = "PLA Premium High Strength Carbon Fiber Reinforced - Galaxy Black"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuickStoreLinkCompactPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MockQuickStoreLinkCompact()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuickStoreLinkVariationsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Quick Store Link Variations",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Full width link without product name
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Standard Link",
                    style = MaterialTheme.typography.titleMedium
                )
                MockQuickStoreLink()
            }
            
            // Full width link with product name
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "With Product Name",
                    style = MaterialTheme.typography.titleMedium
                )
                MockQuickStoreLink(
                    productName = "PLA Basic - Red"
                )
            }
            
            // Compact link
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Compact Version",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    MockQuickStoreLinkCompact()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuickStoreLinkInCardPreview() {
    MaterialTheme {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Product Details",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "High-quality PLA filament with excellent print characteristics and vibrant colours.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                MockQuickStoreLink(
                    productName = "PLA Basic - Blue"
                )
            }
        }
    }
}