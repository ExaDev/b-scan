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
} from '../types/Graph';

export class Graph {
  private entities: Map<string, GraphEntity> = new Map();
  private edges: Map<string, Edge> = new Map();
  private outgoingEdges: Map<string, Set<string>> = new Map(); // entityId -> edgeIds
  private incomingEdges: Map<string, Set<string>> = new Map(); // entityId -> edgeIds
  private typeIndex: Map<EntityType, Set<string>> = new Map(); // entityType -> entityIds
  private relationshipIndex: Map<RelationshipType, Set<string>> = new Map(); // relationshipType -> edgeIds
  
  constructor() {
    // Initialize type indexes
    Object.values(EntityType).forEach(type => {
      this.typeIndex.set(type, new Set());
    });
    
    // Initialize relationship indexes
    Object.values(RelationshipType).forEach(type => {
      this.relationshipIndex.set(type, new Set());
    });
  }

  // Entity Management
  addEntity(entity: GraphEntity): void {
    this.entities.set(entity.id, entity);
    this.typeIndex.get(entity.type)?.add(entity.id);
    
    // Initialize edge sets for new entity
    if (!this.outgoingEdges.has(entity.id)) {
      this.outgoingEdges.set(entity.id, new Set());
    }
    if (!this.incomingEdges.has(entity.id)) {
      this.incomingEdges.set(entity.id, new Set());
    }
  }

  getEntity(entityId: string): GraphEntity | undefined {
    return this.entities.get(entityId);
  }

  updateEntity(entity: GraphEntity): boolean {
    if (!this.entities.has(entity.id)) {
      return false;
    }
    
    const oldEntity = this.entities.get(entity.id)!;
    
    // Update type index if type changed
    if (oldEntity.type !== entity.type) {
      this.typeIndex.get(oldEntity.type)?.delete(entity.id);
      this.typeIndex.get(entity.type)?.add(entity.id);
    }
    
    this.entities.set(entity.id, entity);
    return true;
  }

  deleteEntity(entityId: string): boolean {
    const entity = this.entities.get(entityId);
    if (!entity) {
      return false;
    }

    // Remove from type index
    this.typeIndex.get(entity.type)?.delete(entityId);

    // Remove all connected edges
    const connectedEdges = new Set([
      ...Array.from(this.outgoingEdges.get(entityId) || []),
      ...Array.from(this.incomingEdges.get(entityId) || []),
    ]);

    connectedEdges.forEach(edgeId => {
      this.removeEdge(edgeId);
    });

    // Remove entity
    this.entities.delete(entityId);
    this.outgoingEdges.delete(entityId);
    this.incomingEdges.delete(entityId);

    return true;
  }

  findEntitiesByType(entityType: EntityType): GraphEntity[] {
    const entityIds = this.typeIndex.get(entityType) || new Set();
    return Array.from(entityIds)
      .map(id => this.entities.get(id))
      .filter((entity): entity is GraphEntity => entity !== undefined);
  }

  // Edge Management
  addEdge(edge: Edge): boolean {
    // Validate source and target entities exist
    if (!this.entities.has(edge.sourceEntityId) || !this.entities.has(edge.targetEntityId)) {
      return false;
    }

    this.edges.set(edge.id, edge);
    this.relationshipIndex.get(edge.relationshipType)?.add(edge.id);

    // Update adjacency lists
    this.outgoingEdges.get(edge.sourceEntityId)?.add(edge.id);
    this.incomingEdges.get(edge.targetEntityId)?.add(edge.id);

    return true;
  }

  getEdge(edgeId: string): Edge | undefined {
    return this.edges.get(edgeId);
  }

  updateEdge(edge: Edge): boolean {
    if (!this.edges.has(edge.id)) {
      return false;
    }

    const oldEdge = this.edges.get(edge.id)!;
    
    // Update relationship index if type changed
    if (oldEdge.relationshipType !== edge.relationshipType) {
      this.relationshipIndex.get(oldEdge.relationshipType)?.delete(edge.id);
      this.relationshipIndex.get(edge.relationshipType)?.add(edge.id);
    }

    this.edges.set(edge.id, edge);
    return true;
  }

  removeEdge(edgeId: string): boolean {
    const edge = this.edges.get(edgeId);
    if (!edge) {
      return false;
    }

    // Remove from relationship index
    this.relationshipIndex.get(edge.relationshipType)?.delete(edgeId);

    // Remove from adjacency lists
    this.outgoingEdges.get(edge.sourceEntityId)?.delete(edgeId);
    this.incomingEdges.get(edge.targetEntityId)?.delete(edgeId);

    // Remove edge
    this.edges.delete(edgeId);

    return true;
  }

  // Relationship Queries
  findRelatedEntities(
    entityId: string,
    relationshipType?: RelationshipType,
    direction: 'incoming' | 'outgoing' | 'both' = 'both'
  ): GraphEntity[] {
    const relatedEntityIds = new Set<string>();

    const addRelatedFromEdges = (edgeIds: Set<string>, getTargetId: (edge: Edge) => string) => {
      edgeIds.forEach(edgeId => {
        const edge = this.edges.get(edgeId);
        if (edge && (!relationshipType || edge.relationshipType === relationshipType)) {
          relatedEntityIds.add(getTargetId(edge));
        }
      });
    };

    if (direction === 'outgoing' || direction === 'both') {
      const outgoing = this.outgoingEdges.get(entityId) || new Set();
      addRelatedFromEdges(outgoing, edge => edge.targetEntityId);
    }

    if (direction === 'incoming' || direction === 'both') {
      const incoming = this.incomingEdges.get(entityId) || new Set();
      addRelatedFromEdges(incoming, edge => edge.sourceEntityId);
    }

    return Array.from(relatedEntityIds)
      .map(id => this.entities.get(id))
      .filter((entity): entity is GraphEntity => entity !== undefined);
  }

