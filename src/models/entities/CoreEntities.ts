/**
 * Core entity classes - Activity, Identifier, Location, Person, Information, Virtual
 */

import { Entity, ValidationResult, generateId } from './Entity';
import { PropertyValue } from './PropertyValue';
import { EntityTypes, ActivityTypes, IdentifierTypes } from './types';

/**
 * Identifier entity (RFID UIDs, barcodes, QR codes, etc.)
 */
export class Identifier extends Entity {
  constructor(
    id: string = generateId(),
    public readonly identifierType: string,
    public readonly value: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.IDENTIFIER, `${identifierType}: ${value}`, properties);
    this.setProperty('identifierType', identifierType);
    this.setProperty('value', value);
  }

  get format(): string | undefined {
    return this.getProperty<string>('format');
  }
  
  set format(value: string | undefined) {
    if (value !== undefined) this.setProperty('format', value);
  }

  get purpose(): string | undefined {
    return this.getProperty<string>('purpose');
  }
  
  set purpose(value: string | undefined) {
    if (value !== undefined) this.setProperty('purpose', value);
  }

  get isUnique(): boolean {
    return this.getProperty<boolean>('isUnique') ?? false;
  }
  
  set isUnique(value: boolean) {
    this.setProperty('isUnique', value);
  }

  copy(newId: string = generateId()): Identifier {
    return new Identifier(
      newId,
      this.identifierType,
      this.value,
      new Map(this.properties)
    );
  }

  validate(): ValidationResult {
    const errors: string[] = [];
    
    if (!this.identifierType.trim()) {
      errors.push('Identifier must have a type');
    }
    
    if (!this.value.trim()) {
      errors.push('Identifier must have a value');
    }
    
    return errors.length === 0 
      ? ValidationResult.valid() 
      : ValidationResult.invalid(...errors);
  }
}

/**
 * Location entity (storage locations, workstations, etc.)
 */
export class Location extends Entity {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.LOCATION, label, properties);
  }

  get locationType(): string | undefined {
    return this.getProperty<string>('locationType');
  }
  
  set locationType(value: string | undefined) {
    if (value !== undefined) this.setProperty('locationType', value);
  }

  get address(): string | undefined {
    return this.getProperty<string>('address');
  }
  
  set address(value: string | undefined) {
    if (value !== undefined) this.setProperty('address', value);
  }

  get coordinates(): string | undefined {
    return this.getProperty<string>('coordinates');
  }
  
  set coordinates(value: string | undefined) {
    if (value !== undefined) this.setProperty('coordinates', value);
  }

  copy(newId: string = generateId()): Location {
    return new Location(
      newId,
      this.label,
      new Map(this.properties)
    );
  }
}

/**
 * Person entity (users, manufacturers, suppliers, etc.)
 */
export class Person extends Entity {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.PERSON, label, properties);
  }

  get role(): string | undefined {
    return this.getProperty<string>('role');
  }
  
  set role(value: string | undefined) {
    if (value !== undefined) this.setProperty('role', value);
  }

  get email(): string | undefined {
    return this.getProperty<string>('email');
  }
  
  set email(value: string | undefined) {
    if (value !== undefined) this.setProperty('email', value);
  }

  get organization(): string | undefined {
    return this.getProperty<string>('organization');
  }
  
  set organization(value: string | undefined) {
    if (value !== undefined) this.setProperty('organization', value);
  }

  copy(newId: string = generateId()): Person {
    return new Person(
      newId,
      this.label,
      new Map(this.properties)
    );
  }

  validate(): ValidationResult {
    const errors: string[] = [];
    
    if (!this.label.trim()) {
      errors.push('Person must have a name');
    }
    
    return errors.length === 0 
      ? ValidationResult.valid() 
      : ValidationResult.invalid(...errors);
  }
}

/**
 * Activity/Event entity (scans, maintenance, usage, etc.)
 */
export class Activity extends Entity {
  constructor(
    id: string = generateId(),
    public readonly activityType: string,
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.ACTIVITY, label, properties);
    this.setProperty('activityType', activityType);
    this.setProperty('timestamp', new Date());
  }

  get timestamp(): Date {
    return this.getProperty<Date>('timestamp') ?? new Date();
  }
  
  set timestamp(value: Date) {
    this.setProperty('timestamp', value);
  }

  get duration(): number | undefined {
    return this.getProperty<number>('duration');
  }
  
  set duration(value: number | undefined) {
    if (value !== undefined) this.setProperty('duration', value);
  }

  get status(): string | undefined {
    return this.getProperty<string>('status');
  }
  
  set status(value: string | undefined) {
    if (value !== undefined) this.setProperty('status', value);
  }

  get result(): string | undefined {
    return this.getProperty<string>('result');
  }
  
  set result(value: string | undefined) {
    if (value !== undefined) this.setProperty('result', value);
  }

  copy(newId: string = generateId()): Activity {
    return new Activity(
      newId,
      this.activityType,
      this.label,
      new Map(this.properties)
    );
  }

  validate(): ValidationResult {
    const errors: string[] = [];
    
    if (!this.activityType.trim()) {
      errors.push('Activity must have a type');
    }
    
    if (!this.label.trim()) {
      errors.push('Activity must have a label');
    }
    
    return errors.length === 0 
      ? ValidationResult.valid() 
      : ValidationResult.invalid(...errors);
  }
}

/**
 * Information entity (documents, specifications, manuals, etc.)
 */
export class Information extends Entity {
  constructor(
    id: string = generateId(),
    public readonly informationType: string,
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.INFORMATION, label, properties);
    this.setProperty('informationType', informationType);
  }

  get content(): string | undefined {
    return this.getProperty<string>('content');
  }
  
  set content(value: string | undefined) {
    if (value !== undefined) this.setProperty('content', value);
  }

  get url(): string | undefined {
    return this.getProperty<string>('url');
  }
  
  set url(value: string | undefined) {
    if (value !== undefined) this.setProperty('url', value);
  }

  get mediaType(): string | undefined {
    return this.getProperty<string>('mediaType');
  }
  
  set mediaType(value: string | undefined) {
    if (value !== undefined) this.setProperty('mediaType', value);
  }

  copy(newId: string = generateId()): Information {
    return new Information(
      newId,
      this.informationType,
      this.label,
      new Map(this.properties)
    );
  }
}

/**
 * Virtual entity (concepts, categories, templates, etc.)
 */
export class Virtual extends Entity {
  constructor(
    id: string = generateId(),
    public readonly virtualType: string,
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.VIRTUAL, label, properties);
    this.setProperty('virtualType', virtualType);
  }

  get template(): boolean {
    return this.getProperty<boolean>('template') ?? false;
  }
  
  set template(value: boolean) {
    this.setProperty('template', value);
  }

  get abstract(): boolean {
    return this.getProperty<boolean>('abstract') ?? false;
  }
  
  set abstract(value: boolean) {
    this.setProperty('abstract', value);
  }

  copy(newId: string = generateId()): Virtual {
    return new Virtual(
      newId,
      this.virtualType,
      this.label,
      new Map(this.properties)
    );
  }
}