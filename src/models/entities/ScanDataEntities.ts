/**
 * Scan data entities for RFID/NFC/QR processing pipeline
 */

import { Activity, Information } from './CoreEntities';
import { Entity, ValidationResult, generateId } from './Entity';
import { PropertyValue } from './PropertyValue';
import { ActivityTypes, CacheEntry } from './types';

/**
 * Scan occurrence entity - represents each individual scan event
 * PERSISTENT: One created for each scan
 */
export class ScanOccurrence extends Activity {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, ActivityTypes.SCAN, label, properties);
  }

  get deviceInfo(): string | undefined {
    return this.getProperty<string>('deviceInfo');
  }
  
  set deviceInfo(value: string | undefined) {
    if (value !== undefined) this.setProperty('deviceInfo', value);
  }

  get scanLocation(): string | undefined {
    return this.getProperty<string>('scanLocation');
  }
  
  set scanLocation(value: string | undefined) {
    if (value !== undefined) this.setProperty('scanLocation', value);
  }

  get scanMethod(): string | undefined {
    return this.getProperty<string>('scanMethod'); // "nfc", "qr", "barcode", "manual"
  }
  
  set scanMethod(value: string | undefined) {
    if (value !== undefined) this.setProperty('scanMethod', value);
  }

  get appVersion(): string | undefined {
    return this.getProperty<string>('appVersion');
  }
  
  set appVersion(value: string | undefined) {
    if (value !== undefined) this.setProperty('appVersion', value);
  }

  get userData(): string | undefined {
    return this.getProperty<string>('userData'); // User annotations, notes
  }
  
  set userData(value: string | undefined) {
    if (value !== undefined) this.setProperty('userData', value);
  }
}

/**
 * Raw scan data entity - stores unique encoded/encrypted data
 * PERSISTENT: Shared across multiple scan occurrences via relationships
 * Deduplicated based on content hash
 */
export class RawScanData extends Information {
  constructor(
    id: string = generateId(),
    label: string,
    public readonly scanFormat: string, // "bambu_rfid", "creality_rfid", "qr_code", "barcode"
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, 'raw_scan_data', label, properties);
    this.setProperty('scanFormat', scanFormat);
  }

  get rawData(): string {
    return this.getProperty<string>('rawData') ?? '';
  }
  
  set rawData(value: string) {
    this.setProperty('rawData', value);
  }

  get dataSize(): number {
    return this.getProperty<number>('dataSize') ?? 0;
  }
  
  set dataSize(value: number) {
    this.setProperty('dataSize', value);
  }

  get contentHash(): string | undefined {
    return this.getProperty<string>('contentHash');
  }
  
  set contentHash(value: string | undefined) {
    if (value !== undefined) this.setProperty('contentHash', value);
  }

  get encoding(): string | undefined {
    return this.getProperty<string>('encoding'); // "hex", "base64", "utf8"
  }
  
  set encoding(value: string | undefined) {
    if (value !== undefined) this.setProperty('encoding', value);
  }

  get checksumValid(): boolean | undefined {
    return this.getProperty<boolean>('checksumValid');
  }
  
  set checksumValid(value: boolean | undefined) {
    if (value !== undefined) this.setProperty('checksumValid', value);
  }

  copy(newId: string = generateId()): RawScanData {
    return new RawScanData(
      newId,
      this.label,
      this.scanFormat,
      new Map(this.properties)
    );
  }

  validate(): ValidationResult {
    const errors: string[] = [];
    
    if (!this.scanFormat.trim()) {
      errors.push('Scan format must be specified');
    }
    
    if (!this.rawData.trim()) {
      errors.push('Raw data cannot be empty');
    }
    
    return errors.length === 0 
      ? ValidationResult.valid() 
      : ValidationResult.invalid(...errors);
  }
}

/**
 * Decoded encrypted entity - non-encrypted metadata from raw scan
 * EPHEMERAL: Generated on-demand with TTL caching
 */
