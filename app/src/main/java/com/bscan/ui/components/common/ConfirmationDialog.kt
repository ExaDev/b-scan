package com.bscan.ui.components.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    icon: ImageVector? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let { { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (isDestructive) {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(confirmText)
                }
            } else {
                Button(onClick = onConfirm) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    itemType: String = "item",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    additionalWarning: String? = null
) {
    val message = buildString {
        append("Remove $itemName")
        if (additionalWarning != null) {
            append(" and $additionalWarning")
        }
        append("? This cannot be undone.")
    }
    
    ConfirmationDialog(
        title = "Remove ${itemType.replaceFirstChar { it.uppercase() }}",
        message = message,
        confirmText = "Remove",
        dismissText = "Cancel",
        isDestructive = true,
        icon = Icons.Default.Warning,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ClearDataConfirmationDialog(
    dataType: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Clear $dataType?",
        message = "This will remove all $dataType. This action cannot be undone.",
        confirmText = "Clear",
        dismissText = "Cancel",
        isDestructive = true,
        icon = Icons.Default.Warning,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun CachePurgeConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Purge Cache?",
        message = "This will remove cached data for this spool. The next scan will take longer as it reads fresh data from the tag.",
        confirmText = "Purge",
        dismissText = "Cancel",
        isDestructive = false,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}