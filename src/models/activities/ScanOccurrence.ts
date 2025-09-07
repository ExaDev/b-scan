/**
 * ScanOccurrence activity model for barcode, QR, NFC, and RFID scanning activities
 */

import { BaseActivity } from './BaseActivity';
import { ActivityType, ScanType } from './ActivityTypes';

export interface ScanOccurrenceConfiguration {
  /** Type of scan being performed */
  scanType: ScanType;
  
  /** Expected scan data format/pattern */
  expectedFormat?: string;
  
  /** Whether to validate scanned data against expected format */
  validateFormat: boolean;
  
  /** Whether to allow multiple scan attempts */
  allowRetry: boolean;
  
  /** Maximum number of scan attempts */
  maxRetryAttempts: number;
  
  /** Timeout for scan operation in milliseconds */
  scanTimeout: number;
  
  /** Whether to capture scan location */
  captureLocation: boolean;
  
  /** Whether to capture scan timestamp with high precision */
  highPrecisionTimestamp: boolean;
  
  /** Additional scan settings */
  scanSettings?: {
    /** Camera settings for barcode/QR scanning */
    camera?: {
      resolution?: 'low' | 'medium' | 'high';
      autofocus?: boolean;
      flashEnabled?: boolean;
    };
    
    /** NFC/RFID settings */
    rfid?: {
      frequency?: number;
      powerLevel?: number;
      readDistance?: number;
    };
    
    /** OCR settings for camera text recognition */
    ocr?: {
      language?: string;
      confidence?: number;
      preprocessing?: boolean;
    };
  };
}

export interface ScanData {
  /** Raw scanned data */
  rawData: string;
  
  /** Parsed/decoded data if applicable */
  decodedData?: Record<string, any>;
  
  /** Data format detected */
  detectedFormat?: string;
  
  /** Confidence score of the scan (0-1) */
  confidenceScore?: number;
  
  /** Quality metrics of the scan */
  qualityMetrics?: {
    /** Signal strength for RFID/NFC */
    signalStrength?: number;
    
    /** Image quality for barcode/QR */
    imageQuality?: number;
    
    /** Recognition accuracy for OCR */
    recognitionAccuracy?: number;
  };
}

export interface ScanAttempt {
  /** Attempt number (1-based) */
  attemptNumber: number;
  
  /** Timestamp of the scan attempt */
  timestamp: Date;
  
  /** Duration of the scan attempt in milliseconds */
  duration: number;
  
  /** Whether the attempt was successful */
  successful: boolean;
  
  /** Scan data if successful */
  scanData?: ScanData;
  
  /** Error information if failed */
  error?: {
    code: string;
    message: string;
    details?: Record<string, any>;
  };
  
  /** Location where scan was attempted */
  location?: {
    latitude: number;
    longitude: number;
    accuracy: number;
    altitude?: number;
  };
}

export interface ScanValidationResult {
  /** Whether the scan passed validation */
  isValid: boolean;
  
  /** Validation errors */
  validationErrors: Array<{
    field: string;
    message: string;
    code: string;
  }>;
  
  /** Data integrity check results */
  integrityCheck?: {
    checksumValid?: boolean;
    lengthValid?: boolean;
    formatValid?: boolean;
    rangeValid?: boolean;
  };
  
  /** Business rule validation results */
  businessRules?: {
    ruleId: string;
    passed: boolean;
    message?: string;
  }[];
}

export interface ScanOccurrenceData {
  /** All scan attempts made */
  scanAttempts: ScanAttempt[];
  
  /** Final successful scan data */
  finalScanData?: ScanData;
  
  /** Validation result of the final scan */
  validationResult?: ScanValidationResult;
  
  /** Environmental conditions during scanning */
  environmentalConditions?: {
    /** Ambient light level */
    lightLevel?: number;
    
    /** Device orientation */
    orientation?: 'portrait' | 'landscape' | 'flat';
    
    /** Movement/shake level during scan */
    stabilityLevel?: number;
    
    /** Temperature if available */
    temperature?: number;
  };
  
