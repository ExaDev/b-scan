/**
 * Activity relationship tracking and entity association system
 * Provides sophisticated mapping between activities and various business entities
 */

import { ActivityType, ActivityStatus } from './ActivityTypes';

/**
 * Entity types that can be related to activities
 */
export enum EntityType {
  ITEM = 'ITEM',
  LOCATION = 'LOCATION',
  USER = 'USER',
  DEVICE = 'DEVICE',
  BATCH = 'BATCH',
  ORDER = 'ORDER',
  PROJECT = 'PROJECT',
  COST_CENTER = 'COST_CENTER',
  DEPARTMENT = 'DEPARTMENT',
  SUPPLIER = 'SUPPLIER',
  CUSTOMER = 'CUSTOMER',
  DOCUMENT = 'DOCUMENT',
  WORKFLOW = 'WORKFLOW',
  SYSTEM = 'SYSTEM',
}

/**
 * Relationship types defining the nature of the connection
 */
export enum RelationshipType {
  // Primary relationships (core to the activity)
  PRIMARY_TARGET = 'PRIMARY_TARGET',           // Main entity the activity operates on
  PRIMARY_SOURCE = 'PRIMARY_SOURCE',           // Main source entity
  PRIMARY_DESTINATION = 'PRIMARY_DESTINATION', // Main destination entity
  
  // Secondary relationships (supporting the activity)
  SECONDARY_TARGET = 'SECONDARY_TARGET',       // Additional entities operated on
  REFERENCE = 'REFERENCE',                     // Referenced for information
  DEPENDENCY = 'DEPENDENCY',                   // Required for activity execution
  
  // Process relationships
  INPUT = 'INPUT',                            // Provides input to the activity
  OUTPUT = 'OUTPUT',                          // Receives output from the activity
  TRANSFORMS = 'TRANSFORMS',                  // Entity is transformed by the activity
  
  // Authorization relationships
  AUTHORIZED_BY = 'AUTHORIZED_BY',            // Entity that authorized the activity
  EXECUTED_BY = 'EXECUTED_BY',               // Entity that executed the activity
  SUPERVISED_BY = 'SUPERVISED_BY',           // Entity that supervised the activity
  
  // Temporal relationships
  PRECEDES = 'PRECEDES',                     // Activity comes before this entity's process
  FOLLOWS = 'FOLLOWS',                       // Activity comes after this entity's process
  CONCURRENT = 'CONCURRENT',                 // Activity runs concurrently with entity process
  
  // Validation relationships
  VALIDATES = 'VALIDATES',                   // Activity validates this entity
  VALIDATED_BY = 'VALIDATED_BY',            // Activity is validated by this entity
  
  // Audit relationships
  AUDITS = 'AUDITS',                         // Activity audits this entity
  AUDITED_BY = 'AUDITED_BY',                // Activity is audited by this entity
  
  // Measurement relationships
  MEASURES = 'MEASURES',                     // Activity measures this entity
  CALIBRATES = 'CALIBRATES',                // Activity calibrates this entity
  
  // Custom relationships
  CUSTOM = 'CUSTOM',                         // Custom relationship type
}

/**
 * Relationship strength/importance indicator
 */
export enum RelationshipStrength {
  CRITICAL = 'CRITICAL',       // Activity cannot proceed without this relationship
  IMPORTANT = 'IMPORTANT',     // Activity quality significantly impacted without this
  MODERATE = 'MODERATE',       // Activity somewhat impacted without this
  WEAK = 'WEAK',              // Activity minimally impacted without this
  INFORMATIONAL = 'INFORMATIONAL', // Relationship is for information/tracking only
}

/**
 * Core relationship interface
 */
export interface ActivityRelationship {
  /** Unique identifier for this relationship */
  relationshipId: string;
  
  /** ID of the activity */
  activityId: string;
  
  /** ID of the related entity */
  entityId: string;
  
  /** Type of the related entity */
  entityType: EntityType;
  
  /** Type of relationship */
  relationshipType: RelationshipType;
  
  /** Strength/importance of the relationship */
  strength: RelationshipStrength;
  
  /** Direction of the relationship */
  direction: 'inbound' | 'outbound' | 'bidirectional';
  
  /** When the relationship was established */
  establishedAt: Date;
  
  /** When the relationship expires (if applicable) */
  expiresAt?: Date;
  
  /** Whether the relationship is currently active */
  isActive: boolean;
  
  /** Additional relationship metadata */
  metadata: Record<string, unknown>;
  
  /** Relationship context information */
  context?: {
    /** Business context */
    businessContext?: string;
    
    /** Technical context */
    technicalContext?: string;
    
    /** Process context */
    processContext?: string;
    
    /** Compliance context */
    complianceContext?: string;
  };
  
  /** Relationship constraints */
  constraints?: Array<{
    constraintType: string;
    constraintValue: unknown;
    constraintDescription: string;
    mandatory: boolean;
  }>;
}

