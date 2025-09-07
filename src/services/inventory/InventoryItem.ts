import {
  TrackingMode,
  StockMovementType,
  InventoryItemData,
  StockMovement,
  CalibrationData,
  ReorderConfig,
  WeightMeasurement,
  WeightUpdateResult,
  CalibrationResult,
  InferenceResult,
} from './types';

/**
 * Represents an inventory item with weight-based tracking capabilities
 */
export class InventoryItem {
  private data: InventoryItemData;

  constructor(data: InventoryItemData) {
    this.data = { ...data };
    this.validateData();
  }

  /**
   * Validates the inventory item data for consistency
   */
  private validateData(): void {
    if (this.data.currentQuantity < 0) {
      throw new Error('Current quantity cannot be negative');
    }

    if (this.data.currentWeight !== undefined && this.data.currentWeight < 0) {
      throw new Error('Current weight cannot be negative');
    }

    if (this.data.unitWeight !== undefined && this.data.unitWeight <= 0) {
      throw new Error('Unit weight must be positive');
    }

    if (this.data.reorderConfig.minQuantity < 0) {
      throw new Error('Minimum quantity cannot be negative');
    }

    if (this.data.reorderConfig.reorderPoint < 0) {
      throw new Error('Reorder point cannot be negative');
    }
  }

  // Getters
  get id(): string {
    return this.data.id;
  }

  get name(): string {
    return this.data.name;
  }

  get description(): string | undefined {
    return this.data.description;
  }

  get trackingMode(): TrackingMode {
    return this.data.trackingMode;
  }

  get currentQuantity(): number {
    return this.data.currentQuantity;
  }

  get currentWeight(): number | undefined {
    return this.data.currentWeight;
  }

  get unitWeight(): number | undefined {
    return this.data.unitWeight;
  }

  get calibrationData(): CalibrationData | undefined {
    return this.data.calibrationData;
  }

  get reorderConfig(): ReorderConfig {
    return this.data.reorderConfig;
  }

  get movements(): StockMovement[] {
    return [...this.data.movements];
  }

  get lastUpdated(): Date {
    return this.data.lastUpdated;
  }

  get createdAt(): Date {
    return this.data.createdAt;
  }

  // Reorder management
  get needsReorder(): boolean {
    return (
      this.data.reorderConfig.enabled &&
      this.data.currentQuantity <= this.data.reorderConfig.reorderPoint
    );
  }

  get isLowStock(): boolean {
    return this.data.currentQuantity <= this.data.reorderConfig.minQuantity;
  }

  get isCalibrated(): boolean {
    return (
      this.data.calibrationData !== undefined &&
      this.data.calibrationData.confidence > 0.5 &&
      this.data.calibrationData.sampleCount >= 1
    );
  }

  get calibrationConfidence(): number {
    return this.data.calibrationData?.confidence || 0;
  }

  /**
   * Updates the current quantity and optionally the weight
   */
  updateQuantity(
    newQuantity: number,
    movementType: StockMovementType,
    weight?: number,
    notes?: string,
    confidence: number = 1.0
  ): WeightUpdateResult {
    const previousQuantity = this.data.currentQuantity;
    const quantityChange = newQuantity - previousQuantity;
    const weightChange = weight !== undefined && this.data.currentWeight !== undefined
      ? weight - this.data.currentWeight
      : 0;

    if (newQuantity < 0) {
      return {
        success: false,
        previousQuantity,
        weightChange,
        confidence: 0,
        message: 'Quantity cannot be negative',
        timestamp: new Date(),
      };
    }

    // Create movement record
    const movement: StockMovement = {
      id: this.generateMovementId(),
      itemId: this.data.id,
      type: movementType,
      quantityChange,
      weightChange: weightChange !== 0 ? weightChange : undefined,
      previousQuantity,
      newQuantity,
      timestamp: new Date(),
      confidence,
      notes,
      source: 'MANUAL',
    };

    // Update data
    this.data.currentQuantity = newQuantity;
    if (weight !== undefined) {
      this.data.currentWeight = weight;
    }
    this.data.movements.push(movement);
    this.data.lastUpdated = new Date();

    return {
      success: true,
      previousQuantity,
      newQuantity,
      weightChange,
      confidence,
      movementType,
      message: `Successfully updated quantity from ${previousQuantity} to ${newQuantity}`,
      timestamp: new Date(),
    };
  }