  getEntityWithRelationships(entityId: string): EntityWithRelationships | undefined {
    const entity = this.entities.get(entityId);
    if (!entity) {
      return undefined;
    }

    const incomingEdgeIds = this.incomingEdges.get(entityId) || new Set();
    const outgoingEdgeIds = this.outgoingEdges.get(entityId) || new Set();

    const incomingEdges = Array.from(incomingEdgeIds)
      .map(id => this.edges.get(id))
      .filter((edge): edge is Edge => edge !== undefined);

    const outgoingEdges = Array.from(outgoingEdgeIds)
      .map(id => this.edges.get(id))
      .filter((edge): edge is Edge => edge !== undefined);

    const relatedEntities = this.findRelatedEntities(entityId);

    return {
      ...entity,
      incomingEdges,
      outgoingEdges,
      relatedEntities,
    };
  }

  // Inventory-specific queries
  findInventoryRootEntities(): GraphEntity[] {
    const roots: GraphEntity[] = [];

    this.entities.forEach(entity => {
      const hasIncomingPartOfRelation = Array.from(this.incomingEdges.get(entity.id) || [])
        .some(edgeId => {
          const edge = this.edges.get(edgeId);
          return edge?.relationshipType === RelationshipType.PART_OF;
        });

      if (!hasIncomingPartOfRelation) {
        roots.push(entity);
      }
    });

    return roots;
  }

  // Graph traversal
  traverseBreadthFirst(startEntityId: string, maxDepth: number = 10): GraphEntity[] {
    const visited = new Set<string>();
    const queue: Array<{entityId: string; depth: number}> = [{entityId: startEntityId, depth: 0}];
    const result: GraphEntity[] = [];

    while (queue.length > 0) {
      const {entityId, depth} = queue.shift()!;

      if (visited.has(entityId) || depth > maxDepth) {
        continue;
      }

      visited.add(entityId);
      const entity = this.entities.get(entityId);
      if (entity) {
        result.push(entity);
      }

      // Add connected entities to queue
      const relatedEntities = this.findRelatedEntities(entityId);
      relatedEntities.forEach(related => {
        if (!visited.has(related.id)) {
          queue.push({entityId: related.id, depth: depth + 1});
        }
      });
    }

    return result;
  }

  // Graph statistics
  getMetrics(): GraphMetrics {
    const entityTypeDistribution: Record<EntityType, number> = Object.create(null) as Record<EntityType, number>;
    const relationshipTypeDistribution: Record<RelationshipType, number> = Object.create(null) as Record<RelationshipType, number>;

    // Initialize distributions
    Object.values(EntityType).forEach(type => {
      entityTypeDistribution[type] = this.typeIndex.get(type)?.size || 0;
    });

    Object.values(RelationshipType).forEach(type => {
      relationshipTypeDistribution[type] = this.relationshipIndex.get(type)?.size || 0;
    });

    return {
      entityCount: this.entities.size,
      edgeCount: this.edges.size,
      entityTypeDistribution,
      relationshipTypeDistribution,
      lastUpdateTime: Date.now(),
    };
  }

  // Serialization
  serialize(): {entities: GraphEntity[]; edges: Edge[]} {
    return {
      entities: Array.from(this.entities.values()),
      edges: Array.from(this.edges.values()),
    };
  }

  deserialize(data: {entities: GraphEntity[]; edges: Edge[]}): void {
    this.clear();
    
    // Add entities first
    data.entities.forEach(entity => {
      this.addEntity(entity);
    });
    
    // Then add edges
    data.edges.forEach(edge => {
      this.addEdge(edge);
    });
  }

  clear(): void {
    this.entities.clear();
    this.edges.clear();
    this.outgoingEdges.clear();
    this.incomingEdges.clear();
    
    // Clear type indexes
    this.typeIndex.forEach(set => set.clear());
    this.relationshipIndex.forEach(set => set.clear());
  }

  // Query interface
  query(queryParams: GraphQuery): GraphEntity[] {
    let results = Array.from(this.entities.values());

    if (queryParams.entityType) {
      results = results.filter(entity => entity.type === queryParams.entityType);
    }

    if (queryParams.createdAfter) {
      results = results.filter(entity => entity.createdAt >= queryParams.createdAfter!);
    }

    if (queryParams.createdBefore) {
      results = results.filter(entity => entity.createdAt <= queryParams.createdBefore!);
    }

    if (queryParams.sourceEntityId || queryParams.targetEntityId || queryParams.relationshipType) {
      // Filter based on relationships
      const validEntityIds = new Set<string>();
      
      this.edges.forEach(edge => {
        let matches = true;
        
        if (queryParams.relationshipType && edge.relationshipType !== queryParams.relationshipType) {
          matches = false;
        }
        
        if (queryParams.sourceEntityId && edge.sourceEntityId !== queryParams.sourceEntityId) {
          matches = false;
        }
        
        if (queryParams.targetEntityId && edge.targetEntityId !== queryParams.targetEntityId) {
          matches = false;
        }
        
        if (matches) {
          validEntityIds.add(edge.sourceEntityId);
          validEntityIds.add(edge.targetEntityId);
        }
      });
      
      results = results.filter(entity => validEntityIds.has(entity.id));
    }

    // Apply pagination
    const offset = queryParams.offset || 0;
    const limit = queryParams.limit || results.length;
    
    return results.slice(offset, offset + limit);
  }
}