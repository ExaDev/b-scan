/**
 * Type-safe property value container supporting multiple data types.
 * Handles serialization and type conversion for graph entity/edge properties.
 */

import { TrackingMode } from './types';

/**
 * Supported property types
 */
export enum PropertyType {
  STRING = 'STRING',
  INT = 'INT',
  LONG = 'LONG',
  FLOAT = 'FLOAT',
  DOUBLE = 'DOUBLE',
  BOOLEAN = 'BOOLEAN',
  DATETIME = 'DATETIME',
  DATE = 'DATE',
  LIST = 'LIST',
  MAP = 'MAP',
  BYTES = 'BYTES',
  QUANTITY = 'QUANTITY',
  NULL = 'NULL'
}

/**
 * Interface for quantities with units and discrete/continuous tracking modes
 */
export interface Quantity {
  readonly value: number;
  readonly unit: string;
  readonly trackingMode: TrackingMode;
  toString(): string;
}

/**
 * Discrete quantity for countable items
 */
export class DiscreteQuantity implements Quantity {
  readonly trackingMode = TrackingMode.DISCRETE;

  constructor(
    public readonly value: number,
    public readonly unit: string
  ) {
    if (!Number.isInteger(value)) {
      throw new Error('Discrete quantity value must be an integer');
    }
  }

  toString(): string {
    return `${this.value}${this.unit}-discrete`;
  }

  add(other: DiscreteQuantity): DiscreteQuantity {
    if (this.unit !== other.unit) {
      throw new Error(`Cannot add quantities with different units: ${this.unit} vs ${other.unit}`);
    }
    return new DiscreteQuantity(this.value + other.value, this.unit);
  }

  subtract(other: DiscreteQuantity): DiscreteQuantity {
    if (this.unit !== other.unit) {
      throw new Error(`Cannot subtract quantities with different units: ${this.unit} vs ${other.unit}`);
    }
    return new DiscreteQuantity(this.value - other.value, this.unit);
  }
}

/**
 * Continuous quantity for measurable values
 */
export class ContinuousQuantity implements Quantity {
  readonly trackingMode = TrackingMode.CONTINUOUS;

  constructor(
    public readonly value: number,
    public readonly unit: string
  ) {}

  toString(): string {
    return `${this.value}${this.unit}`;
  }

  add(other: ContinuousQuantity): ContinuousQuantity {
    if (this.unit !== other.unit) {
      throw new Error(`Cannot add quantities with different units: ${this.unit} vs ${other.unit}`);
    }
    return new ContinuousQuantity(this.value + other.value, this.unit);
  }

  subtract(other: ContinuousQuantity): ContinuousQuantity {
    if (this.unit !== other.unit) {
      throw new Error(`Cannot subtract quantities with different units: ${this.unit} vs ${other.unit}`);
    }
    return new ContinuousQuantity(this.value - other.value, this.unit);
  }

  multiply(factor: number): ContinuousQuantity {
    return new ContinuousQuantity(this.value * factor, this.unit);
  }

  divide(divisor: number): ContinuousQuantity {
    if (divisor === 0) {
      throw new Error('Cannot divide by zero');
    }
    return new ContinuousQuantity(this.value / divisor, this.unit);
  }
}

/**
 * Quantity factory methods
 */
export const QuantityFactory = {
  /**
   * Parse a quantity from string representation (e.g., "1000g", "212.5g-discrete")
   */
  fromString(str: string): Quantity {
    const parts = str.split('-');
    const quantityPart = parts[0];
    const trackingMode = parts.length > 1 && parts[1].toLowerCase() === 'discrete'
      ? TrackingMode.DISCRETE
      : TrackingMode.CONTINUOUS;

    // Extract numeric value and unit using regex
    const match = quantityPart.match(/^([0-9]*\.?[0-9]+)(.*)$/);
    if (!match) {
      throw new Error(`Invalid quantity format: ${str}`);
    }

    const value = parseFloat(match[1]);
    const unit = match[2] || 'units';

    return trackingMode === TrackingMode.DISCRETE
      ? new DiscreteQuantity(Math.round(value), unit)
      : new ContinuousQuantity(value, unit);
  },

  discrete(value: number, unit: string): DiscreteQuantity {
    return new DiscreteQuantity(value, unit);
  },

  continuous(value: number, unit: string): ContinuousQuantity {
    return new ContinuousQuantity(value, unit);
  }
};

/**
 * Type-safe property value container
 */
export abstract class PropertyValue {
  abstract readonly type: PropertyType;
  abstract readonly rawValue: any;

  /**
   * Get the value with type safety
   */
  abstract getValue<T = any>(): T | undefined;

  /**
   * Convert to string representation
   */
  abstract asString(): string;

