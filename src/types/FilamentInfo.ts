/**
 * Complete filament information extracted from RFID/NFC tags
 * Enhanced to match sophisticated Kotlin implementation with full metadata support
 */
export interface FilamentInfo {
  /** Individual tag UID (unique per tag) */
  tagUid: string;
  
  /** Tray UID (shared across both tags on a spool) */
  trayUid: string;
  
  /** Detected tag format/standard */
  tagFormat: TagFormat;
  
  /** Manufacturer name (e.g., "Bambu Lab", "Creality") */
  manufacturerName: string;
  
  /** Basic filament type (e.g., "PLA", "ABS", "PETG") */
  filamentType: string;
  
  /** Detailed filament type with variant (e.g., "PLA Basic", "ABS High Flow") */
  detailedFilamentType: string;
  
  /** Color hex code (e.g., "#FF0000") */
  colorHex: string;
  
  /** Human-readable color name (e.g., "Jade White", "Matte Black") */
  colorName: string;
  
  /** Spool weight in grams */
  spoolWeight: number;
  
  /** Filament diameter in mm (typically 1.75 or 3.0) */
  filamentDiameter: number;
  
  /** Filament length in mm */
  filamentLength: number;
  
  /** Production date string (format varies by manufacturer) */
  productionDate: string;
  
  /** Minimum printing temperature in °C */
  minTemperature: number;
  
  /** Maximum printing temperature in °C */
  maxTemperature: number;
  
  /** Bed temperature in °C */
  bedTemperature: number;
  
  /** Drying temperature in °C */
  dryingTemperature: number;
  
  /** Drying time in hours */
  dryingTime: number;
  
  // Extended fields from sophisticated block parsing
  
  /** Material variant identifier for internal tracking */
  materialVariantId: string;
  
  /** Material identifier for product matching */
  materialId: string;
  
  /** Recommended nozzle diameter in mm (default: 0.4) */
  nozzleDiameter: number;
  
  /** Spool width in mm */
  spoolWidth: number;
  
  /** Bed temperature type/profile identifier */
  bedTemperatureType: number;
  
  /** Short production date format */
  shortProductionDate: string;
  
  /** Color count: 1 = single color, 2 = dual color */
  colorCount: number;
  
  // Research and debugging fields
  
  /** Block 13 raw hex data for production date analysis */
  shortProductionDateHex: string;
  
  /** Block 17 full raw hex data for unknown field research */
  unknownBlock17Hex: string;
  
  // Exact product identification
  
  /** 5-digit SKU number (e.g., "10101") from exact RFID mapping */
  exactSku?: string;
  
  /** Full RFID code (e.g., "GFA00:A00-K0") for debugging and support */
  rfidCode?: string;
  
  /** Product information for purchase links and metadata */
  bambuProduct?: BambuProduct;
}

/**
 * Supported RFID tag data formats and standards
 */
export enum TagFormat {
  /** Bambu Lab's proprietary encrypted format on Mifare Classic 1K */
  BAMBU_PROPRIETARY = 'BAMBU_PROPRIETARY',
  
  /** Creality's ASCII-encoded format in blocks 4-6 */
  CREALITY_ASCII = 'CREALITY_ASCII',
  
  /** OpenTag NDEF JSON format */
  NDEF_JSON = 'NDEF_JSON',
  
  /** OpenTag v1.x standard on NTAG216 */
  OPENTAG_V1 = 'OPENTAG_V1',
  
  /** NDEF URI format */
  NDEF_URI = 'NDEF_URI',
  
  /** Other proprietary formats */
  PROPRIETARY = 'PROPRIETARY',
  
  /** User-created custom format */
  USER_DEFINED = 'USER_DEFINED',
  
  /** Unidentified or unsupported format */
  UNKNOWN = 'UNKNOWN',
  
  // Legacy format names for backward compatibility
  /** @deprecated Use BAMBU_PROPRIETARY instead */
  BAMBU_LAB = 'BAMBU_PROPRIETARY',
  
  /** @deprecated Use CREALITY_ASCII instead */
  CREALITY = 'CREALITY_ASCII',
  
