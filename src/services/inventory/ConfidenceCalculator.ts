import { WeightMeasurement, CalibrationData } from './types';

/**
 * Utility class for calculating confidence scores in weight-based inference
 */
export class ConfidenceCalculator {
  /**
   * Calculates calibration confidence based on measurement quality and consistency
   */
  static calculateCalibrationConfidence(
    measurements: WeightMeasurement[],
    unitWeight: number,
    standardDeviation: number
  ): number {
    if (measurements.length === 0 || unitWeight <= 0) return 0;

    // Sample size factor (logarithmic scaling)
    const sampleSizeFactor = this.calculateSampleSizeFactor(measurements.length);
    
    // Precision factor based on coefficient of variation
    const precisionFactor = this.calculatePrecisionFactor(standardDeviation, unitWeight);
    
    // Measurement quality factor
    const qualityFactor = this.calculateMeasurementQualityFactor(measurements);
    
    // Consistency factor
    const consistencyFactor = this.calculateConsistencyFactor(measurements, unitWeight);
    
    // Weighted geometric mean for overall confidence
    const weights = { sample: 0.3, precision: 0.3, quality: 0.2, consistency: 0.2 };
    
    return Math.pow(
      Math.pow(sampleSizeFactor, weights.sample) *
      Math.pow(precisionFactor, weights.precision) *
      Math.pow(qualityFactor, weights.quality) *
      Math.pow(consistencyFactor, weights.consistency),
      1 / Object.values(weights).reduce((sum, w) => sum + w, 0)
    );
  }

  /**
   * Calculates inference confidence for weight-to-quantity predictions
   */
  static calculateInferenceConfidence(
    predictedQuantity: number,
    actualWeight: number,
    unitWeight: number,
    calibrationConfidence: number,
    tolerance: number = 0.1
  ): number {
    if (predictedQuantity <= 0 || actualWeight <= 0 || unitWeight <= 0) return 0;

    // Expected weight for the predicted quantity
    const expectedWeight = predictedQuantity * unitWeight;
    
    // Prediction error
    const absoluteError = Math.abs(actualWeight - expectedWeight);
    const relativeError = absoluteError / expectedWeight;
    
    // Error-based confidence (exponential decay)
    const errorFactor = Math.exp(-relativeError / tolerance);
    
    // Quantity uncertainty (larger quantities have more uncertainty)
    const quantityUncertaintyFactor = this.calculateQuantityUncertaintyFactor(predictedQuantity);
    
    // Calibration inheritance factor
    const calibrationFactor = Math.pow(calibrationConfidence, 0.5); // Square root to reduce penalty
    
    return Math.min(1, errorFactor * quantityUncertaintyFactor * calibrationFactor);
  }

  /**
   * Calculates confidence for weight estimation from quantity
   */
  static calculateEstimationConfidence(
    quantity: number,
    calibrationData: CalibrationData,
    toleranceRange: { min: number; max: number }
  ): number {
    if (quantity <= 0) return 0;

    // Base confidence from calibration
    const baseConfidence = calibrationData.confidence;
    
    // Uncertainty propagation (error grows with square root of quantity)
    const uncertaintyFactor = this.calculateQuantityUncertaintyFactor(quantity);
    
    // Range precision factor
    const rangeSize = toleranceRange.max - toleranceRange.min;
    const expectedWeight = quantity * calibrationData.unitWeight;
    const rangePrecisionFactor = expectedWeight > 0 ? Math.exp(-rangeSize / expectedWeight) : 0;
    
    return baseConfidence * uncertaintyFactor * rangePrecisionFactor;
  }

