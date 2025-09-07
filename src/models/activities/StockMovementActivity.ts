/**
 * StockMovementActivity model for inventory tracking and stock management operations
 */

import { BaseActivity } from './BaseActivity';
import { ActivityType, StockMovementType } from './ActivityTypes';

export interface StockMovementConfiguration {
  /** Type of stock movement */
  movementType: StockMovementType;
  
  /** Items being moved */
  items: StockMovementItem[];
  
  /** Index signature for additional properties */
  [key: string]: unknown;
  
  /** Source location information */
  sourceLocation?: {
    locationId: string;
    locationType: string;
    locationName?: string;
    coordinates?: {
      latitude: number;
      longitude: number;
    };
  };
  
  /** Destination location information */
  destinationLocation?: {
    locationId: string;
    locationType: string;
    locationName?: string;
    coordinates?: {
      latitude: number;
      longitude: number;
    };
  };
  
  /** Movement authorization */
  authorization: {
    /** Whether authorization is required */
    required: boolean;
    
    /** Authorization level needed */
    authorizationLevel?: 'basic' | 'supervisor' | 'manager' | 'admin';
    
    /** Authorized user ID */
    authorizedBy?: string;
    
    /** Authorization timestamp */
    authorizedAt?: Date;
    
    /** Authorization reason/note */
    authorizationNote?: string;
  };
  
  /** Movement validation settings */
  validation: {
    /** Validate item existence */
    validateItemExistence: boolean;
    
    /** Validate available quantity */
    validateQuantity: boolean;
    
    /** Validate location capacity */
    validateLocationCapacity: boolean;
    
    /** Validate business rules */
    validateBusinessRules: boolean;
    
    /** Required documentation */
    requiredDocuments?: string[];
  };
  
  /** Integration settings */
  integration?: {
    /** Update external systems */
    updateExternalSystems: boolean;
    
    /** Target ERP system */
    erpSystem?: string;
    
    /** Target WMS system */
    wmsSystem?: string;
    
    /** Real-time sync */
    realTimeSync: boolean;
  };
}

export interface StockMovementItem {
  /** Item identifier */
  itemId: string;
  
  /** Item description */
  itemDescription?: string;
  
  /** SKU or part number */
  sku?: string;
  
  /** Batch/lot number */
  batchNumber?: string;
  
  /** Serial numbers if applicable */
  serialNumbers?: string[];
  
  /** Quantity being moved */
  quantity: number;
  
  /** Unit of measurement */
  unit: string;
  
  /** Item condition */
  condition: 'new' | 'used' | 'refurbished' | 'damaged' | 'expired';
  
  /** Quality grade */
  qualityGrade?: 'A' | 'B' | 'C' | 'D';
  
  /** Expiration date if applicable */
  expirationDate?: Date;
  
  /** Item value/cost */
  value?: {
    unitCost: number;
    totalCost: number;
    currency: string;
  };
  
  /** Storage requirements */
  storageRequirements?: {
    temperatureRange?: {
      min: number;
      max: number;
      unit: 'celsius' | 'fahrenheit';
    };
    humidityRange?: {
      min: number;
      max: number;
    };
    specialHandling?: string[];
  };
  
  /** Packaging information */
  packaging?: {
    packageType: string;
    packagesCount: number;
    packageWeight?: number;
    packageDimensions?: {
      length: number;
      width: number;
      height: number;
      unit: string;
    };
  };
}

export interface StockMovementExecution {
  /** Execution steps performed */
  executionSteps: StockMovementStep[];
  
  /** Items actually moved */
  actualItemsMoved: Array<StockMovementItem & {
    /** Actual quantity moved (may differ from planned) */
    actualQuantity: number;
    
    /** Any discrepancies found */
    discrepancies?: Array<{
      type: 'quantity' | 'condition' | 'identification' | 'location';
      expected: unknown;
      actual: unknown;
      reason?: string;
    }>;
  }>;
  
  /** Movement verification */
  verification?: {
    /** Whether movement was verified */
    verified: boolean;
    
    /** Verification method */
    verificationMethod: 'visual' | 'scan' | 'count' | 'weigh' | 'measure';
    
    /** Verified by user */
    verifiedBy: string;
    
    /** Verification timestamp */
    verifiedAt: Date;
    
    /** Verification notes */
    notes?: string;
  };
  
  /** Transportation details */
  transportation?: {
    /** Transportation method */
    method: 'manual' | 'forklift' | 'conveyor' | 'truck' | 'other';
    
    /** Vehicle/equipment ID */
    vehicleId?: string;
    
    /** Driver/operator ID */
    operatorId?: string;
    
    /** Route taken */
    route?: string;
    
    /** Transportation start time */
    startTime?: Date;
    
    /** Transportation end time */
    endTime?: Date;
    
    /** Transportation conditions */
    conditions?: {
      temperature?: number;
      humidity?: number;
      handling?: string[];
    };
  };
}

export interface StockMovementStep {
  /** Step number in sequence */
  stepNumber: number;
  
  /** Step description */
  description: string;
  
  /** Step type */
  stepType: 'preparation' | 'picking' | 'packing' | 'loading' | 'transport' | 'unloading' | 'receiving' | 'putaway' | 'verification';
  
  /** Step status */
  status: 'pending' | 'in_progress' | 'completed' | 'failed' | 'skipped';
  
  /** Step start time */
  startTime?: Date;
  
  /** Step end time */
  endTime?: Date;
  
  /** Duration in milliseconds */
  duration?: number;
  
  /** User performing the step */
  performedBy?: string;
  
  /** Location where step was performed */
  location?: string;
  
  /** Items involved in this step */
  itemsInvolved?: string[];
  
  /** Step-specific data */
  stepData?: Record<string, unknown>;
  
