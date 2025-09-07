# Graph-Based Data Persistence Layer

This module implements a graph-based data persistence layer for the B-Scan React Native application using AsyncStorage. It provides entity management, relationship tracking, caching, and background synchronization capabilities.

## Core Components

### GraphRepository
Main repository class that provides:
- Entity CRUD operations (addEntity, getEntity, updateEntity, deleteEntity)
- Relationship management (addRelationship, findRelatedEntities)
- Query interface with caching
- AsyncStorage persistence with JSON serialization
- Background processing and auto-save functionality

### Graph
In-memory graph structure providing:
- Entity and edge management with efficient indexing
- Graph traversal algorithms (breadth-first search)
- Relationship queries and inventory-specific operations
- Graph metrics and serialization support

### EntityCache
High-performance caching layer featuring:
- LRU eviction policy with configurable TTL
- Query result caching with automatic invalidation
- Memory pressure handling
- Performance statistics and monitoring

### EntityFactory
Factory class for creating entities and relationships:
- Type-safe entity creation (PhysicalComponent, InventoryItem, Identifier, Activity)
- Relationship creation with predefined patterns
- Complex entity system creation (filament spool systems)
- Entity validation and cloning utilities

### BackgroundSyncService
Background processing service providing:
- Task scheduling with priority-based execution
- Automatic retry logic with exponential backoff
- App state awareness (pause/resume)
- Performance metrics and monitoring

## Entity Types

- **PhysicalComponent**: Represents physical filament spools with associated FilamentInfo
- **InventoryItem**: Tracks quantities and locations of items
- **Identifier**: RFID, barcode, or QR code identifiers
- **Activity**: Records user actions and system events

## Relationship Types

- **CONTAINS**: Inventory items containing physical components
- **IDENTIFIED_BY**: Physical components identified by RFID/barcodes
- **PART_OF**: Hierarchical relationships
- **LOCATED_AT**: Spatial relationships
- **CREATED_BY/MODIFIED_BY**: Activity relationships

## Usage Examples

### Initialize Repository
```typescript
import { initializeRepository, createFilamentSpoolEntities } from '../repositories';

const { repository, backgroundService } = await initializeRepository({
  persistenceKey: 'main',
  autoSave: true,
  cacheOptions: { ttl: 300000, maxSize: 1000 }
});
```

### Create Filament Spool System
```typescript
const filamentInfo = { /* FilamentInfo data */ };
const system = await createFilamentSpoolEntities(repository, filamentInfo, {
  rfidValue: 'A1B2C3D4',
  location: 'Storage Room A',
  initialWeight: 1000
});
```

### Query Operations
```typescript
// Find spool by RFID
const spool = await findSpoolByRfid(repository, 'A1B2C3D4');

// Get inventory roots
const rootSpools = await findInventoryRootSpools(repository);

// Get activity history
const history = await getSpoolHistory(repository, spoolId);
```

### Manual Entity Management
```typescript
// Create entities
const physicalComponent = EntityFactory.createPhysicalComponent(filamentInfo);
await repository.addEntity(physicalComponent);

// Create relationships
const edge = EntityFactory.createIdentificationRelationship(componentId, identifierId);
await repository.addRelationship(edge);

// Query with filters
const entities = await repository.query({
  entityType: EntityType.PHYSICAL_COMPONENT,
  createdAfter: Date.now() - 86400000, // Last 24 hours
  limit: 10
});
```

## Performance Features

- **Entity Caching**: Automatic caching with LRU eviction and TTL expiration
- **Query Result Caching**: Cached query results with smart invalidation
- **Background Processing**: Non-blocking persistence and maintenance operations
- **Memory Management**: Memory pressure handling and cache cleanup
- **Optimized Indexing**: Efficient entity type and relationship type indexes

## Persistence Strategy

- **JSON Serialization**: Human-readable storage format
- **Version Management**: Forward-compatible data versioning
- **Auto-save**: Configurable automatic persistence intervals
- **Background Sync**: Non-blocking save operations
- **Error Recovery**: Robust error handling and retry logic

## Monitoring and Debugging

### Repository Metrics
```typescript
const metrics = repository.getMetrics();
console.log('Entities:', metrics.entityCount);
console.log('Relationships:', metrics.edgeCount);
console.log('Type Distribution:', metrics.entityTypeDistribution);
```

### Cache Statistics
```typescript
const stats = repository.getCacheStatistics();
console.log('Hit Rate:', stats.hitRate);
console.log('Cache Size:', stats.size);
```

### Background Service Status
```typescript
const syncMetrics = backgroundService.getMetrics();
console.log('Completed Tasks:', syncMetrics.completedTasks);
console.log('Failed Tasks:', syncMetrics.failedTasks);
console.log('Average Execution Time:', syncMetrics.averageExecutionTime);
```

## Memory Management

The system includes comprehensive memory management:
- Configurable cache sizes with automatic eviction
- Memory pressure detection and cleanup
- Background garbage collection
- Efficient data structures minimizing memory footprint

## Thread Safety

All operations are designed to be thread-safe within React Native's single-threaded JavaScript environment:
- Atomic AsyncStorage operations
- Sequential background task processing
- Consistent state management
- Safe concurrent read operations

## Data Migration

The repository supports versioned data migration:
- Backward compatibility checks
- Graceful degradation for unsupported versions
- Import/export functionality for data transfer
- Schema evolution support

This architecture provides a robust, scalable foundation for managing complex relationships between physical inventory items, their digital identifiers, and associated activities within the B-Scan application.