import {
  WeightMeasurement,
  CalibrationResult,
  InferenceResult,
  WeightInferenceConfig,
  CalibrationData,
} from './types';

/**
 * Advanced weight inference engine with bidirectional algorithms
 * Handles weight-to-quantity and quantity-to-weight conversions with confidence scoring
 */
export class WeightInferenceEngine {
  private config: WeightInferenceConfig;

  constructor(config: WeightInferenceConfig) {
    this.config = config;
  }

  /**
   * Calibrates unit weight from multiple weight/quantity measurements
   */
  calibrateUnitWeight(measurements: WeightMeasurement[]): CalibrationResult {
    if (measurements.length === 0) {
      return this.createFailedCalibrationResult('No measurements provided');
    }

    // Filter valid measurements
    const validMeasurements = measurements.filter(
      m => m.weight > 0 && m.quantity !== undefined && m.quantity > 0
    );

    if (validMeasurements.length === 0) {
      return this.createFailedCalibrationResult('No valid measurements with positive weight and quantity');
    }

    if (validMeasurements.length < this.config.minSamplesForCalibration) {
      return this.createFailedCalibrationResult(
        `Insufficient samples: ${validMeasurements.length} < ${this.config.minSamplesForCalibration}`
      );
    }

    // Calculate unit weights for each measurement
    const unitWeights = validMeasurements.map(m => ({
      value: m.weight / m.quantity!,
      measurement: m
    }));

    // Remove outliers if enabled
    const filteredWeights = this.config.outlierDetection
      ? this.removeOutliers(unitWeights)
      : unitWeights;

    if (filteredWeights.length === 0) {
      return this.createFailedCalibrationResult('All measurements were identified as outliers');
    }

    // Calculate final unit weight using configured method
    const unitWeight = this.calculateWeightByMethod(filteredWeights.map(w => w.value));
    const standardDeviation = this.calculateStandardDeviation(filteredWeights.map(w => w.value));
    
    // Calculate confidence
    const confidence = this.calculateCalibrationConfidence(
      filteredWeights.length,
      standardDeviation,
      unitWeight,
      validMeasurements.length
    );

    return {
      success: true,
      unitWeight,
      confidence,
      samplesUsed: filteredWeights.length,
      standardDeviation,
      message: `Calibration successful: ${unitWeight.toFixed(3)}g/unit (${filteredWeights.length} samples, σ=${standardDeviation.toFixed(3)})`,
      timestamp: new Date(),
    };
  }

  /**
   * Infers quantity from weight using calibrated unit weight
   */
  inferFromWeight(
    totalWeight: number,
    calibrationData: CalibrationData,
    tolerance?: number
  ): InferenceResult {
    if (totalWeight <= 0) {
      return this.createFailedInferenceResult(totalWeight, 'Weight must be positive');
    }

    if (!calibrationData || calibrationData.unitWeight <= 0) {
      return this.createFailedInferenceResult(totalWeight, 'Invalid calibration data');
    }

    const unitWeight = calibrationData.unitWeight;
    const exactQuantity = totalWeight / unitWeight;
    
    // Round to nearest integer for discrete quantities
    const inferredQuantity = Math.round(exactQuantity);
    
    if (inferredQuantity <= 0) {
      return this.createFailedInferenceResult(totalWeight, 'Inferred quantity is not positive');
    }

    // Calculate prediction error
    const expectedWeight = inferredQuantity * unitWeight;
    const weightError = Math.abs(totalWeight - expectedWeight);
    const relativeError = weightError / expectedWeight;
    
    // Check tolerance
    const maxTolerance = tolerance || (this.config.maxTolerancePercent / 100);
    if (relativeError > maxTolerance) {
      return {
        success: false,
        confidence: 0,
        unitWeight,
        totalWeight,
        method: 'FALLBACK',
        message: `Weight error ${(relativeError * 100).toFixed(1)}% exceeds tolerance ${(maxTolerance * 100).toFixed(1)}%`,
        timestamp: new Date(),
      };
    }

    // Calculate confidence based on multiple factors
    const confidence = this.calculateInferenceConfidence(
      relativeError,
      calibrationData.confidence,
      calibrationData.sampleCount
    );

    if (confidence < this.config.confidenceThreshold) {
      return {
        success: false,
        confidence,
        unitWeight,
        totalWeight,
        method: 'FALLBACK',
        message: `Inference confidence ${(confidence * 100).toFixed(1)}% below threshold ${(this.config.confidenceThreshold * 100).toFixed(1)}%`,
        timestamp: new Date(),
      };
    }

    return {
      success: true,
      inferredQuantity,
      confidence,
      unitWeight,
      totalWeight,
      method: 'CALIBRATED',
      message: `Inferred ${inferredQuantity} units from ${totalWeight}g (error: ${(relativeError * 100).toFixed(1)}%, confidence: ${(confidence * 100).toFixed(1)}%)`,
      timestamp: new Date(),
    };
  }