export class DecodedEncrypted extends Information {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, 'decoded_encrypted', label, properties);
  }

  get tagType(): string | undefined {
    return this.getProperty<string>('tagType');
  }
  
  set tagType(value: string | undefined) {
    if (value !== undefined) this.setProperty('tagType', value);
  }

  get tagUid(): string | undefined {
    return this.getProperty<string>('tagUid');
  }
  
  set tagUid(value: string | undefined) {
    if (value !== undefined) this.setProperty('tagUid', value);
  }

  get dataBlocks(): number | undefined {
    return this.getProperty<number>('dataBlocks');
  }
  
  set dataBlocks(value: number | undefined) {
    if (value !== undefined) this.setProperty('dataBlocks', value);
  }

  get sectorCount(): number | undefined {
    return this.getProperty<number>('sectorCount');
  }
  
  set sectorCount(value: number | undefined) {
    if (value !== undefined) this.setProperty('sectorCount', value);
  }

  get authenticated(): boolean | undefined {
    return this.getProperty<boolean>('authenticated');
  }
  
  set authenticated(value: boolean | undefined) {
    if (value !== undefined) this.setProperty('authenticated', value);
  }

  get keyDerivationTime(): number | undefined {
    return this.getProperty<number>('keyDerivationTime');
  }
  
  set keyDerivationTime(value: number | undefined) {
    if (value !== undefined) this.setProperty('keyDerivationTime', value);
  }

  get cacheTimestamp(): Date {
    return this.getProperty<Date>('cacheTimestamp') ?? new Date();
  }
  
  set cacheTimestamp(value: Date) {
    this.setProperty('cacheTimestamp', value);
  }

  get cacheTtlMinutes(): number {
    return this.getProperty<number>('cacheTtlMinutes') ?? 60;
  }
  
  set cacheTtlMinutes(value: number) {
    this.setProperty('cacheTtlMinutes', value);
  }

  isExpired(): boolean {
    const expiryTime = new Date(this.cacheTimestamp.getTime() + this.cacheTtlMinutes * 60 * 1000);
    return new Date() > expiryTime;
  }
}

/**
 * Encoded decrypted entity - decrypted but still hex-encoded data  
 * EPHEMERAL: Generated on-demand with TTL caching
 */
export class EncodedDecrypted extends Information {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, 'encoded_decrypted', label, properties);
  }

  get decryptedData(): string {
    return this.getProperty<string>('decryptedData') ?? '';
  }
  
  set decryptedData(value: string) {
    this.setProperty('decryptedData', value);
  }

  get blockStructure(): string | undefined {
    return this.getProperty<string>('blockStructure'); // JSON representation of block layout
  }
  
  set blockStructure(value: string | undefined) {
    if (value !== undefined) this.setProperty('blockStructure', value);
  }

  get keyInfo(): string | undefined {
    return this.getProperty<string>('keyInfo'); // Which keys were used for decryption
  }
  
  set keyInfo(value: string | undefined) {
    if (value !== undefined) this.setProperty('keyInfo', value);
  }

  get decryptionTime(): number | undefined {
    return this.getProperty<number>('decryptionTime');
  }
  
  set decryptionTime(value: number | undefined) {
    if (value !== undefined) this.setProperty('decryptionTime', value);
  }

  get cacheTimestamp(): Date {
    return this.getProperty<Date>('cacheTimestamp') ?? new Date();
  }
  
  set cacheTimestamp(value: Date) {
    this.setProperty('cacheTimestamp', value);
  }

  get cacheTtlMinutes(): number {
    return this.getProperty<number>('cacheTtlMinutes') ?? 30;
  }
  
  set cacheTtlMinutes(value: number) {
    this.setProperty('cacheTtlMinutes', value);
  }

  isExpired(): boolean {
    const expiryTime = new Date(this.cacheTimestamp.getTime() + this.cacheTtlMinutes * 60 * 1000);
    return new Date() > expiryTime;
  }
}

