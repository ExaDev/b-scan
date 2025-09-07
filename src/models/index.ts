/**
 * Models Export
 * 
 * This module exports all model types and interfaces for the B-Scan application.
 */

// Export activity models under namespace to avoid conflicts
export * as Activities from './activities';

// Export entity models under namespace to avoid conflicts  
export * as Entities from './entities';

// Re-export commonly used activity types for convenience
export type {
  ActivityType,
  ActivityStatus,
  ActivityResultType,
  DistributionMethod,
  BaseActivity,
  ActivityMetadata,
  ScanOccurrence,
  CalibrationActivity,
  MeasurementActivity,
  StockMovementActivity,
  ConsumptionDistributionActivity,
  ActivityProgressTracker,
  ActivityRelationship,
} from './activities';