  /**
   * Create PropertyValue from any supported type
   */
  static create(value: any): PropertyValue {
    if (value === null || value === undefined) {
      return new NullValue();
    }
    
    if (typeof value === 'string') {
      return new StringValue(value);
    }
    
    if (typeof value === 'number') {
      return Number.isInteger(value) ? new IntValue(value) : new DoubleValue(value);
    }
    
    if (typeof value === 'boolean') {
      return new BooleanValue(value);
    }
    
    if (value instanceof Date) {
      return new DateTimeValue(value);
    }
    
    if (Array.isArray(value)) {
      return new ListValue(value);
    }
    
    if (value instanceof Uint8Array || value instanceof ArrayBuffer) {
      return new BytesValue(new Uint8Array(value));
    }
    
    if (typeof value === 'object' && value !== null && 'value' in value && 'unit' in value && 'trackingMode' in value) {
      return new QuantityValue(value as Quantity);
    }
    
    if (typeof value === 'object' && value !== null) {
      return new MapValue(value as Record<string, any>);
    }
    
    // Fallback to string
    return new StringValue(String(value));
  }

  /**
   * Create PropertyValue from string with type hint
   */
  static fromString(value: string, type: PropertyType): PropertyValue {
    switch (type) {
      case PropertyType.STRING:
        return new StringValue(value);
      case PropertyType.INT:
        return new IntValue(parseInt(value, 10) || 0);
      case PropertyType.DOUBLE:
      case PropertyType.FLOAT:
        return new DoubleValue(parseFloat(value) || 0);
      case PropertyType.BOOLEAN:
        return new BooleanValue(value.toLowerCase() === 'true');
      case PropertyType.DATETIME:
        return new DateTimeValue(new Date(value));
      case PropertyType.DATE:
        return new DateValue(new Date(value));
      case PropertyType.BYTES:
        const bytes = value.match(/.{1,2}/g)?.map(byte => parseInt(byte, 16)) || [];
        return new BytesValue(new Uint8Array(bytes));
      case PropertyType.QUANTITY:
        return new QuantityValue(QuantityFactory.fromString(value));
      case PropertyType.LIST:
        try {
          return new ListValue(JSON.parse(value));
        } catch {
          return new ListValue([value]);
        }
      case PropertyType.MAP:
        try {
          return new MapValue(JSON.parse(value));
        } catch {
          return new MapValue({ value });
        }
      case PropertyType.NULL:
        return new NullValue();
      default:
        return new StringValue(value);
    }
  }
}

export class StringValue extends PropertyValue {
  readonly type = PropertyType.STRING;

  constructor(public readonly rawValue: string) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return this.rawValue;
  }
}

export class IntValue extends PropertyValue {
  readonly type = PropertyType.INT;

  constructor(public readonly rawValue: number) {
    super();
    if (!Number.isInteger(rawValue)) {
      throw new Error('IntValue requires an integer');
    }
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return this.rawValue.toString();
  }
}

export class DoubleValue extends PropertyValue {
  readonly type = PropertyType.DOUBLE;

  constructor(public readonly rawValue: number) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return this.rawValue.toString();
  }
}

export class BooleanValue extends PropertyValue {
  readonly type = PropertyType.BOOLEAN;

  constructor(public readonly rawValue: boolean) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return this.rawValue.toString();
  }
}

export class DateTimeValue extends PropertyValue {
  readonly type = PropertyType.DATETIME;

  constructor(public readonly rawValue: Date) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return this.rawValue.toISOString();
  }
}

export class DateValue extends PropertyValue {
  readonly type = PropertyType.DATE;

  constructor(public readonly rawValue: Date) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return this.rawValue.toISOString().split('T')[0];
  }
}

export class ListValue extends PropertyValue {
  readonly type = PropertyType.LIST;

  constructor(public readonly rawValue: any[]) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return JSON.stringify(this.rawValue);
  }
}

export class MapValue extends PropertyValue {
  readonly type = PropertyType.MAP;

  constructor(public readonly rawValue: Record<string, any>) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return JSON.stringify(this.rawValue);
  }
}

export class BytesValue extends PropertyValue {
  readonly type = PropertyType.BYTES;

  constructor(public readonly rawValue: Uint8Array) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return Array.from(this.rawValue)
      .map(byte => byte.toString(16).padStart(2, '0'))
      .join('');
  }
}

export class QuantityValue extends PropertyValue {
  readonly type = PropertyType.QUANTITY;

  constructor(public readonly rawValue: Quantity) {
    super();
  }

  getValue<T = any>(): T | undefined {
    return this.rawValue as any;
  }

  asString(): string {
    return this.rawValue.toString();
  }
}

export class NullValue extends PropertyValue {
  readonly type = PropertyType.NULL;
  readonly rawValue = null;

  getValue<T = any>(): T | undefined {
    return undefined;
  }

  asString(): string {
    return 'null';
  }
}