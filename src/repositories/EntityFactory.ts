import uuid from 'react-native-uuid';

import {
  GraphEntity,
  EntityType,
  PhysicalComponent,
  InventoryItem,
  Identifier,
  Activity,
  FilamentInfo,
} from '../types/FilamentInfo';

import {
  Edge,
  RelationshipType,
} from '../types/Graph';

export class EntityFactory {
  // Entity creation methods
  static createPhysicalComponent(
    filamentInfo: FilamentInfo,
    options: {
      currentWeight?: number;
      notes?: string;
    } = {}
  ): PhysicalComponent {
    const now = Date.now();
    
    const physicalComponent: PhysicalComponent = {
      id: uuid.v4() as string,
      type: EntityType.PHYSICAL_COMPONENT,
      createdAt: now,
      updatedAt: now,
      filamentInfo,
    };
    
    // Conditionally assign optional properties
    if (options.currentWeight !== undefined) {
      physicalComponent.currentWeight = options.currentWeight;
    }
    if (options.notes !== undefined) {
      physicalComponent.notes = options.notes;
    }
    
    return physicalComponent;
  }

  static createInventoryItem(options: {
    quantity: number;
    location?: string;
  }): InventoryItem {
    const now = Date.now();
    
    const inventoryItem: InventoryItem = {
      id: uuid.v4() as string,
      type: EntityType.INVENTORY_ITEM,
      createdAt: now,
      updatedAt: now,
      quantity: options.quantity,
      lastUpdated: now,
    };
    
    // Conditionally assign optional properties
    if (options.location !== undefined) {
      inventoryItem.location = options.location;
    }
    
    return inventoryItem;
  }

  static createIdentifier(options: {
    value: string;
    identifierType: 'RFID' | 'BARCODE' | 'QR_CODE';
  }): Identifier {
    const now = Date.now();
    
    return {
      id: uuid.v4() as string,
      type: EntityType.IDENTIFIER,
      createdAt: now,
      updatedAt: now,
      value: options.value,
      identifierType: options.identifierType,
    };
  }

  static createActivity(options: {
    activityType: 'SCAN' | 'UPDATE' | 'CREATE' | 'DELETE';
    description: string;
    relatedEntityId?: string;
  }): Activity {
    const now = Date.now();
    
    const activity: Activity = {
      id: uuid.v4() as string,
      type: EntityType.ACTIVITY,
      createdAt: now,
      updatedAt: now,
      activityType: options.activityType,
      description: options.description,
    };
    
    // Conditionally assign optional properties
    if (options.relatedEntityId !== undefined) {
      activity.relatedEntityId = options.relatedEntityId;
    }
    
    return activity;
  }

  // Edge creation methods
  static createEdge(options: {
    sourceEntityId: string;
    targetEntityId: string;
    relationshipType: RelationshipType;
    weight?: number;
    metadata?: Record<string, unknown>;
  }): Edge {
    const now = Date.now();
    
    const edge: Edge = {
      id: uuid.v4() as string,
      sourceEntityId: options.sourceEntityId,
      targetEntityId: options.targetEntityId,
      relationshipType: options.relationshipType,
      createdAt: now,
      updatedAt: now,
    };
    
    // Conditionally assign optional properties
    if (options.weight !== undefined) {
      edge.weight = options.weight;
    }
    if (options.metadata !== undefined) {
      edge.metadata = options.metadata;
    }
    
    return edge;
  }

  // Relationship convenience methods
  static createIdentificationRelationship(
    physicalComponentId: string,
    identifierId: string,
    metadata?: Record<string, unknown>
  ): Edge {
    const edgeConfig: Parameters<typeof this.createEdge>[0] = {
      sourceEntityId: physicalComponentId,
      targetEntityId: identifierId,
      relationshipType: RelationshipType.IDENTIFIED_BY,
    };
    
    if (metadata !== undefined) {
      edgeConfig.metadata = metadata;
    }
    
    return this.createEdge(edgeConfig);
  }

