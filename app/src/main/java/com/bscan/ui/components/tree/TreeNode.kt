package com.bscan.ui.components.tree

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.logic.WeightUnit
import com.bscan.model.Component
import com.bscan.model.ComponentIdentifier
import com.bscan.model.IdentifierPurpose
import com.bscan.model.IdentifierType

/**
 * Individual component node in the tree with full interaction support
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TreeNode(
    treeItem: ComponentTreeItem,
    treeState: ComponentTreeState,
    treeActions: ComponentTreeActions,
    preferredWeightUnit: WeightUnit = WeightUnit.GRAMS,
    onComponentClick: (Component) -> Unit = {},
    onComponentLongClick: (Component) -> Unit = {},
    onDragDrop: (draggedId: String, targetId: String) -> Unit = { _, _ -> },
    onAddChild: (parentId: String) -> Unit = {},
    onRemoveComponent: (componentId: String) -> Unit = {},
    onEditMass: (componentId: String) -> Unit = {},
    getChildren: (String) -> List<Component> = { emptyList() },
    modifier: Modifier = Modifier
) {
    val component = treeItem.component
    val isExpanded = treeState.isExpanded(component.id)
    val isSelected = treeState.isSelected(component.id)
    val isLoading = treeState.isLoading(component.id)
    val isDragTarget = treeState.dropTarget == component.id
    val isDragged = treeState.draggedNode == component.id
    val haptic = LocalHapticFeedback.current

    // Animation states
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(200),
        label = "expand_rotation"
    )
    
    val cardElevation by animateFloatAsState(
        targetValue = when {
            isDragged -> 8f
            isDragTarget -> 4f
            isSelected -> 2f
            else -> 1f
        },
        animationSpec = tween(200),
        label = "card_elevation"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isDragged) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .scale(cardScale)
            .alpha(if (isDragged) 0.8f else 1f)
            .pointerInput(component.id) {
                detectDragGestures(
                    onDragStart = {
                        treeActions.startDrag(component.id)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        val draggedId = treeState.draggedNode
                        val targetId = treeState.dropTarget
                        if (draggedId != null && targetId != null && draggedId != targetId) {
                            onDragDrop(draggedId, targetId)
                        }
                        treeActions.completeDrop(draggedId ?: "", targetId)
                    },
                    onDrag = { _, _ ->
                        // Update drop target based on position
                        // This would require more complex hit testing implementation
                    }
                )
            }
            .semantics {
                contentDescription = "Component: ${component.name}, Category: ${component.category}"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isDragTarget -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isDragTarget) {
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    )
                ),
                width = 2.dp
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onComponentClick(component)
                    treeActions.selectNode(component.id)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hierarchy indicators
            HierarchyIndicator(
                depth = treeItem.depth,
                isLast = treeItem.isLast,
                parentIds = treeItem.parentIds,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Expand/collapse button
            if (treeItem.childCount > 0) {
                IconButton(
                    onClick = {
                        treeActions.toggleNode(component.id)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(expandRotation)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            // Component icon
            ComponentIcon(
                category = component.category,
                tags = component.tags,
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 12.dp)
            )

            // Component info
            Column(modifier = Modifier.weight(1f)) {
                // Name and identifier
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Primary identifier chip
                    component.getPrimaryTrackingIdentifier()?.let { identifier ->
                        IdentifierChip(identifier = identifier)
                    }
                }

                // Category and tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                component.category,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        modifier = Modifier.height(24.dp)
                    )

                    // Key tags
                    component.tags.take(2).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (component.tags.size > 2) {
                        Text(
                            "+${component.tags.size - 2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mass information
                MassDisplayChip(
                    massGrams = component.massGrams,
                    fullMassGrams = component.fullMassGrams,
                    variableMass = component.variableMass,
                    inferredMass = component.inferredMass,
                    preferredUnit = preferredWeightUnit,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Child count indicator
                if (treeItem.childCount > 0) {
                    Text(
                        text = "${treeItem.childCount} component${if (treeItem.childCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Add child button (for composite components)
                if (component.isComposite || treeItem.childCount == 0) {
                    IconButton(
                        onClick = { onAddChild(component.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add child component",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Edit mass button (for variable mass components)
                if (component.variableMass) {
                    IconButton(
                        onClick = { onEditMass(component.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit mass",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // More actions menu
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Remove Component") },
                            onClick = {
                                onRemoveComponent(component.id)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Component Details") },
                            onClick = {
                                onComponentLongClick(component)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TreeNodePreview() {
    MaterialTheme {
        // Create mock data
        val mockComponent = Component(
            id = "mock-component-1",
            name = "PLA Basic Red Filament",
            category = "filament",
            massGrams = 250.5f,
            fullMassGrams = 250.0f,
            variableMass = true,
            inferredMass = false,
            isComposite = false,
            tags = listOf("thermoplastic", "bambu"),
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.SKU,
                    value = "GFL00A00K0",
                    purpose = IdentifierPurpose.LOOKUP
                )
            )
        )
        
        val mockTreeItem = ComponentTreeItem(
            component = mockComponent,
            depth = 1,
            isLast = false,
            parentIds = listOf("parent-1"),
            childCount = 2
        )
        
        val (state, actions) = rememberComponentTreeState()
        
        TreeNode(
            treeItem = mockTreeItem,
            treeState = state,
            treeActions = actions,
            modifier = Modifier.padding(8.dp)
        )
    }
}

