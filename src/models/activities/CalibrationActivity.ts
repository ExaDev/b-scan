/**
 * CalibrationActivity model for device and measurement calibration activities
 */

import { BaseActivity } from './BaseActivity';
import { ActivityType, CalibrationType } from './ActivityTypes';

export interface CalibrationConfiguration {
  /** Type of calibration being performed */
  calibrationType: CalibrationType;
  
  /** Target device or sensor being calibrated */
  targetDevice: {
    deviceId: string;
    deviceType: string;
    model?: string;
    serialNumber?: string;
  };
  
  /** Index signature for additional properties */
  [key: string]: unknown;
  
  /** Reference standards or known values for calibration */
  referenceStandards: CalibrationStandard[];
  
  /** Calibration procedure settings */
  procedure: {
    /** Number of calibration points */
    calibrationPoints: number;
    
    /** Number of readings per calibration point */
    readingsPerPoint: number;
    
    /** Tolerance levels for calibration acceptance */
    toleranceLevels: {
      acceptable: number;
      warning: number;
      critical: number;
    };
    
    /** Whether to perform automatic drift correction */
    autoDriftCorrection: boolean;
    
    /** Calibration interval in days */
    calibrationInterval?: number;
  };
  
  /** Environmental conditions for calibration */
  environmentalConditions: {
    /** Required temperature range */
    temperatureRange?: {
      min: number;
      max: number;
      unit: 'celsius' | 'fahrenheit';
    };
    
    /** Required humidity range */
    humidityRange?: {
      min: number;
      max: number;
    };
    
    /** Required pressure conditions */
    pressureConditions?: {
      min: number;
      max: number;
      unit: 'pa' | 'psi' | 'bar';
    };
  };
  
  /** Calibration quality requirements */
  qualityRequirements: {
    /** Minimum accuracy required */
    minAccuracy: number;
    
    /** Maximum allowed uncertainty */
    maxUncertainty: number;
    
    /** Required repeatability */
    repeatability: number;
    
    /** Required reproducibility */
    reproducibility: number;
  };
}

export interface CalibrationStandard {
  /** Unique identifier for the standard */
  standardId: string;
  
  /** Type of standard */
  standardType: 'reference_weight' | 'reference_dimension' | 'reference_volume' | 'reference_temperature' | 'custom';
  
  /** Nominal or expected value */
  nominalValue: number;
  
  /** Unit of measurement */
  unit: string;
  
  /** Uncertainty of the standard */
  uncertainty: number;
  
  /** Traceability information */
  traceability?: {
    certificationBody: string;
    certificateNumber: string;
    expirationDate?: Date;
    lastCalibrationDate?: Date;
  };
  
  /** Additional metadata */
  metadata?: Record<string, unknown>;
}

export interface CalibrationPoint {
  /** Point number in the calibration sequence */
  pointNumber: number;
  
  /** Reference/expected value for this point */
  referenceValue: number;
  
  /** Unit of measurement */
  unit: string;
  
  /** Individual readings taken at this point */
  readings: CalibrationReading[];
  
  /** Statistical analysis of readings */
  statistics: {
    mean: number;
    standardDeviation: number;
    variance: number;
    range: number;
    median: number;
  };
  
  /** Deviation from reference value */
  deviation: number;
  
  /** Percentage error */
  percentageError: number;
  
  /** Whether this point passes calibration criteria */
  passes: boolean;
}

export interface CalibrationReading {
  /** Reading number */
  readingNumber: number;
  
  /** Timestamp when reading was taken */
  timestamp: Date;
  
  /** Measured value */
  measuredValue: number;
  
  /** Environmental conditions during reading */
  environmentalConditions: {
    temperature?: number;
    humidity?: number;
    pressure?: number;
  };
  
  /** Quality indicators for this reading */
  quality: {
    stability: 'stable' | 'drift' | 'unstable';
    signalToNoise?: number;
    confidence: number;
  };
}

export interface CalibrationCurve {
  /** Type of curve fitting used */
  curveType: 'linear' | 'polynomial' | 'exponential' | 'logarithmic' | 'custom';
  
  /** Curve coefficients */
  coefficients: number[];
  
  /** Correlation coefficient (R²) */
  correlationCoefficient: number;
  
  /** Standard error of estimate */
  standardError: number;
  
  /** Curve equation as string */
  equation: string;
  
  /** Valid range for the calibration */
  validRange: {
    min: number;
    max: number;
    unit: string;
  };
}

export interface CalibrationCertificate {
  /** Certificate number */
  certificateNumber: string;
  
  /** Calibration date */
  calibrationDate: Date;
  
  /** Next calibration due date */
  nextCalibrationDate?: Date;
  
  /** Calibration technician */
  technician: {
    id: string;
    name: string;
    certification?: string;
  };
  
  /** Calibration results summary */
  resultsSummary: {
    overallResult: 'pass' | 'pass_with_adjustments' | 'fail';
    accuracy: number;
    uncertainty: number;
    calibrationCurve: CalibrationCurve;
  };
  
