/**
 * Common types and enums for the entity system
 */

/**
 * Tracking modes for inventory items
 */
export enum TrackingMode {
  DISCRETE = 'DISCRETE',    // Count-based (pieces, units)
  CONTINUOUS = 'CONTINUOUS' // Weight/volume-based (grams, milliliters)
}

/**
 * Stock movement types
 */
export enum StockMovementType {
  ADDITION = 'ADDITION',       // Adding stock (purchase, refill)
  CONSUMPTION = 'CONSUMPTION', // Using stock (manufacturing, projects)
  ADJUSTMENT = 'ADJUSTMENT',   // Corrections (counting errors)
  TRANSFER = 'TRANSFER',       // Moving between locations
  WASTE = 'WASTE',            // Loss/damage/expiry
  CALIBRATION = 'CALIBRATION'  // Setting initial values
}

/**
 * Distribution methods for composite consumption
 */
export enum DistributionMethod {
  PROPORTIONAL = 'PROPORTIONAL',     // Distribute based on current quantities
  USER_SPECIFIED = 'USER_SPECIFIED', // User manually specified distribution
  EQUAL_SPLIT = 'EQUAL_SPLIT',       // Split equally between all consumables
  WEIGHTED = 'WEIGHTED',             // Distribute based on usage patterns
  INFERRED = 'INFERRED'              // AI/ML-based inference from patterns
}

/**
 * Common entity types
 */
export const EntityTypes = {
  PHYSICAL_COMPONENT: 'physical_component',
  IDENTIFIER: 'identifier',
  LOCATION: 'location',
  PERSON: 'person',
  ACTIVITY: 'activity',
  INFORMATION: 'information',
  VIRTUAL: 'virtual',
  STOCK_DEFINITION: 'stock_definition',
  INVENTORY_ITEM: 'inventory_item'
} as const;

/**
 * Common identifier types
 */
export const IdentifierTypes = {
  RFID_HARDWARE: 'rfid_hardware',
  CONSUMABLE_UNIT: 'consumable_unit',
  QR_CODE: 'qr_code',
  BARCODE: 'barcode',
  SERIAL_NUMBER: 'serial_number',
  SKU: 'sku',
  BATCH_NUMBER: 'batch_number',
  MODEL_NUMBER: 'model_number',
  CUSTOM: 'custom'
} as const;

/**
 * Common activity types
 */
export const ActivityTypes = {
  SCAN: 'scan',
  MAINTENANCE: 'maintenance',
  USAGE: 'usage',
  PURCHASE: 'purchase',
  DISPOSAL: 'disposal',
  CALIBRATION: 'calibration',
  INSPECTION: 'inspection',
  MEASUREMENT: 'measurement',
  STOCK_MOVEMENT: 'stock_movement',
  CONSUMPTION_DISTRIBUTION: 'consumption_distribution'
} as const;

/**
 * Common relationship types for inventory
 */
export const InventoryRelationshipTypes = {
  TRACKS: 'tracks',                              // InventoryItem -> PhysicalComponent/Virtual
  HAD_MOVEMENT: 'had_movement',                  // InventoryItem -> StockMovementActivity
  CALIBRATED_BY: 'calibrated_by',               // InventoryItem -> CalibrationActivity
  MEASURED_BY: 'measured_by',                   // InventoryItem -> MeasurementActivity
  HAS_COMPONENT: 'has_component',               // InventoryItem -> PhysicalComponent (tare weights)
  STORED_AT: 'stored_at',                       // InventoryItem -> Location
  SUPPLIED_BY: 'supplied_by',                   // InventoryItem -> Person (supplier)
  
  // Composite consumption relationships
  DISTRIBUTED_TO: 'distributed_to',             // ConsumptionDistributionActivity -> InventoryItem
  MEASURED_AS_COMPOSITE: 'measured_as_composite', // Entity -> ConsumptionDistributionActivity
  COMPONENT_OF: 'component_of',                 // PhysicalComponent -> PhysicalComponent (composite)
  FIXED_MASS_COMPONENT: 'fixed_mass_component', // Composite -> Fixed mass component
  CONSUMABLE_COMPONENT: 'consumable_component'  // Composite -> Consumable component
} as const;

/**
 * Common scan data relationship types
 */
export const ScanDataRelationshipTypes = {
  SCANNED: 'scanned',                    // ScanOccurrence -> RawScanData
  DECODED_TO: 'decoded_to',              // RawScanData -> DecodedEncrypted
  DECRYPTED_TO: 'decrypted_to',          // RawScanData -> EncodedDecrypted
  INTERPRETED_AS: 'interpreted_as',      // RawScanData -> DecodedDecrypted
  DERIVED_FROM: 'derived_from',          // Any ephemeral -> source entity
  CREATED_ENTITIES: 'created_entities'   // DecodedDecrypted -> PhysicalComponent entities
} as const;

/**
 * Result of inference calculations
 */
export interface InferenceResult {
  inferredQuantity: number;
  inferredWeight: number;
  confidence: number;
  method: string;
  uncertainty?: number;
}

/**
 * Result of calibration calculations
 */
export interface CalibrationResult {
  success: boolean;
  unitWeight?: number;
  netWeight?: number;
  accuracy?: number;
  method?: string;
  error?: string;
}

/**
 * Result of weight-based quantity update
 */
export interface WeightUpdateResult {
  success: boolean;
  newQuantity: number;
  quantityConsumed: number;
  confidence: number;
  inferenceMethod: string;
  error?: string;
}

/**
 * Cache entry for ephemeral entities
 */
export interface CacheEntry<T> {
  entity: T;
  timestamp: Date;
  ttlMinutes: number;
  isExpired(): boolean;
  getRemainingTtl(): number;
}

/**
 * Cache statistics for monitoring
 */
export interface CacheStatistics {
  totalEntries: number;
  expiredEntries: number;
  hitRate: number;
  memoryUsageBytes: number;
  oldestEntryAge: number;
  averageTtl: number;
}