  /** @deprecated Use NDEF_JSON instead */
  OPENSPOOL = 'NDEF_JSON',
}

/**
 * Tag technology types supported by the app
 */
export enum TagTechnology {
  /** Mifare Classic 1K (Bambu, potentially Creality) */
  MIFARE_CLASSIC = 'MIFARE_CLASSIC',
  
  /** NTAG213/215/216 (OpenTag) */
  NTAG = 'NTAG',
  
  /** Unidentified technology */
  UNKNOWN = 'UNKNOWN',
}

/**
 * Result of tag format detection
 */
export interface TagDetectionResult {
  /** Detected tag format */
  tagFormat: TagFormat;
  
  /** Tag technology type */
  technology: TagTechnology;
  
  /** Detection confidence (0.0 to 1.0) */
  confidence: number;
  
  /** Human-readable reason for detection */
  detectionReason: string;
  
  /** Manufacturer name if extractable from tag data */
  manufacturerName: string;
}

/**
 * Spool packaging types for filament products
 */
export enum SpoolPackaging {
  /** Comes with plastic spool */
  WITH_SPOOL = 'WITH_SPOOL',
  
  /** Refill only (cardboard core) */
  REFILL = 'REFILL',
}

/**
 * SKU information for Bambu Lab filament products
 * Contains the 5-digit filament code, display name, and material type
 */
export interface SkuInfo {
  /** 5-digit filament code (e.g., "10101") */
  sku: string;
  
  /** Human-readable color name (e.g., "Black") */
  colorName: string;
  
  /** Material series (e.g., "PLA Basic", "ABS") */
  materialType: string;
}

/**
 * Represents a Bambu Lab filament product with purchase links
 */
export interface BambuProduct {
  /** Product line identifier (e.g., "PLA Basic", "ABS") */
  productLine: string;
  
  /** Color name (e.g., "Jade White", "Black") */
  colorName: string;
  
  /** Internal product code (e.g., "GFL00", "GFL01") */
  internalCode: string;
  
  /** Retail SKU identifier (e.g., "10100", "40101") */
  retailSku?: string;
  
  /** Color hex code (e.g., "#FFFFFF", "#000000") */
  colorHex: string;
  
  /** Purchase URL with spool included */
  spoolUrl?: string;
  
  /** Purchase URL for refill-only option */
  refillUrl?: string;
  
  /** Product mass (e.g., "0.5kg", "0.75kg", "1kg") */
  mass: string;
}

/**
 * Purchase link availability for UI display
 */
export interface PurchaseLinks {
  /** Whether spool option is available for purchase */
  spoolAvailable: boolean;
  
  /** Whether refill option is available for purchase */
  refillAvailable: boolean;
  
  /** URL for spool purchase */
  spoolUrl?: string;
  
  /** URL for refill purchase */
  refillUrl?: string;
}

export interface ScanProgress {
  stage: ScanStage;
  percentage: number;
  currentSector: number;
  statusMessage: string;
}

export enum ScanStage {
  INITIALIZING = 'INITIALIZING',
  NFC_DETECTION = 'NFC_DETECTION',
  TAG_VALIDATION = 'TAG_VALIDATION',
  FORMAT_IDENTIFICATION = 'FORMAT_IDENTIFICATION',
  AUTHENTICATING = 'AUTHENTICATING',
  READING_SECTORS = 'READING_SECTORS',
  READING_DATA = 'READING_DATA',
  DECRYPTING = 'DECRYPTING',
  PARSING_DATA = 'PARSING_DATA',
  VALIDATION = 'VALIDATION',
  CACHING = 'CACHING',
  COMPLETED = 'COMPLETED',
  ERROR = 'ERROR',
}

export type TagReadResult =
  | {type: 'SUCCESS'; filamentInfo: FilamentInfo}
  | {type: 'NO_NFC'}
  | {type: 'INVALID_TAG'}
  | {type: 'READ_ERROR'; error: string}
  | {type: 'AUTHENTICATION_FAILED'}
  | {type: 'PARSING_ERROR'; error: string};

