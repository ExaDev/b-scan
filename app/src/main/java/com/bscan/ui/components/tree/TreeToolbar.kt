package com.bscan.ui.components.tree

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Toolbar with search and tree operations
 */
@Composable
fun TreeToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showOnlyMatching: Boolean,
    onToggleFilter: (Boolean) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    showSearchBar: Boolean = true,
    showToolbar: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search bar
        if (showSearchBar) {
            TreeSearchBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                showOnlyMatching = showOnlyMatching,
                onToggleFilter = onToggleFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Toolbar with actions
        if (showToolbar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/Collapse buttons
                OutlinedButton(
                    onClick = onExpandAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.UnfoldMore,
                        contentDescription = "Expand All",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expand All", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onCollapseAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.UnfoldLess,
                        contentDescription = "Collapse All",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Collapse All", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

