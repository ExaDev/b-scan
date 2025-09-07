import { InventoryItem } from './InventoryItem';
import { WeightInferenceEngine } from './WeightInferenceEngine';
import {
  InventoryItemData,
  TrackingMode,
  StockMovementType,
  WeightMeasurement,
  CalibrationResult,
  InferenceResult,
  WeightUpdateResult,
  WeightInferenceConfig,
  InventoryStats,
  ReorderConfig,
} from './types';

/**
 * Main inventory service that manages inventory items with weight-based inference
 */
export class InventoryService {
  private items: Map<string, InventoryItem> = new Map();
  private inferenceEngine: WeightInferenceEngine;

  constructor(config?: Partial<WeightInferenceConfig>) {
    const defaultConfig: WeightInferenceConfig = {
      enableAutoInference: true,
      confidenceThreshold: 0.7,
      maxTolerancePercent: 10,
      minSamplesForCalibration: 3,
      statisticalMethod: 'MEAN',
      outlierDetection: true,
      outlierThresholdStdDev: 2.0,
    };

    this.inferenceEngine = new WeightInferenceEngine({ ...defaultConfig, ...config });
  }

  /**
   * Adds a new inventory item
   */
  addItem(itemData: InventoryItemData): InventoryItem {
    if (this.items.has(itemData.id)) {
      throw new Error(`Item with ID ${itemData.id} already exists`);
    }

    const item = new InventoryItem(itemData);
    this.items.set(itemData.id, item);
    return item;
  }

  /**
   * Creates a new inventory item with default values
   */
  createItem(
    id: string,
    name: string,
    description?: string,
    trackingMode: TrackingMode = TrackingMode.DISCRETE,
    initialQuantity: number = 0,
    reorderConfig?: Partial<ReorderConfig>
  ): InventoryItem {
    const defaultReorderConfig: ReorderConfig = {
      enabled: false,
      minQuantity: 0,
      reorderQuantity: 10,
      reorderPoint: 5,
      autoReorder: false,
      leadTimeDays: 7,
      ...reorderConfig,
    };

    const itemData: InventoryItemData = {
      id,
      name,
      trackingMode,
      currentQuantity: initialQuantity,
      reorderConfig: defaultReorderConfig,
      movements: [],
      lastUpdated: new Date(),
      createdAt: new Date(),
    };

    // Add optional properties conditionally
    if (description !== undefined) {
      itemData.description = description;
    }

    return this.addItem(itemData);
  }

  /**
   * Gets an inventory item by ID
   */
  getItem(id: string): InventoryItem | undefined {
    return this.items.get(id);
  }

  /**
   * Gets all inventory items
   */
  getAllItems(): InventoryItem[] {
    return Array.from(this.items.values());
  }

  /**
   * Removes an inventory item
   */
  removeItem(id: string): boolean {
    return this.items.delete(id);
  }

  /**
   * Calibrates unit weight for an inventory item using multiple measurements
   */
  calibrateUnitWeight(
    itemId: string,
    measurements: WeightMeasurement[],
    method: 'SINGLE_SAMPLE' | 'MULTIPLE_SAMPLES' | 'STATISTICAL' = 'MULTIPLE_SAMPLES'
  ): CalibrationResult {
    const item = this.items.get(itemId);
    if (!item) {
      return {
        success: false,
        confidence: 0,
        samplesUsed: 0,
        message: `Item with ID ${itemId} not found`,
        timestamp: new Date(),
      };
    }

    if (method === 'SINGLE_SAMPLE' || method === 'MULTIPLE_SAMPLES') {
      // Use item's built-in calibration method
      return item.updateCalibration(measurements, method);
    } else {
      // Use inference engine for statistical method
      const engineResult = this.inferenceEngine.calibrateUnitWeight(measurements);
      
      if (engineResult.success && engineResult.unitWeight !== undefined) {
        // Apply the calibration to the item
        return item.updateCalibration(measurements, 'STATISTICAL');
      }
      
      return engineResult;
    }
  }

