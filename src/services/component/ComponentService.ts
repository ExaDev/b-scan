/**
 * ComponentService - Manages physical components and filament data operations
 * Handles business logic for filament components, weight tracking, and inventory integration
 */

import { BaseService, ServiceResult } from '../core/BaseService';
import { ValidationResult, ValidationUtils } from '../validation';
import { EntityService } from '../entity/EntityService';
import { ActivityService } from '../activity/ActivityService';
import {
  PhysicalComponent,
  FilamentInfo,
  EntityType,
  InventoryItem,
} from '../../types/FilamentInfo';

export interface ComponentCreateRequest {
  filamentInfo: FilamentInfo;
  currentWeight?: number;
  notes?: string;
  initialInventoryQuantity?: number;
  location?: string;
}

export interface ComponentUpdateRequest {
  id: string;
  currentWeight?: number;
  notes?: string;
  filamentInfo?: Partial<FilamentInfo>;
}

export interface ComponentQuery {
  manufacturerName?: string;
  filamentType?: string;
  colorName?: string;
  tagUid?: string;
  hasCurrentWeight?: boolean;
  weightLessThan?: number;
  weightGreaterThan?: number;
  limit?: number;
  offset?: number;
}

export interface WeightUpdateRequest {
  componentId: string;
  newWeight: number;
  reason?: string;
  userId?: string;
}

export interface FilamentUsageCalculation {
  originalWeight: number;
  currentWeight: number;
  weightUsed: number;
  percentageUsed: number;
  remainingWeight: number;
  remainingPercentage: number;
  isLowFilament: boolean;
  isEmpty: boolean;
}

export interface ComponentInventoryInfo {
  component: PhysicalComponent;
  inventoryItem?: InventoryItem;
  usageCalculation?: FilamentUsageCalculation;
  lastScanDate?: number;
  scanCount: number;
}

export class ComponentService extends BaseService {
  private entityService?: EntityService;
  private activityService?: ActivityService;
  private lowFilamentThreshold = 0.15; // 15% remaining
  private emptyFilamentThreshold = 0.05; // 5% remaining

  constructor(...args: unknown[]) {
    super();
    // Dependencies are injected via setter methods after construction
    // See ServiceRegistry.wireServiceDependencies()
    const [entityService, activityService] = args;
    if (entityService && entityService instanceof BaseService) {
      this.entityService = entityService as EntityService;
    }
    if (activityService && activityService instanceof BaseService) {
      this.activityService = activityService as ActivityService;
    }
  }

  /**
   * Set service dependencies
   */
  setEntityService(entityService: EntityService): void {
    this.entityService = entityService;
  }

  setActivityService(activityService: ActivityService): void {
    this.activityService = activityService;
  }

  /**
   * Create a new physical component from scan data
   */
  async createComponent(request: ComponentCreateRequest): Promise<ServiceResult<ComponentInventoryInfo>> {
    this.ensureInitialized();

    if (!this.entityService) {
      return ValidationResult.singleError(
        'entityService',
        'DEPENDENCY_MISSING',
        'EntityService is required but not provided'
      );
    }

    try {
      // Validate request
      const validationResult = this.validateComponentCreateRequest(request);
      if (!validationResult.isValid) {
        return ValidationResult.error(validationResult.issues);
      }

      // Check for existing component with same tag UID
      const existingResult = await this.findComponentByTagUid(request.filamentInfo.tagUid);
      if (existingResult.isValid && existingResult.data) {
        return ValidationResult.singleError(
          'tagUid',
          'DUPLICATE_COMPONENT',
          `Component with tag UID ${request.filamentInfo.tagUid} already exists`
        );
      }

      // Create physical component
      const component: Omit<PhysicalComponent, 'id' | 'createdAt' | 'updatedAt'> = {
        type: EntityType.PHYSICAL_COMPONENT,
        filamentInfo: request.filamentInfo,
      };
      
      // Add optional properties conditionally
      const weightValue = request.currentWeight || request.filamentInfo.spoolWeight;
      if (weightValue !== undefined) {
        component.currentWeight = weightValue;
      }
      
      if (request.notes !== undefined) {
        component.notes = request.notes;
      }

      const componentResult = await this.entityService.createEntity({
        entityData: component,
        generateId: true,
      });

      if (!componentResult.isValid) {
        return ValidationResult.error(componentResult.issues);
      }

      const createdComponent = componentResult.data!;
      let inventoryItem: InventoryItem | undefined;

      // Create initial inventory item if requested
      if (request.initialInventoryQuantity !== undefined) {
        const inventoryResult = await this.createInventoryItem(
          request.initialInventoryQuantity,
          request.location
        );

        if (inventoryResult.isValid) {
          inventoryItem = inventoryResult.data;
        }
      }

      // Record creation activity
      if (this.activityService) {
        await this.activityService.recordActivity({
          activityType: 'CREATE',
          description: `Created component: ${request.filamentInfo.manufacturerName} ${request.filamentInfo.filamentType} (${request.filamentInfo.colorName})`,
          relatedEntityId: createdComponent.id,
          metadata: {
            tagUid: request.filamentInfo.tagUid,
            initialWeight: component.currentWeight,
          },
        });
      }

      const result: ComponentInventoryInfo = {
        component: createdComponent,
        usageCalculation: this.calculateFilamentUsage(createdComponent),
        scanCount: 0,
      };

      // Add optional properties conditionally
      if (inventoryItem !== undefined) {
        result.inventoryItem = inventoryItem;
      }

      this.log('info', `Created component: ${createdComponent.id} (${request.filamentInfo.tagUid})`);
      return ValidationResult.success(result);

    } catch (error) {
      return this.handleError(error, 'createComponent');
    }
  }

