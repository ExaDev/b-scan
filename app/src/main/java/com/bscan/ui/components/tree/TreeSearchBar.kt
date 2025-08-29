package com.bscan.ui.components.tree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Search bar for tree filtering with toggle filter option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    showOnlyMatching: Boolean,
    onToggleFilter: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { 
                Text(
                    "Search components...",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier.weight(1f)
        )

        // Filter toggle
        FilterChip(
            onClick = { onToggleFilter(!showOnlyMatching) },
            label = { 
                Text(
                    "Filter",
                    style = MaterialTheme.typography.labelSmall
                ) 
            },
            selected = showOnlyMatching,
            leadingIcon = if (showOnlyMatching) {
                {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else null
        )
    }
}

