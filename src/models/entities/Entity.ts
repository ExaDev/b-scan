/**
 * Base entity class for the graph data model.
 * All trackable items, components, locations, people, etc. extend from this.
 */

import { v4 as uuidv4 } from 'uuid';

export interface EntityMetadata {
  created: Date;
  lastModified: Date;
  version: number;
  tags: Set<string>;
  source?: string; // Where this entity came from (scan, import, user, etc.)
  confidence: number; // Confidence level for inferred data (0.0 to 1.0)
}

/**
 * Result of entity validation
 */
export abstract class ValidationResult {
  abstract readonly isValid: boolean;
  
  static valid(): ValidationResult.Valid {
    return new ValidationResult.Valid();
  }
  
  static invalid(...errors: string[]): ValidationResult.Invalid {
    return new ValidationResult.Invalid(errors);
  }
}

export namespace ValidationResult {
  export class Valid extends ValidationResult {
    readonly isValid = true;
  }
  
  export class Invalid extends ValidationResult {
    readonly isValid = false;
    
    constructor(public readonly errors: string[]) {
      super();
    }
  }
}

/**
 * Generate unique identifier for entities
 */
export function generateId(): string {
  return uuidv4();
}

/**
 * Base entity class
 */
export abstract class Entity {
  public readonly id: string;
  public readonly entityType: string;
  public readonly label: string;
  public readonly properties: Map<string, PropertyValue>;
  public readonly metadata: EntityMetadata;

  constructor(
    id: string = generateId(),
    entityType: string,
    label: string,
    properties: Map<string, PropertyValue> = new Map(),
    metadata?: Partial<EntityMetadata>
  ) {
    this.id = id;
    this.entityType = entityType;
    this.label = label;
    this.properties = properties;
    this.metadata = {
      created: new Date(),
      lastModified: new Date(),
      version: 1,
      tags: new Set(),
      confidence: 1.0,
      ...metadata
    };
  }

  /**
   * Get a property value with type safety
   */
  getProperty<T = any>(key: string): T | undefined {
    const propertyValue = this.properties.get(key);
    return propertyValue?.getValue<T>();
  }

  /**
   * Set a property value
   */
  setProperty<T>(key: string, value: T): void {
    this.properties.set(key, PropertyValue.create(value));
    this.metadata.lastModified = new Date();
  }

  /**
   * Check if entity has a property
   */
  hasProperty(key: string): boolean {
    return this.properties.has(key);
  }

  /**
   * Remove a property
   */
  removeProperty(key: string): void {
    this.properties.delete(key);
    this.metadata.lastModified = new Date();
  }

  /**
   * Get all property keys
   */
  getPropertyKeys(): string[] {
    return Array.from(this.properties.keys());
  }

  /**
   * Create a copy of this entity with new ID (for versioning/branching)
   */
  abstract copy(newId?: string): Entity;

  /**
   * Validate this entity's properties and constraints
   */
  validate(): ValidationResult {
    return ValidationResult.valid();
  }

  equals(other: any): boolean {
    if (this === other) return true;
    if (!(other instanceof Entity)) return false;
    return this.id === other.id;
  }

  toString(): string {
    return `${this.entityType}(${this.id}): ${this.label}`;
  }
}

/**
 * Import PropertyValue after defining Entity to avoid circular dependency
 */
import { PropertyValue } from './PropertyValue';