  /**
   * Calculates trend confidence for tracking quantity changes over time
   */
  static calculateTrendConfidence(
    recentMeasurements: WeightMeasurement[],
    trendDirection: 'INCREASING' | 'DECREASING' | 'STABLE',
    expectedDirection?: 'INCREASING' | 'DECREASING' | 'STABLE'
  ): number {
    if (recentMeasurements.length < 2) return 0.5; // Neutral confidence

    // Calculate trend consistency
    const trendConsistency = this.calculateTrendConsistency(recentMeasurements, trendDirection);
    
    // Direction match factor
    const directionMatch = expectedDirection 
      ? (trendDirection === expectedDirection ? 1 : 0.3)
      : 0.7; // Neutral if no expectation
    
    // Measurement recency factor (more recent = higher weight)
    const recencyFactor = this.calculateRecencyFactor(recentMeasurements);
    
    return trendConsistency * directionMatch * recencyFactor;
  }

  /**
   * Calculates overall system confidence based on multiple factors
   */
  static calculateSystemConfidence(
    calibrationConfidences: number[],
    inferenceAccuracies: number[],
    systemUptime: number, // in hours
    errorRate: number // fraction of failed operations
  ): number {
    if (calibrationConfidences.length === 0) return 0;

    // Average calibration confidence
    const avgCalibrationConfidence = calibrationConfidences.reduce((sum, c) => sum + c, 0) / calibrationConfidences.length;
    
    // Average inference accuracy
    const avgInferenceAccuracy = inferenceAccuracies.length > 0
      ? inferenceAccuracies.reduce((sum, a) => sum + a, 0) / inferenceAccuracies.length
      : avgCalibrationConfidence;
    
    // System stability factor (based on uptime and error rate)
    const stabilityFactor = this.calculateSystemStabilityFactor(systemUptime, errorRate);
    
    // Data quality factor
    const dataQualityFactor = Math.min(
      calibrationConfidences.length / 10, // More calibrated items = better quality
      1
    );
    
    return (avgCalibrationConfidence * 0.4 + 
            avgInferenceAccuracy * 0.4 + 
            stabilityFactor * 0.1 + 
            dataQualityFactor * 0.1);
  }

  // Private helper methods

  private static calculateSampleSizeFactor(sampleCount: number): number {
    // Logarithmic scaling: 1 sample = 0.5, 10 samples = ~0.8, 100 samples = ~0.95
    return Math.min(0.95, 0.3 + 0.65 * Math.log(sampleCount + 1) / Math.log(101));
  }

  private static calculatePrecisionFactor(standardDeviation: number, unitWeight: number): number {
    if (unitWeight <= 0) return 0;
    
    const coefficientOfVariation = standardDeviation / unitWeight;
    
    // CV < 0.05 = excellent (1.0), CV > 0.2 = poor (0.1)
    if (coefficientOfVariation <= 0.05) return 1.0;
    if (coefficientOfVariation >= 0.2) return 0.1;
    
    // Linear interpolation between excellent and poor
    return 1.0 - (coefficientOfVariation - 0.05) * (0.9 / 0.15);
  }

  private static calculateMeasurementQualityFactor(measurements: WeightMeasurement[]): number {
    if (measurements.length === 0) return 0;

    // Average individual measurement confidence
    const avgMeasurementConfidence = measurements.reduce((sum, m) => sum + m.confidence, 0) / measurements.length;
    
    // Source quality factor
    const sourceScores = { MANUAL: 0.7, SCALE: 1.0, INFERENCE: 0.6, CALIBRATION: 0.9 };
    const avgSourceScore = measurements.reduce((sum, m) => sum + sourceScores[m.source], 0) / measurements.length;
    
    return Math.sqrt(avgMeasurementConfidence * avgSourceScore);
  }

  private static calculateConsistencyFactor(measurements: WeightMeasurement[], unitWeight: number): number {
    if (measurements.length < 2 || unitWeight <= 0) return 1.0;

    const unitWeights = measurements
      .filter(m => m.quantity !== undefined && m.quantity > 0)
      .map(m => m.weight / m.quantity!);

    if (unitWeights.length < 2) return 1.0;

    const mean = unitWeights.reduce((sum, w) => sum + w, 0) / unitWeights.length;
    const variance = unitWeights.reduce((sum, w) => sum + Math.pow(w - mean, 2), 0) / unitWeights.length;
    const cv = Math.sqrt(variance) / mean;

    // Lower CV = higher consistency
    return Math.max(0.1, Math.exp(-cv / 0.1));
  }

