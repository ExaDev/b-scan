import {GraphEntity, EntityType} from './FilamentInfo';

export interface Edge {
  id: string;
  sourceEntityId: string;
  targetEntityId: string;
  relationshipType: RelationshipType;
  createdAt: number;
  updatedAt: number;
  weight?: number;
  metadata?: Record<string, any>;
}

export enum RelationshipType {
  // Physical relationships
  CONTAINS = 'CONTAINS',
  PART_OF = 'PART_OF',
  ATTACHED_TO = 'ATTACHED_TO',
  
  // Identification relationships
  IDENTIFIED_BY = 'IDENTIFIED_BY',
  REPRESENTS = 'REPRESENTS',
  
  // Inventory relationships
  INSTANCE_OF = 'INSTANCE_OF',
  LOCATED_AT = 'LOCATED_AT',
  OWNED_BY = 'OWNED_BY',
  
  // Activity relationships
  CREATED_BY = 'CREATED_BY',
  MODIFIED_BY = 'MODIFIED_BY',
  TRIGGERED_BY = 'TRIGGERED_BY',
  
  // Temporal relationships
  PRECEDES = 'PRECEDES',
  FOLLOWS = 'FOLLOWS',
  CONCURRENT_WITH = 'CONCURRENT_WITH',
}

export interface GraphQuery {
  entityType?: EntityType;
  relationshipType?: RelationshipType;
  sourceEntityId?: string;
  targetEntityId?: string;
  createdAfter?: number;
  createdBefore?: number;
  limit?: number;
  offset?: number;
}

export interface GraphMetrics {
  entityCount: number;
  edgeCount: number;
  entityTypeDistribution: Record<EntityType, number>;
  relationshipTypeDistribution: Record<RelationshipType, number>;
  lastUpdateTime: number;
}

export interface EntityWithRelationships extends GraphEntity {
  incomingEdges: Edge[];
  outgoingEdges: Edge[];
  relatedEntities: GraphEntity[];
}

export interface GraphDiff {
  addedEntities: GraphEntity[];
  removedEntities: string[];
  updatedEntities: GraphEntity[];
  addedEdges: Edge[];
  removedEdges: string[];
  updatedEdges: Edge[];
}

export interface CacheOptions {
  ttl?: number; // Time to live in milliseconds
  maxSize?: number; // Maximum number of cached items
  enableStatistics?: boolean;
}

export interface CacheStatistics {
  hits: number;
  misses: number;
  size: number;
  hitRate: number;
}