/**
 * Activity result types and progress tracking interfaces for sophisticated activity management
 */

import {
  ActivityType,
  ActivityStatus,
  ActivityResultType,
  ScanType,
  CalibrationType,
  MeasurementType,
  StockMovementType,
  DistributionMethod,
} from './ActivityTypes';

/**
 * Enhanced progress tracking with real-time updates and predictive analytics
 */
export interface ActivityProgressTracker {
  /** Current progress information */
  currentProgress: {
    /** Overall completion percentage (0-100) */
    completionPercentage: number;
    
    /** Current phase/stage of the activity */
    currentPhase: string;
    
    /** Current step within the phase */
    currentStep: string;
    
    /** Step progress within current step (0-100) */
    stepProgress: number;
    
    /** Time spent so far (milliseconds) */
    elapsedTime: number;
    
    /** Estimated time remaining (milliseconds) */
    estimatedTimeRemaining: number;
  };
  
  /** Progress history */
  progressHistory: Array<{
    timestamp: Date;
    completionPercentage: number;
    phase: string;
    step: string;
    milestone?: string;
    notes?: string;
  }>;
  
  /** Milestone tracking */
  milestones: Array<{
    milestoneId: string;
    milestoneName: string;
    targetCompletionPercentage: number;
    actualCompletionPercentage?: number;
    status: 'pending' | 'in_progress' | 'completed' | 'missed';
    plannedTime?: number;
    actualTime?: number;
    importance: 'low' | 'medium' | 'high' | 'critical';
  }>;
  
  /** Performance metrics */
  performanceMetrics: {
    /** Velocity (progress per unit time) */
    velocity: number;
    
    /** Acceleration/deceleration in progress */
    acceleration: number;
    
    /** Efficiency rating (actual vs. planned progress) */
    efficiency: number;
    
    /** Quality score based on intermediate results */
    qualityScore: number;
    
    /** Consistency of progress rate */
    consistencyScore: number;
  };
  
  /** Predictive analytics */
  predictions: {
    /** Predicted completion time */
    predictedCompletionTime: Date;
    
    /** Confidence in prediction (0-1) */
    predictionConfidence: number;
    
    /** Risk factors that could affect completion */
    riskFactors: Array<{
      riskType: string;
      probability: number;
      impact: 'low' | 'medium' | 'high' | 'critical';
      mitigation?: string;
    }>;
    
    /** Alternative scenarios */
    scenarios: Array<{
      scenarioName: string;
      probability: number;
      estimatedCompletionTime: Date;
      requiredActions?: string[];
    }>;
  };
  
  /** Blocking/dependency issues */
  blockers: Array<{
    blockerId: string;
    blockerType: 'dependency' | 'resource' | 'authorization' | 'technical' | 'external';
    blockerDescription: string;
    severity: 'low' | 'medium' | 'high' | 'critical';
    estimatedResolutionTime?: number;
    resolutionActions?: string[];
    blockedSince: Date;
  }>;
}

/**
 * Detailed result tracking for different activity types
 */
export interface TypedActivityResult {
  /** Base result information */
  baseResult: {
    type: ActivityResultType;
    message?: string;
    completedAt: Date;
    duration: number;
    success: boolean;
  };
  
  /** Type-specific results */
  specificResult: ScanOccurrenceResult | CalibrationResult | MeasurementResult | StockMovementResult | ConsumptionDistributionResult;
  
  /** Quality assessment */
  qualityAssessment: {
    overallQuality: 'excellent' | 'good' | 'acceptable' | 'poor';
    qualityScore: number; // 0-100
    qualityFactors: Record<string, number>;
    qualityNotes?: string[];
  };
  
  /** Performance metrics */
  performanceMetrics: {
    executionTime: number;
    resourceUtilization: Record<string, number>;
    throughput?: number;
    errorRate: number;
    retryCount: number;
  };
  
  /** Business impact */
  businessImpact?: {
    costImpact: number;
    timeImpact: number;
    qualityImpact: number;
    complianceImpact: number;
    customerImpact?: number;
  };
}

/**
 * Scan occurrence specific results
 */
export interface ScanOccurrenceResult {
  /** Scan success details */
  scanSuccess: {
    scanType: ScanType;
    successful: boolean;
    attemptCount: number;
    finalScanData?: {
      rawData: string;
      decodedData?: Record<string, any>;
      confidenceScore: number;
      qualityMetrics: Record<string, number>;
    };
  };
  