  /**
   * Updates calibration data based on new measurements
   */
  updateCalibration(
    measurements: WeightMeasurement[],
    method: 'SINGLE_SAMPLE' | 'MULTIPLE_SAMPLES' | 'STATISTICAL' = 'MULTIPLE_SAMPLES'
  ): CalibrationResult {
    if (measurements.length === 0) {
      return {
        success: false,
        confidence: 0,
        samplesUsed: 0,
        message: 'No measurements provided for calibration',
        timestamp: new Date(),
      };
    }

    // Filter valid measurements (must have both weight and quantity)
    const validMeasurements = measurements.filter(
      m => m.weight > 0 && m.quantity !== undefined && m.quantity > 0
    );

    if (validMeasurements.length === 0) {
      return {
        success: false,
        confidence: 0,
        samplesUsed: 0,
        message: 'No valid measurements with both weight and quantity',
        timestamp: new Date(),
      };
    }

    // Calculate unit weights from measurements
    const unitWeights = validMeasurements.map(m => m.weight / m.quantity!);
    
    let unitWeight: number;
    let standardDeviation: number;
    
    if (method === 'SINGLE_SAMPLE' || unitWeights.length === 1) {
      unitWeight = unitWeights[0];
      standardDeviation = 0;
    } else if (method === 'STATISTICAL') {
      // Remove outliers using IQR method
      const sortedWeights = [...unitWeights].sort((a, b) => a - b);
      const q1 = this.calculatePercentile(sortedWeights, 25);
      const q3 = this.calculatePercentile(sortedWeights, 75);
      const iqr = q3 - q1;
      const lowerBound = q1 - 1.5 * iqr;
      const upperBound = q3 + 1.5 * iqr;
      
      const filteredWeights = unitWeights.filter(w => w >= lowerBound && w <= upperBound);
      
      if (filteredWeights.length === 0) {
        unitWeight = this.calculateMean(unitWeights);
        standardDeviation = this.calculateStandardDeviation(unitWeights);
      } else {
        unitWeight = this.calculateMean(filteredWeights);
        standardDeviation = this.calculateStandardDeviation(filteredWeights);
      }
    } else {
      // MULTIPLE_SAMPLES - simple average
      unitWeight = this.calculateMean(unitWeights);
      standardDeviation = this.calculateStandardDeviation(unitWeights);
    }

    // Calculate confidence based on standard deviation and sample size
    const confidence = this.calculateCalibrationConfidence(
      unitWeights.length,
      standardDeviation,
      unitWeight
    );

    // Update calibration data
    this.data.unitWeight = unitWeight;
    this.data.calibrationData = {
      unitWeight,
      confidence,
      sampleCount: validMeasurements.length,
      standardDeviation,
      measurements: validMeasurements,
      lastCalibrated: new Date(),
      calibrationMethod: method,
    };

    this.data.lastUpdated = new Date();

    return {
      success: true,
      unitWeight,
      confidence,
      samplesUsed: validMeasurements.length,
      standardDeviation,
      message: `Calibration successful with ${validMeasurements.length} samples. Unit weight: ${unitWeight.toFixed(3)}g, confidence: ${(confidence * 100).toFixed(1)}%`,
      timestamp: new Date(),
    };
  }

