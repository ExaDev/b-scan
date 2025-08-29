package com.bscan.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.bscan.repository.UserDataRepository
import com.bscan.repository.UnifiedDataAccess
import com.bscan.ui.components.list.OverscrollListWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.bscan.ScanState
import com.bscan.model.*
import com.bscan.repository.CatalogRepository
import com.bscan.repository.InterpretedScan
import com.bscan.ui.components.FilamentColorBox
import com.bscan.ui.components.MaterialDisplaySettings
import com.bscan.ui.screens.DetailType
import com.bscan.ui.screens.home.GroupHeader

/**
 * Multi-manufacturer product catalog browser
 * Displays products from all manufacturers with filtering and grouping
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogBrowser(
    allScans: List<InterpretedScan>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: Dp = 100.dp,
    fullPromptHeightDp: Dp = 400.dp
) {
    val context = LocalContext.current
    val catalogRepository = remember { CatalogRepository(context) }
    val userDataRepository = remember { UserDataRepository(context) }
    val unifiedDataAccess = remember { UnifiedDataAccess(catalogRepository, userDataRepository) }
    
    // Get user preferences for catalog display mode and material display settings
    val userData by remember { derivedStateOf { userDataRepository.getUserData() } }
    val catalogDisplayMode = userData?.preferences?.catalogDisplayMode ?: CatalogDisplayMode.COMPLETE_TITLE
    val materialDisplaySettings = userData?.preferences?.materialDisplaySettings ?: MaterialDisplaySettings.DEFAULT
    
    
    // Get catalog data
    val catalog by remember { derivedStateOf { catalogRepository.getCatalog() } }
    
    // Get all products across manufacturers using UnifiedDataAccess
    // Group by unique filament SKU to avoid duplicates (multiple Shopify products per filament)
    val allProducts = remember(catalog) {
        catalog.manufacturers.flatMap { (manufacturerId, manufacturerCatalog) ->
            // Get products from UnifiedDataAccess (includes catalog + user products)
            val products = unifiedDataAccess.getProducts(manufacturerId)
            
            // Group by variantId (filament SKU) and take the first product from each group
            products.groupBy { it.variantId }.values.mapNotNull { productGroup ->
                val product = productGroup.first() // Take first product for each unique filament
                
                ProductWithManufacturer.create(
                    product = product,
                    manufacturerId = manufacturerId,
                    manufacturerName = manufacturerCatalog.displayName,
                    hasRfidMapping = manufacturerCatalog.rfidMappings.values.any { 
                        it.sku == product.variantId 
                    },
                    materialDefinition = manufacturerCatalog.materials[product.materialType],
                    temperatureProfile = manufacturerCatalog.materials[product.materialType]?.let {
                        manufacturerCatalog.temperatureProfiles[it.temperatureProfile]
                    },
                    alternateProducts = productGroup.drop(1) // Store alternate Shopify products for details page
                )
            }
        }.sortedWith(
            compareBy<ProductWithManufacturer> { it.manufacturerName }
                .thenBy { it.product.materialType }
                .thenBy { it.product.colorName }
        )
    }
    
    // Group products based on groupByOption (following pattern from ListComponents.kt)
    val filteredGroupedAndSortedProducts = remember(allProducts, groupByOption) {
        when (groupByOption) {
            GroupByOption.NONE -> allProducts.map { "ungrouped" to listOf(it) }
            GroupByOption.COLOR -> allProducts.groupBy { it.product.colorName }.toList()
            GroupByOption.BASE_MATERIAL -> allProducts.groupBy { 
                it.product.materialType.split(" ").firstOrNull() ?: "Unknown"
            }.toList()
            GroupByOption.MATERIAL_SERIES -> allProducts.groupBy { 
                val parts = it.product.materialType.split(" ")
                if (parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
            }.toList()
        }
    }
    
    OverscrollListWrapper(
        lazyListState = lazyListState,
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan,
        compactPromptHeightDp = compactPromptHeightDp,
        fullPromptHeightDp = fullPromptHeightDp
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Flatten the grouped products into a single list with headers and items
            filteredGroupedAndSortedProducts.forEach { (groupKey, groupProducts) ->
                // Show group header if grouping is enabled
                if (groupByOption != GroupByOption.NONE) {
                    item(key = "header_$groupKey") {
                        GroupHeader(title = groupKey, itemCount = groupProducts.size)
                    }
                }
                
                // Add each product as individual items (not nested items() call)
                groupProducts.forEach { productInfo ->
                    item(key = "${productInfo.manufacturerId}_${productInfo.product.variantId}") {
                        ProductCard(
                            productInfo = productInfo,
                            catalogDisplayMode = catalogDisplayMode,
                            materialDisplaySettings = materialDisplaySettings,
                            onClick = {
                                onNavigateToDetails?.invoke(
                                    DetailType.SKU, 
                                    "${productInfo.manufacturerId}:${productInfo.product.variantId}"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Product information combined with manufacturer details
 */
