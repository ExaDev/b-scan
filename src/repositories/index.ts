// Core graph components
export {Graph} from './Graph';
export {GraphRepository} from './GraphRepository';
export {EntityCache} from './EntityCache';
export {EntityFactory} from './EntityFactory';
export {BackgroundSyncService} from './BackgroundSyncService';

// Type exports
export * from '../types/Graph';

// Re-export entity types for convenience
export type {
  GraphEntity,
  PhysicalComponent,
  InventoryItem,
  Identifier,
  Activity,
  FilamentInfo,
} from '../types/FilamentInfo';

// Re-export enums as values
export {
  EntityType,
  TagFormat,
} from '../types/FilamentInfo';

// Repository factory function
export const createGraphRepository = (options?: {
  persistenceKey?: string;
  autoSave?: boolean;
  autoSaveInterval?: number;
  enableBackgroundSync?: boolean;
  cacheOptions?: {
    ttl?: number;
    maxSize?: number;
    enableStatistics?: boolean;
  };
}) => {
  return new GraphRepository({
    persistenceKey: options?.persistenceKey || 'default',
    autoSave: options?.autoSave ?? true,
    autoSaveInterval: options?.autoSaveInterval || 30000,
    enableBackgroundSync: options?.enableBackgroundSync ?? true,
    cacheOptions: options?.cacheOptions || {
      ttl: 5 * 60 * 1000, // 5 minutes
      maxSize: 1000,
      enableStatistics: true,
    },
  });
};

// Background service factory function
export const createBackgroundSyncService = (repository: GraphRepository) => {
  return new BackgroundSyncService(repository);
};

// Utility functions
export const createFilamentSpoolEntities = async (
  repository: GraphRepository,
  filamentInfo: FilamentInfo,
  options: {
    rfidValue?: string;
    location?: string;
    initialWeight?: number;
    notes?: string;
  } = {}
) => {
  const system = EntityFactory.createFilamentSpoolSystem(filamentInfo, options);
  
  // Add entities to repository
  await repository.addEntity(system.physicalComponent);
  await repository.addEntity(system.inventoryItem);
  await repository.addEntity(system.scanActivity);
  
  if (system.identifier) {
    await repository.addEntity(system.identifier);
  }
  
  // Add relationships
  await repository.addRelationships(system.edges);
  
  return system;
};

// Query helper functions
export const findSpoolByRfid = async (
  repository: GraphRepository,
  rfidValue: string
): Promise<PhysicalComponent | undefined> => {
  const identifiers = await repository.query({
    entityType: EntityType.IDENTIFIER,
  });
  
  const rfidIdentifier = identifiers.find(
    (identifier) =>
      EntityFactory.isIdentifier(identifier) &&
      identifier.identifierType === 'RFID' &&
      identifier.value === rfidValue
  );
  
  if (!rfidIdentifier) {
    return undefined;
  }
  
  const relatedEntities = await repository.findRelatedEntities(
    rfidIdentifier.id,
    RelationshipType.IDENTIFIED_BY,
    'incoming'
  );
  
  const physicalComponent = relatedEntities.find(entity =>
    EntityFactory.isPhysicalComponent(entity)
  );
  
  return physicalComponent as PhysicalComponent | undefined;
};

export const findInventoryRootSpools = async (
  repository: GraphRepository
): Promise<PhysicalComponent[]> => {
  const rootEntities = await repository.findInventoryRootEntities();
  
  return rootEntities.filter(entity =>
    EntityFactory.isPhysicalComponent(entity)
  ) as PhysicalComponent[];
};

export const getSpoolHistory = async (
  repository: GraphRepository,
  spoolId: string
): Promise<Activity[]> => {
  const activities = await repository.findRelatedEntities(
    spoolId,
    undefined, // Any relationship type
    'incoming'
  );
  
  return activities
    .filter(entity => EntityFactory.isActivity(entity))
    .sort((a, b) => b.createdAt - a.createdAt) as Activity[];
};

// Repository management helpers
export const initializeRepository = async (
  options?: Parameters<typeof createGraphRepository>[0]
): Promise<{
  repository: GraphRepository;
  backgroundService: BackgroundSyncService;
}> => {
  const repository = createGraphRepository(options);
  await repository.initialize();
  
  const backgroundService = createBackgroundSyncService(repository);
  backgroundService.start();
  
  return {
    repository,
    backgroundService,
  };
};

export const disposeRepository = async (
  repository: GraphRepository,
  backgroundService?: BackgroundSyncService
) => {
  if (backgroundService) {
    backgroundService.dispose();
  }
  
  await repository.dispose();
};