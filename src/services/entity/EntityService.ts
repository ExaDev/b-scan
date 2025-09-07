/**
 * EntityService - CRUD operations for graph entities with validation
 * Manages the lifecycle of all entity types with business rule enforcement
 */

import { BaseService, ServiceResult } from '../core/BaseService';
import { ValidationResult, ValidationUtils } from '../validation';
import {
  GraphEntity,
  EntityType,
  PhysicalComponent,
  InventoryItem,
  Identifier,
  Activity,
  FilamentInfo,
} from '../../types/FilamentInfo';

export interface EntityQuery {
  entityType?: EntityType;
  id?: string;
  ids?: string[];
  createdAfter?: number;
  createdBefore?: number;
  updatedAfter?: number;
  updatedBefore?: number;
  limit?: number;
  offset?: number;
}

export interface EntityUpdateRequest<T extends GraphEntity> {
  id: string;
  updates: Partial<Omit<T, 'id' | 'type' | 'createdAt'>>;
}

export interface EntityCreateRequest<T extends GraphEntity> {
  entityData: Omit<T, 'id' | 'createdAt' | 'updatedAt'>;
  generateId?: boolean;
}

export interface EntityRelationship {
  sourceEntityId: string;
  targetEntityId: string;
  relationshipType: string;
  metadata?: Record<string, any>;
  createdAt: number;
}

export class EntityService extends BaseService {
  private entities: Map<string, GraphEntity> = new Map();
  private entityTypeIndex: Map<EntityType, Set<string>> = new Map();
  private relationships: Map<string, EntityRelationship[]> = new Map();

  /**
   * Create a new entity
   */
  async createEntity<T extends GraphEntity>(
    request: EntityCreateRequest<T>
  ): Promise<ServiceResult<T>> {
    this.ensureInitialized();

    try {
      const { entityData, generateId = true } = request;

      // Validate entity data
      const validationResult = await this.validateEntityForCreation(entityData);
      if (!validationResult.isValid) {
        return ValidationResult.error(validationResult.issues);
      }

      // Generate ID if needed
      const id = generateId ? this.generateEntityId(entityData.type) : (entityData as any).id;
      if (!id) {
        return ValidationResult.singleError(
          'id',
          'MISSING_ID',
          'Entity ID is required when generateId is false'
        );
      }

      // Check for duplicate ID
      if (this.entities.has(id)) {
        return ValidationResult.singleError(
          'id',
          'DUPLICATE_ID',
          `Entity with ID ${id} already exists`
        );
      }

      // Create entity
      const now = Date.now();
      const entity: T = {
        ...entityData,
        id,
        createdAt: now,
        updatedAt: now,
      } as T;

      // Perform type-specific validation and setup
      const setupResult = await this.setupEntityByType(entity);
      if (!setupResult.isValid) {
        return ValidationResult.error(setupResult.issues);
      }

      // Store entity
      this.entities.set(id, entity);
      this.addToTypeIndex(entity.type, id);

      this.log('info', `Created entity: ${entity.type} ${id}`);
      return ValidationResult.success(entity);

    } catch (error) {
      return this.handleError(error, 'createEntity');
    }
  }

  /**
   * Get entity by ID
   */
  async getEntity<T extends GraphEntity>(id: string): Promise<ServiceResult<T | null>> {
    this.ensureInitialized();

    try {
      const entity = this.entities.get(id);
      return ValidationResult.success((entity as T) || null);
    } catch (error) {
      return this.handleError(error, 'getEntity');
    }
  }

  /**
   * Query entities with filters
   */
  async queryEntities<T extends GraphEntity>(query: EntityQuery): Promise<ServiceResult<T[]>> {
    this.ensureInitialized();

    try {
      let entities = Array.from(this.entities.values());

      // Apply filters
      if (query.entityType) {
        entities = entities.filter(entity => entity.type === query.entityType);
      }

      if (query.id) {
        entities = entities.filter(entity => entity.id === query.id);
      }

      if (query.ids && query.ids.length > 0) {
        const idSet = new Set(query.ids);
        entities = entities.filter(entity => idSet.has(entity.id));
      }

      if (query.createdAfter !== undefined) {
        entities = entities.filter(entity => entity.createdAt > query.createdAfter!);
      }

      if (query.createdBefore !== undefined) {
        entities = entities.filter(entity => entity.createdAt < query.createdBefore!);
      }

      if (query.updatedAfter !== undefined) {
        entities = entities.filter(entity => entity.updatedAt > query.updatedAfter!);
      }

      if (query.updatedBefore !== undefined) {
        entities = entities.filter(entity => entity.updatedAt < query.updatedBefore!);
      }

      // Sort by creation date (newest first)
      entities.sort((a, b) => b.createdAt - a.createdAt);

      // Apply pagination
      const offset = query.offset || 0;
      const limit = query.limit || 100;
      
      const paginatedEntities = entities.slice(offset, offset + limit);

      return ValidationResult.success(paginatedEntities as T[]);

    } catch (error) {
      return this.handleError(error, 'queryEntities');
    }
  }