  /**
   * Update component weight and track usage
   */
  async updateComponentWeight(request: WeightUpdateRequest): Promise<ServiceResult<ComponentInventoryInfo>> {
    this.ensureInitialized();

    if (!this.entityService) {
      return ValidationResult.singleError(
        'entityService',
        'DEPENDENCY_MISSING',
        'EntityService is required but not provided'
      );
    }

    try {
      // Validate weight update request
      const validationResult = this.validateWeightUpdateRequest(request);
      if (!validationResult.isValid) {
        return ValidationResult.error(validationResult.issues);
      }

      // Get existing component
      const componentResult = await this.entityService.getEntity<PhysicalComponent>(request.componentId);
      if (!componentResult.isValid || !componentResult.data) {
        return ValidationResult.singleError(
          'componentId',
          'COMPONENT_NOT_FOUND',
          `Component ${request.componentId} not found`
        );
      }

      const existingComponent = componentResult.data;
      const oldWeight = existingComponent.currentWeight || 0;

      // Update component weight
      const updateResult = await this.entityService.updateEntity<PhysicalComponent>({
        id: request.componentId,
        updates: {
          currentWeight: request.newWeight,
        },
      });

      if (!updateResult.isValid) {
        return ValidationResult.error(updateResult.issues);
      }

      const updatedComponent = updateResult.data! as PhysicalComponent;

      // Record weight update activity
      if (this.activityService) {
        const weightDelta = request.newWeight - oldWeight;
        const action = weightDelta > 0 ? 'Added' : 'Used';
        const amount = Math.abs(weightDelta);

        await this.activityService.recordActivity({
          activityType: 'UPDATE',
          description: `${action} ${amount}g of filament. Weight: ${oldWeight}g → ${request.newWeight}g`,
          relatedEntityId: request.componentId,
          metadata: {
            oldWeight,
            newWeight: request.newWeight,
            weightDelta,
            reason: request.reason,
            userId: request.userId,
          },
        });
      }

      // Get inventory information
      const inventoryResult = await this.getComponentInventoryInfo(request.componentId);
      if (inventoryResult.isValid) {
        return ValidationResult.success(inventoryResult.data!);
      }

      // Fallback result without inventory info
      const result: ComponentInventoryInfo = {
        component: updatedComponent,
        usageCalculation: this.calculateFilamentUsage(updatedComponent),
        scanCount: 0,
      };

      return ValidationResult.success(result);

    } catch (error) {
      return this.handleError(error, 'updateComponentWeight');
    }
  }

  /**
   * Find component by tag UID
   */
  async findComponentByTagUid(tagUid: string): Promise<ServiceResult<PhysicalComponent | null>> {
    this.ensureInitialized();

    if (!this.entityService) {
      return ValidationResult.singleError(
        'entityService',
        'DEPENDENCY_MISSING',
        'EntityService is required but not provided'
      );
    }

    try {
      const componentsResult = await this.entityService.getEntitiesByType<PhysicalComponent>(
        EntityType.PHYSICAL_COMPONENT
      );

      if (!componentsResult.isValid) {
        return ValidationResult.error(componentsResult.issues);
      }

      const components = componentsResult.data!;
      const matchingComponent = components.find(
        component => component.filamentInfo.tagUid === tagUid
      );

      return ValidationResult.success(matchingComponent || null);

    } catch (error) {
      return this.handleError(error, 'findComponentByTagUid');
    }
  }

