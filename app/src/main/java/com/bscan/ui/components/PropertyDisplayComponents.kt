package com.bscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bscan.model.graph.*
import com.bscan.model.graph.entities.TrackingMode
import com.bscan.ui.components.visual.MaterialDisplayBox
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * Enhanced property display component that renders PropertyValue types with appropriate UI
 */
@Composable
fun PropertyRow(
    key: String,
    value: PropertyValue,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Property key with icon
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PropertyTypeIcon(
                propertyType = value.type,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = formatPropertyKey(key),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Property value with type-specific rendering
        PropertyValueDisplay(
            value = value,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Type-specific property value display
 */
@Composable
private fun PropertyValueDisplay(
    value: PropertyValue,
    modifier: Modifier = Modifier
) {
    when (value) {
        is PropertyValue.StringValue -> StringPropertyDisplay(
            value = value.rawValue,
            modifier = modifier
        )
        
        is PropertyValue.IntValue -> NumberPropertyDisplay(
            value = value.rawValue,
            unit = null,
            modifier = modifier
        )
        
        is PropertyValue.LongValue -> NumberPropertyDisplay(
            value = value.rawValue,
            unit = null,
            modifier = modifier
        )
        
        is PropertyValue.FloatValue -> NumberPropertyDisplay(
            value = value.rawValue,
            unit = null,
            modifier = modifier
        )
        
        is PropertyValue.DoubleValue -> NumberPropertyDisplay(
            value = value.rawValue,
            unit = null,
            modifier = modifier
        )
        
        is PropertyValue.BooleanValue -> BooleanPropertyDisplay(
            value = value.rawValue,
            modifier = modifier
        )
        
        is PropertyValue.DateTimeValue -> DateTimePropertyDisplay(
            value = value.rawValue,
            modifier = modifier
        )
        
        is PropertyValue.DateValue -> DatePropertyDisplay(
            value = value.rawValue,
            modifier = modifier
        )
        
        is PropertyValue.ListValue -> ListPropertyDisplay(
            value = value.rawValue,
            modifier = modifier
        )
        
        is PropertyValue.MapValue -> MapPropertyDisplay(
            value = value.rawValue,
            modifier = modifier
        )
        
        is PropertyValue.BytesValue -> BytesPropertyDisplay(
            value = value.rawValue,
            modifier = modifier
        )
        
        is PropertyValue.QuantityValue -> QuantityPropertyDisplay(
            value = value.rawValue,
            modifier = modifier
        )
        
        is PropertyValue.NullValue -> NullPropertyDisplay(modifier = modifier)
    }
}

/**
 * String property display with color preview support
 */
@Composable
private fun StringPropertyDisplay(
    value: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Check if this is a color hex value
        if (isColorHex(value)) {
            ColorPreview(colorHex = value)
        }
        
        // Check if this is a long value that should be truncated
        if (value.length > 30) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value.take(30) + "...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(value))
                    },
                    modifier = Modifier.padding(0.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Copy full value",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Number property display with formatting
 */
@Composable
private fun NumberPropertyDisplay(
    value: Number,
    unit: String?,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatNumber(value) + (unit?.let { " $it" } ?: ""),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
    )
}

/**
 * Boolean property display with chips
 */
@Composable
private fun BooleanPropertyDisplay(
    value: Boolean,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = if (value) "True" else "False",
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (value) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (value) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            labelColor = if (value) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            leadingIconContentColor = if (value) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
        ),
        modifier = modifier
    )
}

/**
 * DateTime property display with relative time
 */
@Composable
private fun DateTimePropertyDisplay(
    value: java.time.LocalDateTime,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = value.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatRelativeTime(value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Date property display
 */
@Composable
private fun DatePropertyDisplay(
    value: java.time.LocalDate,
    modifier: Modifier = Modifier
) {
    Text(
        text = value.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * List property display with expandable preview
 */
@Composable
private fun ListPropertyDisplay(
    value: List<*>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${value.size} items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (value.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.padding(0.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        text = if (expanded) "Collapse" else "Expand",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        if (expanded && value.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    value.take(5).forEachIndexed { index, item ->
                        Text(
                            text = "[$index] ${item.toString().take(50)}${if (item.toString().length > 50) "..." else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (value.size > 5) {
                        Text(
                            text = "... and ${value.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Map property display with expandable key-value pairs
 */
@Composable
private fun MapPropertyDisplay(
    value: Map<String, *>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${value.size} keys",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (value.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.padding(0.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        text = if (expanded) "Collapse" else "Expand",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        if (expanded && value.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    value.entries.take(5).forEach { (key, mapValue) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = mapValue.toString().take(30) + if (mapValue.toString().length > 30) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (value.size > 5) {
                        Text(
                            text = "... and ${value.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bytes property display with hex formatting
 */
@Composable
private fun BytesPropertyDisplay(
    value: ByteArray,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val hexString = value.joinToString("") { "%02x".format(it) }
    
    Column(modifier = modifier) {
        Text(
            text = "${value.size} bytes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (value.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (hexString.length > 24) {
                        hexString.take(24) + "..."
                    } else hexString,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                if (hexString.length > 24) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(hexString))
                        },
                        modifier = Modifier.padding(0.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quantity property display with units and tracking mode
 */
@Composable
private fun QuantityPropertyDisplay(
    value: Quantity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatNumber(value.value) + " " + value.unit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        
        // Tracking mode chip
        AssistChip(
            onClick = { },
            label = {
                Text(
                    text = value.trackingMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = when (value.trackingMode) {
                        TrackingMode.DISCRETE -> Icons.Default.Numbers
                        TrackingMode.CONTINUOUS -> Icons.Default.LinearScale
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            },
            modifier = Modifier.height(24.dp)
        )
    }
}

/**
 * Null property display
 */
@Composable
private fun NullPropertyDisplay(modifier: Modifier = Modifier) {
    Text(
        text = "null",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        fontFamily = FontFamily.Monospace,
        modifier = modifier
    )
}

/**
 * Property type icon
 */
@Composable
private fun PropertyTypeIcon(
    propertyType: PropertyType,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = when (propertyType) {
            PropertyType.STRING -> Icons.Default.TextFields
            PropertyType.INT, PropertyType.LONG, PropertyType.FLOAT, PropertyType.DOUBLE -> Icons.Default.Numbers
            PropertyType.BOOLEAN -> Icons.Default.ToggleOn
            PropertyType.DATETIME -> Icons.Default.Schedule
            PropertyType.DATE -> Icons.Default.DateRange
            PropertyType.LIST -> Icons.AutoMirrored.Filled.List
            PropertyType.MAP -> Icons.Default.DataObject
            PropertyType.BYTES -> Icons.Default.Memory
            PropertyType.QUANTITY -> Icons.Default.Balance
            PropertyType.NULL -> Icons.Default.Block
        },
        contentDescription = propertyType.name,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    )
}

/**
 * Color preview box
 */
@Composable
private fun ColorPreview(
    colorHex: String,
    modifier: Modifier = Modifier
) {
    val color = parseColor(colorHex)
    
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

// Helper functions

private fun formatPropertyKey(key: String): String {
    return key
        .replace("_", " ")
        .replace("([a-z])([A-Z])".toRegex(), "$1 $2")
        .split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
}

private fun formatNumber(value: Number): String {
    return when (value) {
        is Float -> if (value % 1 == 0f) value.toInt().toString() else "%.2f".format(value)
        is Double -> if (value % 1 == 0.0) value.toInt().toString() else "%.2f".format(value)
        else -> value.toString()
    }
}

private fun isColorHex(value: String): Boolean {
    return value.matches(Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"))
}

private fun parseColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: IllegalArgumentException) {
        Color.Gray
    }
}

private fun formatRelativeTime(dateTime: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val duration = java.time.Duration.between(dateTime, now)
    
    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        duration.toDays() < 7 -> "${duration.toDays()}d ago"
        duration.toDays() < 30 -> "${duration.toDays() / 7}w ago"
        duration.toDays() < 365 -> "${duration.toDays() / 30}mo ago"
        else -> "${duration.toDays() / 365}y ago"
    }
}