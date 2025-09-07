# Graph-Based Entity Architecture

This directory contains a graph-based entity system for the B-Scan React Native application, based on the Kotlin implementation's sophisticated entity model.

## Overview

The entity system provides a flexible, type-safe foundation for modeling complex inventory management scenarios with support for:

- **Property-based storage** with type safety and validation
- **Discrete and continuous quantity tracking**
- **Weight-based inference** for inventory management
- **Scan data processing pipeline** with caching
- **Activity tracking** for audit trails

## Core Architecture

### Base Classes

- **`Entity`**: Abstract base class for all trackable objects
- **`PropertyValue`**: Type-safe property value container with serialization
- **`ValidationResult`**: Validation framework for entity constraints
- **`Quantity`**: Interface for discrete/continuous measurements

### Entity Types

#### Inventory Entities (`InventoryEntities.ts`)
- **`PhysicalComponent`**: Physical items (RFID tags, filament, tools)
- **`StockDefinition`**: Product catalog specifications  
- **`InventoryItem`**: Quantity tracking with weight inference

#### Core Entities (`CoreEntities.ts`)
- **`Identifier`**: RFID UIDs, barcodes, QR codes
- **`Location`**: Storage locations, workstations
- **`Person`**: Users, manufacturers, suppliers
- **`Activity`**: Base class for trackable events
- **`Information`**: Documents, specifications, manuals
- **`Virtual`**: Concepts, categories, templates

#### Activity Entities (`ActivityEntities.ts`)
- **`CalibrationActivity`**: Weight/quantity relationship establishment
- **`MeasurementActivity`**: Bidirectional weight/quantity inference
- **`StockMovementActivity`**: Inventory change tracking
- **`ConsumptionDistributionActivity`**: Composite consumption distribution

#### Scan Data Entities (`ScanDataEntities.ts`)
- **`ScanOccurrence`**: Individual scan events (persistent)
- **`RawScanData`**: Encoded/encrypted data (persistent, deduplicated)
- **`DecodedEncrypted`**: Non-encrypted metadata (ephemeral, cached)
- **`EncodedDecrypted`**: Hex-encoded decrypted data (ephemeral, cached)
- **`DecodedDecrypted`**: Fully interpreted data (ephemeral, cached)

## Key Features

### Type-Safe Properties

```typescript
// Get/set with type safety
const quantity = inventoryItem.getProperty<number>('currentQuantity');
inventoryItem.setProperty('manufacturer', 'Bambu Lab');

// Convenience accessors
inventoryItem.currentQuantity = 100;
inventoryItem.manufacturer = 'Bambu Lab';
```

### Quantity System

```typescript
// Discrete quantities (countable items)
const screws = QuantityFactory.discrete(100, 'pieces');
const bolts = QuantityFactory.fromString('50pieces-discrete');

// Continuous quantities (measurable values)  
const filament = QuantityFactory.continuous(1000, 'g');
const plastic = QuantityFactory.fromString('750g');
```

### Weight-Based Inference

```typescript
const item = new InventoryItem('inv-1', 'M3 Screws', TrackingMode.DISCRETE);

// Calibrate: "I have 100 screws weighing 247g total, box weighs 47g"
const calibResult = item.calibrateUnitWeight(247, 100, 47);

// Update: "Box now weighs 187g, how many screws are left?"
const updateResult = item.updateFromWeightMeasurement(187);
console.log(`${updateResult.newQuantity} screws remaining`);
```

### Validation

```typescript
const component = new PhysicalComponent('pc-1', '');
const validation = component.validate();

if (!validation.isValid) {
  console.log('Errors:', (validation as ValidationResult.Invalid).errors);
}
```

### Caching for Ephemeral Entities

```typescript
const decodedData = new DecodedDecrypted('dd-1', 'Bambu PLA Basic');
decodedData.cacheTtlMinutes = 15;

if (decodedData.isExpired()) {
  // Re-process raw scan data
}
```

## Property Types

The system supports these property types with automatic serialization:

- `STRING`, `INT`, `DOUBLE`, `BOOLEAN`
- `DATETIME`, `DATE`
- `LIST`, `MAP`
- `BYTES` (for binary data)
- `QUANTITY` (discrete/continuous with units)

## Constants

### Entity Types
```typescript
EntityTypes.PHYSICAL_COMPONENT = 'physical_component'
EntityTypes.INVENTORY_ITEM = 'inventory_item'
EntityTypes.STOCK_DEFINITION = 'stock_definition'
// ... etc
```

### Activity Types
```typescript
ActivityTypes.SCAN = 'scan'
ActivityTypes.CALIBRATION = 'calibration'
ActivityTypes.MEASUREMENT = 'measurement'
ActivityTypes.STOCK_MOVEMENT = 'stock_movement'
// ... etc
```

### Relationship Types
```typescript
InventoryRelationshipTypes.TRACKS = 'tracks'
InventoryRelationshipTypes.CALIBRATED_BY = 'calibrated_by'
ScanDataRelationshipTypes.SCANNED = 'scanned'
ScanDataRelationshipTypes.DECODED_TO = 'decoded_to'
// ... etc
```

## Usage Examples

### Creating Inventory Items

```typescript
import { 
  InventoryItem, 
  StockDefinition, 
  PhysicalComponent,
  TrackingMode,
  QuantityFactory 
} from './entities';

// Create a stock definition for PLA filament
const stockDef = new StockDefinition('sd-1', 'PLA Basic Black');
stockDef.sku = 'BL-PLA-BK-1KG';
stockDef.manufacturer = 'Bambu Lab';
stockDef.weight = QuantityFactory.continuous(1000, 'g');
stockDef.materialType = 'PLA';
stockDef.colorName = 'Black';

// Create an inventory item tracking this filament spool
const inventoryItem = new InventoryItem('inv-1', 'PLA Spool #1', TrackingMode.CONTINUOUS);
inventoryItem.currentQuantity = 800; // grams remaining
inventoryItem.tareWeight = 200; // empty spool weight
```

### Processing Scan Data

```typescript
import { ScanOccurrence, RawScanData, DecodedDecrypted } from './entities';

// Record scan event
const scanEvent = new ScanOccurrence('scan-1', 'RFID Scan');
scanEvent.scanMethod = 'nfc';
scanEvent.deviceInfo = 'iPhone 15 Pro';

// Store raw scan data (deduplicated by content hash)
const rawData = new RawScanData('raw-1', 'Bambu RFID', 'bambu_rfid');
rawData.rawData = '04E1A2B3C4D5E6F7...';
rawData.contentHash = 'sha256:abc123...';

// Process into structured data (ephemeral, cached)
const decodedData = new DecodedDecrypted('decoded-1', 'Interpreted Data');
decodedData.filamentProperties = JSON.stringify({
  material: 'PLA',
  color: 'Black',
  brand: 'Bambu Lab'
});
decodedData.cacheTtlMinutes = 15;
```

## Architecture Benefits

1. **Type Safety**: TypeScript generics ensure property access safety
2. **Flexibility**: Property-based storage adapts to new requirements
3. **Performance**: Ephemeral entities with TTL caching reduce processing
4. **Auditability**: Activity entities provide complete change history
5. **Extensibility**: New entity types inherit base functionality
6. **Validation**: Built-in constraint checking prevents invalid data

This architecture provides a solid foundation for complex inventory management scenarios while maintaining the flexibility needed for future enhancements.