import AsyncStorage from '@react-native-async-storage/async-storage';
import {Graph} from './Graph';
import {EntityCache} from './EntityCache';

import {
  GraphEntity,
  EntityType,
} from '../types/FilamentInfo';

import {
  Edge,
  RelationshipType,
  GraphQuery,
  GraphMetrics,
  EntityWithRelationships,
  GraphDiff,
  CacheOptions,
} from '../types/Graph';

interface RepositoryOptions {
  cacheOptions?: CacheOptions;
  persistenceKey?: string;
  autoSave?: boolean;
  autoSaveInterval?: number;
  enableBackgroundSync?: boolean;
}

interface PersistedGraphData {
  entities: GraphEntity[];
  edges: Edge[];
  version: number;
  lastSaved: number;
}

export class GraphRepository {
  private graph: Graph;
  private cache: EntityCache;
  private readonly options: Required<RepositoryOptions>;
  private autoSaveTimer?: NodeJS.Timeout;
  private isDirty = false;
  private isLoading = false;
  private isSaving = false;
  private backgroundQueue: Array<() => Promise<void>> = [];
  private isProcessingQueue = false;

  private static readonly STORAGE_KEY_PREFIX = '@bscan_graph_';
  private static readonly CURRENT_VERSION = 1;

  constructor(options: RepositoryOptions = {}) {
    this.options = {
      cacheOptions: options.cacheOptions || {},
      persistenceKey: options.persistenceKey || 'default',
      autoSave: options.autoSave ?? true,
      autoSaveInterval: options.autoSaveInterval || 30000, // 30 seconds
      enableBackgroundSync: options.enableBackgroundSync ?? true,
    };

    this.graph = new Graph();
    this.cache = new EntityCache(this.options.cacheOptions);

    if (this.options.autoSave) {
      this.startAutoSave();
    }

    if (this.options.enableBackgroundSync) {
      this.startBackgroundProcessing();
    }
  }

  // Initialization and persistence
  async initialize(): Promise<void> {
    if (this.isLoading) {
      return;
    }

    this.isLoading = true;
    try {
      await this.loadFromStorage();
    } finally {
      this.isLoading = false;
    }
  }

  async loadFromStorage(): Promise<boolean> {
    try {
      const key = this.getStorageKey();
      const serializedData = await AsyncStorage.getItem(key);
      
      if (!serializedData) {
        return false;
      }

      const data: PersistedGraphData = JSON.parse(serializedData);
      
      // Version compatibility check
      if (data.version !== GraphRepository.CURRENT_VERSION) {
        console.warn(`Graph data version mismatch. Expected: ${GraphRepository.CURRENT_VERSION}, Found: ${data.version}`);
        return false;
      }

      this.graph.deserialize({entities: data.entities, edges: data.edges});
      
      // Warm the cache with loaded entities
      this.cache.warmCache(data.entities);
      
      this.isDirty = false;
      return true;
    } catch (error) {
      console.error('Failed to load graph from storage:', error);
      return false;
    }
  }

  async saveToStorage(): Promise<boolean> {
    if (this.isSaving) {
      return false;
    }

    this.isSaving = true;
    try {
      const serialized = this.graph.serialize();
      const data: PersistedGraphData = {
        entities: serialized.entities,
        edges: serialized.edges,
        version: GraphRepository.CURRENT_VERSION,
        lastSaved: Date.now(),
      };

      const key = this.getStorageKey();
      await AsyncStorage.setItem(key, JSON.stringify(data));
      
      this.isDirty = false;
      return true;
    } catch (error) {
      console.error('Failed to save graph to storage:', error);
      return false;
    } finally {
      this.isSaving = false;
    }
  }

  // Entity management
  async addEntity(entity: GraphEntity): Promise<void> {
    const updatedEntity = {
      ...entity,
      createdAt: entity.createdAt || Date.now(),
      updatedAt: Date.now(),
    };

    this.graph.addEntity(updatedEntity);
    this.cache.setEntity(updatedEntity);
    this.markDirty();

    if (this.options.enableBackgroundSync) {
      this.enqueueBackgroundTask(() => this.saveToStorage().then(() => {}));
    }
  }

  async getEntity(entityId: string): Promise<GraphEntity | undefined> {
    // Try cache first
    let entity = this.cache.getEntity(entityId);
    
    if (!entity) {
      // Try in-memory graph
      entity = this.graph.getEntity(entityId);
      
      if (entity) {
        // Update cache
        this.cache.setEntity(entity);
      }
    }

    return entity;
  }