export interface ScanHistoryEntry {
  id: string;
  timestamp: number;
  filamentInfo?: FilamentInfo;
  result: TagReadResult['type'];
  error?: string;
}

export interface GraphEntity {
  id: string;
  type: EntityType;
  createdAt: number;
  updatedAt: number;
  [key: string]: unknown;
}

export enum EntityType {
  PHYSICAL_COMPONENT = 'PHYSICAL_COMPONENT',
  INVENTORY_ITEM = 'INVENTORY_ITEM',
  IDENTIFIER = 'IDENTIFIER',
  ACTIVITY = 'ACTIVITY',
  STOCK_DEFINITION = 'STOCK_DEFINITION',
}

export interface PhysicalComponent extends GraphEntity {
  type: EntityType.PHYSICAL_COMPONENT;
  filamentInfo: FilamentInfo;
  currentWeight?: number;
  notes?: string;
}

export interface InventoryItem extends GraphEntity {
  type: EntityType.INVENTORY_ITEM;
  quantity: number;
  location?: string;
  lastUpdated: number;
}

export interface Identifier extends GraphEntity {
  type: EntityType.IDENTIFIER;
  value: string;
  identifierType: 'RFID' | 'BARCODE' | 'QR_CODE';
}

export interface Activity extends GraphEntity {
  type: EntityType.ACTIVITY;
  activityType: 'SCAN' | 'UPDATE' | 'CREATE' | 'DELETE';
  description: string;
  relatedEntityId?: string;
}

// Enhanced NFC Data Structures for B-Scan

export interface NfcTagData {
  uid: string;
  technology: NfcTechnology;
  format: TagFormat;
  size: number;
  isWritable: boolean;
  sectors?: MifareClassicSector[];
  ndefMessage?: NdefMessage;
  rawData: Uint8Array;
  metadata: TagMetadata;
  discoveredAt: number;
}

export interface EncryptedScanData {
  tagUid: string;
  encryptedSectors: EncryptedSector[];
  format: TagFormat;
  encryptionMethod: EncryptionMethod;
  keyDerivationInfo: KeyDerivationInfo;
  checksum: string;
  timestamp: number;
}

export interface DecryptedScanData {
  tagUid: string;
  decryptedSectors: DecryptedSector[];
  format: TagFormat;
  parsedFilamentInfo?: FilamentInfo;
  validationResult: ValidationResult;
  timestamp: number;
}

export interface MifareClassicSector {
  sectorNumber: number;
  blocks: MifareBlock[];
  keyA?: Uint8Array;
  keyB?: Uint8Array;
  accessBits: Uint8Array;
  isAuthenticated: boolean;
}

export interface MifareBlock {
  blockNumber: number;
  data: Uint8Array;
  isTrailerBlock: boolean;
  readSuccess: boolean;
}

export interface EncryptedSector {
  sectorNumber: number;
  encryptedData: Uint8Array;
  keyUsed: string; // Key fingerprint, not actual key
  authenticationMethod: AuthenticationMethod;
  encryptionAlgorithm: EncryptionMethod;
}

export interface DecryptedSector {
  sectorNumber: number;
  plainTextData: Uint8Array;
  dataIntegrity: boolean;
  contentType: SectorContentType;
}

export interface NdefMessage {
  records: NdefRecord[];
  totalLength: number;
}

export interface NdefRecord {
  tnf: number; // Type Name Format
  type: Uint8Array;
  id: Uint8Array;
  payload: Uint8Array;
  recordType: NdefRecordType;
}

export interface TagMetadata {
  manufacturer: string;
  capacity: number;
  blockSize: number;
  sectorCount: number;
  applicationAreas: string[];
  isLocked: boolean;
  lastModified?: number;
}

export interface KeyDerivationInfo {
  algorithm: KeyDerivationAlgorithm;
  iterations?: number;
  salt: string;
  keyLength: number;
  derivedKeyCount: number;
}

export interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
  confidenceLevel: number; // 0-100
  dataIntegrity: boolean;
}

