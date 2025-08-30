package com.bscan.ui.components.inventory

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.ui.screens.InventoryViewMode
import com.bscan.ui.screens.InventorySortMode

/**
 * View mode selector with animated icons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryViewModeSelector(
    selectedViewMode: InventoryViewMode,
    onViewModeChange: (InventoryViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InventoryViewMode.entries.forEach { viewMode ->
                val isSelected = selectedViewMode == viewMode
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200),
                    label = "view_mode_color"
                )
                
                IconButton(
                    onClick = { onViewModeChange(viewMode) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = getViewModeIcon(viewMode),
                            contentDescription = getViewModeLabel(viewMode),
                            tint = animatedColor,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Text(
                            text = getViewModeLabel(viewMode),
                            style = MaterialTheme.typography.labelSmall,
                            color = animatedColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced sorting selector with grouping options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySortingSelector(
    selectedSortMode: InventorySortMode,
    onSortModeChange: (InventorySortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = getSortModeLabel(selectedSortMode),
            onValueChange = { },
            readOnly = true,
            label = { Text("Sort by") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            colors = OutlinedTextFieldDefaults.colors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            InventorySortMode.entries.forEach { sortMode ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = getSortModeIcon(sortMode),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(getSortModeLabel(sortMode))
                        }
                    },
                    onClick = {
                        onSortModeChange(sortMode)
                        isExpanded = false
                    },
                    leadingIcon = if (selectedSortMode == sortMode) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

/**
 * Enhanced filter panel with multiple options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryFilterPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    selectedTag: String?,
    onTagChange: (String?) -> Unit,
    selectedManufacturer: String?,
    onManufacturerChange: (String?) -> Unit,
    availableCategories: List<String>,
    availableTags: List<String>,
    availableManufacturers: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search inventory") },
                placeholder = { Text("Name, category, manufacturer...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Filter chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category filter
                FilterDropdownChip(
                    label = "Category",
                    selectedValue = selectedCategory,
                    onValueChange = onCategoryChange,
                    options = availableCategories,
                    modifier = Modifier.weight(1f)
                )
                
                // Manufacturer filter
                FilterDropdownChip(
                    label = "Manufacturer",
                    selectedValue = selectedManufacturer,
                    onValueChange = onManufacturerChange,
                    options = availableManufacturers,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Tag filter (horizontal scrollable)
            if (availableTags.isNotEmpty()) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(availableTags) { tag ->
                        FilterChip(
                            onClick = {
                                onTagChange(if (selectedTag == tag) null else tag)
                            },
                            label = { Text(tag) },
                            selected = selectedTag == tag,
                            leadingIcon = if (selectedTag == tag) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
            
            // Active filters summary
            val activeFiltersCount = listOfNotNull(
                selectedCategory,
                selectedTag,
                selectedManufacturer,
                if (searchQuery.isNotEmpty()) "search" else null
            ).size
            
            if (activeFiltersCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeFiltersCount filter${if (activeFiltersCount > 1) "s" else ""} applied",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    TextButton(
                        onClick = {
                            onSearchQueryChange("")
                            onCategoryChange(null)
                            onTagChange(null)
                            onManufacturerChange(null)
                        }
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }
    }
}

/**
 * Reusable dropdown chip for filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdownChip(
    label: String,
    selectedValue: String?,
    onValueChange: (String?) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue ?: "All",
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            colors = OutlinedTextFieldDefaults.colors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            // "All" option
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onValueChange(null)
                    isExpanded = false
                },
                leadingIcon = if (selectedValue == null) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
            
            // Individual options
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        isExpanded = false
                    },
                    leadingIcon = if (selectedValue == option) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

// Helper functions for icons and labels
private fun getViewModeIcon(viewMode: InventoryViewMode): ImageVector = when (viewMode) {
    InventoryViewMode.DETAILED -> Icons.Default.ViewAgenda
    InventoryViewMode.COMPACT -> Icons.AutoMirrored.Filled.ViewList
    InventoryViewMode.TABLE -> Icons.Default.TableChart
    InventoryViewMode.GALLERY -> Icons.Default.ViewModule
}

private fun getViewModeLabel(viewMode: InventoryViewMode): String = when (viewMode) {
    InventoryViewMode.DETAILED -> "Detailed"
    InventoryViewMode.COMPACT -> "Compact"
    InventoryViewMode.TABLE -> "Table"
    InventoryViewMode.GALLERY -> "Gallery"
}

private fun getSortModeIcon(sortMode: InventorySortMode): ImageVector = when (sortMode) {
    InventorySortMode.NAME_ASC -> Icons.Default.SortByAlpha
    InventorySortMode.NAME_DESC -> Icons.Default.SortByAlpha
    InventorySortMode.CATEGORY -> Icons.Default.Category
    InventorySortMode.MANUFACTURER -> Icons.Default.Business
    InventorySortMode.LAST_UPDATED -> Icons.Default.Schedule
    InventorySortMode.MASS_DESC -> Icons.Default.FitnessCenter
    InventorySortMode.CHILD_COUNT -> Icons.Default.AccountTree
    InventorySortMode.CREATED_DATE -> Icons.Default.DateRange
}

private fun getSortModeLabel(sortMode: InventorySortMode): String = when (sortMode) {
    InventorySortMode.NAME_ASC -> "Name (A-Z)"
    InventorySortMode.NAME_DESC -> "Name (Z-A)"
    InventorySortMode.CATEGORY -> "Category"
    InventorySortMode.MANUFACTURER -> "Manufacturer"
    InventorySortMode.LAST_UPDATED -> "Last Updated"
    InventorySortMode.MASS_DESC -> "Mass (Heavy First)"
    InventorySortMode.CHILD_COUNT -> "Components Count"
    InventorySortMode.CREATED_DATE -> "Date Created"
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun InventoryViewModeSelectorPreview() {
    MaterialTheme {
        InventoryViewModeSelector(
            selectedViewMode = InventoryViewMode.COMPACT,
            onViewModeChange = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryFilterPanelPreview() {
    MaterialTheme {
        InventoryFilterPanel(
            searchQuery = "",
            onSearchQueryChange = { },
            selectedCategory = null,
            onCategoryChange = { },
            selectedTag = "PLA",
            onTagChange = { },
            selectedManufacturer = null,
            onManufacturerChange = { },
            availableCategories = listOf("filament", "spool", "core"),
            availableTags = listOf("PLA", "Orange", "1.75mm"),
            availableManufacturers = listOf("Bambu Lab", "Prusa", "Hatchbox")
        )
    }
}