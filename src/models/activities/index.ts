/**
 * Activity Models Export
 * 
 * This module exports all activity-related types, interfaces, and models
 * for the B-Scan application's activity tracking system.
 */

// Base activity types and constants
export * from './ActivityTypes';
export * from './BaseActivity';

// Specific activity models
export * from './ScanOccurrence';
export * from './CalibrationActivity';
export * from './MeasurementActivity';
export * from './StockMovementActivity';
export * from './ConsumptionDistributionActivity';

// Activity results and progress tracking
export * from './ActivityResults';

// Activity relationships and entity tracking
export * from './ActivityRelationships';

// Re-export commonly used types for convenience
export type {
  BaseActivity,
  ActivityMetadata,
  ActivityResult,
  ActivityProgress,
  ActivityRelationship as Relationship,
} from './BaseActivity';

export type {
  ScanOccurrence,
  ScanData,
  ScanAttempt,
} from './ScanOccurrence';

export type {
  CalibrationActivity,
  CalibrationConfiguration,
  CalibrationData,
} from './CalibrationActivity';

export type {
  MeasurementActivity,
  MeasurementConfiguration,
  MeasurementData,
} from './MeasurementActivity';

export type {
  StockMovementActivity,
  StockMovementConfiguration,
  StockMovementData,
} from './StockMovementActivity';

export type {
  ConsumptionDistributionActivity,
  ConsumptionDistributionConfiguration,
  ConsumptionDistributionData,
} from './ConsumptionDistributionActivity';

export type {
  ActivityProgressTracker,
  TypedActivityResult,
  ActivityResultAnalytics,
} from './ActivityResults';

export type {
  ActivityRelationship,
  ActivityEntity,
  RelationshipGraph,
} from './ActivityRelationships';

// Import types needed for unions and type guards
import type { BaseActivity } from './BaseActivity';
import type { ScanOccurrence } from './ScanOccurrence';
import type { CalibrationActivity } from './CalibrationActivity';
import type { MeasurementActivity } from './MeasurementActivity';
import type { StockMovementActivity } from './StockMovementActivity';
import type { ConsumptionDistributionActivity } from './ConsumptionDistributionActivity';
import { ActivityType } from './ActivityTypes';

// Type unions for discriminated unions
export type AnyActivity = 
  | ScanOccurrence
  | CalibrationActivity
  | MeasurementActivity
  | StockMovementActivity
  | ConsumptionDistributionActivity;

export type AnyActivityConfiguration =
  | ScanOccurrence['configuration']
  | CalibrationActivity['configuration']
  | MeasurementActivity['configuration']
  | StockMovementActivity['configuration']
  | ConsumptionDistributionActivity['configuration'];

export type AnyActivityData =
  | ScanOccurrence['scanData']
  | CalibrationActivity['calibrationData']
  | MeasurementActivity['measurementData']
  | StockMovementActivity['movementData']
  | ConsumptionDistributionActivity['distributionData'];

// Helper type guards
export const isActivity = (obj: unknown): obj is BaseActivity => {
  return (
    obj != null &&
    typeof obj === 'object' &&
    'metadata' in obj &&
    'configuration' in obj &&
    typeof (obj as BaseActivity).metadata === 'object' &&
    typeof (obj as BaseActivity).configuration === 'object'
  );
};

export const isScanOccurrence = (activity: BaseActivity): activity is ScanOccurrence => {
  return activity.metadata.type === ActivityType.SCAN_OCCURRENCE;
};

export const isCalibrationActivity = (activity: BaseActivity): activity is CalibrationActivity => {
  return activity.metadata.type === ActivityType.CALIBRATION;
};

export const isMeasurementActivity = (activity: BaseActivity): activity is MeasurementActivity => {
  return activity.metadata.type === ActivityType.MEASUREMENT;
};

export const isStockMovementActivity = (activity: BaseActivity): activity is StockMovementActivity => {
  return activity.metadata.type === ActivityType.STOCK_MOVEMENT;
};

export const isConsumptionDistributionActivity = (activity: BaseActivity): activity is ConsumptionDistributionActivity => {
  return activity.metadata.type === ActivityType.CONSUMPTION_DISTRIBUTION;
};