  static createInventoryRelationship(
    inventoryItemId: string,
    physicalComponentId: string,
    metadata?: Record<string, unknown>
  ): Edge {
    const edgeConfig: Parameters<typeof this.createEdge>[0] = {
      sourceEntityId: inventoryItemId,
      targetEntityId: physicalComponentId,
      relationshipType: RelationshipType.CONTAINS,
    };
    
    if (metadata !== undefined) {
      edgeConfig.metadata = metadata;
    }
    
    return this.createEdge(edgeConfig);
  }

  static createActivityRelationship(
    activityId: string,
    targetEntityId: string,
    relationshipType: RelationshipType = RelationshipType.TRIGGERED_BY,
    metadata?: Record<string, unknown>
  ): Edge {
    const edgeConfig: Parameters<typeof this.createEdge>[0] = {
      sourceEntityId: activityId,
      targetEntityId: targetEntityId,
      relationshipType,
    };
    
    if (metadata !== undefined) {
      edgeConfig.metadata = metadata;
    }
    
    return this.createEdge(edgeConfig);
  }

  static createPartOfRelationship(
    partId: string,
    wholeId: string,
    metadata?: Record<string, unknown>
  ): Edge {
    const edgeConfig: Parameters<typeof this.createEdge>[0] = {
      sourceEntityId: partId,
      targetEntityId: wholeId,
      relationshipType: RelationshipType.PART_OF,
    };
    
    if (metadata !== undefined) {
      edgeConfig.metadata = metadata;
    }
    
    return this.createEdge(edgeConfig);
  }

  static createLocationRelationship(
    entityId: string,
    locationEntityId: string,
    metadata?: Record<string, unknown>
  ): Edge {
    const edgeConfig: Parameters<typeof this.createEdge>[0] = {
      sourceEntityId: entityId,
      targetEntityId: locationEntityId,
      relationshipType: RelationshipType.LOCATED_AT,
    };
    
    if (metadata !== undefined) {
      edgeConfig.metadata = metadata;
    }
    
    return this.createEdge(edgeConfig);
  }

  // Complex entity creation patterns
  static createFilamentSpoolSystem(filamentInfo: FilamentInfo, options: {
    rfidValue?: string;
    location?: string;
    initialWeight?: number;
    notes?: string;
  } = {}): {
    physicalComponent: PhysicalComponent;
    inventoryItem: InventoryItem;
    identifier?: Identifier;
    scanActivity: Activity;
    edges: Edge[];
  } {
    // Create the physical component (the actual spool)
    const physicalComponentOptions: Parameters<typeof this.createPhysicalComponent>[1] = {
      currentWeight: options.initialWeight || filamentInfo.spoolWeight,
    };
    if (options.notes !== undefined) {
      physicalComponentOptions.notes = options.notes;
    }
    const physicalComponent = this.createPhysicalComponent(filamentInfo, physicalComponentOptions);

    // Create the inventory item
    const inventoryItemOptions: Parameters<typeof this.createInventoryItem>[0] = {
      quantity: 1,
    };
    if (options.location !== undefined) {
      inventoryItemOptions.location = options.location;
    }
    const inventoryItem = this.createInventoryItem(inventoryItemOptions);

    // Create scan activity
    const scanActivity = this.createActivity({
      activityType: 'SCAN',
      description: `Scanned filament spool: ${filamentInfo.manufacturerName} ${filamentInfo.filamentType}`,
      relatedEntityId: physicalComponent.id,
    });

    const edges: Edge[] = [];

    // Create inventory relationship
    edges.push(
      this.createInventoryRelationship(
        inventoryItem.id,
        physicalComponent.id,
        {
          scannedAt: Date.now(),
          scanner: 'NFC',
        }
      )
    );

    // Create activity relationship
    edges.push(
      this.createActivityRelationship(
        scanActivity.id,
        physicalComponent.id,
        RelationshipType.CREATED_BY
      )
    );

    let identifier: Identifier | undefined;

    // Create RFID identifier if provided
    if (options.rfidValue) {
      identifier = this.createIdentifier({
        value: options.rfidValue,
        identifierType: 'RFID',
      });

      edges.push(
        this.createIdentificationRelationship(
          physicalComponent.id,
          identifier.id,
          {
            tagUid: filamentInfo.tagUid,
            trayUid: filamentInfo.trayUid,
            tagFormat: filamentInfo.tagFormat,
          }
        )
      );
    }

    const result: {
      physicalComponent: PhysicalComponent;
      inventoryItem: InventoryItem;
      identifier?: Identifier;
      scanActivity: Activity;
      edges: Edge[];
    } = {
      physicalComponent,
      inventoryItem,
      scanActivity,
      edges,
    };
    
    // Conditionally assign optional identifier
    if (identifier !== undefined) {
      result.identifier = identifier;
    }
    
    return result;
  }