@ConsistentCopyVisibility
data class ProductWithManufacturer private constructor(
    val product: ProductEntry,
    val manufacturerId: String,
    val manufacturerName: String,
    val hasRfidMapping: Boolean,
    val materialDefinition: MaterialDefinition?,
    val temperatureProfile: TemperatureProfile?,
    val normalizedProduct: com.bscan.data.bambu.NormalizedBambuData.NormalizedProduct,
    val materialColor: com.bscan.data.bambu.NormalizedBambuData.MaterialColor,
    val alternateProducts: List<ProductEntry> = emptyList()
) {
    companion object {
        /**
         * Create ProductWithManufacturer only if complete normalized data exists
         * Returns null if any required data is missing
         */
        fun create(
            product: ProductEntry,
            manufacturerId: String,
            manufacturerName: String,
            hasRfidMapping: Boolean,
            materialDefinition: MaterialDefinition?,
            temperatureProfile: TemperatureProfile?,
            alternateProducts: List<ProductEntry> = emptyList()
        ): ProductWithManufacturer? {
            val normalizedProduct = com.bscan.data.bambu.NormalizedBambuData.getNormalizedProductBySku(product.variantId)
                ?: return null
            
            val materialColor = com.bscan.data.bambu.NormalizedBambuData.getAllMaterialColors()
                .find { 
                    it.colorCode == normalizedProduct.colorCode && 
                    it.materialName == normalizedProduct.materialName && 
                    it.variantName == normalizedProduct.variantName 
                } ?: return null
            
            return ProductWithManufacturer(
                product = product,
                manufacturerId = manufacturerId,
                manufacturerName = manufacturerName,
                hasRfidMapping = hasRfidMapping,
                materialDefinition = materialDefinition,
                temperatureProfile = temperatureProfile,
                normalizedProduct = normalizedProduct,
                materialColor = materialColor,
                alternateProducts = alternateProducts
            )
        }
    }
}






/**
 * Individual product card
 */
@Composable
fun ProductCard(
    productInfo: ProductWithManufacturer,
    catalogDisplayMode: CatalogDisplayMode,
    materialDisplaySettings: MaterialDisplaySettings,
    onClick: () -> Unit
) {
    val product = productInfo.product
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview
            FilamentColorBox(
                colorHex = product.colorHex ?: "#808080", // ProductLookupService should now provide proper colors
                filamentType = product.materialType,
                materialDisplaySettings = materialDisplaySettings,
                modifier = Modifier.size(40.dp)
            )
            
            // Product information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title based on catalog display mode
                Text(
                    text = when (catalogDisplayMode) {
                        CatalogDisplayMode.COMPLETE_TITLE -> 
                            "${productInfo.normalizedProduct.variantName} ${productInfo.materialColor.colorName} ${productInfo.normalizedProduct.materialName}"
                        CatalogDisplayMode.COLOR_FOCUSED -> 
                            productInfo.materialColor.colorName
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Properties based on display mode
                when (catalogDisplayMode) {
                    CatalogDisplayMode.COMPLETE_TITLE -> {
                        Text(
                            text = "SKU: ${product.variantId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CatalogDisplayMode.COLOR_FOCUSED -> {
                        Text(
                            text = "${productInfo.normalizedProduct.materialName} ${productInfo.normalizedProduct.variantName} • SKU: ${product.variantId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Temperature info if available
                productInfo.temperatureProfile?.let { profile ->
                    Text(
                        text = "${profile.minNozzle}-${profile.maxNozzle}°C • Bed: ${profile.bed}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Status indicators
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // RFID mapping status
                Icon(
                    imageVector = if (productInfo.hasRfidMapping) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.Error
                    },
                    contentDescription = if (productInfo.hasRfidMapping) {
                        "Has RFID mapping"
                    } else {
                        "No RFID mapping"
                    },
                    tint = if (productInfo.hasRfidMapping) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(20.dp)
                )
                
                // Material category badge
                productInfo.materialDefinition?.properties?.category?.let { category ->
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

