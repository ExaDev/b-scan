/**
 * Entity system exports - Graph-based entity architecture for B-Scan React Native app
 * 
 * Based on the Kotlin implementation's sophisticated entity model with:
 * - Property-based storage system with type safety
 * - Validation framework
 * - Quantity system for discrete/continuous tracking
 * - Specialized entities for inventory, activities, and scan data
 */

// Base entity system
export * from './Entity';
export * from './PropertyValue';
export * from './types';

// Core entities
export * from './CoreEntities';
export * from './InventoryEntities';
export * from './ActivityEntities';
export * from './ScanDataEntities';

// Re-export commonly used types for convenience
export type {
  EntityMetadata,
  ValidationResult
} from './Entity';

export type {
  Quantity,
  PropertyType,
  PropertySchema,
  PropertyValidation
} from './PropertyValue';

export type {
  TrackingMode,
  StockMovementType,
  DistributionMethod,
  InferenceResult,
  CalibrationResult,
  WeightUpdateResult,
  CacheEntry,
  CacheStatistics
} from './types';

// Entity type constants
export {
  EntityTypes,
  IdentifierTypes,
  ActivityTypes,
  InventoryRelationshipTypes,
  ScanDataRelationshipTypes
} from './types';

// Utility functions
export {
  generateId
} from './Entity';

export {
  QuantityFactory
} from './PropertyValue';