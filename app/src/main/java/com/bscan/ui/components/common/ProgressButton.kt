package com.bscan.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ProgressButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(if (isLoading) loadingText else text)
    }
}

@Composable
fun OutlinedProgressButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(if (isLoading) loadingText else text)
    }
}

@Composable
fun TextProgressButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(if (isLoading) loadingText else text)
    }
}

@Composable
fun DestructiveProgressButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onError
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(if (isLoading) loadingText else text)
    }
}

@Composable
fun ProgressButtonWithIcon(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                icon()
            }
            Text(if (isLoading) loadingText else text)
        }
    }
}

// Preview Functions
@Preview(showBackground = true)
@Composable
fun ProgressButtonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressButton(
                text = "Save Changes",
                loadingText = "Saving...",
                isLoading = false,
                onClick = { }
            )
            
            ProgressButton(
                text = "Save Changes", 
                loadingText = "Saving...",
                isLoading = true,
                onClick = { }
            )
            
            ProgressButton(
                text = "Save Changes",
                loadingText = "Saving...",
                isLoading = false,
                enabled = false,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OutlinedProgressButtonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedProgressButton(
                text = "Import Data",
                loadingText = "Importing...",
                isLoading = false,
                onClick = { }
            )
            
            OutlinedProgressButton(
                text = "Import Data",
                loadingText = "Importing...",
                isLoading = true,
                onClick = { }
            )
            
            OutlinedProgressButton(
                text = "Import Data",
                loadingText = "Importing...",
                isLoading = false,
                enabled = false,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TextProgressButtonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextProgressButton(
                text = "Skip",
                loadingText = "Processing...",
                isLoading = false,
                onClick = { }
            )
            
            TextProgressButton(
                text = "Skip",
                loadingText = "Processing...",
                isLoading = true,
                onClick = { }
            )
            
            TextProgressButton(
                text = "Skip",
                loadingText = "Processing...",
                isLoading = false,
                enabled = false,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DestructiveProgressButtonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DestructiveProgressButton(
                text = "Delete All",
                loadingText = "Deleting...",
                isLoading = false,
                onClick = { }
            )
            
            DestructiveProgressButton(
                text = "Delete All",
                loadingText = "Deleting...",
                isLoading = true,
                onClick = { }
            )
            
            DestructiveProgressButton(
                text = "Delete All",
                loadingText = "Deleting...",
                isLoading = false,
                enabled = false,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressButtonWithIconPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressButtonWithIcon(
                text = "Download",
                loadingText = "Downloading...",
                isLoading = false,
                onClick = { },
                icon = {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            
            ProgressButtonWithIcon(
                text = "Download",
                loadingText = "Downloading...",
                isLoading = true,
                onClick = { },
                icon = {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            
            ProgressButtonWithIcon(
                text = "Download",
                loadingText = "Downloading...",
                isLoading = false,
                enabled = false,
                onClick = { },
                icon = {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}