/**
 * Enhanced entity information with activity context
 */
export interface ActivityEntity {
  /** Entity identifier */
  entityId: string;
  
  /** Entity type */
  entityType: EntityType;
  
  /** Entity display name */
  displayName?: string;
  
  /** Entity description */
  description?: string;
  
  /** Entity properties relevant to activities */
  properties: {
    /** Core properties */
    coreProperties: Record<string, unknown>;
    
    /** Activity-specific properties */
    activityProperties?: Record<ActivityType, Record<string, unknown>>;
    
    /** Temporal properties */
    temporalProperties?: {
      createdAt?: Date;
      updatedAt?: Date;
      validFrom?: Date;
      validTo?: Date;
    };
    
    /** Location properties */
    locationProperties?: {
      physicalLocation?: string;
      coordinates?: {
        latitude: number;
        longitude: number;
      };
      zone?: string;
    };
  };
  
  /** Entity state information */
  state: {
    /** Current state */
    currentState: string;
    
    /** Available states */
    availableStates: string[];
    
    /** State transition rules */
    stateTransitions?: Record<string, string[]>;
    
    /** Last state change */
    lastStateChange?: {
      previousState: string;
      newState: string;
      changedAt: Date;
      changedBy?: string;
      reason?: string;
    };
  };
  
  /** Entity capabilities */
  capabilities?: Array<{
    capabilityName: string;
    capabilityType: string;
    available: boolean;
    parameters?: Record<string, unknown>;
  }>;
}

/**
 * Relationship validation and consistency checking
 */
export interface RelationshipValidation {
  /** Overall validation result */
  isValid: boolean;
  
  /** Validation errors */
  validationErrors: Array<{
    errorCode: string;
    errorMessage: string;
    severity: 'warning' | 'error' | 'critical';
    affectedRelationshipIds: string[];
    suggestedAction?: string;
  }>;
  
  /** Consistency checks */
  consistencyChecks: Array<{
    checkName: string;
    passed: boolean;
    details?: string;
    affectedEntities?: string[];
  }>;
  
  /** Constraint violations */
  constraintViolations?: Array<{
    constraintType: string;
    violationDescription: string;
    affectedRelationships: string[];
    canProceed: boolean;
  }>;
  
  /** Orphaned relationships */
  orphanedRelationships?: Array<{
    relationshipId: string;
    reason: 'missing_activity' | 'missing_entity' | 'expired' | 'invalid_type';
    discoveredAt: Date;
  }>;
}

/**
 * Relationship graph analysis
 */
export interface RelationshipGraph {
  /** Graph nodes (activities and entities) */
  nodes: Array<{
    nodeId: string;
    nodeType: 'activity' | 'entity';
    entityType?: EntityType;
    activityType?: ActivityType;
    properties: Record<string, unknown>;
  }>;
  
  /** Graph edges (relationships) */
  edges: Array<{
    edgeId: string;
    sourceNodeId: string;
    targetNodeId: string;
    relationshipType: RelationshipType;
    weight: number;
    properties: Record<string, unknown>;
  }>;
  
  /** Graph analytics */
  analytics: {
    /** Total nodes and edges */
    nodeCount: number;
    edgeCount: number;
    
    /** Connectivity metrics */
    connectivityMetrics: {
      averageDegree: number;
      clusteringCoefficient: number;
      networkDensity: number;
      averagePathLength: number;
    };
    
    /** Central nodes */
    centralNodes: Array<{
      nodeId: string;
      centralityType: 'degree' | 'betweenness' | 'closeness' | 'eigenvector';
      centralityScore: number;
    }>;
    
    /** Community detection */
    communities?: Array<{
      communityId: string;
      nodeIds: string[];
      communityType: string;
      cohesionScore: number;
    }>;
    
    /** Critical paths */
    criticalPaths?: Array<{
      pathId: string;
      nodeSequence: string[];
      pathLength: number;
      pathWeight: number;
      criticalityScore: number;
    }>;
  };
}

/**
 * Relationship lifecycle management
 */
export interface RelationshipLifecycle {
  /** Lifecycle stages */
  stages: Array<{
    stageName: 'proposed' | 'established' | 'active' | 'suspended' | 'terminated' | 'archived';
    stageStatus: 'pending' | 'current' | 'completed';
    enteredAt?: Date;
    exitedAt?: Date;
    duration?: number;
    stageMetadata?: Record<string, unknown>;
  }>;
  
  /** Lifecycle triggers */
  triggers: Array<{
    triggerType: 'time_based' | 'event_based' | 'condition_based' | 'manual';
    triggerCondition: string;
    targetStage: string;
    triggerAction: string;
    isActive: boolean;
  }>;
  
  /** Automatic lifecycle management */
  automaticManagement: {
    enabled: boolean;
    rules: Array<{
      ruleId: string;
      condition: string;
      action: string;
      priority: number;
    }>;
  };
  