  /** Scan performance */
  scanPerformance: {
    averageScanTime: number;
    scanAccuracy: number;
    dataIntegrityScore: number;
    environmentalOptimization: number;
  };
  
  /** Validation results */
  validationResults: {
    formatValidation: boolean;
    businessRuleValidation: boolean;
    integrityValidation: boolean;
    validationErrors: string[];
  };
  
  /** Post-scan actions */
  postScanActions: Array<{
    action: string;
    successful: boolean;
    duration: number;
    result?: any;
  }>;
}

/**
 * Calibration activity specific results
 */
export interface CalibrationResult {
  /** Calibration outcome */
  calibrationOutcome: {
    calibrationType: CalibrationType;
    result: 'pass' | 'pass_with_adjustments' | 'fail';
    accuracy: number;
    uncertainty: number;
    calibrationCurve?: {
      equation: string;
      correlationCoefficient: number;
      standardError: number;
    };
  };
  
  /** Measurement statistics */
  measurementStatistics: {
    totalReadings: number;
    validReadings: number;
    averageDeviation: number;
    standardDeviation: number;
    repeatability: number;
    reproducibility: number;
  };
  
  /** Calibration adjustments */
  adjustmentsMade: Array<{
    parameter: string;
    originalValue: number;
    adjustedValue: number;
    adjustmentType: string;
    reason: string;
  }>;
  
  /** Certificate information */
  certificateInfo?: {
    certificateNumber: string;
    issuedDate: Date;
    validUntil: Date;
    certifiedBy: string;
  };
}

/**
 * Measurement activity specific results
 */
export interface MeasurementResult {
  /** Final measurement */
  finalMeasurement: {
    measurementType: MeasurementType;
    value: number;
    unit: string;
    uncertainty: number;
    confidenceLevel: number;
  };
  
  /** Statistical analysis */
  statisticalAnalysis: {
    sampleCount: number;
    mean: number;
    standardDeviation: number;
    variance: number;
    coefficientOfVariation: number;
    outlierCount: number;
  };
  
  /** Measurement validation */
  validation: {
    withinSpecification: boolean;
    repeatabilityAcceptable: boolean;
    uncertaintyAcceptable: boolean;
    validationNotes?: string[];
  };
  
  /** Traceability */
  traceability: {
    standardsUsed: string[];
    calibrationReferences: string[];
    measurementProcedure: string;
    environmentalConditions: Record<string, number>;
  };
}

/**
 * Stock movement activity specific results
 */
export interface StockMovementResult {
  /** Movement execution summary */
  movementSummary: {
    movementType: StockMovementType;
    totalItemsMoved: number;
    totalQuantityMoved: number;
    totalValueMoved?: number;
    movementAccuracy: number;
  };
  
  /** Item-level results */
  itemResults: Array<{
    itemId: string;
    plannedQuantity: number;
    actualQuantity: number;
    discrepancies: Array<{
      type: string;
      expected: any;
      actual: any;
      resolved: boolean;
    }>;
  }>;
  
  /** Location updates */
  locationUpdates: Array<{
    locationId: string;
    locationType: 'source' | 'destination';
    previousStockLevel: number;
    newStockLevel: number;
    capacityUtilization: number;
  }>;
  
  /** Integration results */
  integrationResults: Record<string, {
    system: string;
    status: 'success' | 'failed' | 'pending';
    timestamp: Date;
    error?: string;
  }>;
}

/**
 * Consumption distribution activity specific results
 */
export interface ConsumptionDistributionResult {
  /** Distribution summary */
  distributionSummary: {
    distributionMethod: DistributionMethod;
    totalConsumptionDistributed: number;
    numberOfTargets: number;
    distributionAccuracy: number;
    distributionEfficiency: number;
  };
  
  /** Target allocations */
  targetAllocations: Array<{
    targetId: string;
    targetType: string;
    allocatedAmount: number;
    allocatedPercentage: number;
    allocationBasis: string;
    confidence: number;
  }>;
  
  /** Method performance */
  methodPerformance: {
    calculationTime: number;
    convergenceIterations?: number;
    optimizationScore?: number;
    alternativeMethodsConsidered: DistributionMethod[];
  };
  
