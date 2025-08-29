package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.BambuProduct
import com.bscan.model.PurchaseLinks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseLinksCard(
    product: BambuProduct,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val purchaseLinks = PurchaseLinks.from(product)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Purchase Options",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${product.productLine} • ${product.colorName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Spool purchase button
                if (purchaseLinks.spoolAvailable) {
                    OutlinedButton(
                        onClick = { 
                            purchaseLinks.spoolUrl?.let { url ->
                                uriHandler.openUri(url)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "With Spool",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = product.mass,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                // Refill purchase button
                if (purchaseLinks.refillAvailable) {
                    OutlinedButton(
                        onClick = { 
                            purchaseLinks.refillUrl?.let { url ->
                                uriHandler.openUri(url)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Refill Only",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = product.mass,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                // Handle case where neither is available (shouldn't happen with our data)
                if (!purchaseLinks.spoolAvailable && !purchaseLinks.refillAvailable) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Purchase links not available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Retail SKU info if available
            product.retailSku?.let { sku ->
                Text(
                    text = "SKU: $sku",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactPurchaseLinksCard(
    product: BambuProduct,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val purchaseLinks = PurchaseLinks.from(product)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Buy this filament",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${product.productLine} • ${product.colorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (purchaseLinks.spoolAvailable) {
                FilledTonalButton(
                    onClick = { 
                        purchaseLinks.spoolUrl?.let { url ->
                            uriHandler.openUri(url)
                        }
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Spool")
                }
            }
            
            if (purchaseLinks.refillAvailable) {
                OutlinedButton(
                    onClick = { 
                        purchaseLinks.refillUrl?.let { url ->
                            uriHandler.openUri(url)
                        }
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refill")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PurchaseLinksCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product with both spool and refill options
            PurchaseLinksCard(
                product = BambuProduct(
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
            
            // Product with spool only
            PurchaseLinksCard(
                product = BambuProduct(
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

@Preview(showBackground = true)
@Composable
private fun CompactPurchaseLinksCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactPurchaseLinksCard(
                product = BambuProduct(
                    productLine = "PETG Basic",
                    colorName = "Turquoise",
                    internalCode = "GFG01", 
                    retailSku = "30102",
                    colorHex = "#4ECDC4",
                    spoolUrl = "https://us.store.bambulab.com/products/petg-basic-filament?sku=30102",
                    refillUrl = "https://us.store.bambulab.com/products/petg-basic-refill?sku=30102-refill",
                    mass = "1kg"
                )
            )
            
            CompactPurchaseLinksCard(
                product = BambuProduct(
                    productLine = "PLA Silk",
                    colorName = "Gold",
                    internalCode = "GFS00",
                    retailSku = "11200",
                    colorHex = "#FFD700",
                    spoolUrl = null,
                    refillUrl = "https://us.store.bambulab.com/products/pla-silk-refill?sku=11200-refill",
                    mass = "1kg"
                )
            )
        }
    }
}