  /**
   * Update an existing entity
   */
  async updateEntity<T extends GraphEntity>(
    request: EntityUpdateRequest<T>
  ): Promise<ServiceResult<T>> {
    this.ensureInitialized();

    try {
      const { id, updates } = request;

      // Get existing entity
      const existingEntity = this.entities.get(id);
      if (!existingEntity) {
        return ValidationResult.singleError(
          'id',
          'ENTITY_NOT_FOUND',
          `Entity with ID ${id} not found`
        );
      }

      // Validate updates
      const validationResult = await this.validateEntityForUpdate(existingEntity, updates);
      if (!validationResult.isValid) {
        return ValidationResult.error(validationResult.issues);
      }

      // Apply updates
      const updatedEntity: T = {
        ...existingEntity,
        ...updates,
        id: existingEntity.id, // Preserve ID
        type: existingEntity.type, // Preserve type
        createdAt: existingEntity.createdAt, // Preserve creation date
        updatedAt: Date.now(),
      } as T;

      // Store updated entity
      this.entities.set(id, updatedEntity);

      this.log('info', `Updated entity: ${updatedEntity.type} ${id}`);
      return ValidationResult.success(updatedEntity);

    } catch (error) {
      return this.handleError(error, 'updateEntity');
    }
  }

  /**
   * Delete an entity
   */
  async deleteEntity(id: string): Promise<ServiceResult<boolean>> {
    this.ensureInitialized();

    try {
      const entity = this.entities.get(id);
      if (!entity) {
        return ValidationResult.singleError(
          'id',
          'ENTITY_NOT_FOUND',
          `Entity with ID ${id} not found`
        );
      }

      // Check for dependent entities
      const dependenciesResult = await this.checkEntityDependencies(id);
      if (!dependenciesResult.isValid) {
        return ValidationResult.error(dependenciesResult.issues);
      }

      // Remove from storage
      this.entities.delete(id);
      this.removeFromTypeIndex(entity.type, id);
      
      // Remove relationships
      this.relationships.delete(id);
      for (const [entityId, relationships] of this.relationships) {
        const filtered = relationships.filter(rel => 
          rel.sourceEntityId !== id && rel.targetEntityId !== id
        );
        if (filtered.length !== relationships.length) {
          this.relationships.set(entityId, filtered);
        }
      }

      this.log('info', `Deleted entity: ${entity.type} ${id}`);
      return ValidationResult.success(true);

    } catch (error) {
      return this.handleError(error, 'deleteEntity');
    }
  }

  /**
   * Get entities by type
   */
  async getEntitiesByType<T extends GraphEntity>(entityType: EntityType): Promise<ServiceResult<T[]>> {
    return this.queryEntities<T>({ entityType });
  }

  /**
   * Count entities by type
   */
  async countEntitiesByType(entityType?: EntityType): Promise<ServiceResult<number>> {
    this.ensureInitialized();

    try {
      if (entityType) {
        const typeIds = this.entityTypeIndex.get(entityType);
        return ValidationResult.success(typeIds?.size || 0);
      } else {
        return ValidationResult.success(this.entities.size);
      }
    } catch (error) {
      return this.handleError(error, 'countEntitiesByType');
    }
  }

  /**
   * Create relationship between entities
   */
  async createRelationship(relationship: EntityRelationship): Promise<ServiceResult<EntityRelationship>> {
    this.ensureInitialized();

    try {
      const { sourceEntityId, targetEntityId } = relationship;

      // Validate entities exist
      if (!this.entities.has(sourceEntityId)) {
        return ValidationResult.singleError(
          'sourceEntityId',
          'ENTITY_NOT_FOUND',
          `Source entity ${sourceEntityId} not found`
        );
      }

      if (!this.entities.has(targetEntityId)) {
        return ValidationResult.singleError(
          'targetEntityId',
          'ENTITY_NOT_FOUND',
          `Target entity ${targetEntityId} not found`
        );
      }

      // Store relationship
      const sourceRelationships = this.relationships.get(sourceEntityId) || [];
      sourceRelationships.push(relationship);
      this.relationships.set(sourceEntityId, sourceRelationships);

      return ValidationResult.success(relationship);

    } catch (error) {
      return this.handleError(error, 'createRelationship');
    }
  }

