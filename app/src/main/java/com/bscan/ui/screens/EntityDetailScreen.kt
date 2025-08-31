package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.graph.Entity
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.launch

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
            // Load the entity
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
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