/**
 * Activity types and constants for B-Scan activity tracking system
 * Matches the sophisticated Kotlin activity system implementation
 */

export enum ActivityType {
  SCAN_OCCURRENCE = 'SCAN_OCCURRENCE',
  CALIBRATION = 'CALIBRATION',
  MEASUREMENT = 'MEASUREMENT',
  STOCK_MOVEMENT = 'STOCK_MOVEMENT',
  CONSUMPTION_DISTRIBUTION = 'CONSUMPTION_DISTRIBUTION',
}

export enum ActivityStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
  PAUSED = 'PAUSED',
}

export enum ActivityResultType {
  SUCCESS = 'SUCCESS',
  PARTIAL_SUCCESS = 'PARTIAL_SUCCESS',
  FAILURE = 'FAILURE',
  TIMEOUT = 'TIMEOUT',
  USER_CANCELLED = 'USER_CANCELLED',
  SYSTEM_ERROR = 'SYSTEM_ERROR',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
}

export enum ScanType {
  BARCODE = 'BARCODE',
  QR_CODE = 'QR_CODE',
  NFC = 'NFC',
  RFID = 'RFID',
  MANUAL_ENTRY = 'MANUAL_ENTRY',
  CAMERA_OCR = 'CAMERA_OCR',
}

export enum CalibrationType {
  WEIGHT_SCALE = 'WEIGHT_SCALE',
  DIMENSION_MEASUREMENT = 'DIMENSION_MEASUREMENT',
  VOLUME_CALCULATION = 'VOLUME_CALCULATION',
  DENSITY_CALIBRATION = 'DENSITY_CALIBRATION',
  SENSOR_ALIGNMENT = 'SENSOR_ALIGNMENT',
}

export enum MeasurementType {
  WEIGHT = 'WEIGHT',
  DIMENSIONS = 'DIMENSIONS',
  VOLUME = 'VOLUME',
  DENSITY = 'DENSITY',
  TEMPERATURE = 'TEMPERATURE',
  HUMIDITY = 'HUMIDITY',
}

export enum StockMovementType {
  INBOUND = 'INBOUND',
  OUTBOUND = 'OUTBOUND',
  TRANSFER = 'TRANSFER',
  ADJUSTMENT = 'ADJUSTMENT',
  CONSUMPTION = 'CONSUMPTION',
  PRODUCTION = 'PRODUCTION',
  RETURN = 'RETURN',
  DAMAGED = 'DAMAGED',
  EXPIRED = 'EXPIRED',
}

export enum DistributionMethod {
  PROPORTIONAL = 'PROPORTIONAL',
  USER_SPECIFIED = 'USER_SPECIFIED',
  EQUAL_SPLIT = 'EQUAL_SPLIT',
  WEIGHTED = 'WEIGHTED',
  INFERRED = 'INFERRED',
}

export enum Priority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export enum ValidationState {
  VALID = 'VALID',
  INVALID = 'INVALID',
  PENDING_VALIDATION = 'PENDING_VALIDATION',
  VALIDATION_FAILED = 'VALIDATION_FAILED',
}

export const ActivityTypeLabels: Record<ActivityType, string> = {
  [ActivityType.SCAN_OCCURRENCE]: 'Scan Occurrence',
  [ActivityType.CALIBRATION]: 'Calibration',
  [ActivityType.MEASUREMENT]: 'Measurement',
  [ActivityType.STOCK_MOVEMENT]: 'Stock Movement',
  [ActivityType.CONSUMPTION_DISTRIBUTION]: 'Consumption Distribution',
};

export const ActivityStatusLabels: Record<ActivityStatus, string> = {
  [ActivityStatus.PENDING]: 'Pending',
  [ActivityStatus.IN_PROGRESS]: 'In Progress',
  [ActivityStatus.COMPLETED]: 'Completed',
  [ActivityStatus.FAILED]: 'Failed',
  [ActivityStatus.CANCELLED]: 'Cancelled',
  [ActivityStatus.PAUSED]: 'Paused',
};

export const ActivityResultTypeLabels: Record<ActivityResultType, string> = {
  [ActivityResultType.SUCCESS]: 'Success',
  [ActivityResultType.PARTIAL_SUCCESS]: 'Partial Success',
  [ActivityResultType.FAILURE]: 'Failure',
  [ActivityResultType.TIMEOUT]: 'Timeout',
  [ActivityResultType.USER_CANCELLED]: 'User Cancelled',
  [ActivityResultType.SYSTEM_ERROR]: 'System Error',
  [ActivityResultType.VALIDATION_ERROR]: 'Validation Error',
};

export const DistributionMethodLabels: Record<DistributionMethod, string> = {
  [DistributionMethod.PROPORTIONAL]: 'Proportional Distribution',
  [DistributionMethod.USER_SPECIFIED]: 'User Specified',
  [DistributionMethod.EQUAL_SPLIT]: 'Equal Split',
  [DistributionMethod.WEIGHTED]: 'Weighted Distribution',
  [DistributionMethod.INFERRED]: 'Inferred Distribution',
};

export const ACTIVITY_TIMEOUT_MS = {
  [ActivityType.SCAN_OCCURRENCE]: 30000, // 30 seconds
  [ActivityType.CALIBRATION]: 300000, // 5 minutes
  [ActivityType.MEASUREMENT]: 60000, // 1 minute
  [ActivityType.STOCK_MOVEMENT]: 120000, // 2 minutes
  [ActivityType.CONSUMPTION_DISTRIBUTION]: 180000, // 3 minutes
} as const;

export const ACTIVITY_RETRY_LIMITS = {
  [ActivityType.SCAN_OCCURRENCE]: 3,
  [ActivityType.CALIBRATION]: 2,
  [ActivityType.MEASUREMENT]: 3,
  [ActivityType.STOCK_MOVEMENT]: 2,
  [ActivityType.CONSUMPTION_DISTRIBUTION]: 1,
} as const;