  private static calculateQuantityUncertaintyFactor(quantity: number): number {
    // Uncertainty grows with square root of quantity (Poisson-like behavior)
    // Normalized so that quantity 1 = 1.0 confidence, quantity 100 = ~0.7 confidence
    return Math.exp(-Math.sqrt(quantity) / 20);
  }

  private static calculateTrendConsistency(
    measurements: WeightMeasurement[],
    expectedTrend: 'INCREASING' | 'DECREASING' | 'STABLE'
  ): number {
    if (measurements.length < 2) return 0.5;

    const sortedMeasurements = [...measurements].sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime());
    let consistentChanges = 0;
    let totalChanges = 0;

    for (let i = 1; i < sortedMeasurements.length; i++) {
      const prev = sortedMeasurements[i - 1];
      const curr = sortedMeasurements[i];
      const change = curr.weight - prev.weight;

      totalChanges++;

      if (expectedTrend === 'INCREASING' && change > 0) consistentChanges++;
      else if (expectedTrend === 'DECREASING' && change < 0) consistentChanges++;
      else if (expectedTrend === 'STABLE' && Math.abs(change) < 0.01) consistentChanges++;
    }

    return totalChanges > 0 ? consistentChanges / totalChanges : 0.5;
  }

  private static calculateRecencyFactor(measurements: WeightMeasurement[]): number {
    if (measurements.length === 0) return 0;

    const now = new Date();
    const maxAge = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    const weightedSum = measurements.reduce((sum, measurement) => {
      const age = now.getTime() - measurement.timestamp.getTime();
      const recencyWeight = Math.exp(-age / maxAge); // Exponential decay
      return sum + recencyWeight;
    }, 0);

    const maxPossibleSum = measurements.length; // If all measurements were recent
    return Math.min(1, weightedSum / maxPossibleSum);
  }

  private static calculateSystemStabilityFactor(uptimeHours: number, errorRate: number): number {
    // Uptime factor: 24 hours = 0.9, 168 hours (week) = 0.95, 720 hours (month) = 1.0
    const uptimeFactor = Math.min(1, 0.5 + 0.5 * Math.log(uptimeHours + 1) / Math.log(721));
    
    // Error rate factor: 0% errors = 1.0, 5% errors = 0.8, 10% errors = 0.5
    const errorFactor = Math.max(0, 1 - errorRate * 5);
    
    return Math.sqrt(uptimeFactor * errorFactor);
  }

  /**
   * Creates a confidence report with detailed breakdown
   */
  static createConfidenceReport(
    calibrationConfidence: number,
    inferenceConfidence: number,
    systemConfidence: number
  ): {
    overall: number;
    breakdown: {
      calibration: { score: number; level: string };
      inference: { score: number; level: string };
      system: { score: number; level: string };
    };
    recommendation: string;
  } {
    const overall = Math.pow(calibrationConfidence * inferenceConfidence * systemConfidence, 1/3);
    
    const getLevelDescription = (score: number): string => {
      if (score >= 0.9) return 'Excellent';
      if (score >= 0.8) return 'Very Good';
      if (score >= 0.7) return 'Good';
      if (score >= 0.6) return 'Fair';
      if (score >= 0.5) return 'Poor';
      return 'Very Poor';
    };

    const getRecommendation = (overall: number): string => {
      if (overall >= 0.8) return 'System is highly reliable for automated operations';
      if (overall >= 0.7) return 'System is suitable for most operations with occasional manual verification';
      if (overall >= 0.6) return 'System requires regular manual verification';
      if (overall >= 0.5) return 'System needs significant improvement before reliable use';
      return 'System requires recalibration and manual oversight';
    };

    return {
      overall,
      breakdown: {
        calibration: { score: calibrationConfidence, level: getLevelDescription(calibrationConfidence) },
        inference: { score: inferenceConfidence, level: getLevelDescription(inferenceConfidence) },
        system: { score: systemConfidence, level: getLevelDescription(systemConfidence) },
      },
      recommendation: getRecommendation(overall),
    };
  }
}