  /** Issues encountered */
  issues?: Array<{
    type: string;
    description: string;
    severity: 'low' | 'medium' | 'high' | 'critical';
    resolved: boolean;
    resolution?: string;
  }>;
}

export interface StockMovementValidationResult {
  /** Overall validation result */
  isValid: boolean;
  
  /** Validation errors */
  validationErrors: Array<{
    itemId?: string;
    errorType: 'item_not_found' | 'insufficient_quantity' | 'location_full' | 'unauthorized' | 'business_rule' | 'system_error';
    errorMessage: string;
    errorDetails?: Record<string, unknown>;
    canProceed: boolean;
    suggestedAction?: string;
  }>;
  
  /** Validation warnings */
  validationWarnings: Array<{
    itemId?: string;
    warningType: string;
    warningMessage: string;
    impact: 'low' | 'medium' | 'high';
  }>;
  
  /** Business rule validation */
  businessRuleValidation?: {
    rulesChecked: string[];
    rulesPassed: string[];
    rulesFailed: Array<{
      ruleId: string;
      ruleName: string;
      failureReason: string;
    }>;
  };
  
  /** System integration validation */
  systemValidation?: {
    systemsChecked: string[];
    systemsReachable: string[];
    systemsUnreachable: string[];
    syncStatus: 'synchronized' | 'pending' | 'failed';
  };
}

export interface StockMovementImpact {
  /** Stock level changes */
  stockLevelChanges: Array<{
    itemId: string;
    locationId: string;
    previousLevel: number;
    newLevel: number;
    changeAmount: number;
  }>;
  
  /** Financial impact */
  financialImpact?: {
    /** Total value moved */
    totalValueMoved: number;
    
    /** Cost impact */
    costImpact: number;
    
    /** Currency */
    currency: string;
    
    /** Inventory valuation change */
    inventoryValuationChange?: number;
  };
  
  /** Capacity impact */
  capacityImpact?: Array<{
    locationId: string;
    previousCapacityUsed: number;
    newCapacityUsed: number;
    capacityChangePercentage: number;
  }>;
  
  /** Downstream effects */
  downstreamEffects?: Array<{
    affectedSystem: string;
    effectType: 'stock_update' | 'reorder_trigger' | 'alert_generation' | 'workflow_trigger';
    effectDescription: string;
    processed: boolean;
  }>;
}

export interface StockMovementData {
  /** Movement execution details */
  execution: StockMovementExecution;
  
  /** Validation results */
  validation: StockMovementValidationResult;
  
  /** Impact assessment */
  impact: StockMovementImpact;
  
  /** Generated documentation */
  documentation?: Array<{
    documentType: 'picking_list' | 'packing_slip' | 'delivery_note' | 'receipt' | 'transfer_order';
    documentId: string;
    generatedAt: Date;
    documentUrl?: string;
    printed: boolean;
  }>;
  
  /** Compliance tracking */
  compliance?: {
    /** Regulatory compliance */
    regulatoryCompliance: Array<{
      regulation: string;
      compliant: boolean;
      notes?: string;
    }>;
    
    /** Audit trail requirements */
    auditRequirements: boolean;
    
    /** Retention period */
    retentionPeriod?: number;
  };
}

export interface StockMovementActivity extends BaseActivity {
  metadata: BaseActivity['metadata'] & {
    type: ActivityType.STOCK_MOVEMENT;
  };
  
  /** Stock movement configuration */
  configuration: StockMovementConfiguration;
  
  /** Stock movement execution data */
  movementData: StockMovementData;
}

export interface StockMovementBuilder {
  /** Set movement type */
  setMovementType(type: StockMovementType): StockMovementBuilder;
  
  /** Add item to movement */
  addItem(item: StockMovementItem): StockMovementBuilder;
  
  /** Set source location */
  setSourceLocation(locationId: string, locationType: string, locationName?: string): StockMovementBuilder;
  
  /** Set destination location */
  setDestinationLocation(locationId: string, locationType: string, locationName?: string): StockMovementBuilder;
  
  /** Set authorization requirements */
  setAuthorizationRequirements(required: boolean, level?: StockMovementConfiguration['authorization']['authorizationLevel']): StockMovementBuilder;
  
  /** Enable validation checks */
  enableValidation(
    validateItems: boolean,
    validateQuantity: boolean,
    validateCapacity: boolean,
    validateBusinessRules: boolean
  ): StockMovementBuilder;
  
  /** Configure system integration */
  configureIntegration(updateExternal: boolean, erpSystem?: string, wmsSystem?: string): StockMovementBuilder;
  
  /** Build the stock movement activity */
  build(): StockMovementActivity;
}

/** Movement priority levels */
export enum MovementPriority {
  ROUTINE = 'ROUTINE',
  STANDARD = 'STANDARD',
  URGENT = 'URGENT',
  EMERGENCY = 'EMERGENCY',
}

/** Movement approval status */
export enum MovementApprovalStatus {
  NOT_REQUIRED = 'NOT_REQUIRED',
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  EXPIRED = 'EXPIRED',
}

export interface StockMovementSummary {
  /** Summary statistics */
  summary: {
    totalItems: number;
    totalQuantity: number;
    totalValue?: number;
    averageExecutionTime?: number;
    successRate: number;
  };
  
  /** Movement breakdown by type */
  movementBreakdown: Record<StockMovementType, number>;
  
  /** Top locations */
  topLocations: {
    source: Array<{ locationId: string; count: number }>;
    destination: Array<{ locationId: string; count: number }>;
  };
  
  /** Performance metrics */
  performance: {
    averagePickTime?: number;
    averageTransportTime?: number;
    averagePutawayTime?: number;
    errorRate: number;
    accuracyRate: number;
  };
}