  /**
   * Estimates weight from quantity using calibrated unit weight
   */
  estimateWeightFromQuantity(
    quantity: number,
    calibrationData: CalibrationData
  ): { estimatedWeight: number; confidence: number; range: { min: number; max: number } } {
    if (quantity <= 0) {
      throw new Error('Quantity must be positive');
    }

    if (!calibrationData || calibrationData.unitWeight <= 0) {
      throw new Error('Invalid calibration data');
    }

    const estimatedWeight = quantity * calibrationData.unitWeight;
    
    // Calculate uncertainty range based on standard deviation
    const uncertainty = calibrationData.standardDeviation * Math.sqrt(quantity);
    const range = {
      min: Math.max(0, estimatedWeight - 2 * uncertainty), // 95% confidence interval
      max: estimatedWeight + 2 * uncertainty
    };

    // Confidence decreases with quantity due to accumulated error
    const quantityFactor = Math.exp(-quantity / 100); // Exponential decay
    const confidence = calibrationData.confidence * (0.5 + 0.5 * quantityFactor);

    return {
      estimatedWeight,
      confidence: Math.min(1, confidence),
      range,
    };
  }

  /**
   * Validates if a weight measurement is consistent with expected quantity
   */
  validateWeightQuantityConsistency(
    weight: number,
    quantity: number,
    calibrationData: CalibrationData,
    tolerance?: number
  ): { isConsistent: boolean; confidence: number; expectedWeight: number; error: number } {
    const expectedWeight = quantity * calibrationData.unitWeight;
    const error = Math.abs(weight - expectedWeight);
    const relativeError = error / expectedWeight;
    
    const maxTolerance = tolerance || (this.config.maxTolerancePercent / 100);
    const isConsistent = relativeError <= maxTolerance;
    
    // Calculate confidence based on how close the measurement is to expectation
    const confidence = isConsistent 
      ? Math.max(0, 1 - (relativeError / maxTolerance)) * calibrationData.confidence
      : 0;

    return {
      isConsistent,
      confidence,
      expectedWeight,
      error: relativeError,
    };
  }

  /**
   * Updates inference configuration
   */
  updateConfig(newConfig: Partial<WeightInferenceConfig>): void {
    this.config = { ...this.config, ...newConfig };
  }

  /**
   * Gets current configuration
   */
  getConfig(): WeightInferenceConfig {
    return { ...this.config };
  }

  // Private helper methods

  private removeOutliers(
    weightData: { value: number; measurement: WeightMeasurement }[]
  ): { value: number; measurement: WeightMeasurement }[] {
    if (weightData.length < 3) return weightData; // Need at least 3 points for outlier detection

    const values = weightData.map(w => w.value);
    const mean = this.calculateMean(values);
    const stdDev = this.calculateStandardDeviation(values);
    const threshold = this.config.outlierThresholdStdDev;

    return weightData.filter(w => {
      const zScore = Math.abs(w.value - mean) / stdDev;
      return zScore <= threshold;
    });
  }

