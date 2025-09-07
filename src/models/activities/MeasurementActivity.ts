/**
 * MeasurementActivity model for various measurement operations including weight, dimensions, volume, density, etc.
 */

import { BaseActivity } from './BaseActivity';
import { ActivityType, MeasurementType } from './ActivityTypes';

export interface MeasurementConfiguration {
  /** Type of measurement being performed */
  measurementType: MeasurementType;
  
  /** Index signature for additional properties */
  [key: string]: unknown;
  
  /** Target item or entity being measured */
  targetEntity: {
    entityId: string;
    entityType: string;
    description?: string;
  };
  
  /** Measurement settings */
  measurementSettings: {
    /** Unit of measurement */
    unit: string;
    
    /** Number of measurements to take */
    numberOfMeasurements: number;
    
    /** Required precision (decimal places) */
    precision: number;
    
    /** Measurement range */
    measurementRange?: {
      min: number;
      max: number;
    };
    
    /** Whether to calculate statistical values */
    calculateStatistics: boolean;
    
    /** Measurement method */
    measurementMethod: 'manual' | 'sensor' | 'calculated' | 'estimated';
    
    /** Auto-averaging settings */
    autoAveraging?: {
      enabled: boolean;
      windowSize: number;
      outlierRemoval: boolean;
    };
  };
  
  /** Measurement device configuration */
  deviceConfiguration?: {
    deviceId: string;
    deviceType: string;
    calibrationStatus: 'calibrated' | 'uncalibrated' | 'expired';
    lastCalibrationDate?: Date;
    sensitivity: number;
    resolution: number;
  };
  
  /** Environmental compensation */
  environmentalCompensation?: {
    /** Temperature compensation */
    temperatureCompensation: boolean;
    
    /** Humidity compensation */
    humidityCompensation: boolean;
    
    /** Pressure compensation */
    pressureCompensation: boolean;
    
    /** Reference conditions */
    referenceConditions: {
      temperature: number;
      humidity: number;
      pressure: number;
    };
  };
  
  /** Quality control settings */
  qualityControl: {
    /** Acceptable tolerance range */
    toleranceRange: {
      lower: number;
      upper: number;
    };
    
    /** Maximum allowed uncertainty */
    maxUncertainty: number;
    
    /** Repeatability requirement */
    repeatabilityLimit: number;
    
    /** Whether to require verification measurements */
    requireVerification: boolean;
  };
}

export interface MeasurementReading {
  /** Reading number in sequence */
  readingNumber: number;
  
  /** Timestamp of the reading */
  timestamp: Date;
  
  /** Raw measured value */
  rawValue: number;
  
  /** Compensated/corrected value */
  correctedValue?: number;
  
  /** Final processed value */
  finalValue: number;
  
  /** Unit of measurement */
  unit: string;
  
  /** Environmental conditions during measurement */
  environmentalConditions: {
    temperature?: number;
    humidity?: number;
    pressure?: number;
    vibration?: number;
  };
  
  /** Measurement quality indicators */
  qualityIndicators: {
    /** Stability of reading */
    stability: 'stable' | 'fluctuating' | 'drift' | 'unstable';
    
    /** Signal quality */
    signalQuality?: number;
    
    /** Confidence level (0-1) */
    confidence: number;
    
    /** Any warnings or flags */
    warnings?: string[];
  };
  
  /** Corrections applied */
  corrections?: Array<{
    type: string;
    correctionValue: number;
    reason: string;
  }>;
}

export interface MeasurementStatistics {
  /** Number of readings */
  count: number;
  
  /** Mean/average value */
  mean: number;
  
  /** Standard deviation */
  standardDeviation: number;
  
  /** Variance */
  variance: number;
  
  /** Minimum value */
  minimum: number;
  
  /** Maximum value */
  maximum: number;
  
  /** Median value */
  median: number;
  
  /** Range (max - min) */
  range: number;
  
  /** Standard error of the mean */
  standardError: number;
  
  /** Coefficient of variation */
  coefficientOfVariation: number;
  
  /** 95% confidence interval */
  confidenceInterval?: {
    lower: number;
    upper: number;
  };
  
  /** Outliers detected */
  outliers?: number[];
}

export interface UncertaintyAnalysis {
  /** Combined standard uncertainty */
  combinedUncertainty: number;
  
  /** Expanded uncertainty (k=2) */
  expandedUncertainty: number;
  
  /** Coverage factor */
  coverageFactor: number;
  
  /** Uncertainty components */
  uncertaintyComponents: Array<{
    source: string;
    type: 'type_a' | 'type_b';
    value: number;
    contribution: number; // percentage
    description?: string;
  }>;
  
  /** Uncertainty budget summary */
  uncertaintyBudget: {
    totalTypeA: number;
    totalTypeB: number;
    majorContributor: string;
  };
}

export interface MeasurementValidation {
  /** Overall validation result */
  isValid: boolean;
  
  /** Validation checks performed */
  validationChecks: Array<{
    checkName: string;
    passed: boolean;
    expectedValue?: number;
    actualValue: number;
    tolerance?: number;
    message?: string;
  }>;
  