/**
 * Decoded decrypted entity - fully interpreted structured data
 * EPHEMERAL: Generated on-demand with TTL caching
 */
export class DecodedDecrypted extends Information {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, 'decoded_decrypted', label, properties);
  }

  get interpretedData(): string | undefined {
    return this.getProperty<string>('interpretedData'); // JSON representation of structured data
  }
  
  set interpretedData(value: string | undefined) {
    if (value !== undefined) this.setProperty('interpretedData', value);
  }

  get filamentProperties(): string | undefined {
    return this.getProperty<string>('filamentProperties'); // Bambu-specific: material, color, etc.
  }
  
  set filamentProperties(value: string | undefined) {
    if (value !== undefined) this.setProperty('filamentProperties', value);
  }

  get productInfo(): string | undefined {
    return this.getProperty<string>('productInfo'); // SKU, manufacturer, model
  }
  
  set productInfo(value: string | undefined) {
    if (value !== undefined) this.setProperty('productInfo', value);
  }

  get temperatureSettings(): string | undefined {
    return this.getProperty<string>('temperatureSettings'); // Print/bed temperatures
  }
  
  set temperatureSettings(value: string | undefined) {
    if (value !== undefined) this.setProperty('temperatureSettings', value);
  }

  get physicalProperties(): string | undefined {
    return this.getProperty<string>('physicalProperties'); // Mass, dimensions, etc.
  }
  
  set physicalProperties(value: string | undefined) {
    if (value !== undefined) this.setProperty('physicalProperties', value);
  }

  get identifiers(): string | undefined {
    return this.getProperty<string>('identifiers'); // UID, tray ID, consumable ID
  }
  
  set identifiers(value: string | undefined) {
    if (value !== undefined) this.setProperty('identifiers', value);
  }

  get interpretationVersion(): string | undefined {
    return this.getProperty<string>('interpretationVersion'); // Version of interpretation logic
  }
  
  set interpretationVersion(value: string | undefined) {
    if (value !== undefined) this.setProperty('interpretationVersion', value);
  }

  get interpretationTime(): number | undefined {
    return this.getProperty<number>('interpretationTime');
  }
  
  set interpretationTime(value: number | undefined) {
    if (value !== undefined) this.setProperty('interpretationTime', value);
  }

  get cacheTimestamp(): Date {
    return this.getProperty<Date>('cacheTimestamp') ?? new Date();
  }
  
  set cacheTimestamp(value: Date) {
    this.setProperty('cacheTimestamp', value);
  }

  get cacheTtlMinutes(): number {
    return this.getProperty<number>('cacheTtlMinutes') ?? 15; // Shorter TTL for complex interpretations
  }
  
  set cacheTtlMinutes(value: number) {
    this.setProperty('cacheTtlMinutes', value);
  }

  isExpired(): boolean {
    const expiryTime = new Date(this.cacheTimestamp.getTime() + this.cacheTtlMinutes * 60 * 1000);
    return new Date() > expiryTime;
  }
}

/**
 * Cache management implementation for ephemeral entities
 */
export class CacheEntryImpl<T extends Entity> implements CacheEntry<T> {
  constructor(
    public readonly entity: T,
    public readonly timestamp: Date = new Date(),
    public readonly ttlMinutes: number
  ) {}

  isExpired(): boolean {
    const expiryTime = new Date(this.timestamp.getTime() + this.ttlMinutes * 60 * 1000);
    return new Date() > expiryTime;
  }

  getRemainingTtl(): number {
    const expiryTime = new Date(this.timestamp.getTime() + this.ttlMinutes * 60 * 1000);
    const now = new Date();
    return now < expiryTime 
      ? Math.floor((expiryTime.getTime() - now.getTime()) / (60 * 1000))
      : 0;
  }
}