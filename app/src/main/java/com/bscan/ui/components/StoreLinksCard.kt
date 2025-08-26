package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.BambuProduct
import com.bscan.model.PurchaseLinks
import com.bscan.utils.UrlUtils

@Composable
fun StoreLinksCard(
    bambuProduct: BambuProduct,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val purchaseLinks = remember(bambuProduct) { PurchaseLinks.from(bambuProduct) }
    
    if (!purchaseLinks.spoolAvailable && !purchaseLinks.refillAvailable) {
        return // Don't show card if no links available
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Purchase Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (purchaseLinks.spoolAvailable) {
                    PurchaseButton(
                        label = "Buy with Spool",
                        subtitle = "Includes reusable plastic spool",
                        onClick = {
                            purchaseLinks.spoolUrl?.let { url ->
                                UrlUtils.openUrl(context, url)
                            }
                        }
                    )
                }
                
                if (purchaseLinks.refillAvailable) {
                    PurchaseButton(
                        label = "Buy Refill Only",
                        subtitle = "Filament only, no spool included",
                        onClick = {
                            purchaseLinks.refillUrl?.let { url ->
                                UrlUtils.openUrl(context, url)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseButton(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open in browser",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}