  /**
   * Infers quantity from weight for an inventory item
   */
  inferFromWeight(itemId: string, weight: number, tolerance?: number): InferenceResult {
    const item = this.items.get(itemId);
    if (!item) {
      return {
        success: false,
        confidence: 0,
        totalWeight: weight,
        method: 'FALLBACK',
        message: `Item with ID ${itemId} not found`,
        timestamp: new Date(),
      };
    }

    if (!item.isCalibrated) {
      return {
        success: false,
        confidence: 0,
        totalWeight: weight,
        method: 'FALLBACK',
        message: `Item ${itemId} is not calibrated for weight inference`,
        timestamp: new Date(),
      };
    }

    // Use inference engine for advanced inference
    return this.inferenceEngine.inferFromWeight(weight, item.calibrationData!, tolerance);
  }

  /**
   * Updates inventory quantity based on weight measurement
   */
  updateFromWeightMeasurement(
    itemId: string,
    weight: number,
    movementType: StockMovementType = StockMovementType.ADJUSTMENT,
    notes?: string,
    autoApply: boolean = false
  ): WeightUpdateResult {
    const item = this.items.get(itemId);
    if (!item) {
      return {
        success: false,
        previousQuantity: 0,
        weightChange: 0,
        confidence: 0,
        message: `Item with ID ${itemId} not found`,
        timestamp: new Date(),
      };
    }

    if (!this.inferenceEngine.getConfig().enableAutoInference && !autoApply) {
      return {
        success: false,
        previousQuantity: item.currentQuantity,
        weightChange: weight - (item.currentWeight || 0),
        confidence: 0,
        message: 'Auto inference is disabled',
        timestamp: new Date(),
      };
    }

    const inferenceResult = this.inferFromWeight(itemId, weight);
    
    if (!inferenceResult.success || inferenceResult.inferredQuantity === undefined) {
      return {
        success: false,
        previousQuantity: item.currentQuantity,
        weightChange: weight - (item.currentWeight || 0),
        confidence: 0,
        message: `Inference failed: ${inferenceResult.message}`,
        timestamp: new Date(),
      };
    }

    // Check confidence threshold
    const config = this.inferenceEngine.getConfig();
    if (inferenceResult.confidence < config.confidenceThreshold && !autoApply) {
      return {
        success: false,
        previousQuantity: item.currentQuantity,
        weightChange: weight - (item.currentWeight || 0),
        confidence: inferenceResult.confidence,
        message: `Inference confidence ${(inferenceResult.confidence * 100).toFixed(1)}% below threshold ${(config.confidenceThreshold * 100).toFixed(1)}%`,
        timestamp: new Date(),
      };
    }

    // Update the item
    return item.updateQuantity(
      inferenceResult.inferredQuantity,
      movementType,
      weight,
      notes,
      inferenceResult.confidence
    );
  }