  /**
   * Query components with filters
   */
  async queryComponents(query: ComponentQuery = {}): Promise<ServiceResult<PhysicalComponent[]>> {
    this.ensureInitialized();

    if (!this.entityService) {
      return ValidationResult.singleError(
        'entityService',
        'DEPENDENCY_MISSING',
        'EntityService is required but not provided'
      );
    }

    try {
      const entityQuery: {
        entityType: EntityType;
        limit?: number;
        offset?: number;
      } = {
        entityType: EntityType.PHYSICAL_COMPONENT,
      };

      // Add optional properties conditionally
      if (query.limit !== undefined) {
        entityQuery.limit = query.limit;
      }
      if (query.offset !== undefined) {
        entityQuery.offset = query.offset;
      }

      const componentsResult = await this.entityService.queryEntities<PhysicalComponent>(entityQuery);
      if (!componentsResult.isValid) {
        return ValidationResult.error(componentsResult.issues);
      }

      let components = componentsResult.data!;

      // Apply filters
      if (query.manufacturerName) {
        components = components.filter(
          component => component.filamentInfo.manufacturerName
            .toLowerCase()
            .includes(query.manufacturerName!.toLowerCase())
        );
      }

      if (query.filamentType) {
        components = components.filter(
          component => component.filamentInfo.filamentType
            .toLowerCase()
            .includes(query.filamentType!.toLowerCase())
        );
      }

      if (query.colorName) {
        components = components.filter(
          component => component.filamentInfo.colorName
            .toLowerCase()
            .includes(query.colorName!.toLowerCase())
        );
      }

      if (query.tagUid) {
        components = components.filter(
          component => component.filamentInfo.tagUid === query.tagUid
        );
      }

      if (query.hasCurrentWeight !== undefined) {
        components = components.filter(
          component => (component.currentWeight !== undefined) === query.hasCurrentWeight
        );
      }

      if (query.weightLessThan !== undefined) {
        components = components.filter(
          component => (component.currentWeight || 0) < query.weightLessThan!
        );
      }

      if (query.weightGreaterThan !== undefined) {
        components = components.filter(
          component => (component.currentWeight || 0) > query.weightGreaterThan!
        );
      }

      return ValidationResult.success(components);

    } catch (error) {
      return this.handleError(error, 'queryComponents');
    }
  }

  /**
   * Get components that are low on filament
   */
  async getLowFilamentComponents(): Promise<ServiceResult<ComponentInventoryInfo[]>> {
    const componentsResult = await this.queryComponents({ hasCurrentWeight: true });
    if (!componentsResult.isValid) {
      return ValidationResult.error(componentsResult.issues);
    }

    const components = componentsResult.data!;
    const lowFilamentComponents: ComponentInventoryInfo[] = [];

    for (const component of components) {
      const usage = this.calculateFilamentUsage(component);
      if (usage.isLowFilament) {
        lowFilamentComponents.push({
          component,
          usageCalculation: usage,
          scanCount: 0,
        });
      }
    }

    return ValidationResult.success(lowFilamentComponents);
  }

  /**
   * Get component with full inventory information
   */
  async getComponentInventoryInfo(componentId: string): Promise<ServiceResult<ComponentInventoryInfo>> {
    this.ensureInitialized();

    if (!this.entityService) {
      return ValidationResult.singleError(
        'entityService',
        'DEPENDENCY_MISSING',
        'EntityService is required but not provided'
      );
    }

    try {
      // Get component
      const componentResult = await this.entityService.getEntity<PhysicalComponent>(componentId);
      if (!componentResult.isValid || !componentResult.data) {
        return ValidationResult.singleError(
          'componentId',
          'COMPONENT_NOT_FOUND',
          `Component ${componentId} not found`
        );
      }

      const component = componentResult.data;

      // Find related inventory item
      const inventoryResult = await this.findInventoryItemForComponent();
      const inventoryItem = inventoryResult.isValid ? inventoryResult.data || undefined : undefined;

      // Calculate usage
      const usageCalculation = this.calculateFilamentUsage(component);

      // Get scan count from activities (if activity service available)
      let scanCount = 0;
      if (this.activityService) {
        const activitiesResult = await this.activityService.getActivities({
          activityType: 'SCAN',
          relatedEntityId: componentId,
        });
        if (activitiesResult.isValid) {
          scanCount = activitiesResult.data!.length;
        }
      }

      const result: ComponentInventoryInfo = {
        component,
        usageCalculation,
        scanCount,
      };

      // Add optional properties conditionally
      if (inventoryItem !== undefined) {
        result.inventoryItem = inventoryItem;
      }

      return ValidationResult.success(result);

    } catch (error) {
      return this.handleError(error, 'getComponentInventoryInfo');
    }
  }