  async updateEntity(entity: GraphEntity): Promise<boolean> {
    const updatedEntity = {
      ...entity,
      updatedAt: Date.now(),
    };

    const success = this.graph.updateEntity(updatedEntity);
    
    if (success) {
      this.cache.setEntity(updatedEntity);
      this.cache.invalidateRelated(entity.id);
      this.markDirty();

      if (this.options.enableBackgroundSync) {
        this.enqueueBackgroundTask(() => this.saveToStorage().then(() => {}));
      }
    }

    return success;
  }

  async deleteEntity(entityId: string): Promise<boolean> {
    const success = this.graph.deleteEntity(entityId);
    
    if (success) {
      this.cache.removeEntity(entityId);
      this.cache.invalidateRelated(entityId);
      this.markDirty();

      if (this.options.enableBackgroundSync) {
        this.enqueueBackgroundTask(() => this.saveToStorage().then(() => {}));
      }
    }

    return success;
  }

  async findEntitiesByType(entityType: EntityType): Promise<GraphEntity[]> {
    const queryKey = this.cache.generateQueryKey({type: 'findEntitiesByType', entityType});
    let results = this.cache.getQueryResult(queryKey);

    if (!results) {
      results = this.graph.findEntitiesByType(entityType);
      this.cache.setQueryResult(queryKey, results);
    }

    return results;
  }

  // Relationship management
  async addRelationship(edge: Edge): Promise<boolean> {
    const updatedEdge = {
      ...edge,
      createdAt: edge.createdAt || Date.now(),
      updatedAt: Date.now(),
    };

    const success = this.graph.addEdge(updatedEdge);
    
    if (success) {
      // Invalidate related cache entries
      this.cache.invalidateRelated(edge.sourceEntityId);
      this.cache.invalidateRelated(edge.targetEntityId);
      this.markDirty();

      if (this.options.enableBackgroundSync) {
        this.enqueueBackgroundTask(() => this.saveToStorage().then(() => {}));
      }
    }

    return success;
  }

  async findRelatedEntities(
    entityId: string,
    relationshipType?: RelationshipType,
    direction: 'incoming' | 'outgoing' | 'both' = 'both'
  ): Promise<GraphEntity[]> {
    const queryKey = this.cache.generateQueryKey({
      type: 'findRelatedEntities',
      entityId,
      relationshipType,
      direction,
    });

    let results = this.cache.getQueryResult(queryKey);

    if (!results) {
      results = this.graph.findRelatedEntities(entityId, relationshipType, direction);
      this.cache.setQueryResult(queryKey, results);
    }

    return results;
  }

  async findInventoryRootEntities(): Promise<GraphEntity[]> {
    const queryKey = this.cache.generateQueryKey({type: 'findInventoryRootEntities'});
    let results = this.cache.getQueryResult(queryKey);

    if (!results) {
      results = this.graph.findInventoryRootEntities();
      this.cache.setQueryResult(queryKey, results);
    }

    return results;
  }

  async getEntityWithRelationships(entityId: string): Promise<EntityWithRelationships | undefined> {
    return this.graph.getEntityWithRelationships(entityId);
  }

  // Advanced queries
  async query(queryParams: GraphQuery): Promise<GraphEntity[]> {
    const queryKey = this.cache.generateQueryKey({type: 'query', ...queryParams});
    let results = this.cache.getQueryResult(queryKey);

    if (!results) {
      results = this.graph.query(queryParams);
      this.cache.setQueryResult(queryKey, results);
    }

    return results;
  }

  async traverseBreadthFirst(startEntityId: string, maxDepth: number = 10): Promise<GraphEntity[]> {
    const queryKey = this.cache.generateQueryKey({
      type: 'traverseBreadthFirst',
      startEntityId,
      maxDepth,
    });

    let results = this.cache.getQueryResult(queryKey);

    if (!results) {
      results = this.graph.traverseBreadthFirst(startEntityId, maxDepth);
      this.cache.setQueryResult(queryKey, results);
    }

    return results;
  }

  // Batch operations
  async addEntities(entities: GraphEntity[]): Promise<void> {
    const now = Date.now();
    const updatedEntities = entities.map(entity => ({
      ...entity,
      createdAt: entity.createdAt || now,
      updatedAt: now,
    }));

    updatedEntities.forEach(entity => {
      this.graph.addEntity(entity);
    });

    this.cache.setEntities(updatedEntities);
    this.markDirty();

    if (this.options.enableBackgroundSync) {
      this.enqueueBackgroundTask(() => this.saveToStorage().then(() => {}));
    }
  }

