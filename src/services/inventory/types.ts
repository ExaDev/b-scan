/**
 * Inventory management types and enums for weight-based inference system
 */

/**
 * Tracking mode for inventory items
 */
export enum TrackingMode {
  DISCRETE = 'DISCRETE',
  CONTINUOUS = 'CONTINUOUS'
}

/**
 * Stock movement types for inventory transactions
 */
export enum StockMovementType {
  INITIAL_STOCK = 'INITIAL_STOCK',
  RESTOCK = 'RESTOCK',
  CONSUMPTION = 'CONSUMPTION',
  ADJUSTMENT = 'ADJUSTMENT',
  WASTE = 'WASTE',
  RETURN = 'RETURN',
  TRANSFER_IN = 'TRANSFER_IN',
  TRANSFER_OUT = 'TRANSFER_OUT'
}

/**
 * Result of weight calibration operations
 */
export interface CalibrationResult {
  success: boolean;
  unitWeight?: number;
  confidence: number;
  samplesUsed: number;
  standardDeviation?: number;
  message: string;
  timestamp: Date;
}

/**
 * Result of weight-to-quantity inference
 */
export interface InferenceResult {
  success: boolean;
  inferredQuantity?: number;
  confidence: number;
  unitWeight?: number;
  totalWeight: number;
  method: 'CALIBRATED' | 'ESTIMATED' | 'FALLBACK';
  message: string;
  timestamp: Date;
}

/**
 * Result of weight measurement update
 */
export interface WeightUpdateResult {
  success: boolean;
  previousQuantity: number;
  newQuantity?: number;
  weightChange: number;
  confidence: number;
  movementType?: StockMovementType;
  message: string;
  timestamp: Date;
}

/**
 * Weight measurement data point
 */
export interface WeightMeasurement {
  weight: number;
  quantity?: number;
  timestamp: Date;
  confidence: number;
  source: 'MANUAL' | 'SCALE' | 'INFERENCE' | 'CALIBRATION';
}

/**
 * Calibration data for weight inference
 */
export interface CalibrationData {
  unitWeight: number;
  confidence: number;
  sampleCount: number;
  standardDeviation: number;
  measurements: WeightMeasurement[];
  lastCalibrated: Date;
  calibrationMethod: 'SINGLE_SAMPLE' | 'MULTIPLE_SAMPLES' | 'STATISTICAL';
}

/**
 * Reorder point configuration
 */
export interface ReorderConfig {
  enabled: boolean;
  minQuantity: number;
  reorderQuantity: number;
  reorderPoint: number;
  autoReorder: boolean;
  leadTimeDays: number;
}

/**
 * Stock movement record
 */
export interface StockMovement {
  id: string;
  itemId: string;
  type: StockMovementType;
  quantityChange: number;
  weightChange?: number;
  previousQuantity: number;
  newQuantity: number;
  timestamp: Date;
  confidence: number;
  notes?: string;
  source: 'MANUAL' | 'SCALE' | 'AUTO_INFERENCE';
}

/**
 * Inventory item tracking data
 */
export interface InventoryItemData {
  id: string;
  name: string;
  description?: string;
  trackingMode: TrackingMode;
  currentQuantity: number;
  currentWeight?: number;
  unitWeight?: number;
  calibrationData?: CalibrationData;
  reorderConfig: ReorderConfig;
  movements: StockMovement[];
  lastUpdated: Date;
  createdAt: Date;
}

/**
 * Weight inference configuration
 */
export interface WeightInferenceConfig {
  enableAutoInference: boolean;
  confidenceThreshold: number;
  maxTolerancePercent: number;
  minSamplesForCalibration: number;
  statisticalMethod: 'MEAN' | 'MEDIAN' | 'WEIGHTED_AVERAGE';
  outlierDetection: boolean;
  outlierThresholdStdDev: number;
}

/**
 * Inventory summary statistics
 */
export interface InventoryStats {
  totalItems: number;
  itemsNeedingReorder: number;
  itemsWithCalibration: number;
  averageConfidence: number;
  totalStockValue: number;
  lastUpdateTime: Date;
}