  // Update entity timestamps
  static updateEntityTimestamp<T extends GraphEntity>(entity: T): T {
    return {
      ...entity,
      updatedAt: Date.now(),
    };
  }

  // Entity validation
  static validateEntity(entity: GraphEntity): boolean {
    if (!entity.id || typeof entity.id !== 'string') {
      return false;
    }

    if (!entity.type || !Object.values(EntityType).includes(entity.type)) {
      return false;
    }

    if (!entity.createdAt || typeof entity.createdAt !== 'number') {
      return false;
    }

    if (!entity.updatedAt || typeof entity.updatedAt !== 'number') {
      return false;
    }

    return true;
  }

  static validateEdge(edge: Edge): boolean {
    if (!edge.id || typeof edge.id !== 'string') {
      return false;
    }

    if (!edge.sourceEntityId || typeof edge.sourceEntityId !== 'string') {
      return false;
    }

    if (!edge.targetEntityId || typeof edge.targetEntityId !== 'string') {
      return false;
    }

    if (!edge.relationshipType || !Object.values(RelationshipType).includes(edge.relationshipType)) {
      return false;
    }

    if (!edge.createdAt || typeof edge.createdAt !== 'number') {
      return false;
    }

    if (!edge.updatedAt || typeof edge.updatedAt !== 'number') {
      return false;
    }

    return true;
  }

  // Clone entities with new IDs
  static cloneEntity<T extends GraphEntity>(entity: T): T {
    const now = Date.now();
    return {
      ...entity,
      id: uuid.v4() as string,
      createdAt: now,
      updatedAt: now,
    };
  }

  static cloneEdge(edge: Edge, newSourceId?: string, newTargetId?: string): Edge {
    const now = Date.now();
    return {
      ...edge,
      id: uuid.v4() as string,
      sourceEntityId: newSourceId || edge.sourceEntityId,
      targetEntityId: newTargetId || edge.targetEntityId,
      createdAt: now,
      updatedAt: now,
    };
  }

  // Entity type guards
  static isPhysicalComponent(entity: GraphEntity): entity is PhysicalComponent {
    return entity.type === EntityType.PHYSICAL_COMPONENT;
  }

  static isInventoryItem(entity: GraphEntity): entity is InventoryItem {
    return entity.type === EntityType.INVENTORY_ITEM;
  }

  static isIdentifier(entity: GraphEntity): entity is Identifier {
    return entity.type === EntityType.IDENTIFIER;
  }

  static isActivity(entity: GraphEntity): entity is Activity {
    return entity.type === EntityType.ACTIVITY;
  }

  // Relationship type predicates
  static isIdentificationRelationship(edge: Edge): boolean {
    return edge.relationshipType === RelationshipType.IDENTIFIED_BY;
  }

  static isInventoryRelationship(edge: Edge): boolean {
    return edge.relationshipType === RelationshipType.CONTAINS;
  }

  static isPartOfRelationship(edge: Edge): boolean {
    return edge.relationshipType === RelationshipType.PART_OF;
  }

  static isActivityRelationship(edge: Edge): boolean {
    return [
      RelationshipType.CREATED_BY,
      RelationshipType.MODIFIED_BY,
      RelationshipType.TRIGGERED_BY,
    ].includes(edge.relationshipType);
  }
}