  async addRelationships(edges: Edge[]): Promise<boolean[]> {
    const now = Date.now();
    const results: boolean[] = [];
    const invalidateEntityIds = new Set<string>();

    for (const edge of edges) {
      const updatedEdge = {
        ...edge,
        createdAt: edge.createdAt || now,
        updatedAt: now,
      };

      const success = this.graph.addEdge(updatedEdge);
      results.push(success);

      if (success) {
        invalidateEntityIds.add(edge.sourceEntityId);
        invalidateEntityIds.add(edge.targetEntityId);
      }
    }

    // Invalidate cache for affected entities
    invalidateEntityIds.forEach(entityId => {
      this.cache.invalidateRelated(entityId);
    });

    this.markDirty();

    if (this.options.enableBackgroundSync) {
      this.enqueueBackgroundTask(() => this.saveToStorage().then(() => {}));
    }

    return results;
  }

  // Statistics and metrics
  getMetrics(): GraphMetrics {
    return this.graph.getMetrics();
  }

  getCacheStatistics() {
    return this.cache.getStatistics();
  }

  // Cache management
  clearCache(): void {
    this.cache.clear();
  }

  cleanupCache(): void {
    this.cache.cleanup();
  }

  // Background processing
  private startBackgroundProcessing(): void {
    if (this.isProcessingQueue) {
      return;
    }

    this.isProcessingQueue = true;
    this.processBackgroundQueue();
  }

  private async processBackgroundQueue(): Promise<void> {
    while (this.backgroundQueue.length > 0 && this.isProcessingQueue) {
      const task = this.backgroundQueue.shift();
      if (task) {
        try {
          await task();
        } catch (error) {
          console.error('Background task failed:', error);
        }
      }
      
      // Small delay to prevent blocking
      await new Promise(resolve => setTimeout(resolve, 10));
    }

    // Continue processing after a short delay
    setTimeout(() => {
      if (this.isProcessingQueue && this.backgroundQueue.length > 0) {
        this.processBackgroundQueue();
      }
    }, 1000);
  }

  private enqueueBackgroundTask(task: () => Promise<void>): void {
    this.backgroundQueue.push(task);
    
    // Start processing if not already running
    if (!this.isProcessingQueue) {
      this.startBackgroundProcessing();
    }
  }

  // Auto-save management
  private startAutoSave(): void {
    if (this.autoSaveTimer) {
      clearInterval(this.autoSaveTimer);
    }

    this.autoSaveTimer = setInterval(() => {
      if (this.isDirty && !this.isSaving) {
        this.saveToStorage();
      }
    }, this.options.autoSaveInterval);
  }

  private stopAutoSave(): void {
    if (this.autoSaveTimer) {
      clearInterval(this.autoSaveTimer);
      this.autoSaveTimer = undefined;
    }
  }

  private markDirty(): void {
    this.isDirty = true;
  }

  private getStorageKey(): string {
    return `${GraphRepository.STORAGE_KEY_PREFIX}${this.options.persistenceKey}`;
  }

  // Cleanup and disposal
  async dispose(): Promise<void> {
    this.isProcessingQueue = false;
    this.stopAutoSave();

    // Process remaining background tasks
    const remainingTasks = [...this.backgroundQueue];
    this.backgroundQueue = [];

    for (const task of remainingTasks) {
      try {
        await task();
      } catch (error) {
        console.error('Failed to complete background task during disposal:', error);
      }
    }

    // Final save if dirty
    if (this.isDirty) {
      await this.saveToStorage();
    }

    this.cache.clear();
  }

  // Data import/export
  async exportData(): Promise<PersistedGraphData> {
    const serialized = this.graph.serialize();
    return {
      entities: serialized.entities,
      edges: serialized.edges,
      version: GraphRepository.CURRENT_VERSION,
      lastSaved: Date.now(),
    };
  }

  async importData(data: PersistedGraphData): Promise<boolean> {
    try {
      // Validate version
      if (data.version !== GraphRepository.CURRENT_VERSION) {
        throw new Error(`Incompatible data version: ${data.version}`);
      }

      this.graph.deserialize({entities: data.entities, edges: data.edges});
      this.cache.clear();
      this.cache.warmCache(data.entities);
      this.markDirty();

      return true;
    } catch (error) {
      console.error('Failed to import data:', error);
      return false;
    }
  }

  // Memory management
  async handleMemoryWarning(): Promise<void> {
    this.cache.handleMemoryPressure();
    
    // Force save if dirty
    if (this.isDirty) {
      await this.saveToStorage();
    }
  }
}