  private calculateWeightByMethod(values: number[]): number {
    switch (this.config.statisticalMethod) {
      case 'MEDIAN':
        return this.calculateMedian(values);
      case 'WEIGHTED_AVERAGE':
        return this.calculateWeightedAverage(values);
      case 'MEAN':
      default:
        return this.calculateMean(values);
    }
  }

  private calculateMean(values: number[]): number {
    return values.reduce((sum, val) => sum + val, 0) / values.length;
  }

  private calculateMedian(values: number[]): number {
    const sorted = [...values].sort((a, b) => a - b);
    const mid = Math.floor(sorted.length / 2);
    
    if (sorted.length % 2 === 0) {
      const leftMid = sorted[mid - 1];
      const rightMid = sorted[mid];
      if (leftMid !== undefined && rightMid !== undefined) {
        return (leftMid + rightMid) / 2;
      }
    } else {
      const midValue = sorted[mid];
      if (midValue !== undefined) {
        return midValue;
      }
    }
    return 0; // Fallback if all values are undefined
  }

  private calculateWeightedAverage(values: number[]): number {
    // Weight more recent measurements higher (assuming they're ordered by time)
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const weights = values.map((_value, index) => index + 1);
    const totalWeight = weights.reduce((sum, w) => sum + w, 0);
    
    return values.reduce((sum, val, index) => {
      const weight = weights[index];
      if (weight !== undefined) {
        return sum + (val * weight) / totalWeight;
      }
      return sum;
    }, 0);
  }

  private calculateStandardDeviation(values: number[]): number {
    if (values.length <= 1) return 0;
    
    const mean = this.calculateMean(values);
    const squaredDifferences = values.map(val => Math.pow(val - mean, 2));
    const variance = this.calculateMean(squaredDifferences);
    return Math.sqrt(variance);
  }

  private calculateCalibrationConfidence(
    usedSamples: number,
    standardDeviation: number,
    unitWeight: number,
    totalSamples: number
  ): number {
    if (usedSamples === 0) return 0;

    // Base confidence from sample size (logarithmic scaling)
    const sizeConfidence = Math.min(1, Math.log(usedSamples + 1) / Math.log(20));
    
    // Precision confidence based on coefficient of variation
    const cv = standardDeviation / unitWeight;
    const precisionConfidence = Math.max(0, 1 - (cv / 0.15)); // CV < 15% is good
    
    // Outlier penalty (if many samples were rejected)
    const outlierRatio = (totalSamples - usedSamples) / totalSamples;
    const outlierPenalty = Math.max(0.5, 1 - outlierRatio);
    
    // Combined confidence
    return Math.pow(sizeConfidence * precisionConfidence * outlierPenalty, 1/3);
  }

  private calculateInferenceConfidence(
    relativeError: number,
    calibrationConfidence: number,
    sampleCount: number
  ): number {
    // Error-based confidence (lower error = higher confidence)
    const errorConfidence = Math.max(0, 1 - (relativeError / (this.config.maxTolerancePercent / 100)));
    
    // Sample size bonus
    const sampleBonus = Math.min(1, Math.log(sampleCount + 1) / Math.log(10));
    
    // Combined confidence
    return Math.sqrt(errorConfidence * calibrationConfidence * sampleBonus);
  }

  private createFailedCalibrationResult(message: string): CalibrationResult {
    return {
      success: false,
      confidence: 0,
      samplesUsed: 0,
      message,
      timestamp: new Date(),
    };
  }

  private createFailedInferenceResult(totalWeight: number, message: string): InferenceResult {
    return {
      success: false,
      confidence: 0,
      totalWeight,
      method: 'FALLBACK',
      message,
      timestamp: new Date(),
    };
  }
}