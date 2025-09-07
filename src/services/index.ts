/**
 * Services module exports
 * Central entry point for all business logic services
 */

// Core infrastructure
export { BaseService, ServiceState } from './core/BaseService';
export type { ServiceResult, ServiceConfig } from './core/BaseService';

// Validation system
export { ValidationResult, ValidationResultType, ValidationUtils } from './validation';
export type { ValidationIssue } from './validation';

// Business services
export { ScanService } from './scan/ScanService';
export type { 
  ScanOperationRequest, 
  ScanOperationResult, 
  ScanProcessingOptions 
} from './scan/ScanService';

export { EntityService } from './entity/EntityService';
export type { 
  EntityQuery, 
  EntityUpdateRequest, 
  EntityCreateRequest, 
  EntityRelationship 
} from './entity/EntityService';

export { ActivityService } from './activity/ActivityService';
export type { 
  ActivityCreateRequest, 
  ActivityQuery, 
  ActivityStats, 
  ScanHistoryQuery 
} from './activity/ActivityService';

export { ComponentService } from './component/ComponentService';
export type { 
  ComponentCreateRequest, 
  ComponentUpdateRequest, 
  ComponentQuery, 
  WeightUpdateRequest, 
  FilamentUsageCalculation, 
  ComponentInventoryInfo 
} from './component/ComponentService';

// Dependency injection
export { ServiceContainer } from './container/ServiceContainer';
export type { ServiceConstructor, ServiceRegistration } from './container/ServiceContainer';

export { 
  ServiceRegistry, 
  getServiceRegistry, 
  configureServices, 
  cleanupServices 
} from './container/ServiceRegistry';

// Inventory management system
export { 
  InventoryService,
  InventoryItem,
  WeightInferenceEngine,
  ConfidenceCalculator,
  TrackingMode,
  StockMovementType,
} from './inventory';

export type {
  CalibrationResult,
  InferenceResult,
  WeightUpdateResult,
  WeightMeasurement,
  CalibrationData,
  ReorderConfig,
  StockMovement,
  InventoryItemData,
  WeightInferenceConfig,
  InventoryStats,
} from './inventory';

// Legacy services (existing)
export { NfcManagerService } from './NfcManagerService';
export { NfcManager } from './NfcManager';
export { BambuKeyDerivation } from './BambuKeyDerivation';