  /** Post-scan actions performed */
  postScanActions?: Array<{
    action: string;
    timestamp: Date;
    successful: boolean;
    details?: Record<string, any>;
  }>;
}

export interface ScanOccurrence extends BaseActivity {
  metadata: BaseActivity['metadata'] & {
    type: ActivityType.SCAN_OCCURRENCE;
  };
  
  /** Scan-specific configuration */
  configuration: ScanOccurrenceConfiguration;
  
  /** Scan attempt data and results */
  scanData: ScanOccurrenceData;
}

export interface ScanOccurrenceBuilder {
  /** Set the scan type */
  setScanType(scanType: ScanType): ScanOccurrenceBuilder;
  
  /** Set expected data format */
  setExpectedFormat(format: string): ScanOccurrenceBuilder;
  
  /** Enable/disable format validation */
  setValidateFormat(validate: boolean): ScanOccurrenceBuilder;
  
  /** Set retry configuration */
  setRetryConfig(allowRetry: boolean, maxAttempts?: number): ScanOccurrenceBuilder;
  
  /** Set scan timeout */
  setTimeout(timeoutMs: number): ScanOccurrenceBuilder;
  
  /** Enable location capture */
  enableLocationCapture(enable: boolean): ScanOccurrenceBuilder;
  
  /** Set camera settings for visual scanning */
  setCameraSettings(settings: ScanOccurrenceConfiguration['scanSettings']['camera']): ScanOccurrenceBuilder;
  
  /** Set RFID/NFC settings */
  setRfidSettings(settings: ScanOccurrenceConfiguration['scanSettings']['rfid']): ScanOccurrenceBuilder;
  
  /** Set OCR settings */
  setOcrSettings(settings: ScanOccurrenceConfiguration['scanSettings']['ocr']): ScanOccurrenceBuilder;
  
  /** Add custom configuration */
  addCustomConfig(key: string, value: any): ScanOccurrenceBuilder;
  
  /** Build the scan occurrence activity */
  build(): ScanOccurrence;
}

/** Scan result categories for classification */
export enum ScanResultCategory {
  PRODUCT_IDENTIFIER = 'PRODUCT_IDENTIFIER',
  BATCH_NUMBER = 'BATCH_NUMBER',
  SERIAL_NUMBER = 'SERIAL_NUMBER',
  LOCATION_CODE = 'LOCATION_CODE',
  USER_IDENTIFIER = 'USER_IDENTIFIER',
  TRANSACTION_ID = 'TRANSACTION_ID',
  CUSTOM_DATA = 'CUSTOM_DATA',
  UNKNOWN = 'UNKNOWN',
}

/** Scan quality indicators */
export enum ScanQuality {
  EXCELLENT = 'EXCELLENT',
  GOOD = 'GOOD',
  FAIR = 'FAIR',
  POOR = 'POOR',
  FAILED = 'FAILED',
}

export interface ScanResultClassification {
  /** Detected category of scanned data */
  category: ScanResultCategory;
  
  /** Confidence in the classification (0-1) */
  confidence: number;
  
  /** Additional classification metadata */
  metadata?: Record<string, any>;
}

export interface ScanQualityAssessment {
  /** Overall scan quality rating */
  quality: ScanQuality;
  
  /** Quality score (0-100) */
  qualityScore: number;
  
  /** Factors affecting quality */
  qualityFactors: {
    lighting?: 'poor' | 'adequate' | 'good' | 'excellent';
    focus?: 'poor' | 'adequate' | 'good' | 'excellent';
    angle?: 'poor' | 'adequate' | 'good' | 'excellent';
    distance?: 'poor' | 'adequate' | 'good' | 'excellent';
    stability?: 'poor' | 'adequate' | 'good' | 'excellent';
  };
  
  /** Recommendations for improvement */
  recommendations?: string[];
}