  /**
   * Estimates weight from quantity for an inventory item
   */
  estimateWeightFromQuantity(itemId: string, quantity: number): {
    success: boolean;
    estimatedWeight?: number;
    confidence: number;
    range?: { min: number; max: number };
    message: string;
  } {
    const item = this.items.get(itemId);
    if (!item) {
      return {
        success: false,
        confidence: 0,
        message: `Item with ID ${itemId} not found`,
      };
    }

    if (!item.isCalibrated) {
      return {
        success: false,
        confidence: 0,
        message: `Item ${itemId} is not calibrated for weight estimation`,
      };
    }

    try {
      const result = this.inferenceEngine.estimateWeightFromQuantity(quantity, item.calibrationData!);
      return {
        success: true,
        estimatedWeight: result.estimatedWeight,
        confidence: result.confidence,
        range: result.range,
        message: `Estimated ${result.estimatedWeight.toFixed(1)}g for ${quantity} units (confidence: ${(result.confidence * 100).toFixed(1)}%)`,
      };
    } catch (error) {
      return {
        success: false,
        confidence: 0,
        message: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

  /**
   * Validates if a weight measurement is consistent with expected quantity
   */
  validateWeightQuantityConsistency(
    itemId: string,
    weight: number,
    quantity: number,
    tolerance?: number
  ): {
    success: boolean;
    isConsistent: boolean;
    confidence: number;
    expectedWeight?: number;
    error?: number;
    message: string;
  } {
    const item = this.items.get(itemId);
    if (!item) {
      return {
        success: false,
        isConsistent: false,
        confidence: 0,
        message: `Item with ID ${itemId} not found`,
      };
    }

    if (!item.isCalibrated) {
      return {
        success: false,
        isConsistent: false,
        confidence: 0,
        message: `Item ${itemId} is not calibrated for consistency validation`,
      };
    }

    const result = this.inferenceEngine.validateWeightQuantityConsistency(
      weight,
      quantity,
      item.calibrationData!,
      tolerance
    );

    return {
      success: true,
      isConsistent: result.isConsistent,
      confidence: result.confidence,
      expectedWeight: result.expectedWeight,
      error: result.error,
      message: result.isConsistent
        ? `Weight ${weight}g is consistent with ${quantity} units (error: ${(result.error * 100).toFixed(1)}%)`
        : `Weight ${weight}g is inconsistent with ${quantity} units (error: ${(result.error * 100).toFixed(1)}%, expected: ${result.expectedWeight.toFixed(1)}g)`,
    };
  }

  /**
   * Gets items that need reordering
   */
  getItemsNeedingReorder(): InventoryItem[] {
    return this.getAllItems().filter(item => item.needsReorder);
  }

  /**
   * Gets items with low stock
   */
  getLowStockItems(): InventoryItem[] {
    return this.getAllItems().filter(item => item.isLowStock);
  }

  /**
   * Gets items that are calibrated
   */
  getCalibratedItems(): InventoryItem[] {
    return this.getAllItems().filter(item => item.isCalibrated);
  }

  /**
   * Gets inventory statistics
   */
  getInventoryStats(): InventoryStats {
    const allItems = this.getAllItems();
    
    const stats: InventoryStats = {
      totalItems: allItems.length,
      itemsNeedingReorder: allItems.filter(item => item.needsReorder).length,
      itemsWithCalibration: allItems.filter(item => item.isCalibrated).length,
      averageConfidence: 0,
      totalStockValue: 0, // This would need price data to calculate
      lastUpdateTime: new Date(),
    };

    // Calculate average confidence
    const calibratedItems = allItems.filter(item => item.isCalibrated);
    if (calibratedItems.length > 0) {
      stats.averageConfidence = calibratedItems.reduce(
        (sum, item) => sum + item.calibrationConfidence,
        0
      ) / calibratedItems.length;
    }

    return stats;
  }

  /**
   * Searches items by name or description
   */
  searchItems(query: string): InventoryItem[] {
    const lowerQuery = query.toLowerCase();
    return this.getAllItems().filter(item =>
      item.name.toLowerCase().includes(lowerQuery) ||
      (item.description && item.description.toLowerCase().includes(lowerQuery))
    );
  }

  /**
   * Updates inference engine configuration
   */
  updateInferenceConfig(config: Partial<WeightInferenceConfig>): void {
    this.inferenceEngine.updateConfig(config);
  }

  /**
   * Gets current inference engine configuration
   */
  getInferenceConfig(): WeightInferenceConfig {
    return this.inferenceEngine.getConfig();
  }

  /**
   * Exports all inventory data
   */
  exportData(): InventoryItemData[] {
    return this.getAllItems().map(item => item.toData());
  }

  /**
   * Imports inventory data
   */
  importData(itemsData: InventoryItemData[]): { success: boolean; imported: number; failed: number; errors: string[] } {
    let imported = 0;
    let failed = 0;
    const errors: string[] = [];

    for (const itemData of itemsData) {
      try {
        if (this.items.has(itemData.id)) {
          // Update existing item
          const item = new InventoryItem(itemData);
          this.items.set(itemData.id, item);
        } else {
          // Add new item
          this.addItem(itemData);
        }
        imported++;
      } catch (error) {
        failed++;
        errors.push(`Failed to import item ${itemData.id}: ${error instanceof Error ? error.message : 'Unknown error'}`);
      }
    }

    return {
      success: failed === 0,
      imported,
      failed,
      errors,
    };
  }

  /**
   * Clears all inventory data
   */
  clearAll(): void {
    this.items.clear();
  }

  /**
   * Gets the total number of items
   */
  get itemCount(): number {
    return this.items.size;
  }
}