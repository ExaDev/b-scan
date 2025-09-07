import {GraphEntity} from '../types/FilamentInfo';
import {CacheOptions, CacheStatistics} from '../types/Graph';

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  accessCount: number;
  lastAccessed: number;
}

export class EntityCache {
  private entities: Map<string, CacheEntry<GraphEntity>> = new Map();
  private queries: Map<string, CacheEntry<GraphEntity[]>> = new Map();
  private readonly options: Required<CacheOptions>;
  private statistics: CacheStatistics;

  constructor(options: CacheOptions = {}) {
    this.options = {
      ttl: options.ttl || 5 * 60 * 1000, // 5 minutes default
      maxSize: options.maxSize || 1000, // 1000 items default
      enableStatistics: options.enableStatistics ?? true,
    };

    this.statistics = {
      hits: 0,
      misses: 0,
      size: 0,
      hitRate: 0,
    };
  }

  // Entity caching
  getEntity(entityId: string): GraphEntity | undefined {
    const entry = this.entities.get(entityId);
    
    if (!entry) {
      this.recordMiss();
      return undefined;
    }

    if (this.isExpired(entry)) {
      this.entities.delete(entityId);
      this.recordMiss();
      return undefined;
    }

    this.updateAccess(entry);
    this.recordHit();
    return entry.data;
  }

  setEntity(entity: GraphEntity): void {
    const now = Date.now();
    const entry: CacheEntry<GraphEntity> = {
      data: entity,
      timestamp: now,
      accessCount: 1,
      lastAccessed: now,
    };

    // Evict if at capacity
    if (this.entities.size >= this.options.maxSize && !this.entities.has(entity.id)) {
      this.evictLeastRecentlyUsed();
    }

    this.entities.set(entity.id, entry);
    this.updateSize();
  }

  removeEntity(entityId: string): boolean {
    const removed = this.entities.delete(entityId);
    this.updateSize();
    return removed;
  }

  // Query result caching
  getQueryResult(queryKey: string): GraphEntity[] | undefined {
    const entry = this.queries.get(queryKey);
    
    if (!entry) {
      this.recordMiss();
      return undefined;
    }

    if (this.isExpired(entry)) {
      this.queries.delete(queryKey);
      this.recordMiss();
      return undefined;
    }

    this.updateAccess(entry);
    this.recordHit();
    return entry.data;
  }

  setQueryResult(queryKey: string, results: GraphEntity[]): void {
    const now = Date.now();
    const entry: CacheEntry<GraphEntity[]> = {
      data: results,
      timestamp: now,
      accessCount: 1,
      lastAccessed: now,
    };

    // Evict if at capacity
    if (this.queries.size >= this.options.maxSize && !this.queries.has(queryKey)) {
      this.evictLeastRecentlyUsedQuery();
    }

    this.queries.set(queryKey, entry);
    this.updateSize();
  }

  invalidateQuery(queryKey: string): boolean {
    const removed = this.queries.delete(queryKey);
    this.updateSize();
    return removed;
  }

  // Query key generation
  generateQueryKey(params: any): string {
    return JSON.stringify(params, Object.keys(params).sort());
  }

  // Cache management
  clear(): void {
    this.entities.clear();
    this.queries.clear();
    this.statistics = {
      hits: 0,
      misses: 0,
      size: 0,
      hitRate: 0,
    };
  }

  cleanup(): void {
    const now = Date.now();
    
    // Clean expired entities
    for (const [key, entry] of this.entities.entries()) {
      if (this.isExpired(entry)) {
        this.entities.delete(key);
      }
    }

    // Clean expired queries
    for (const [key, entry] of this.queries.entries()) {
      if (this.isExpired(entry)) {
        this.queries.delete(key);
      }
    }

    this.updateSize();
  }

  getStatistics(): CacheStatistics {
    return {...this.statistics};
  }

  // Internal methods
  private isExpired(entry: CacheEntry<any>): boolean {
    return Date.now() - entry.timestamp > this.options.ttl;
  }

  private updateAccess(entry: CacheEntry<any>): void {
    entry.accessCount++;
    entry.lastAccessed = Date.now();
  }