  /** Environmental conditions during calibration */
  environmentalConditions: {
    temperature: number;
    humidity: number;
    pressure?: number;
  };
  
  /** Standards used */
  standardsUsed: string[];
  
  /** Adjustments made during calibration */
  adjustments?: Array<{
    parameter: string;
    originalValue: number;
    adjustedValue: number;
    reason: string;
  }>;
}

export interface CalibrationData {
  /** All calibration points measured */
  calibrationPoints: CalibrationPoint[];
  
  /** Generated calibration curve */
  calibrationCurve?: CalibrationCurve;
  
  /** Pre-calibration readings for comparison */
  preCalibrationReadings?: CalibrationReading[];
  
  /** Post-calibration verification readings */
  verificationReadings?: CalibrationReading[];
  
  /** Drift analysis results */
  driftAnalysis?: {
    driftRate: number;
    driftDirection: 'positive' | 'negative' | 'random';
    timeConstant?: number;
  };
  
  /** Calibration certificate if generated */
  certificate?: CalibrationCertificate;
  
  /** Quality assessment of calibration */
  qualityAssessment: {
    overallQuality: 'excellent' | 'good' | 'acceptable' | 'poor';
    accuracyGrade: 'A' | 'B' | 'C' | 'D';
    repeatabilityScore: number;
    linearityScore: number;
  };
}

export interface CalibrationActivity extends BaseActivity {
  metadata: BaseActivity['metadata'] & {
    type: ActivityType.CALIBRATION;
  };
  
  /** Calibration-specific configuration */
  configuration: CalibrationConfiguration;
  
  /** Calibration data and results */
  calibrationData: CalibrationData;
}

export interface CalibrationBuilder {
  /** Set the calibration type */
  setCalibrationType(type: CalibrationType): CalibrationBuilder;
  
  /** Set target device information */
  setTargetDevice(deviceId: string, deviceType: string, model?: string): CalibrationBuilder;
  
  /** Add reference standard */
  addReferenceStandard(standard: CalibrationStandard): CalibrationBuilder;
  
  /** Set calibration procedure parameters */
  setProcedure(
    points: number,
    readingsPerPoint: number,
    tolerances: CalibrationConfiguration['procedure']['toleranceLevels']
  ): CalibrationBuilder;
  
  /** Set environmental conditions */
  setEnvironmentalConditions(conditions: CalibrationConfiguration['environmentalConditions']): CalibrationBuilder;
  
  /** Set quality requirements */
  setQualityRequirements(requirements: CalibrationConfiguration['qualityRequirements']): CalibrationBuilder;
  
  /** Enable auto drift correction */
  enableAutoDriftCorrection(enable: boolean): CalibrationBuilder;
  
  /** Set calibration interval */
  setCalibrationInterval(intervalDays: number): CalibrationBuilder;
  
  /** Build the calibration activity */
  build(): CalibrationActivity;
}

/** Calibration status indicators */
export enum CalibrationStatus {
  NOT_CALIBRATED = 'NOT_CALIBRATED',
  IN_CALIBRATION = 'IN_CALIBRATION',
  CALIBRATED = 'CALIBRATED',
  OUT_OF_TOLERANCE = 'OUT_OF_TOLERANCE',
  EXPIRED = 'EXPIRED',
  FAILED = 'FAILED',
}

/** Calibration adjustment types */
export enum CalibrationAdjustmentType {
  ZERO_ADJUSTMENT = 'ZERO_ADJUSTMENT',
  SPAN_ADJUSTMENT = 'SPAN_ADJUSTMENT',
  LINEARITY_ADJUSTMENT = 'LINEARITY_ADJUSTMENT',
  OFFSET_CORRECTION = 'OFFSET_CORRECTION',
  GAIN_CORRECTION = 'GAIN_CORRECTION',
  TEMPERATURE_COMPENSATION = 'TEMPERATURE_COMPENSATION',
}

export interface CalibrationHistory {
  /** Historical calibration records */
  calibrationRecords: Array<{
    calibrationDate: Date;
    result: 'pass' | 'pass_with_adjustments' | 'fail';
    accuracy: number;
    uncertainty: number;
    technician: string;
    notes?: string;
  }>;
  
  /** Trend analysis */
  trendAnalysis?: {
    accuracyTrend: 'improving' | 'stable' | 'degrading';
    driftTrend: 'increasing' | 'stable' | 'decreasing';
    recommendedInterval: number;
  };
}

export interface CalibrationAlert {
  /** Alert type */
  type: 'due' | 'overdue' | 'out_of_tolerance' | 'drift_detected';
  
  /** Alert severity */
  severity: 'info' | 'warning' | 'critical';
  
  /** Alert message */
  message: string;
  
  /** Recommended action */
  recommendedAction: string;
  
  /** Alert timestamp */
  timestamp: Date;
}