  /**
   * Calculate filament usage from component data
   */
  calculateFilamentUsage(component: PhysicalComponent): FilamentUsageCalculation {
    const originalWeight = component.filamentInfo.spoolWeight;
    const currentWeight = component.currentWeight || originalWeight;
    
    const weightUsed = Math.max(0, originalWeight - currentWeight);
    const remainingWeight = currentWeight;
    
    const percentageUsed = originalWeight > 0 ? (weightUsed / originalWeight) * 100 : 0;
    const remainingPercentage = originalWeight > 0 ? (remainingWeight / originalWeight) * 100 : 100;
    
    const isLowFilament = remainingPercentage <= (this.lowFilamentThreshold * 100);
    const isEmpty = remainingPercentage <= (this.emptyFilamentThreshold * 100);

    return {
      originalWeight,
      currentWeight,
      weightUsed,
      percentageUsed,
      remainingWeight,
      remainingPercentage,
      isLowFilament,
      isEmpty,
    };
  }

  /**
   * Set low filament threshold (percentage)
   */
  setLowFilamentThreshold(percentage: number): void {
    this.lowFilamentThreshold = Math.max(0, Math.min(1, percentage / 100));
  }

  /**
   * Set empty filament threshold (percentage)
   */
  setEmptyFilamentThreshold(percentage: number): void {
    this.emptyFilamentThreshold = Math.max(0, Math.min(1, percentage / 100));
  }

  // Private methods

  private validateComponentCreateRequest(request: ComponentCreateRequest): ValidationResult<void> {
    const validations = [
      ValidationUtils.validateRequired(request.filamentInfo.tagUid, 'tagUid'),
      ValidationUtils.validateRequired(request.filamentInfo.manufacturerName, 'manufacturerName'),
      ValidationUtils.validateRequired(request.filamentInfo.filamentType, 'filamentType'),
      ValidationUtils.validatePositiveNumber(request.filamentInfo.spoolWeight, 'spoolWeight'),
    ];

    if (request.currentWeight !== undefined) {
      validations.push(
        ValidationUtils.validateNonNegativeNumber(request.currentWeight, 'currentWeight')
      );
    }

    if (request.notes !== undefined) {
      validations.push(
        ValidationUtils.validateLength(request.notes, 'notes', 0, 500)
      );
    }

    return ValidationUtils.combineResults(...validations).map(() => undefined);
  }

  private validateWeightUpdateRequest(request: WeightUpdateRequest): ValidationResult<void> {
    const validations = [
      ValidationUtils.validateRequired(request.componentId, 'componentId'),
      ValidationUtils.validateNonNegativeNumber(request.newWeight, 'newWeight'),
    ];

    if (request.reason !== undefined) {
      validations.push(
        ValidationUtils.validateLength(request.reason, 'reason', 0, 200)
      );
    }

    return ValidationUtils.combineResults(...validations).map(() => undefined);
  }

  private async createInventoryItem(
    quantity: number,
    location?: string
  ): Promise<ServiceResult<InventoryItem>> {
    if (!this.entityService) {
      return ValidationResult.singleError(
        'entityService',
        'DEPENDENCY_MISSING',
        'EntityService is required'
      );
    }

    const inventoryItem: Omit<InventoryItem, 'id' | 'createdAt' | 'updatedAt'> = {
      type: EntityType.INVENTORY_ITEM,
      quantity,
      lastUpdated: Date.now(),
    };

    // Add optional properties conditionally
    if (location !== undefined) {
      inventoryItem.location = location;
    }

    return this.entityService.createEntity({
      entityData: inventoryItem,
      generateId: true,
    });
  }

  private async findInventoryItemForComponent(): Promise<ServiceResult<InventoryItem | null>> {
    if (!this.entityService) {
      return ValidationResult.success(null);
    }

    // This would typically use relationships, but for now we'll return null
    // In a real implementation, you'd query for inventory items related to this component
    return ValidationResult.success(null);
  }

  protected override async doCleanup(): Promise<void> {
    // Service cleanup if needed
  }
}