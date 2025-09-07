/**
 * Base Activity interface and metadata types for B-Scan activity tracking system
 */

import {
  ActivityType,
  ActivityStatus,
  ActivityResultType,
  Priority,
  ValidationState,
} from './ActivityTypes';

export interface ActivityMetadata {
  /** Unique identifier for the activity */
  id: string;
  
  /** Type of activity */
  type: ActivityType;
  
  /** Current status of the activity */
  status: ActivityStatus;
  
  /** Activity priority level */
  priority: Priority;
  
  /** Human-readable title/name for the activity */
  title: string;
  
  /** Optional description providing more context */
  description?: string;
  
  /** Timestamp when the activity was created */
  createdAt: Date;
  
  /** Timestamp when the activity was started */
  startedAt?: Date;
  
  /** Timestamp when the activity was completed or failed */
  completedAt?: Date;
  
  /** Timestamp of the last update to this activity */
  updatedAt: Date;
  
  /** Duration of the activity in milliseconds (calculated or estimated) */
  duration?: number;
  
  /** Expected/estimated duration in milliseconds */
  estimatedDuration?: number;
  
  /** Progress percentage (0-100) */
  progressPercentage: number;
  
  /** Current validation state */
  validationState: ValidationState;
  
  /** Tags for categorization and filtering */
  tags: string[];
  
  /** Custom metadata as key-value pairs */
  metadata: Record<string, unknown>;
  
  /** Index signature for additional properties */
  [key: string]: unknown;
}

export interface ActivityResult {
  /** Type of result */
  type: ActivityResultType;
  
  /** Human-readable message describing the result */
  message?: string;
  
  /** Structured data representing the result */
  data?: Record<string, unknown>;
  
  /** Any errors that occurred during the activity */
  errors?: ActivityError[];
  
  /** Warnings generated during the activity */
  warnings?: ActivityWarning[];
  
  /** Metrics collected during the activity execution */
  metrics?: ActivityMetrics;
}

export interface ActivityError {
  /** Error code for programmatic handling */
  code: string;
  
  /** Human-readable error message */
  message: string;
  
  /** Optional error details */
  details?: Record<string, unknown>;
  
  /** Timestamp when the error occurred */
  timestamp: Date;
  
  /** Severity level of the error */
  severity: 'low' | 'medium' | 'high' | 'critical';
}

export interface ActivityWarning {
  /** Warning code for programmatic handling */
  code: string;
  
  /** Human-readable warning message */
  message: string;
  
  /** Optional warning details */
  details?: Record<string, unknown>;
  
  /** Timestamp when the warning was generated */
  timestamp: Date;
}

export interface ActivityMetrics {
  /** Execution time in milliseconds */
  executionTime?: number;
  
  /** Memory usage in bytes */
  memoryUsage?: number;
  
  /** CPU usage percentage */
  cpuUsage?: number;
  
  /** Number of operations performed */
  operationCount?: number;
  
  /** Success rate (0-1) */
  successRate?: number;
  
  /** Custom metrics */
  customMetrics?: Record<string, number>;
}

export interface ActivityProgress {
  /** Current step being executed */
  currentStep: number;
  
  /** Total number of steps */
  totalSteps: number;
  
  /** Description of current step */
  currentStepDescription?: string;
  
  /** Progress percentage for current step (0-100) */
  stepProgressPercentage: number;
  
  /** Overall progress percentage (0-100) */
  overallProgressPercentage: number;
  
  /** Estimated time remaining in milliseconds */
  estimatedTimeRemaining?: number;
}

export interface ActivityDependency {
  /** ID of the dependent activity */
  activityId: string;
  
  /** Type of dependency */
  type: 'prerequisite' | 'concurrent' | 'successor';
  
  /** Whether this dependency is required for execution */
  required: boolean;
}

export interface ActivityRelationship {
  /** ID of the related entity */
  entityId: string;
  
  /** Type of entity (item, location, user, etc.) */
  entityType: string;
  
  /** Type of relationship */
  relationshipType: 'primary' | 'secondary' | 'reference' | 'target';
  
  /** Additional relationship metadata */
  metadata?: Record<string, unknown>;
}

export interface BaseActivity {
  /** Core activity metadata */
  metadata: ActivityMetadata;
  
  /** Current result of the activity */
  result?: ActivityResult;
  
  /** Progress tracking information */
  progress?: ActivityProgress;
  
  /** Dependencies on other activities */
  dependencies: ActivityDependency[];
  
  /** Relationships to other entities */
  relationships: ActivityRelationship[];
  
  /** Configuration specific to the activity type */
  configuration: Record<string, unknown>;
  
  /** Audit trail of state changes */
  auditTrail: ActivityAuditEntry[];
  
  /** Index signature for additional properties */
  [key: string]: unknown;
}

export interface ActivityAuditEntry {
  /** Timestamp of the state change */
  timestamp: Date;
  
  /** Previous status */
  previousStatus?: ActivityStatus;
  
  /** New status */
  newStatus: ActivityStatus;
  
  /** User or system that made the change */
  changedBy: string;
  
  /** Type of change */
  changeType: 'status' | 'progress' | 'configuration' | 'metadata';
  
  /** Description of the change */
  description?: string;
  
  /** Additional change details */
  details?: Record<string, unknown>;
}

export interface ActivityValidationResult {
  /** Whether the activity passed validation */
  isValid: boolean;
  
  /** Validation errors */
  errors: ActivityError[];
  
  /** Validation warnings */
  warnings: ActivityWarning[];
  
  /** Validation score (0-100) */
  score?: number;
  
  /** Detailed validation results by field/section */
  fieldResults?: Record<string, boolean>;
}

export interface ActivityExecutionContext {
  /** User ID executing the activity */
  userId?: string;
  
  /** Session ID */
  sessionId?: string;
  
  /** Device information */
  deviceInfo?: {
    platform: string;
    version: string;
    model?: string;
  };
  
  /** Location context */
  locationContext?: {
    latitude?: number;
    longitude?: number;
    accuracy?: number;
    locationName?: string;
  };
  
  /** Network context */
  networkContext?: {
    type: 'wifi' | 'cellular' | 'offline';
    strength?: number;
  };
  
  /** Additional execution context */
  customContext?: Record<string, unknown>;
}