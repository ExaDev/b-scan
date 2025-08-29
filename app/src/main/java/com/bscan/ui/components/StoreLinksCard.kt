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
import androidx.compose.ui.tooling.preview.Preview
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

@Preview(showBackground = true)
@Composable
private fun StoreLinksCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product with both spool and refill options
            StoreLinksCard(
                bambuProduct = BambuProduct(
                    productLine = "PLA Basic",
                    colorName = "Black",
                    internalCode = "GFL00",
                    retailSku = "10100",
                    colorHex = "#000000",
                    spoolUrl = "https://us.store.bambulab.com/products/pla-basic-filament?sku=10100",
                    refillUrl = "https://us.store.bambulab.com/products/pla-basic-refill?sku=10100-refill",
                    mass = "1kg"
                )
            )
            
            // Product with refill only
            StoreLinksCard(
                bambuProduct = BambuProduct(
                    productLine = "PETG Basic",
                    colorName = "Turquoise",
                    internalCode = "GFG01",
                    retailSku = "30102", 
                    colorHex = "#4ECDC4",
                    spoolUrl = null,
                    refillUrl = "https://us.store.bambulab.com/products/petg-basic-refill?sku=30102-refill",
                    mass = "1kg"
                )
            )
            
            // Product with spool only
            StoreLinksCard(
                bambuProduct = BambuProduct(
                    productLine = "ABS",
                    colorName = "Orange Red",
                    internalCode = "GFA01",
                    retailSku = "40101",
                    colorHex = "#FF6B35", 
                    spoolUrl = "https://us.store.bambulab.com/products/abs-filament?sku=40101",
                    refillUrl = null,
                    mass = "1kg"
                )
            )
        }
    }
}