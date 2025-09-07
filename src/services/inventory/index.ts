/**
 * Inventory Management System
 * 
 * Advanced inventory management with weight-based inference for React Native B-Scan app.
 * Provides bidirectional weight-quantity inference, calibration, and confidence scoring.
 */

// Main service classes
export { InventoryService } from './InventoryService';
export { InventoryItem } from './InventoryItem';
export { WeightInferenceEngine } from './WeightInferenceEngine';
export { ConfidenceCalculator } from './ConfidenceCalculator';

// Enums (values)
export {
  TrackingMode,
  StockMovementType,
} from './types';

// Types (interfaces)
export type {
  CalibrationResult,
  InferenceResult,
  WeightUpdateResult,
  WeightMeasurement,
  CalibrationData,
  ReorderConfig,
  StockMovement,
  InventoryItemData,
  WeightInferenceConfig,
  InventoryStats,
} from './types';

// Type exports for convenience
export type {
  CalibrationResult as ICalibrationResult,
  InferenceResult as IInferenceResult,
  WeightUpdateResult as IWeightUpdateResult,
  WeightMeasurement as IWeightMeasurement,
  CalibrationData as ICalibrationData,
  ReorderConfig as IReorderConfig,
  StockMovement as IStockMovement,
  InventoryItemData as IInventoryItemData,
  WeightInferenceConfig as IWeightInferenceConfig,
  InventoryStats as IInventoryStats,
} from './types';