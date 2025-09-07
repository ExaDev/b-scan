/**
 * ConsumptionDistributionActivity model for sophisticated consumption tracking and distribution
 * across multiple items, locations, or cost centers
 */

import { BaseActivity } from './BaseActivity';
import { ActivityType, DistributionMethod } from './ActivityTypes';

export interface ConsumptionDistributionConfiguration {
  /** Distribution method to use */
  distributionMethod: DistributionMethod;
  
  /** Source items being consumed */
  sourceItems: SourceConsumptionItem[];
  
  /** Target destinations for distribution */
  distributionTargets: DistributionTarget[];
  
  /** Total consumption amount to distribute */
  totalConsumption: {
    quantity: number;
    unit: string;
    value?: number;
    currency?: string;
  };
  
  /** Distribution calculation settings */
  distributionSettings: {
    /** Rounding precision for distributed amounts */
    roundingPrecision: number;
    
    /** How to handle rounding remainders */
    remainderHandling: 'ignore' | 'allocate_to_largest' | 'allocate_to_first' | 'create_separate_entry';
    
    /** Minimum allocation threshold */
    minimumAllocation?: number;
    
    /** Whether to validate total equals source */
    validateTotalMatch: boolean;
    
    /** Allow partial distributions */
    allowPartialDistribution: boolean;
  };
  
  /** Method-specific parameters */
  methodParameters?: {
    /** For PROPORTIONAL method */
    proportional?: {
      /** Basis for proportional calculation */
      proportionalBasis: 'quantity' | 'value' | 'weight' | 'volume' | 'custom';
      
      /** Custom proportional factors if basis is 'custom' */
      customFactors?: Record<string, number>;
    };
    
    /** For WEIGHTED method */
    weighted?: {
      /** Weight factors for each target */
      weightFactors: Record<string, number>;
      
      /** Whether weights should be normalized */
      normalizeWeights: boolean;
    };
    
    /** For USER_SPECIFIED method */
    userSpecified?: {
      /** Explicit allocations */
      allocations: Record<string, number>;
      
      /** Whether to validate allocations sum to total */
      validateSum: boolean;
    };
    
    /** For INFERRED method */
    inferred?: {
      /** Data sources for inference */
      inferenceDataSources: string[];
      
      /** Inference algorithm */
      inferenceAlgorithm: 'historical_average' | 'regression' | 'ml_prediction' | 'rule_based';
      
      /** Confidence threshold for inference */
      confidenceThreshold: number;
      
      /** Fallback method if inference fails */
      fallbackMethod: DistributionMethod;
    };
  };
  
  /** Approval and authorization */
  authorization?: {
    required: boolean;
    approvalLevel: 'automatic' | 'supervisor' | 'manager' | 'finance';
    approvalThreshold?: number;
    authorizedBy?: string;
    authorizedAt?: Date;
  };
  
  /** Audit and compliance */
  auditRequirements?: {
    auditTrailRequired: boolean;
    complianceStandards: string[];
    retentionPeriod: number;
    approvalDocumentation: boolean;
  };
}

export interface SourceConsumptionItem {
  /** Item identifier */
  itemId: string;
  
  /** Item description */
  description?: string;
  
  /** SKU or part number */
  sku?: string;
  
  /** Batch/lot information */
  batchInfo?: {
    batchNumber: string;
    expirationDate?: Date;
    manufactureDate?: Date;
  };
  
  /** Consumed quantity */
  consumedQuantity: number;
  
  /** Unit of measurement */
  unit: string;
  
  /** Consumption cost information */
  costInformation?: {
    unitCost: number;
    totalCost: number;
    costBasis: 'standard' | 'average' | 'fifo' | 'lifo' | 'specific';
    currency: string;
  };
  
  /** Source location */
  sourceLocation?: {
    locationId: string;
    locationName?: string;
  };
  
  /** Consumption timestamp */
  consumedAt: Date;
  