  /**
   * Get relationships for an entity
   */
  async getEntityRelationships(entityId: string): Promise<ServiceResult<EntityRelationship[]>> {
    this.ensureInitialized();

    try {
      const relationships = this.relationships.get(entityId) || [];
      return ValidationResult.success([...relationships]);
    } catch (error) {
      return this.handleError(error, 'getEntityRelationships');
    }
  }

  // Private methods

  private async validateEntityForCreation(entityData: Partial<GraphEntity>): Promise<ValidationResult<void>> {
    const validations = [
      ValidationUtils.validateEntityType(entityData.type as string),
    ];

    return ValidationUtils.combineResults(...validations).map(() => undefined);
  }

  private async validateEntityForUpdate(
    existingEntity: GraphEntity,
    updates: Partial<GraphEntity>
  ): Promise<ValidationResult<void>> {
    // Ensure critical fields cannot be updated
    if ('id' in updates) {
      return ValidationResult.singleError('id', 'IMMUTABLE_FIELD', 'Entity ID cannot be updated');
    }

    if ('type' in updates) {
      return ValidationResult.singleError('type', 'IMMUTABLE_FIELD', 'Entity type cannot be updated');
    }

    if ('createdAt' in updates) {
      return ValidationResult.singleError('createdAt', 'IMMUTABLE_FIELD', 'Creation date cannot be updated');
    }

    return ValidationResult.success(undefined);
  }

  private async setupEntityByType(entity: GraphEntity): Promise<ValidationResult<void>> {
    switch (entity.type) {
      case EntityType.PHYSICAL_COMPONENT:
        return this.validatePhysicalComponent(entity as PhysicalComponent);
      case EntityType.INVENTORY_ITEM:
        return this.validateInventoryItem(entity as InventoryItem);
      case EntityType.IDENTIFIER:
        return this.validateIdentifier(entity as Identifier);
      case EntityType.ACTIVITY:
        return this.validateActivity(entity as Activity);
      default:
        return ValidationResult.success(undefined);
    }
  }

  private validatePhysicalComponent(component: PhysicalComponent): ValidationResult<void> {
    if (!component.filamentInfo) {
      return ValidationResult.singleError(
        'filamentInfo',
        'REQUIRED',
        'Physical component must have filament information'
      );
    }

    // Additional component-specific validation would go here
    return ValidationResult.success(undefined);
  }

  private validateInventoryItem(item: InventoryItem): ValidationResult<void> {
    const validations = [
      ValidationUtils.validateNonNegativeNumber(item.quantity, 'quantity'),
    ];

    return ValidationUtils.combineResults(...validations).map(() => undefined);
  }

  private validateIdentifier(identifier: Identifier): ValidationResult<void> {
    const validations = [
      ValidationUtils.validateRequired(identifier.value, 'value'),
      ValidationUtils.validateRequired(identifier.identifierType, 'identifierType'),
    ];

    return ValidationUtils.combineResults(...validations).map(() => undefined);
  }

  private validateActivity(activity: Activity): ValidationResult<void> {
    const validations = [
      ValidationUtils.validateRequired(activity.description, 'description'),
      ValidationUtils.validateRequired(activity.activityType, 'activityType'),
    ];

    return ValidationUtils.combineResults(...validations).map(() => undefined);
  }

  private async checkEntityDependencies(entityId: string): Promise<ValidationResult<void>> {
    // Check if other entities depend on this one
    const relationships = this.relationships.get(entityId) || [];
    if (relationships.length > 0) {
      return ValidationResult.singleError(
        'dependencies',
        'HAS_DEPENDENCIES',
        `Cannot delete entity ${entityId} - it has ${relationships.length} relationships`
      );
    }

    return ValidationResult.success(undefined);
  }

  private generateEntityId(entityType: EntityType): string {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substr(2, 9);
    return `${entityType.toLowerCase()}_${timestamp}_${random}`;
  }

  private addToTypeIndex(entityType: EntityType, id: string): void {
    if (!this.entityTypeIndex.has(entityType)) {
      this.entityTypeIndex.set(entityType, new Set());
    }
    this.entityTypeIndex.get(entityType)!.add(id);
  }

  private removeFromTypeIndex(entityType: EntityType, id: string): void {
    const typeSet = this.entityTypeIndex.get(entityType);
    if (typeSet) {
      typeSet.delete(id);
      if (typeSet.size === 0) {
        this.entityTypeIndex.delete(entityType);
      }
    }
  }

  protected async doCleanup(): Promise<void> {
    this.entities.clear();
    this.entityTypeIndex.clear();
    this.relationships.clear();
  }
}