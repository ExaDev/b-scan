package com.bscan.ui.components.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.*
import java.time.LocalDateTime

/**
 * Search and filter controls for inventory management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySearchAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    selectedTag: String?,
    onTagChange: (String?) -> Unit,
    allComponents: List<Component>,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Get available categories and tags
    val availableCategories = remember(allComponents) {
        allComponents.map { it.category }.distinct().sorted()
    }
    val availableTags = remember(allComponents) {
        allComponents.flatMap { it.tags }.distinct().sorted()
    }
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Search & Filter",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search components...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                )
            )
            
            // Category filter
            if (availableCategories.isNotEmpty()) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { onCategoryChange(null) },
                            label = { Text("All") },
                            selected = selectedCategory == null
                        )
                    }
                    
                    items(availableCategories) { category ->
                        FilterChip(
                            onClick = { 
                                onCategoryChange(if (selectedCategory == category) null else category)
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        getComponentIcon(category),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(category)
                                }
                            },
                            selected = selectedCategory == category
                        )
                    }
                }
            }
            
            // Tag filter
            if (availableTags.isNotEmpty()) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { onTagChange(null) },
                            label = { Text("All") },
                            selected = selectedTag == null
                        )
                    }
                    
                    items(availableTags.take(10)) { tag -> // Limit to prevent UI overflow
                        FilterChip(
                            onClick = { 
                                onTagChange(if (selectedTag == tag) null else tag)
                            },
                            label = { Text(tag) },
                            selected = selectedTag == tag
                        )
                    }
                }
            }
        }
    }
}