  /** Consumption reason/purpose */
  consumptionReason?: string;
  
  /** Additional metadata */
  metadata?: Record<string, any>;
}

export interface DistributionTarget {
  /** Target identifier */
  targetId: string;
  
  /** Target type */
  targetType: 'cost_center' | 'project' | 'department' | 'location' | 'product' | 'customer' | 'job_order';
  
  /** Target name/description */
  targetName?: string;
  
  /** Target capacity or limit */
  capacity?: {
    maxQuantity?: number;
    maxValue?: number;
    unit?: string;
  };
  
  /** Current allocation before this distribution */
  currentAllocation?: {
    quantity: number;
    value?: number;
    unit: string;
  };
  
  /** Priority for allocation */
  priority: number;
  
  /** Whether this target is active/available */
  isActive: boolean;
  
  /** Target-specific attributes for calculation */
  attributes?: {
    /** For proportional distribution */
    proportionalFactor?: number;
    
    /** For weighted distribution */
    weight?: number;
    
    /** Historical consumption data */
    historicalData?: Array<{
      period: string;
      quantity: number;
      value?: number;
    }>;
    
    /** Business rules */
    businessRules?: Array<{
      ruleType: string;
      ruleValue: any;
      ruleDescription: string;
    }>;
  };
  
  /** Additional target metadata */
  metadata?: Record<string, any>;
}

export interface DistributionCalculation {
  /** Calculation method used */
  method: DistributionMethod;
  
  /** Input parameters */
  inputParameters: Record<string, any>;
  
  /** Calculation steps performed */
  calculationSteps: Array<{
    stepNumber: number;
    stepDescription: string;
    inputValues: Record<string, number>;
    calculation: string;
    outputValues: Record<string, number>;
    notes?: string;
  }>;
  
  /** Raw distribution results */
  rawResults: Array<{
    targetId: string;
    allocatedQuantity: number;
    allocatedValue?: number;
    calculationBasis: string;
    calculatedPercentage: number;
  }>;
  
  /** Adjusted results after rounding and validation */
  adjustedResults: Array<{
    targetId: string;
    finalQuantity: number;
    finalValue?: number;
    adjustmentReason?: string;
    adjustmentAmount?: number;
  }>;
  
  /** Calculation accuracy metrics */
  accuracyMetrics: {
    /** Sum of distributed amounts vs. total */
    totalMatchAccuracy: number;
    
    /** Rounding error amount */
    roundingError: number;
    
    /** Maximum individual error */
    maxIndividualError: number;
    
    /** Root mean square error */
    rmsError: number;
  };
  
  /** Calculation confidence (for inferred methods) */
  confidence?: number;
  
  /** Alternative calculation results for comparison */
  alternativeResults?: Record<DistributionMethod, any>;
}

export interface DistributionAllocation {
  /** Target receiving the allocation */
  targetId: string;
  
  /** Allocated quantity */
  quantity: number;
  
  /** Allocated value */
  value?: number;
  
  /** Unit of measurement */
  unit: string;
  
  /** Currency for value */
  currency?: string;
  
  /** Percentage of total distribution */
  percentage: number;
  
  /** Allocation basis/reason */
  allocationBasis: string;
  
  /** Priority rank in allocation */
  rank: number;
  
  /** Allocation confidence (0-1) */
  confidence: number;
  
  /** Allocation timestamp */
  allocatedAt: Date;
  
  /** Any adjustments made */
  adjustments?: Array<{
    adjustmentType: 'rounding' | 'minimum_threshold' | 'capacity_limit' | 'business_rule';
    originalValue: number;
    adjustedValue: number;
    adjustmentAmount: number;
    reason: string;
  }>;
  
  /** Validation results */
  validation?: {
    withinCapacity: boolean;
    meetsMinimumThreshold: boolean;
    passesBusinessRules: boolean;
    validationErrors: string[];
    validationWarnings: string[];
  };
}