  /**
   * Infers quantity from weight measurement
   */
  inferQuantityFromWeight(weight: number): InferenceResult {
    if (weight <= 0) {
      return {
        success: false,
        confidence: 0,
        totalWeight: weight,
        method: 'FALLBACK',
        message: 'Weight must be positive',
        timestamp: new Date(),
      };
    }

    if (!this.isCalibrated) {
      return {
        success: false,
        confidence: 0,
        totalWeight: weight,
        method: 'FALLBACK',
        message: 'Item is not calibrated for weight inference',
        timestamp: new Date(),
      };
    }

    const unitWeight = this.data.unitWeight!;
    const inferredQuantity = Math.round(weight / unitWeight);
    
    // Calculate confidence based on how close the weight is to a whole number multiple
    const exactQuantity = weight / unitWeight;
    const quantityError = Math.abs(exactQuantity - inferredQuantity);
    const maxError = 0.5; // Maximum error for full confidence
    const confidence = Math.max(0, 1 - (quantityError / maxError)) * this.calibrationConfidence;

    if (inferredQuantity < 0) {
      return {
        success: false,
        confidence: 0,
        totalWeight: weight,
        method: 'FALLBACK',
        message: 'Inferred quantity is negative',
        timestamp: new Date(),
      };
    }

    return {
      success: true,
      inferredQuantity,
      confidence,
      unitWeight,
      totalWeight: weight,
      method: 'CALIBRATED',
      message: `Inferred ${inferredQuantity} units from ${weight}g (confidence: ${(confidence * 100).toFixed(1)}%)`,
      timestamp: new Date(),
    };
  }

  /**
   * Updates quantity based on weight measurement
   */
  updateFromWeightMeasurement(
    weight: number,
    movementType: StockMovementType = StockMovementType.ADJUSTMENT,
    notes?: string
  ): WeightUpdateResult {
    const inferenceResult = this.inferQuantityFromWeight(weight);
    
    if (!inferenceResult.success || inferenceResult.inferredQuantity === undefined) {
      return {
        success: false,
        previousQuantity: this.data.currentQuantity,
        weightChange: weight - (this.data.currentWeight || 0),
        confidence: 0,
        message: `Failed to infer quantity: ${inferenceResult.message}`,
        timestamp: new Date(),
      };
    }

    return this.updateQuantity(
      inferenceResult.inferredQuantity,
      movementType,
      weight,
      notes,
      inferenceResult.confidence
    );
  }

  /**
   * Gets movement history filtered by type or time range
   */
  getMovements(
    type?: StockMovementType,
    startDate?: Date,
    endDate?: Date
  ): StockMovement[] {
    let filteredMovements = [...this.data.movements];

    if (type) {
      filteredMovements = filteredMovements.filter(m => m.type === type);
    }

    if (startDate) {
      filteredMovements = filteredMovements.filter(m => m.timestamp >= startDate);
    }

    if (endDate) {
      filteredMovements = filteredMovements.filter(m => m.timestamp <= endDate);
    }

    return filteredMovements.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  }

  /**
   * Exports item data
   */
  toData(): InventoryItemData {
    return { ...this.data };
  }

  // Private helper methods
  private generateMovementId(): string {
    return `${this.data.id}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private calculateMean(values: number[]): number {
    return values.reduce((sum, val) => sum + val, 0) / values.length;
  }

  private calculateStandardDeviation(values: number[]): number {
    if (values.length <= 1) return 0;
    
    const mean = this.calculateMean(values);
    const squaredDifferences = values.map(val => Math.pow(val - mean, 2));
    const variance = this.calculateMean(squaredDifferences);
    return Math.sqrt(variance);
  }

  private calculatePercentile(sortedValues: number[], percentile: number): number {
    const index = (percentile / 100) * (sortedValues.length - 1);
    const lower = Math.floor(index);
    const upper = Math.ceil(index);
    
    if (lower === upper) {
      return sortedValues[lower];
    }
    
    const weight = index - lower;
    return sortedValues[lower] * (1 - weight) + sortedValues[upper] * weight;
  }

  private calculateCalibrationConfidence(
    sampleCount: number,
    standardDeviation: number,
    unitWeight: number
  ): number {
    if (sampleCount === 0) return 0;
    if (sampleCount === 1) return 0.7; // Single sample has moderate confidence
    
    // Coefficient of variation (CV) = std dev / mean
    const cv = standardDeviation / unitWeight;
    
    // Base confidence from sample size (logarithmic scaling)
    const sizeConfidence = Math.min(1, Math.log(sampleCount + 1) / Math.log(10));
    
    // Precision confidence based on coefficient of variation
    // CV < 0.05 is excellent, CV > 0.2 is poor
    const precisionConfidence = Math.max(0, 1 - (cv / 0.2));
    
    // Combined confidence (geometric mean)
    return Math.sqrt(sizeConfidence * precisionConfidence);
  }
}