  /** Validation and compliance */
  validationCompliance: {
    totalValidation: boolean;
    businessRuleCompliance: boolean;
    auditTrailComplete: boolean;
    approvalStatus: 'not_required' | 'pending' | 'approved' | 'rejected';
  };
}

/**
 * Activity result aggregation and analytics
 */
export interface ActivityResultAnalytics {
  /** Aggregate statistics across activities */
  aggregateStatistics: {
    totalActivities: number;
    successRate: number;
    averageExecutionTime: number;
    averageQualityScore: number;
    mostCommonFailureReason: string;
  };
  
  /** Performance trends */
  performanceTrends: Array<{
    period: string;
    activityCount: number;
    successRate: number;
    averageTime: number;
    qualityTrend: 'improving' | 'stable' | 'declining';
  }>;
  
  /** Activity type breakdown */
  activityTypeBreakdown: Record<ActivityType, {
    count: number;
    successRate: number;
    averageTime: number;
    qualityScore: number;
  }>;
  
  /** Resource utilization */
  resourceUtilization: Record<string, {
    totalUsage: number;
    averageUsage: number;
    peakUsage: number;
    utilizationTrend: 'increasing' | 'stable' | 'decreasing';
  }>;
  
  /** Recommendations */
  recommendations: Array<{
    recommendationType: 'performance' | 'quality' | 'efficiency' | 'cost';
    priority: 'low' | 'medium' | 'high' | 'critical';
    recommendation: string;
    expectedImpact: string;
    implementationEffort: 'low' | 'medium' | 'high';
  }>;
}

/**
 * Real-time activity monitoring
 */
export interface ActivityMonitor {
  /** Current system state */
  systemState: {
    activeActivities: number;
    queuedActivities: number;
    failedActivities: number;
    systemHealth: 'healthy' | 'degraded' | 'critical';
    resourceAvailability: Record<string, number>;
  };
  
  /** Active activity monitoring */
  activeActivityMonitoring: Array<{
    activityId: string;
    activityType: ActivityType;
    status: ActivityStatus;
    progress: number;
    estimatedCompletion: Date;
    resourceUsage: Record<string, number>;
    issues: string[];
  }>;
  
  /** System alerts */
  alerts: Array<{
    alertId: string;
    alertType: 'performance' | 'error' | 'resource' | 'security' | 'business';
    severity: 'info' | 'warning' | 'error' | 'critical';
    message: string;
    timestamp: Date;
    acknowledged: boolean;
    resolvedAt?: Date;
  }>;
  
  /** Performance thresholds */
  performanceThresholds: {
    maxExecutionTime: Record<ActivityType, number>;
    minSuccessRate: number;
    maxErrorRate: number;
    resourceThresholds: Record<string, number>;
  };
}

/**
 * Activity result export and reporting
 */
export interface ActivityResultExport {
  /** Export format */
  format: 'json' | 'csv' | 'xlsx' | 'pdf' | 'xml';
  
  /** Export scope */
  scope: {
    activityTypes?: ActivityType[];
    dateRange?: {
      start: Date;
      end: Date;
    };
    statusFilter?: ActivityStatus[];
    includeDetails: boolean;
    includeAnalytics: boolean;
  };
  
  /** Generated export */
  exportData?: {
    exportId: string;
    generatedAt: Date;
    fileSize: number;
    recordCount: number;
    downloadUrl?: string;
    expiresAt?: Date;
  };
}

/**
 * Activity result comparison and benchmarking
 */
export interface ActivityBenchmarking {
  /** Internal benchmarks */
  internalBenchmarks: {
    historicalAverage: Record<ActivityType, number>;
    bestPerformance: Record<ActivityType, number>;
    targetPerformance: Record<ActivityType, number>;
  };
  
  /** Industry benchmarks */
  industryBenchmarks?: {
    industryAverage: Record<ActivityType, number>;
    topPercentile: Record<ActivityType, number>;
    benchmarkSource: string;
    benchmarkDate: Date;
  };
  
  /** Benchmark comparison */
  benchmarkComparison: Array<{
    activityType: ActivityType;
    currentPerformance: number;
    benchmarkPerformance: number;
    performanceGap: number;
    performanceRank: 'below_average' | 'average' | 'above_average' | 'excellent';
    improvementOpportunity: string;
  }>;
}