  /** Lifecycle notifications */
  notifications: Array<{
    notificationType: string;
    recipients: string[];
    trigger: string;
    template: string;
    enabled: boolean;
  }>;
}

/**
 * Relationship query and filtering
 */
export interface RelationshipQuery {
  /** Query filters */
  filters: {
    /** Filter by activity */
    activityFilters?: {
      activityIds?: string[];
      activityTypes?: ActivityType[];
      activityStatuses?: ActivityStatus[];
      dateRange?: {
        start: Date;
        end: Date;
      };
    };
    
    /** Filter by entity */
    entityFilters?: {
      entityIds?: string[];
      entityTypes?: EntityType[];
      entityProperties?: Record<string, unknown>;
    };
    
    /** Filter by relationship */
    relationshipFilters?: {
      relationshipTypes?: RelationshipType[];
      relationshipStrengths?: RelationshipStrength[];
      directions?: ('inbound' | 'outbound' | 'bidirectional')[];
      activeOnly?: boolean;
    };
  };
  
  /** Query options */
  options: {
    /** Include entity details */
    includeEntityDetails: boolean;
    
    /** Include relationship metadata */
    includeMetadata: boolean;
    
    /** Maximum results */
    limit?: number;
    
    /** Result offset for pagination */
    offset?: number;
    
    /** Sort by */
    sortBy?: string;
    
    /** Sort direction */
    sortDirection?: 'asc' | 'desc';
  };
}

/**
 * Relationship impact analysis
 */
export interface RelationshipImpactAnalysis {
  /** Impact assessment for changes */
  impactAssessment: {
    /** Affected activities */
    affectedActivities: Array<{
      activityId: string;
      activityType: ActivityType;
      impactType: 'blocking' | 'degraded' | 'enhanced' | 'informational';
      impactSeverity: 'low' | 'medium' | 'high' | 'critical';
      impactDescription: string;
      mitigationRequired: boolean;
    }>;
    
    /** Affected entities */
    affectedEntities: Array<{
      entityId: string;
      entityType: EntityType;
      impactType: string;
      impactDescription: string;
      requiresAction: boolean;
    }>;
    
    /** Cascade effects */
    cascadeEffects: Array<{
      originRelationshipId: string;
      affectedRelationshipId: string;
      effectType: 'dependency' | 'constraint' | 'validation' | 'notification';
      effectDescription: string;
      propagationDepth: number;
    }>;
  };
  
  /** Risk assessment */
  riskAssessment: {
    /** Overall risk level */
    overallRisk: 'low' | 'medium' | 'high' | 'critical';
    
    /** Risk factors */
    riskFactors: Array<{
      factorType: string;
      probability: number;
      impact: number;
      riskScore: number;
      description: string;
      mitigation?: string;
    }>;
    
    /** Contingency plans */
    contingencyPlans?: Array<{
      scenario: string;
      triggerConditions: string[];
      responseActions: string[];
      responsibleParty: string;
    }>;
  };
}

/**
 * Relationship reporting and analytics
 */
export interface RelationshipReporting {
  /** Summary statistics */
  summaryStatistics: {
    totalRelationships: number;
    activeRelationships: number;
    relationshipsByType: Record<RelationshipType, number>;
    relationshipsByEntity: Record<EntityType, number>;
    averageRelationshipsPerActivity: number;
    relationshipGrowthRate: number;
  };
  
  /** Usage patterns */
  usagePatterns: {
    mostCommonRelationshipTypes: Array<{
      relationshipType: RelationshipType;
      count: number;
      percentage: number;
    }>;
    
    mostConnectedEntities: Array<{
      entityId: string;
      entityType: EntityType;
      connectionCount: number;
    }>;
    
    relationshipLifespan: {
      averageLifespan: number;
      lifespanByType: Record<RelationshipType, number>;
    };
  };
  
  /** Trend analysis */
  trendAnalysis: {
    relationshipCreationTrends: Array<{
      period: string;
      creationCount: number;
      terminationCount: number;
      netGrowth: number;
    }>;
    
    entityTypeTrends: Record<EntityType, {
      trend: 'increasing' | 'stable' | 'decreasing';
      changeRate: number;
    }>;
  };
}

/**
 * Relationship builder for creating complex relationship structures
 */
export interface RelationshipBuilder {
  /** Build a new relationship */
  createRelationship(
    activityId: string,
    entityId: string,
    entityType: EntityType,
    relationshipType: RelationshipType,
    strength: RelationshipStrength
  ): RelationshipBuilder;
  
  /** Add metadata to the relationship */
  withMetadata(metadata: Record<string, unknown>): RelationshipBuilder;
  
  /** Add context information */
  withContext(context: ActivityRelationship['context']): RelationshipBuilder;
  
  /** Add constraints */
  withConstraints(constraints: ActivityRelationship['constraints']): RelationshipBuilder;
  
  /** Set expiration */
  withExpiration(expiresAt: Date): RelationshipBuilder;
  
  /** Build the final relationship */
  build(): ActivityRelationship;
}