  /** Range validation */
  rangeValidation: {
    withinRange: boolean;
    lowerLimit: number;
    upperLimit: number;
    actualValue: number;
  };
  
  /** Repeatability assessment */
  repeatabilityAssessment?: {
    acceptable: boolean;
    repeatabilityValue: number;
    limit: number;
  };
  
  /** Comparison with reference value */
  referenceComparison?: {
    referenceValue: number;
    deviation: number;
    percentageDeviation: number;
    acceptable: boolean;
  };
}

export interface MeasurementData {
  /** All individual readings */
  readings: MeasurementReading[];
  
  /** Statistical analysis of readings */
  statistics: MeasurementStatistics;
  
  /** Final measurement result */
  finalResult: {
    value: number;
    unit: string;
    uncertainty: number;
    confidenceLevel: number;
  };
  
  /** Uncertainty analysis */
  uncertaintyAnalysis: UncertaintyAnalysis;
  
  /** Validation results */
  validation: MeasurementValidation;
  
  /** Traceability information */
  traceability?: {
    standardUsed?: string;
    calibrationCertificate?: string;
    measurementProcedure?: string;
    operatorId?: string;
  };
  
  /** Additional calculated values */
  derivedValues?: Array<{
    name: string;
    value: number;
    unit: string;
    calculation: string;
  }>;
}

export interface MeasurementActivity extends BaseActivity {
  metadata: BaseActivity['metadata'] & {
    type: ActivityType.MEASUREMENT;
  };
  
  /** Measurement-specific configuration */
  configuration: MeasurementConfiguration;
  
  /** Measurement data and results */
  measurementData: MeasurementData;
}

export interface MeasurementBuilder {
  /** Set measurement type */
  setMeasurementType(type: MeasurementType): MeasurementBuilder;
  
  /** Set target entity */
  setTargetEntity(entityId: string, entityType: string, description?: string): MeasurementBuilder;
  
  /** Set measurement settings */
  setMeasurementSettings(
    unit: string,
    numberOfMeasurements: number,
    precision: number,
    method: MeasurementConfiguration['measurementSettings']['measurementMethod']
  ): MeasurementBuilder;
  
  /** Set measurement range */
  setMeasurementRange(min: number, max: number): MeasurementBuilder;
  
  /** Configure measurement device */
  setDevice(deviceId: string, deviceType: string): MeasurementBuilder;
  
  /** Enable environmental compensation */
  enableEnvironmentalCompensation(
    temperature: boolean,
    humidity: boolean,
    pressure: boolean
  ): MeasurementBuilder;
  
  /** Set quality control parameters */
  setQualityControl(
    toleranceRange: { lower: number; upper: number },
    maxUncertainty: number,
    repeatabilityLimit: number
  ): MeasurementBuilder;
  
  /** Enable auto-averaging */
  enableAutoAveraging(windowSize: number, outlierRemoval: boolean): MeasurementBuilder;
  
  /** Build the measurement activity */
  build(): MeasurementActivity;
}

/** Measurement accuracy classes */
export enum MeasurementAccuracyClass {
  CLASS_I = 'CLASS_I',     // Highest accuracy
  CLASS_II = 'CLASS_II',   // High accuracy  
  CLASS_III = 'CLASS_III', // Medium accuracy
  CLASS_IV = 'CLASS_IV',   // Lower accuracy
}

/** Measurement status indicators */
export enum MeasurementStatus {
  AWAITING_MEASUREMENT = 'AWAITING_MEASUREMENT',
  MEASURING = 'MEASURING',
  STABILIZING = 'STABILIZING',
  COMPLETED = 'COMPLETED',
  OUT_OF_RANGE = 'OUT_OF_RANGE',
  REQUIRES_CALIBRATION = 'REQUIRES_CALIBRATION',
  ERROR = 'ERROR',
}

/** Environmental condition categories */
export enum EnvironmentalCondition {
  NORMAL = 'NORMAL',
  CONTROLLED = 'CONTROLLED',
  EXTREME = 'EXTREME',
  UNCONTROLLED = 'UNCONTROLLED',
}

export interface MeasurementReport {
  /** Report header */
  header: {
    reportId: string;
    generatedDate: Date;
    generatedBy: string;
    measurementDate: Date;
  };
  
  /** Measurement summary */
  summary: {
    measurementType: MeasurementType;
    finalValue: number;
    unit: string;
    uncertainty: number;
    accuracyClass: MeasurementAccuracyClass;
  };
  
  /** Detailed results */
  detailedResults: {
    individualReadings: MeasurementReading[];
    statistics: MeasurementStatistics;
    uncertaintyAnalysis: UncertaintyAnalysis;
  };
  
  /** Equipment and conditions */
  equipment: {
    measurementDevice?: string;
    calibrationStatus: string;
    environmentalConditions: Record<string, number>;
  };
  
  /** Quality assessment */
  qualityAssessment: {
    overallQuality: 'excellent' | 'good' | 'acceptable' | 'poor';
    validationStatus: boolean;
    recommendations?: string[];
  };
}