export interface DistributionValidation {
  /** Overall validation result */
  isValid: boolean;
  
  /** Total quantity validation */
  quantityValidation: {
    sourceTotal: number;
    distributedTotal: number;
    difference: number;
    withinTolerance: boolean;
    tolerance: number;
  };
  
  /** Total value validation */
  valueValidation?: {
    sourceTotal: number;
    distributedTotal: number;
    difference: number;
    withinTolerance: boolean;
    tolerance: number;
  };
  
  /** Individual allocation validations */
  allocationValidations: Array<{
    targetId: string;
    validationResult: 'valid' | 'warning' | 'error';
    issues: Array<{
      issueType: 'capacity_exceeded' | 'below_minimum' | 'business_rule_violation' | 'data_quality';
      issueDescription: string;
      severity: 'low' | 'medium' | 'high' | 'critical';
      suggestedAction?: string;
    }>;
  }>;
  
  /** Business rule validations */
  businessRuleValidations: Array<{
    ruleId: string;
    ruleName: string;
    passed: boolean;
    affectedTargets: string[];
    violationDetails?: string;
  }>;
  
  /** Data quality checks */
  dataQualityChecks: {
    completeness: number; // 0-1
    consistency: number; // 0-1
    accuracy: number; // 0-1
    issues: Array<{
      dataField: string;
      issueType: string;
      issueDescription: string;
      impact: 'low' | 'medium' | 'high';
    }>;
  };
}

export interface DistributionAnalysis {
  /** Distribution efficiency metrics */
  efficiencyMetrics: {
    /** Utilization rate of targets */
    targetUtilization: number;
    
    /** Distribution evenness (0-1, 1 = perfectly even) */
    distributionEvenness: number;
    
    /** Concentration index (higher = more concentrated) */
    concentrationIndex: number;
    
    /** Gini coefficient for inequality measurement */
    giniCoefficient?: number;
  };
  
  /** Sensitivity analysis */
  sensitivityAnalysis?: {
    /** How much results change with small input changes */
    sensitivityScore: number;
    
    /** Most sensitive parameters */
    sensitiveParameters: Array<{
      parameterName: string;
      sensitivityValue: number;
      impactDescription: string;
    }>;
    
    /** Robustness score */
    robustnessScore: number;
  };
  
  /** Comparison with alternative methods */
  methodComparison?: Array<{
    alternativeMethod: DistributionMethod;
    differencePercentage: number;
    advantagesOverCurrent: string[];
    disadvantagesVsCurrent: string[];
  }>;
  
  /** Historical comparison */
  historicalComparison?: {
    previousDistributions: Array<{
      date: Date;
      method: DistributionMethod;
      results: Record<string, number>;
    }>;
    
    trendAnalysis: {
      trend: 'increasing' | 'decreasing' | 'stable' | 'volatile';
      trendDescription: string;
      forecastReliability: number;
    };
  };
}

export interface ConsumptionDistributionData {
  /** Distribution calculation results */
  calculation: DistributionCalculation;
  
  /** Final allocations to targets */
  allocations: DistributionAllocation[];
  
  /** Validation results */
  validation: DistributionValidation;
  
  /** Analysis and insights */
  analysis: DistributionAnalysis;
  
  /** Approval workflow */
  approvalWorkflow?: {
    currentStage: string;
    approvalStages: Array<{
      stageName: string;
      status: 'pending' | 'approved' | 'rejected' | 'skipped';
      approver?: string;
      approvedAt?: Date;
      comments?: string;
    }>;
  };
  
  /** Integration with external systems */
  systemIntegration?: {
    updatedSystems: string[];
    integrationStatus: Record<string, 'success' | 'pending' | 'failed'>;
    integrationTimestamps: Record<string, Date>;
    integrationErrors?: Record<string, string>;
  };
}