export interface ValidationError {
  code: string;
  message: string;
  severity: ErrorSeverity;
  location?: string; // Sector/block location
}

export interface ValidationWarning {
  code: string;
  message: string;
  recommendation?: string;
}

// Enhanced Error Types

export interface NfcScanError {
  type: NfcErrorType;
  message: string;
  code: string;
  stage: ScanStage;
  details?: NfcErrorDetails;
  recoveryActions?: RecoveryAction[];
  timestamp: number;
}

export interface NfcErrorDetails {
  tagUid?: string;
  sectorNumber?: number;
  blockNumber?: number;
  attemptedAction?: string;
  systemErrorCode?: number;
  stackTrace?: string;
}

export interface RecoveryAction {
  action: string;
  description: string;
  automatic: boolean;
}

export interface ScanCache {
  entries: Map<string, CachedScanResult>;
  maxSize: number;
  ttlMs: number;
}

export interface CachedScanResult {
  tagUid: string;
  filamentInfo: FilamentInfo;
  timestamp: number;
  hitCount: number;
  lastAccessed: number;
}

// Enums

export enum NfcTechnology {
  MIFARE_CLASSIC = 'MIFARE_CLASSIC',
  MIFARE_ULTRALIGHT = 'MIFARE_ULTRALIGHT',
  NTAG213 = 'NTAG213',
  NTAG215 = 'NTAG215',
  NTAG216 = 'NTAG216',
  ISO14443A = 'ISO14443A',
  ISO14443B = 'ISO14443B',
  ISO15693 = 'ISO15693',
  NDEF = 'NDEF',
  UNKNOWN = 'UNKNOWN',
}

export enum EncryptionMethod {
  NONE = 'NONE',
  AES_128 = 'AES_128',
  AES_256 = 'AES_256',
  BAMBU_PROPRIETARY = 'BAMBU_PROPRIETARY',
  CREALITY_PROPRIETARY = 'CREALITY_PROPRIETARY',
}

export enum AuthenticationMethod {
  KEY_A = 'KEY_A',
  KEY_B = 'KEY_B',
  BOTH_KEYS = 'BOTH_KEYS',
  NONE = 'NONE',
}

export enum SectorContentType {
  FILAMENT_DATA = 'FILAMENT_DATA',
  MANUFACTURER_INFO = 'MANUFACTURER_INFO',
  SECURITY_DATA = 'SECURITY_DATA',
  USER_DATA = 'USER_DATA',
  EMPTY = 'EMPTY',
  INVALID = 'INVALID',
}

export enum NdefRecordType {
  TEXT = 'TEXT',
  URI = 'URI',
  JSON = 'JSON',
  BINARY = 'BINARY',
  CUSTOM = 'CUSTOM',
}

export enum KeyDerivationAlgorithm {
  HKDF_SHA256 = 'HKDF_SHA256',
  PBKDF2_SHA256 = 'PBKDF2_SHA256',
  BAMBU_CUSTOM = 'BAMBU_CUSTOM',
  SIMPLE_XOR = 'SIMPLE_XOR',
}

export enum ErrorSeverity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export enum NfcErrorType {
  NFC_DISABLED = 'NFC_DISABLED',
  NFC_UNAVAILABLE = 'NFC_UNAVAILABLE',
  TAG_LOST = 'TAG_LOST',
  AUTHENTICATION_FAILED = 'AUTHENTICATION_FAILED',
  READ_TIMEOUT = 'READ_TIMEOUT',
  WRITE_FAILED = 'WRITE_FAILED',
  INVALID_TAG_FORMAT = 'INVALID_TAG_FORMAT',
  ENCRYPTION_ERROR = 'ENCRYPTION_ERROR',
  PARSING_ERROR = 'PARSING_ERROR',
  CHECKSUM_MISMATCH = 'CHECKSUM_MISMATCH',
  UNSUPPORTED_FORMAT = 'UNSUPPORTED_FORMAT',
  HARDWARE_ERROR = 'HARDWARE_ERROR',
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  UNKNOWN = 'UNKNOWN',
}
