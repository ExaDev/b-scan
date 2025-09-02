package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.bscan.model.graph.Entity
import com.bscan.repository.GraphRepository
import com.bscan.ui.components.visual.parseColorWithAlpha
import com.bscan.ui.components.visual.MaterialDisplayBox
import com.bscan.ui.components.FilamentColorBox
import com.bscan.ui.components.PropertyRow
import com.bscan.ui.components.consumption.ConsumptionEntryBottomSheet
import com.bscan.service.UnitConversionService
import com.bscan.model.graph.entities.InventoryItem
import kotlinx.coroutines.launch

// EntityType constants
private object EntityTypes {
    const val PHYSICAL_COMPONENT = "physical_component"
    const val INVENTORY_ITEM = "inventory_item" 
    const val IDENTIFIER = "identifier"
    const val ACTIVITY = "activity"
    const val LOCATION = "location"
    const val INFORMATION = "information"
    const val VIRTUAL = "virtual"
    const val PERSON = "person"
    const val STOCK_DEFINITION = "stock_definition"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityDetailScreen(
    entityId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val graphRepository = remember { GraphRepository(context) }
    val scope = rememberCoroutineScope()
    
    // State
    var entity by remember { mutableStateOf<Entity?>(null) }
    var relatedEntities by remember { mutableStateOf<List<Entity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(entityId) {
        try {
            // Load entity from GraphRepository (now includes StockDefinitions)
            val foundEntity = graphRepository.getEntity(entityId)
            if (foundEntity != null) {
                entity = foundEntity
                // Get connected entities
                relatedEntities = graphRepository.getConnectedEntities(entityId)
            } else {
                error = "Entity not found"
            }
        } catch (e: Exception) {
            error = "Failed to load entity: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(entity?.label ?: "Entity Details")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                entity?.let { currentEntity ->
                    // Entity Information Card
                    item {
                        EntityInfoCard(entity = currentEntity)
                    }
                    
                    // Entity Properties Card
                    item {
                        EntityPropertiesCard(entity = currentEntity)
                    }
                    
                    // Specialized Content Based on Entity Type
                    item {
                        EntitySpecializedContent(entity = currentEntity)
                    }
                    
                    // Related Entities Section
                    if (relatedEntities.isNotEmpty()) {
                        item {
                            Text(
                                text = "Related Entities",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        items(relatedEntities) { relatedEntity ->
                            RelatedEntityCard(
                                entity = relatedEntity,
                                onNavigateToDetails = onNavigateToDetails
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityInfoCard(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = entity.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "Type: ${entity.entityType}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "ID: ${entity.id}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun EntityPropertiesCard(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    if (entity.properties.isNotEmpty()) {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Properties",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                entity.properties.forEach { (key, value) ->
                    PropertyRow(
                        key = key,
                        value = value,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelatedEntityCard(
    entity: Entity,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            // Navigate to another entity using the entity route
            onNavigateToDetails?.invoke(DetailType.COMPONENT, "entity/${entity.id}")
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entity.entityType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntitySpecializedContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    when (entity.entityType) {
        EntityTypes.PHYSICAL_COMPONENT -> PhysicalComponentContent(entity, modifier)
        EntityTypes.INVENTORY_ITEM -> InventoryItemContent(entity, modifier)
        EntityTypes.IDENTIFIER -> IdentifierContent(entity, modifier)
        EntityTypes.ACTIVITY -> ActivityContent(entity, modifier)
        EntityTypes.LOCATION -> LocationContent(entity, modifier)
        EntityTypes.INFORMATION -> InformationContent(entity, modifier)
        EntityTypes.VIRTUAL -> VirtualContent(entity, modifier)
        EntityTypes.PERSON -> PersonContent(entity, modifier)
        EntityTypes.STOCK_DEFINITION -> StockDefinitionContent(entity, modifier)
        else -> {
            // Default content for unknown entity types
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Entity Type: ${entity.entityType}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "No specialized view available for this entity type.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PhysicalComponentContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    var showConsumptionEntry by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Check if this component has related inventory items
    var hasInventoryTracking by remember { mutableStateOf(false) }
    var relatedInventoryItems by remember { mutableStateOf<List<Entity>>(emptyList()) }
    
    LaunchedEffect(entity.id) {
        scope.launch {
            val graphRepository = GraphRepository(context)
            val connected = graphRepository.getConnectedEntities(entity.id)
            val inventoryItems = connected.filter { it.entityType == "inventory_item" }
            relatedInventoryItems = inventoryItems
            hasInventoryTracking = inventoryItems.isNotEmpty()
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Physical Component",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Display physical properties
            entity.getProperty<String>("material")?.let { material ->
                Text("Material: $material", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("color")?.let { color ->
                Text("Color: $color", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("colorHex")?.let { colorHex ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Color Preview:", style = MaterialTheme.typography.bodyMedium)
                    
                    val material = entity.getProperty<String>("material") ?: "Unknown"
                    MaterialDisplayBox(
                        colorHex = colorHex,
                        materialType = material,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            entity.getProperty<Float>("weight")?.let { weight ->
                Text("Weight: ${weight}g", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("manufacturer")?.let { manufacturer ->
                Text("Manufacturer: $manufacturer", style = MaterialTheme.typography.bodyMedium)
            }
            
            // Show consumption actions if inventory tracking is available
            if (hasInventoryTracking) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Inventory Actions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showConsumptionEntry = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Scale, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Measure & Record")
                    }
                    
                    OutlinedButton(
                        onClick = { /* TODO: Quick add stock action */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Stock")
                    }
                }
            }
        }
    }
    
    // Consumption Entry Sheet
    if (showConsumptionEntry && relatedInventoryItems.isNotEmpty()) {
        // Convert entities to InventoryItems - simplified for this demo
        val inventoryItems = relatedInventoryItems.mapNotNull { entity ->
            // Create InventoryItem from entity for testing
            InventoryItem(
                id = entity.id,
                label = entity.label
            ).apply {
                isConsumable = entity.getProperty<Boolean>("isConsumable") ?: false
            }
        }
        
        ConsumptionEntryBottomSheet(
            availableEntities = inventoryItems,
            unitConversionService = UnitConversionService(),
            onIndividualConsumption = { item, amount, unit, notes ->
                // TODO: Handle individual consumption recording
                showConsumptionEntry = false
            },
            onCompositeConsumption = { compositeId, totalWeight, notes ->
                // TODO: Handle composite consumption recording
                showConsumptionEntry = false
            },
            onDismiss = { showConsumptionEntry = false }
        )
    }
}

@Composable
private fun InventoryItemContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    var showConsumptionEntry by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Inventory Item",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            entity.getProperty<Int>("quantity")?.let { quantity ->
                Text("Quantity: $quantity", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<Float>("currentWeight")?.let { weight ->
                Text("Current Weight: ${weight}g", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<Int>("reorderPoint")?.let { reorderPoint ->
                Text("Reorder Point: $reorderPoint", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("trackingMode")?.let { trackingMode ->
                Text("Tracking Mode: $trackingMode", style = MaterialTheme.typography.bodyMedium)
            }
            
            // Composite consumption properties
            entity.getProperty<Boolean>("isConsumable")?.let { isConsumable ->
                if (isConsumable) {
                    Text("Consumable: Yes", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            entity.getProperty<Float>("fixedMass")?.let { fixedMass ->
                Text("Fixed Mass: ${fixedMass}g", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("componentType")?.let { componentType ->
                Text("Component Type: $componentType", style = MaterialTheme.typography.bodyMedium)
            }
            
            // Consumption Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showConsumptionEntry = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Scale, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record Consumption")
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Quick consume action */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quick Use")
                }
            }
        }
    }
    
    // Consumption Entry Sheet
    if (showConsumptionEntry) {
        // Convert entity to InventoryItem for the sheet
        val inventoryItem = InventoryItem(
            id = entity.id,
            label = entity.label
        ).apply {
            isConsumable = entity.getProperty<Boolean>("isConsumable") ?: false
        }
        
        ConsumptionEntryBottomSheet(
            availableEntities = listOf(inventoryItem),
            unitConversionService = UnitConversionService(),
            onIndividualConsumption = { item, amount, unit, notes ->
                // TODO: Handle individual consumption recording
                showConsumptionEntry = false
            },
            onCompositeConsumption = { compositeId, totalWeight, notes ->
                // TODO: Handle composite consumption recording
                showConsumptionEntry = false
            },
            onDismiss = { showConsumptionEntry = false }
        )
    }
}

@Composable
private fun IdentifierContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Identifier",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            entity.getProperty<String>("identifierType")?.let { type ->
                Text("Type: $type", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("value")?.let { value ->
                Text(
                    text = "Value: $value",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            entity.getProperty<String>("purpose")?.let { purpose ->
                Text("Purpose: $purpose", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ActivityContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            entity.getProperty<String>("activityType")?.let { type ->
                Text("Activity Type: $type", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("timestamp")?.let { timestamp ->
                Text("Timestamp: $timestamp", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("deviceInfo")?.let { deviceInfo ->
                Text("Device: $deviceInfo", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("scanMethod")?.let { method ->
                Text("Scan Method: $method", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LocationContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            entity.getProperty<String>("locationType")?.let { type ->
                Text("Location Type: $type", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("address")?.let { address ->
                Text("Address: $address", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("coordinates")?.let { coordinates ->
                Text("Coordinates: $coordinates", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InformationContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            entity.getProperty<String>("contentType")?.let { type ->
                Text("Content Type: $type", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("encoding")?.let { encoding ->
                Text("Encoding: $encoding", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<Int>("dataSize")?.let { size ->
                Text("Data Size: ${size} bytes", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("content")?.let { content ->
                if (content.length > 200) {
                    Text(
                        text = "Content: ${content.take(200)}...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "Content: $content",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun VirtualContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Virtual Entity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            entity.getProperty<String>("category")?.let { category ->
                Text("Category: $category", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("template")?.let { template ->
                Text("Template: $template", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("abstractConcept")?.let { concept ->
                Text("Concept: $concept", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PersonContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Person",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            entity.getProperty<String>("role")?.let { role ->
                Text("Role: $role", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("organization")?.let { org ->
                Text("Organization: $org", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("contact")?.let { contact ->
                Text("Contact: $contact", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StockDefinitionContent(
    entity: Entity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Stock Definition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            entity.getProperty<String>("sku")?.let { sku ->
                Text("SKU: $sku", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("manufacturer")?.let { manufacturer ->
                Text("Manufacturer: $manufacturer", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("material")?.let { material ->
                Text("Material: $material", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("color")?.let { color ->
                Text("Color: $color", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("colorHex")?.let { colorHex ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Color Preview:", style = MaterialTheme.typography.bodyMedium)
                    
                    val material = entity.getProperty<String>("material") ?: "Unknown"
                    MaterialDisplayBox(
                        colorHex = colorHex,
                        materialType = material,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            entity.getProperty<Float>("weight")?.let { weight ->
                Text("Weight: ${weight}g", style = MaterialTheme.typography.bodyMedium)
            }
            
            entity.getProperty<String>("description")?.let { description ->
                if (description.isNotEmpty()) {
                    Text(
                        text = "Description: $description",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}