  private evictLeastRecentlyUsed(): void {
    let lruKey: string | undefined;
    let oldestAccess = Date.now();

    for (const [key, entry] of this.entities.entries()) {
      if (entry.lastAccessed < oldestAccess) {
        oldestAccess = entry.lastAccessed;
        lruKey = key;
      }
    }

    if (lruKey) {
      this.entities.delete(lruKey);
    }
  }

  private evictLeastRecentlyUsedQuery(): void {
    let lruKey: string | undefined;
    let oldestAccess = Date.now();

    for (const [key, entry] of this.queries.entries()) {
      if (entry.lastAccessed < oldestAccess) {
        oldestAccess = entry.lastAccessed;
        lruKey = key;
      }
    }

    if (lruKey) {
      this.queries.delete(lruKey);
    }
  }

  private recordHit(): void {
    if (this.options.enableStatistics) {
      this.statistics.hits++;
      this.updateHitRate();
    }
  }

  private recordMiss(): void {
    if (this.options.enableStatistics) {
      this.statistics.misses++;
      this.updateHitRate();
    }
  }

  private updateHitRate(): void {
    const total = this.statistics.hits + this.statistics.misses;
    this.statistics.hitRate = total > 0 ? this.statistics.hits / total : 0;
  }

  private updateSize(): void {
    this.statistics.size = this.entities.size + this.queries.size;
  }

  // Batch operations
  setEntities(entities: GraphEntity[]): void {
    entities.forEach(entity => this.setEntity(entity));
  }

  getEntities(entityIds: string[]): Map<string, GraphEntity> {
    const results = new Map<string, GraphEntity>();
    
    entityIds.forEach(id => {
      const entity = this.getEntity(id);
      if (entity) {
        results.set(id, entity);
      }
    });

    return results;
  }

  // Cache warming
  warmCache(entities: GraphEntity[]): void {
    entities.forEach(entity => this.setEntity(entity));
  }

  // Invalidation patterns
  invalidateByType(entityType: string): number {
    let invalidatedCount = 0;

    // Invalidate entity cache entries of specific type
    for (const [key, entry] of this.entities.entries()) {
      if (entry.data.type === entityType) {
        this.entities.delete(key);
        invalidatedCount++;
      }
    }

    // Invalidate query cache (queries may be affected by type changes)
    const queryCount = this.queries.size;
    this.queries.clear();
    invalidatedCount += queryCount;

    this.updateSize();
    return invalidatedCount;
  }

  invalidateRelated(entityId: string): number {
    let invalidatedCount = 0;

    // For simplicity, invalidate all query cache when entity relationships might be affected
    const queryCount = this.queries.size;
    this.queries.clear();
    invalidatedCount += queryCount;

    this.updateSize();
    return invalidatedCount;
  }

  // Memory pressure handling
  handleMemoryPressure(): void {
    const targetSize = Math.floor(this.options.maxSize * 0.7); // Remove 30% of entries
    
    // Sort entities by access patterns (LRU)
    const entityEntries = Array.from(this.entities.entries())
      .sort(([, a], [, b]) => a.lastAccessed - b.lastAccessed);
    
    const queryEntries = Array.from(this.queries.entries())
      .sort(([, a], [, b]) => a.lastAccessed - b.lastAccessed);
    
    // Remove least recently used entries
    const totalEntries = entityEntries.length + queryEntries.length;
    const entriesToRemove = Math.max(0, totalEntries - targetSize);
    
    let removed = 0;
    let entityIndex = 0;
    let queryIndex = 0;
    
    while (removed < entriesToRemove && (entityIndex < entityEntries.length || queryIndex < queryEntries.length)) {
      // Interleave removal between entities and queries based on access time
      const nextEntity = entityEntries[entityIndex];
      const nextQuery = queryEntries[queryIndex];
      
      if (!nextEntity || (nextQuery && nextQuery[1].lastAccessed < nextEntity[1].lastAccessed)) {
        this.queries.delete(nextQuery[0]);
        queryIndex++;
      } else {
        this.entities.delete(nextEntity[0]);
        entityIndex++;
      }
      
      removed++;
    }
    
    this.updateSize();
  }
}