export interface ConsumptionDistributionActivity extends BaseActivity {
  metadata: BaseActivity['metadata'] & {
    type: ActivityType.CONSUMPTION_DISTRIBUTION;
  };
  
  /** Consumption distribution configuration */
  configuration: ConsumptionDistributionConfiguration;
  
  /** Distribution execution data */
  distributionData: ConsumptionDistributionData;
}

export interface ConsumptionDistributionBuilder {
  /** Set distribution method */
  setDistributionMethod(method: DistributionMethod): ConsumptionDistributionBuilder;
  
  /** Add source consumption item */
  addSourceItem(item: SourceConsumptionItem): ConsumptionDistributionBuilder;
  
  /** Add distribution target */
  addDistributionTarget(target: DistributionTarget): ConsumptionDistributionBuilder;
  
  /** Set total consumption to distribute */
  setTotalConsumption(quantity: number, unit: string, value?: number, currency?: string): ConsumptionDistributionBuilder;
  
  /** Configure distribution settings */
  setDistributionSettings(
    roundingPrecision: number,
    remainderHandling: ConsumptionDistributionConfiguration['distributionSettings']['remainderHandling'],
    validateTotal: boolean
  ): ConsumptionDistributionBuilder;
  
  /** Set proportional parameters */
  setProportionalParameters(
    basis: ConsumptionDistributionConfiguration['methodParameters']['proportional']['proportionalBasis'],
    customFactors?: Record<string, number>
  ): ConsumptionDistributionBuilder;
  
  /** Set weighted parameters */
  setWeightedParameters(
    weightFactors: Record<string, number>,
    normalizeWeights: boolean
  ): ConsumptionDistributionBuilder;
  
  /** Set user-specified allocations */
  setUserSpecifiedAllocations(
    allocations: Record<string, number>,
    validateSum: boolean
  ): ConsumptionDistributionBuilder;
  
  /** Set inference parameters */
  setInferenceParameters(
    dataSources: string[],
    algorithm: ConsumptionDistributionConfiguration['methodParameters']['inferred']['inferenceAlgorithm'],
    confidenceThreshold: number,
    fallbackMethod: DistributionMethod
  ): ConsumptionDistributionBuilder;
  
  /** Set authorization requirements */
  setAuthorizationRequirements(
    required: boolean,
    approvalLevel?: ConsumptionDistributionConfiguration['authorization']['approvalLevel'],
    threshold?: number
  ): ConsumptionDistributionBuilder;
  
  /** Build the consumption distribution activity */
  build(): ConsumptionDistributionActivity;
}

/** Distribution result quality indicators */
export enum DistributionQuality {
  EXCELLENT = 'EXCELLENT',
  GOOD = 'GOOD',
  ACCEPTABLE = 'ACCEPTABLE',
  POOR = 'POOR',
  FAILED = 'FAILED',
}

/** Distribution fairness measures */
export enum FairnessMeasure {
  EQUAL = 'EQUAL',           // All targets get equal amounts
  PROPORTIONAL = 'PROPORTIONAL', // Based on some proportional factor
  NEED_BASED = 'NEED_BASED', // Based on need/capacity
  MERIT_BASED = 'MERIT_BASED', // Based on performance/merit
}

export interface DistributionOptimization {
  /** Optimization objective */
  objective: 'minimize_variance' | 'maximize_utilization' | 'minimize_cost' | 'maximize_fairness' | 'custom';
  
  /** Constraints */
  constraints: Array<{
    constraintType: 'capacity' | 'minimum' | 'maximum' | 'ratio' | 'business_rule';
    targetId?: string;
    constraintValue: number;
    constraintDescription: string;
  }>;
  
  /** Optimization results */
  optimizationResults?: {
    objectiveValue: number;
    optimizedAllocations: Record<string, number>;
    improvementPercentage: number;
    constraintsSatisfied: boolean;
    optimizationTime: number;
  };
}